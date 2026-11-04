package com.resonance.player.together

import com.resonance.player.model.*
import com.resonance.player.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.*
import java.net.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

/** One host and one invited friend; only the current track and opted-in next track are sent. */
class JvmListenTogetherService(
    private val cacheDirectory: Path,
    private val openAudio: (Track) -> InputStream,
    private val onGuestStopped: () -> Unit = {},
    private val invitationTtlMs: Long = 10 * 60_000L,
    private val prefetchNetworkAllowed: (suspend () -> Boolean)? = null,
    private val companionStore: TogetherCompanionStore? = null,
    private val companionPort: Int = 45874,
    private val companionAddresses: (() -> List<String>)? = null,
    private val allowTestRendezvousHttp: Boolean = false,
) : ListenTogetherService {
    init { require(invitationTtlMs in 50..86_400_000) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val social = companionStore?.let { TogetherCompanionSession(it, scope, ::preparedPartnerContact) }
    override val companion: StateFlow<TogetherCompanionState> = social?.state ?: MutableStateFlow(TogetherCompanionState())
    override val chatMessageEvents: Flow<Unit> = social?.chatMessageEvents ?: emptyFlow()
    override fun requestPairing(peerId: String) { social?.pair(peerId) }
    override fun respondPairing(accept: Boolean, peerId: String) { social?.respond(accept, peerId) }
    override fun removePartner(peerId: String) { social?.remove(peerId) }
    @Volatile private var localDevice: TogetherTls.Identity? = null
    private data class PartnerOffer(val partner: TogetherPartner, val invitation: String, val expires: Long)
    @Volatile private var pendingPartnerOffer: PartnerOffer? = null
    @Volatile private var trustedPartnerOffer: Pair<String, Long>? = null
    private val partnerInviting = AtomicBoolean(false)
    private val rendezvousWake = Channel<Unit>(Channel.CONFLATED)
    private val partnerHub = companionStore?.let { TogetherPartnerHub(it, scope, { companionAddresses?.invoke() ?: localAddresses() }, ::offerPartner, companionPort) }
    private fun partnerContact(): JSONObject = partnerHub?.contact() ?: JSONObject()
    private suspend fun preparedPartnerContact(): JSONObject { ensurePartnerHub(); return partnerContact() }
    private suspend fun offerPartner(partner: TogetherPartner, invitation: String): Boolean {
        val offered = PartnerOffer(partner, invitation, System.nanoTime() + 120_000_000_000)
        synchronized(lock) {
            if (closed.get() || state.value.status !in setOf(TogetherStatus.Idle, TogetherStatus.Error, TogetherStatus.Listening)) return false
            val current = pendingPartnerOffer
            if (current != null && current.expires > System.nanoTime()) return current.partner.id == partner.id && current.invitation == invitation
            pendingPartnerOffer = offered
        }
        social?.incomingInvitation(partner)
        scope.launch { delay(120_000); synchronized(lock) {
            if (pendingPartnerOffer === offered) { pendingPartnerOffer = null; social?.incomingInvitation(null) }
        } }
        return true
    }
    override fun invitePartner(peerId: String) {
        if (!partnerInviting.compareAndSet(false, true)) return
        scope.launch {
            try {
                val partner = companionStore?.partners()?.firstOrNull { it.id == peerId } ?: error("Unknown partner")
                require(state.value.status in setOf(TogetherStatus.Idle, TogetherStatus.Error, TogetherStatus.Listening))
                partnerHub?.start()
                if (state.value.role != TogetherRole.Host) host(45873)
                check(state.value.role == TogetherRole.Host)
                val addresses = companionAddresses?.invoke() ?: state.value.addresses
                check(addresses.isNotEmpty())
                trustedPartnerOffer = peerId to (System.nanoTime() + 120_000_000_000)
                social?.notice("正在邀请伙伴…")
                var route = partner
                val rendezvousUrl = companionStore?.rendezvousUrl().orEmpty()
                if (rendezvousUrl.isNotEmpty()) {
                    try {
                        val client = TogetherRendezvousClient(rendezvousUrl, companionStore!!.identity(), allowTestRendezvousHttp)
                        client.register(partnerContact(), companionStore.partners())
                        route = client.lookup(partner) ?: partner
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* Cached authenticated direct routes remain usable. */ }
                }
                val offered = partnerHub?.invite(route, invitation(addresses.first(), state.value.port, addresses)) == true
                if (!offered) trustedPartnerOffer = null
                social?.notice(if (offered) "邀请已送达，等朋友确认；不会自动加入" else "未能送达。请双方打开 Resonance 并检查直连网络；也可以发送新的房间链接")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { trustedPartnerOffer = null; social?.notice("暂时无法邀请，请检查房间状态，或发送新的房间链接") }
            finally { partnerInviting.set(false) }
        }
    }
    override fun respondPartnerInvitation(accept: Boolean, peerId: String) {
        val offered = synchronized(lock) { pendingPartnerOffer?.takeIf { it.partner.id == peerId }?.also { pendingPartnerOffer = null } } ?: return
        social?.incomingInvitation(null)
        if (!accept) return
        scope.launch {
            if (offered.expires <= System.nanoTime() || companionStore?.partners()?.none { it.id == offered.partner.id } != false) {
                social?.notice("邀请已过期或伙伴已移除，请让朋友重新邀请"); return@launch
            }
            joinRoom(offered.invitation, offered.partner.id)
        }
    }
    override fun configureRendezvous(url: String) {
        scope.launch {
            try {
                val clean = url.trim()
                val normalized = if (clean.isEmpty()) "" else TogetherRendezvousClient.checkedUrl(clean, allowTestRendezvousHttp).toASCIIString()
                companionStore?.setRendezvousUrl(normalized)
                social?.notice(if (normalized.isEmpty()) "已关闭寻址服务，继续使用设备直连" else "已保存寻址服务；双方需配置同一地址")
                rendezvousWake.trySend(Unit)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { social?.notice("地址未保存，请使用有效的 HTTPS 服务地址") }
        }
    }
    override fun configureRelay(url: String, secret: String) {
        scope.launch {
            try {
                val store = requireNotNull(companionStore)
                val clean = url.trim()
                if (clean.isEmpty()) store.setRelaySettings("", "")
                else {
                    val endpoint = TogetherRelayTransport.endpoint(clean).toASCIIString()
                    val credential = secret.trim()
                    require(endpoint.startsWith("wss://", ignoreCase = true) || (credential.matches(Regex("[A-Za-z0-9_-]{43}")) && java.util.Base64.getUrlDecoder().decode(credential).size == 32))
                    store.setRelaySettings(endpoint, credential)
                }
                social?.notice(if (clean.isEmpty()) "已关闭中继；下次创建房间生效" else "已保存中继；下次创建房间生效")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { social?.notice("中继未保存，请检查 HTTPS 地址和 32 字节访问密钥") }
        }
    }
    private suspend fun rendezvousLoop() {
        var previous = ""
        var client: TogetherRendezvousClient? = null
        while (currentCoroutineContext().isActive) {
            try {
                val url = companionStore?.rendezvousUrl().orEmpty()
                if (url != previous) {
                    runCatching { client?.withdraw() }
                    previous = url
                    client = if (url.isEmpty()) null else TogetherRendezvousClient(url, companionStore!!.identity(), allowTestRendezvousHttp)
                }
                val partners = companionStore?.partners().orEmpty()
                val contact = partnerContact()
                if (client != null && partners.isNotEmpty() && contact.optInt("port") > 0 && contact.getJSONArray("addresses").length() > 0) {
                    client.register(contact, partners)
                    social?.rendezvousStatus("寻址服务已连接；内容是否经中继由房间连接方式决定")
                } else if (client != null && partners.isEmpty()) {
                    client.withdraw()
                    social?.rendezvousStatus("配对伙伴后才登记可达地址")
                } else if (url.isEmpty()) social?.rendezvousStatus("")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { social?.rendezvousStatus("寻址暂不可用，仍可尝试已知地址或发送新链接") }
            withTimeoutOrNull(45_000) { rendezvousWake.receive() }
        }
    }
    private val epoch = AtomicLong()
    private val epochSignal = MutableStateFlow(0L)
    private val closed = AtomicBoolean(false)
    private val lock = Any()
    private val sockets = ConcurrentHashMap.newKeySet<java.net.Socket>()
    private fun retainSocket(socket: java.net.Socket, generation: Long) {
        synchronized(lock) {
            if (generation != epoch.get() || closed.get()) {
                runCatching { socket.close() }
                throw CancellationException("Room already closed")
            }
            sockets.add(socket)
        }
    }
    private fun retireSocket(socket: java.net.Socket) { synchronized(lock) { sockets.remove(socket) }; runCatching { socket.close() } }
    private val _state = MutableStateFlow(TogetherState())
    override val state: StateFlow<TogetherState> = _state.asStateFlow()
    private val _hostQueue = MutableStateFlow<TogetherHostQueue?>(null)
    override val hostQueue: StateFlow<TogetherHostQueue?> = _hostQueue.asStateFlow()
    private val _hostPlayback = MutableStateFlow<PlayerState?>(null)
    override val hostPlayback: StateFlow<PlayerState?> = _hostPlayback.asStateFlow()
    private var awaitingQueueProjection: List<String>? = null
    private val localCommandSequence = AtomicLong(0)
    private val outgoingCommandSequence = AtomicLong(0)
    override fun inspectInvitation(value: String): TogetherInvitationPreview? = runCatching {
        TogetherInvitation.parse(value).let { TogetherInvitationPreview(it.host, it.port, it.candidates,
            it.relayUrl.takeIf(String::isNotEmpty)?.let { url -> TogetherRelayTransport.endpoint(url).host }.orEmpty()) }
    }.getOrNull()
    override fun invitationQr(value: String): TogetherQr? = runCatching {
        val invitation = TogetherInvitation.parse(value).encode()
        val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(invitation,
            com.google.zxing.BarcodeFormat.QR_CODE, 0, 0,
            mapOf(com.google.zxing.EncodeHintType.MARGIN to 4))
        TogetherQr(matrix.width, List(matrix.width * matrix.height) { matrix[it % matrix.width, it / matrix.width] })
    }.getOrNull()
    private val _remote = MutableStateFlow<TogetherPlayback?>(null)
    override val remotePlayback: StateFlow<TogetherPlayback?> = _remote.asStateFlow()
    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    override val remoteLyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()
    @Volatile private var publishedLyrics: Pair<String, Lyrics?> = "" to null
    override fun publishLyrics(trackId: String, lyrics: Lyrics?) {
        val next = trackId to lyrics?.takeIf { it.trackId == trackId }
        if (publishedLyrics != next) { publishedLyrics = next; wake.trySend(Unit) }
    }
    @Volatile private var server: SSLServerSocket? = null
    @Volatile private var peer: SSLSocket? = null
    @Volatile private var activeRelayUrl = ""
    private val relayWaiting = ConcurrentHashMap.newKeySet<java.net.Socket>()
    private val relaySocketRoutes = ConcurrentHashMap<java.net.Socket, Pair<Long, String>>()
    @Volatile private var peerInvitationKey = ""
    @Volatile private var job: Job? = null
    @Volatile private var identity: TogetherTls.Identity? = null
    private data class Published(val player: PlayerState, val at: Long = System.nanoTime())
    @Volatile private var published = Published(PlayerState())
    @Volatile private var nativePublished: Published? = null
    fun nativeTrack(id: String): Track? = sharedTracks.firstOrNull { it.id == id } ?: published.player.currentTrack?.takeIf { it.id == id }
    fun publishNativePlayback(player: PlayerState, roomSessionId: String = state.value.roomSessionId) = synchronized(lock) {
        if (state.value.role != TogetherRole.Host || state.value.roomSessionId != roomSessionId || player.currentTrack?.let { it.isTogetherTrack() && !canShareTrack(it) } == true) return@synchronized
        publish(player, native = true)
        val previous = _hostPlayback.value
        if (previous?.currentTrack?.id != player.currentTrack?.id || previous?.isPlaying != player.isPlaying || previous?.playbackSpeed != player.playbackSpeed)
            _hostPlayback.value = player
    }
    private val ownedFiles = mutableMapOf<Path, Long>()
    // Bytes are reserved before each write so concurrent member uploads share one limit.
    private val ownedFileBytes = mutableMapOf<Path, Long>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val actions = MutableSharedFlow<TogetherRequest>(extraBufferCapacity = 16)
    private data class ControlPacket(val generation: Long, val wire: Wire, val message: JSONObject)
    private val outgoingControls = Channel<ControlPacket>(32)
    override val approvedActions: Flow<TogetherRequest> = actions.asSharedFlow()
    private val queueIds = ConcurrentHashMap<String, String>()
    @Volatile private var sharedTracks: List<Track> = emptyList()
    @Volatile private var approvalRequired = false
    @Volatile private var pendingApproval: CompletableDeferred<Boolean>? = null
    @Volatile private var activeWire: Wire? = null
    @Volatile private var peerEnhanced = false
    @Volatile private var inviteKey = ""
    @Volatile private var inviteExpires = 0L
    @Volatile private var inviteDeadlineNanos = 0L
    @Volatile private var resumeKey = ""
    @Volatile private var resumeUntil = 0L
    @Volatile private var guestResumeKey = ""
    @Volatile private var guestMemberId = ""
    @Volatile private var roomConversationId = ""
    private val hostChatSendLock = Mutex()
    private val hostChatFlushScheduled = AtomicBoolean(false)
    @Volatile private var readinessTracking = false
    @Volatile private var prefetchEnabled = false

    private class ConnectedMember(
        val id: String,
        val name: String,
        val socket: SSLSocket,
        val wire: Wire,
        val enhanced: Boolean,
        val viaRelay: Boolean,
        val groupCapable: Boolean,
        val chatReceipts: Boolean = false,
        val joinedAtNanos: Long = System.nanoTime(),
    ) {
        @Volatile var ready: Boolean = false
        @Volatile var resumeKey: String = ""
        @Volatile var resumeUntil: Long = 0L
        @Volatile var pingNanos: Long = 0L
        val sequenceFloor = java.util.concurrent.atomic.AtomicLong(0L)
        val mediaSendLock = Mutex()
        val pendingMedia = ConcurrentHashMap.newKeySet<String>()
        val sentMedia = ConcurrentHashMap.newKeySet<String>()
        @Volatile var closed = false
        fun close() {
            closed = true
            runCatching { socket.close() }
        }
    }
    private val activeMembers = ConcurrentHashMap<String, ConnectedMember>()
    private val memberResumeTokens = ConcurrentHashMap<String, Pair<String, Long>>()
    private val memberInviteRoutes = ConcurrentHashMap<String, String>()
    private val guestContributedTracks = ConcurrentHashMap<String, Track>()
    private val mediaCatalog = ConcurrentHashMap<Pair<String, String?>, String>()
    private fun mediaKey(track: Track): String = queueIds[track.id]
        ?: mediaCatalog.getOrPut(track.id to track.sourceUri) { UUID.randomUUID().toString() }
    private val coordinator = TogetherGroupCoordinator(capacity = 8)

    private fun canShareTrack(track: Track): Boolean {
        val source = track.sourceUri?.takeIf { it.isNotBlank() } ?: return false
        if (!track.isTogetherTrack()) return true
        val path = runCatching { Path.of(source).toAbsolutePath().normalize() }.getOrNull() ?: return false
        return synchronized(lock) { ownedFiles[path] == epoch.get() }
    }

    private suspend fun broadcast(json: JSONObject, generation: Long) {
        for (member in activeMembers.values) {
            if (generation != epoch.get()) return
            if (json.optString("type") in setOf("groupSnapshot", "memberLeft", "chatBroadcast") && !member.groupCapable) continue
            if (json.optString("type") == "lyrics" && !member.enhanced) continue
            if (!member.closed && activeMembers[member.id] === member) {
                runCatching { member.wire.json(json) }
            }
        }
    }

    private suspend fun broadcastExcept(excludeId: String, json: JSONObject, generation: Long) {
        for (member in activeMembers.values) {
            if (generation != epoch.get()) return
            if (json.optString("type") in setOf("groupSnapshot", "memberLeft", "chatBroadcast") && !member.groupCapable) continue
            if (member.id != excludeId && !member.closed && activeMembers[member.id] === member) {
                runCatching { member.wire.json(json) }
            }
        }
    }

    private fun updateMemberList(generation: Long) {
        val conversation = synchronized(lock) { if (generation == epoch.get()) state.value.roomSessionId else "" }
        if (conversation.isEmpty()) return
        val snap = coordinator.snapshot()
        val localId = identity?.pin.orEmpty()
        val memberList = snap.members.map {
            TogetherMember(it.id, it.name, it.ready, isCoordinator = (it.id == localId))
        }
        val queueList = snap.queue.map { t ->
            val authorName = snap.members.firstOrNull { it.id == t.sourceMemberId }?.name ?: "成员"
            TogetherQueueItem(t.id, t.title, t.artist, t.id == snap.currentTrackId, t.sourceMemberId, authorName,
                sharedTracks.any { queueIds[it.id] == t.id && canShareTrack(it) })
        }
        update(generation) {
            it.copy(
                members = memberList,
                sharedQueue = queueList,
                peerReady = activeMembers.isNotEmpty() && activeMembers.values.all { it.ready },
                status = if (it.pendingJoin) TogetherStatus.AwaitingApproval else if (activeMembers.isEmpty()) TogetherStatus.Listening else TogetherStatus.Connected,
                multiplayer = true,
                equalControls = true,
                collaborative = true,
            )
        }
        social?.updateMembers(memberList, conversation)
        publishHostQueue(generation)
    }

    private fun publishHostQueue(generation: Long) = synchronized(lock) {
        if (generation != epoch.get() || state.value.role != TogetherRole.Host) return@synchronized
        val snapshot = coordinator.snapshot()
        val liveIds = snapshot.queue.mapTo(mutableSetOf()) { it.id }
        sharedTracks = sharedTracks.filter { queueIds[it.id] in liveIds }
        val playable = snapshot.queue.mapNotNull { item -> sharedTracks.firstOrNull { queueIds[it.id] == item.id && canShareTrack(it) } }
        val next = TogetherHostQueue(roomConversationId, snapshot.revision, playable,
            playable.firstOrNull { queueIds[it.id] == snapshot.currentTrackId }?.id,
            playable.associate { it.id to requireNotNull(queueIds[it.id]) })
        if (_hostQueue.value?.tracks != playable) awaitingQueueProjection = playable.map { it.id }
        _hostQueue.value = next
    }

    private suspend fun broadcastGroupSnapshot(generation: Long) {
        if (generation != epoch.get()) return
        val snap = coordinator.snapshot()
        val membersArray = org.json.JSONArray(snap.members.map { m ->
            JSONObject().put("id", m.id).put("name", m.name).put("ready", m.ready).put("isCoordinator", m.id == identity?.pin)
        })
        val queueArray = org.json.JSONArray(snap.queue.map { t ->
            val authorName = snap.members.firstOrNull { it.id == t.sourceMemberId }?.name ?: "成员"
            JSONObject().put("id", t.id).put("title", t.title).put("artist", t.artist)
                .put("durationMs", t.durationMs).put("current", t.id == snap.currentTrackId)
                .put("playable", sharedTracks.any { queueIds[it.id] == t.id && canShareTrack(it) })
                .put("sourceMemberId", t.sourceMemberId).put("sourceMemberName", authorName)
        })
        val json = JSONObject().put("type", "groupSnapshot")
            .put("roomId", roomConversationId)
            .put("revision", snap.revision)
            .put("playing", snap.playing)
            .put("currentTrackId", snap.currentTrackId ?: "")
            .put("members", membersArray)
            .put("queue", queueArray)
        broadcast(json, generation)
        val legacyQueue = JSONObject().put("type", "queue").put("items", org.json.JSONArray(snap.queue.map { t ->
            JSONObject().put("id", t.id).put("title", t.title).put("artist", t.artist).put("current", t.id == snap.currentTrackId)
        }))
        for (member in activeMembers.values) {
            if (generation != epoch.get()) return
            if (!member.groupCapable && member.enhanced && !member.closed && activeMembers[member.id] === member)
                runCatching { member.wire.json(legacyQueue) }
        }
    }

    private fun isMediaCached(trackId: String): Boolean = synchronized(lock) {
        sharedTracks.any { queueIds[it.id] == trackId && canShareTrack(it) }
    }

    private suspend fun attachHostCompanion(generation: Long, member: ConnectedMember) {
        if (member.id.length != 64) return
        val roomId = synchronized(lock) { if (generation == epoch.get() && state.value.role == TogetherRole.Host) roomConversationId else "" }
        if (roomId.isEmpty()) return
        social?.attach(member.id, member.wire, { member.wire.json(it) }, conversationId = roomId,
            name = member.name, members = state.value.members, flushPending = false,
            isCurrent = { generation == epoch.get() && activeMembers[member.id] === member && !member.closed })
    }

    override fun enablePrefetch(enabled: Boolean) { prefetchEnabled = enabled; wake.trySend(Unit) }
    override fun trackPlayerReadiness(enabled: Boolean) { readinessTracking = enabled }
    override fun pauseGuestAudio(reason: String) {
        _state.update { if (it.isGuest) it.copy(localAudioPaused = true, audioPauseReason = reason.take(120)) else it }
        if (state.value.isGuest) runCatching(onGuestStopped)
    }
    override fun resumeGuestAudio() {
        _state.update { if (it.isGuest) it.copy(localAudioPaused = false, audioPauseReason = "") else it }
    }

    override fun requireJoinApproval(required: Boolean) { approvalRequired = required }
    override fun approveJoin(allow: Boolean) { pendingApproval?.complete(allow) }
    override fun renewInvitation() {
        check(state.value.role == TogetherRole.Host)
        pendingApproval?.complete(false)
        val waiting = synchronized(lock) {
            inviteKey = newKey()
            inviteExpires = System.currentTimeMillis() + invitationTtlMs
            inviteDeadlineNanos = System.nanoTime() + invitationTtlMs * 1_000_000
            relayWaiting.toList()
        }
        waiting.forEach { runCatching { it.close() } }
        _state.update { it.copy(invitationExpiresAtMs = inviteExpires) }
    }
    override fun publishQueue(tracks: List<Track>) {
        val generation = epoch.get()
        synchronized(lock) {
            if (generation != epoch.get() || state.value.isGuest) return
            val playable = tracks.filter(::canShareTrack).distinctBy { it.id }
            val current = (nativePublished ?: published).player.currentTrack
            val first = playable.take(50)
            val next = if (current != null && playable.any { it.id == current.id } && first.none { it.id == current.id }) first.take(49) + current else first
            val expected = awaitingQueueProjection
            if (state.value.role == TogetherRole.Host && expected != null) {
                if (next.map { it.id } != expected) return
                awaitingQueueProjection = null
                return
            }
            if (_hostQueue.value?.tracks == next && state.value.role == TogetherRole.Host) return
            val cached = sharedTracks.filter { it.isTogetherTrack() && canShareTrack(it) }
            sharedTracks = (next + cached).distinctBy { it.id }.take(50)
            wake.trySend(Unit)
            if (state.value.role != TogetherRole.Host) return
            val localId = identity?.pin.orEmpty()
            val roomId = roomConversationId
            if (roomId.isEmpty()) return
            val existing = coordinator.snapshot().queue.associateBy { it.id }
            val hostTracks = next.mapNotNull { track ->
                val title = track.title.filterNot { it.code in 0..31 || it.code in 127..159 }.take(128)
                val artist = track.artist.filterNot { it.code in 0..31 || it.code in 127..159 }.take(80)
                val duration = durationTextToSeconds(track.durationText) * 1000L
                if (title.isBlank() || duration <= 0L || duration > 86_400_000L) null else {
                    val id = queueIds.getOrPut(track.id) { UUID.randomUUID().toString() }
                    TogetherGroupCoordinator.Track(id, existing[id]?.sourceMemberId ?: localId, title, artist, duration)
                }
            }
            val readyIds = sharedTracks.mapNotNull { queueIds[it.id] }.toSet()
            val pending = existing.values.filter { it.sourceMemberId != localId && it.id !in readyIds }.mapTo(mutableSetOf()) { it.id }
            val changed = coordinator.syncPlayableQueue(localId, hostTracks, pending)
            publishHostQueue(generation)
            if (changed) scope.launch {
                if (generation == epoch.get()) { updateMemberList(generation); broadcastGroupSnapshot(generation) }
            }
        }
    }
    override fun reportPlaybackReady(mediaId: String) {
        if (state.value.isGuest && state.value.status != TogetherStatus.Reconnecting && peerEnhanced && mediaId == _remote.value?.mediaId) {
            scope.launch { runCatching { activeWire?.json(JSONObject().put("type", "ready").put("id", mediaId)) } }
            _state.update { it.copy(status = TogetherStatus.Connected, peerReady = true, message = "播放器已就绪") }
        }
    }
    override fun reportPlaybackFailure(mediaId: String) {
        if (state.value.isGuest && mediaId == _remote.value?.mediaId) fail(epoch.get(), "歌曲无法在当前设备播放，已停止跟听。请更换音频后重新加入")
    }
    override fun reportHostPlaybackFailure() {
        val generation = epoch.get()
        if (state.value.role == TogetherRole.Host) update(generation) { it.copy(message = "播放器未能完成房间操作，请检查歌曲是否可读取后重试") }
    }
    override fun requestControl(action: TogetherControl, trackId: String) {
        if (state.value.role == TogetherRole.Host) {
            val generation = epoch.get()
            val localId = identity?.pin.orEmpty()
            val roomId = roomConversationId
            if (roomId.isEmpty()) return
            if (action in setOf(TogetherControl.Play, TogetherControl.Pause, TogetherControl.Next, TogetherControl.Previous, TogetherControl.Seek, TogetherControl.Select)) {
                val resolvedId = when (action) {
                    TogetherControl.Select -> hostQueue.value?.tracks?.firstOrNull { queueIds[it.id] == trackId }?.id ?: return
                    TogetherControl.Seek -> trackId.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..1f }?.toString() ?: return
                    else -> ""
                }
                if (actions.subscriptionCount.value == 0 || !actions.tryEmit(TogetherRequest(UUID.randomUUID().toString(), action, resolvedId, localId, "我", null, roomId)))
                    update(epoch.get()) { it.copy(message = "播放器尚未就绪，请稍后重试") }
                return
            }
            if (action == TogetherControl.Remove) applyQueueAction(generation, localId, localCommandSequence.incrementAndGet(), TogetherGroupCoordinator.Action.Remove(trackId))
            return
        }
        if (!state.value.isGuest || !peerEnhanced || state.value.status != TogetherStatus.Connected) return
        if (action == TogetherControl.Select && state.value.sharedQueue.none { it.id == trackId }) return
        if (action in setOf(TogetherControl.Previous, TogetherControl.Seek) && !state.value.equalControls) return
        if (action in setOf(TogetherControl.Append, TogetherControl.Remove, TogetherControl.Move) && !state.value.multiplayer) return
        if (action == TogetherControl.Seek && trackId.toFloatOrNull()?.let { it.isFinite() && it in 0f..1f } != true) return
        val wire = activeWire ?: return
        val packet = ControlPacket(epoch.get(), wire, JSONObject().put("type", if (action in setOf(TogetherControl.Append, TogetherControl.Remove, TogetherControl.Move)) "action" else "request")
            .put("id", UUID.randomUUID().toString())
            .put("action", action.name).put("trackId", trackId)
            .put("seq", outgoingCommandSequence.incrementAndGet()))
        if (!outgoingControls.trySend(packet).isSuccess)
            _state.update { it.copy(message = "操作发送过快，请稍后重试") }
    }

    override fun appendSharedTrack(track: Track) {
        if (state.value.role == TogetherRole.Host) {
            synchronized(lock) {
                val current = hostQueue.value?.tracks ?: return
                if (current.size >= 50 || !canShareTrack(track)) { update(epoch.get()) { it.copy(message = "队列已满或歌曲暂时不可读取") }; return }
                awaitingQueueProjection = null
                publishQueue((current + track).distinctBy { it.id })
            }
        } else if (state.value.role == TogetherRole.Guest && state.value.status == TogetherStatus.Connected && state.value.multiplayer) {
            if (guestContributedTracks.size >= 50 || !canShareTrack(track)) return
            val trackId = UUID.randomUUID().toString()
            guestContributedTracks[trackId] = track
            val wire = activeWire ?: return
            val duration = durationTextToSeconds(track.durationText) * 1000L
            val meta = JSONObject().put("id", trackId).put("title", track.title).put("artist", track.artist).put("durationMs", duration)
            val packet = ControlPacket(epoch.get(), wire, JSONObject().put("type", "action")
                .put("id", UUID.randomUUID().toString()).put("action", "Append").put("trackId", trackId).put("track", meta).put("seq", outgoingCommandSequence.incrementAndGet()))
            if (!outgoingControls.trySend(packet).isSuccess) { guestContributedTracks.remove(trackId); update(packet.generation) { it.copy(message = "操作发送过快，请稍后重试") } }
        }
    }

    override fun removeSharedTrack(trackId: String) {
        requestControl(TogetherControl.Remove, trackId)
    }

    override fun moveSharedTrack(trackId: String, destination: Int) {
        if (state.value.role == TogetherRole.Host) {
            val localId = identity?.pin.orEmpty()
            val generation = epoch.get()
            applyQueueAction(generation, localId, localCommandSequence.incrementAndGet(), TogetherGroupCoordinator.Action.Move(trackId, destination))
        } else if (state.value.role == TogetherRole.Guest && state.value.status == TogetherStatus.Connected && state.value.multiplayer) {
            val wire = activeWire ?: return
            val packet = ControlPacket(epoch.get(), wire, JSONObject().put("type", "action")
                .put("id", UUID.randomUUID().toString()).put("action", "Move").put("trackId", trackId).put("destination", destination).put("seq", outgoingCommandSequence.incrementAndGet()))
            if (!outgoingControls.trySend(packet).isSuccess) update(packet.generation) { it.copy(message = "操作发送过快，请稍后重试") }
        }
    }

    private fun applyQueueAction(generation: Long, memberId: String, sequence: Long, action: TogetherGroupCoordinator.Action): Boolean = synchronized(lock) {
        if (generation != epoch.get() || state.value.role != TogetherRole.Host) return@synchronized false
        val result = coordinator.submitVerified(memberId, sequence, action)
        if (result.outcome != TogetherGroupCoordinator.Outcome.Applied) return@synchronized false
        updateMemberList(generation)
        scope.launch { broadcastGroupSnapshot(generation) }
        wake.trySend(Unit)
        true
    }

    override suspend fun sendChat(text: String, peerId: String): Boolean {
        if (state.value.role == TogetherRole.Host) {
            val roomId = roomConversationId
            val generation = epoch.get()
            val attachedPeer = social?.state?.value?.peerId.orEmpty()
            if (roomId.isEmpty() || activeMembers.isEmpty()) return false
            if (peerId.isNotEmpty() && peerId != roomId && peerId != attachedPeer && !activeMembers.containsKey(peerId)) return false
            val recipients = activeMembers.values.toList()
            if (recipients.any { !it.groupCapable || !it.chatReceipts }) {
                social?.notice("有朋友尚不支持房间消息确认，请更新后重试")
                return false
            }
            val convId = roomId
            val storageId = if (activeMembers.size > 1) roomId else attachedPeer.ifEmpty { roomId }
            val localId = companionStore?.let { TogetherTls.publicKeyId(it.identity().public) } ?: identity?.pin.orEmpty()
            val msg = try {
                withContext(Dispatchers.IO) {
                    companionStore?.enqueue(storageId, text.trim(), senderId = localId, senderName = "我", conversationId = convId,
                        recipients = recipients.map { it.id })
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { social?.notice("消息未能保存，请检查长度、待发数量或存储空间后重试"); return false }
            if (msg == null) return false
            social?.notice("")
            scheduleHostChatFlush(generation, roomId)
            return true
        } else if (state.value.role == TogetherRole.Guest) {
            val localId = guestMemberId.ifEmpty {
                companionStore?.let { TogetherTls.publicKeyId(it.identity().public) } ?: localDevice?.pin.orEmpty()
            }
            return social?.sendText(text, peerId, localId = localId, localName = "我") == true
        } else {
            return social?.sendText(text, peerId) == true
        }
    }

    private fun scheduleHostChatFlush(generation: Long, roomId: String) {
        if (!hostChatFlushScheduled.compareAndSet(false, true)) return
        scope.launch {
            try { flushHostMessages(generation, roomId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* The durable pending set is retried while this room remains open. */ }
            finally { hostChatFlushScheduled.set(false) }
        }
    }

    /** Preserve this sender's sequence order on every member connection. */
    private suspend fun flushHostMessages(generation: Long, roomId: String) = hostChatSendLock.withLock {
        if (generation != epoch.get() || roomConversationId != roomId || state.value.role != TogetherRole.Host) return@withLock
        val storageId = if (activeMembers.size > 1) roomId else activeMembers.keys.firstOrNull().orEmpty().ifEmpty { roomId }
        val messages = (companionStore?.messages(storageId).orEmpty() + (if (storageId != roomId) companionStore?.messages(roomId).orEmpty() else emptyList()))
            .filter { it.pendingRecipients.isNotEmpty() }
            .sortedWith(compareBy<TogetherChatMessage> { it.senderId }.thenBy { it.sequence })
        val failed = mutableSetOf<String>()
        for (message in messages) {
            for (id in message.pendingRecipients) {
                if (generation != epoch.get() || roomConversationId != roomId) return@withLock
                val recipient = activeMembers[id]?.takeIf { !it.closed && it.chatReceipts && id !in failed } ?: continue
                try {
                    recipient.wire.json(JSONObject().put("type", "chatBroadcast").put("receiptVersion", 1)
                        .put("senderId", message.senderId).put("senderName", if (message.outgoing) "房间发起者" else message.senderName)
                        .put("sequence", message.sequence).put("text", message.text)
                        .put("conversationId", roomId).put("time", message.timeMs))
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { failed.add(id) }
            }
        }
    }

    private suspend fun reportGroupDelivery(generation: Long, roomId: String, message: TogetherChatMessage) {
        if (message.outgoing || generation != epoch.get() || roomConversationId != roomId) return
        val author = activeMembers[message.senderId]?.takeIf { !it.closed && it.chatReceipts } ?: return
        author.wire.json(JSONObject().put("type", "chatAccepted").put("conversationId", roomId)
            .put("senderId", message.senderId).put("sequence", message.sequence)
            .put("pendingRecipients", org.json.JSONArray(message.pendingRecipients)).put("recipientCount", message.recipientCount + 1))
    }

    override fun clearChat(peerId: String) {
        val roomId = roomConversationId
        if (roomId.isNotEmpty() && (peerId == roomId || peerId == social?.state?.value?.peerId || peerId.isEmpty())) {
            social?.clear(roomId)
        }
        social?.clear(peerId)
    }
    override fun markChatRead(conversationId: String) { social?.markRead(conversationId) }
    override fun resolveRequest(requestId: String, allow: Boolean) {
        val request = state.value.requests.firstOrNull { it.id == requestId } ?: return
        _state.update { it.copy(requests = it.requests.filterNot { value -> value.id == requestId }) }
        val roomId = roomConversationId
        if (allow && roomId.isNotEmpty()) actions.tryEmit(request.copy(roomSessionId = roomId))
        val wire = activeWire ?: return
        scope.launch { runCatching { wire.json(JSONObject().put("type", "resolved").put("id", requestId).put("allowed", allow)) } }
    }

    override fun publishPlayback(player: PlayerState) { publish(player, native = false) }
    private fun publish(player: PlayerState, native: Boolean) {
        val old = if (native) nativePublished ?: published else published
        val now = Published(player)
        if (native) nativePublished = now else published = now
        val elapsed = if (old.player.isPlaying) (now.at - old.at) / 1_000_000.0 * old.player.playbackSpeed else 0.0
        val duration = player.currentTrack?.let { durationTextToSeconds(it.durationText) * 1_000L } ?: 0
        val before = old.player.positionMs?.toDouble() ?: (old.player.progress * duration).toDouble()
        val after = player.positionMs?.toDouble() ?: (player.progress * duration).toDouble()
        if (old.player.currentTrack?.id != player.currentTrack?.id || old.player.isPlaying != player.isPlaying ||
            old.player.playbackSpeed != player.playbackSpeed || kotlin.math.abs(after - before - elapsed) > 200) wake.trySend(Unit)
    }

    private suspend fun ensurePartnerHub() {
        try { partnerHub?.start() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { social?.notice("伙伴邀请监听未能开启，仍可使用房间链接") }
    }
    internal val companionStartup = scope.async {
        if (companionStore?.partners()?.isNotEmpty() == true) ensurePartnerHub()
    }
    internal val listeningPartnerPort: Int get() = partnerHub?.listeningPort ?: 0
    init {
        scope.launch { companionStartup.await(); if (companionStore != null) rendezvousLoop() }
        scope.launch {
            for (packet in outgoingControls) {
                if (packet.generation == epoch.get() && activeWire === packet.wire) {
                    try { packet.wire.json(packet.message) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { update(packet.generation) { it.copy(message = "操作暂未送达，请稍后重试") } }
                }
            }
        }
    }

    override suspend fun host(port: Int) = withContext(Dispatchers.IO) {
        check(!closed.get())
        require(port in 0..65535) { "端口需在 1–65535 之间" }
        leave()
        val generation = epoch.get()
        try {
            val relay = companionStore?.relaySettings() ?: ("" to "")
            if (relay.first.isNotEmpty()) {
                TogetherRelayTransport.endpoint(relay.first)
                require(relay.first.startsWith("wss://", ignoreCase = true) || relay.second.matches(Regex("[A-Za-z0-9_-]{43}")))
            }
            val keys = TogetherTls.identity(companionStore?.identity())
            localDevice = keys.takeIf { companionStore != null }
            val listener = (keys.context.serverSocketFactory.createServerSocket() as SSLServerSocket).apply {
                reuseAddress = true
                TogetherTls.configure(this)
                wantClientAuth = companionStore != null
                val address = if (relay.first.isNotEmpty()) InetSocketAddress("127.0.0.1", port) else InetSocketAddress(port)
                try { bind(address, 8) } catch (error: Exception) { close(); throw error }
            }
            synchronized(lock) {
                if (generation != epoch.get() || closed.get()) { listener.close(); return@withContext }
                identity = keys
                inviteKey = keys.key
                inviteExpires = System.currentTimeMillis() + invitationTtlMs
                inviteDeadlineNanos = System.nanoTime() + invitationTtlMs * 1_000_000
                activeRelayUrl = relay.first
                roomConversationId = UUID.randomUUID().toString().replace("-", "")
                server = listener
            }
            val localId = keys.pin
            coordinator.admitVerified(localId, "我")
            coordinator.setMemberReady(localId, true)
            update(generation) { TogetherState(TogetherRole.Host, TogetherStatus.Listening,
                addresses = if (relay.first.isNotEmpty()) listOf("127.0.0.1") else localAddresses(), port = listener.localPort, message = "等待朋友通过邀请加入", verificationCode = code(keys.pin), invitationExpiresAtMs = inviteExpires,
                multiplayer = true, coordinatorId = localId, equalControls = true, collaborative = true,
                members = listOf(TogetherMember(localId, "我", ready = true, isCoordinator = true)), roomSessionId = roomConversationId) }
            publishQueue(sharedTracks)
            val handshakes = Semaphore(8)
            job = scope.launch {
                val ticker = launch {
                    var sourceKey: Pair<String, String?>? = null
                    var mediaId = ""
                    var sequence = 0L
                    var sentLyrics = ""
                    var chatRetryAt = 0L
                    try {
                        while (isActive && generation == epoch.get()) {
                            val now = System.nanoTime()
                            if (now >= chatRetryAt) {
                                chatRetryAt = now + 5_000_000_000L
                                scheduleHostChatFlush(generation, state.value.roomSessionId)
                            }
                            val snapshot = nativePublished ?: published
                            val track = snapshot.player.currentTrack?.takeIf(::canShareTrack)
                            val currentQueueId = track?.let { queueIds[it.id] }
                            val changed = synchronized(lock) {
                                generation == epoch.get() && coordinator.reflectPlayback(currentQueueId, snapshot.player.isPlaying, snapshot.player.progress)
                            }
                            if (changed) {
                                updateMemberList(generation)
                                broadcastGroupSnapshot(generation)
                            }
                            val nextKey = track?.let { it.id to it.sourceUri }
                            if (nextKey != sourceKey) {
                                sourceKey = nextKey
                                mediaId = if (track == null) "" else mediaKey(track)
                                update(generation) { it.copy(peerReady = false) }
                                synchronized(lock) {
                                    if (generation == epoch.get()) activeMembers.values.forEach {
                                        it.ready = false; coordinator.setMemberReady(it.id, false)
                                    }
                                }
                                updateMemberList(generation)
                                broadcastGroupSnapshot(generation)
                                if (track != null) {
                                    val transferId = mediaId
                                    for (member in activeMembers.values) {
                                        if (!member.closed && transferId !in member.sentMedia) {
                                            launch { sendMediaToMember(member, track, transferId, false) }
                                        }
                                    }
                                }
                            }
                            val nextTrack = track?.let { current ->
                                val index = sharedTracks.indexOfFirst { it.id == current.id }
                                if (index >= 0 && index + 1 < sharedTracks.size) sharedTracks[index + 1].takeIf(::canShareTrack) else null
                            }
                            if (prefetchEnabled && nextTrack != null && prefetchNetworkAllowed?.invoke() != false) {
                                val nextId = mediaKey(nextTrack)
                                for (member in activeMembers.values) {
                                    if (!member.closed && nextId !in member.sentMedia && member.enhanced) {
                                        launch { sendMediaToMember(member, nextTrack, nextId, true) }
                                    }
                                }
                            }
                            val elapsed = if (snapshot.player.isPlaying) (System.nanoTime() - snapshot.at) / 1_000_000.0 * snapshot.player.playbackSpeed else 0.0
                            val duration = track?.let { durationTextToSeconds(it.durationText).toLong() * 1000 } ?: 0
                            val position = snapshot.player.positionMs?.toDouble() ?: (snapshot.player.progress * duration).toDouble()
                            val statePacket = JSONObject().put("type", "state").put("id", mediaId).put("playing", snapshot.player.isPlaying)
                                .put("positionMs", (position + elapsed).coerceIn(0.0, duration.toDouble()).toLong())
                                .put("durationMs", duration).put("speed", snapshot.player.playbackSpeed.toDouble()).put("seq", ++sequence)
                                .put("sending", false)
                            broadcast(statePacket, generation)

                            val context = publishedLyrics.takeIf { it.first == track?.id }?.second
                            val payloadContext = lyricsMessage(mediaId, context).toString()
                            if (payloadContext != sentLyrics) {
                                broadcast(JSONObject(payloadContext), generation)
                                sentLyrics = payloadContext
                            }
                            withTimeoutOrNull(500) { wake.receive() }
                        }
                    } catch (_: Exception) {}
                }
                fun acceptPeer(socket: SSLSocket, viaRelay: Boolean) {
                    if (!handshakes.tryAcquire()) { retireSocket(socket); return }
                    try { retainSocket(socket, generation) } catch (_: CancellationException) { handshakes.release(); return }
                    val started = AtomicBoolean(false)
                    launch {
                        started.set(true)
                        var authenticated = false
                        var established = false
                        var admittedId = ""
                        var admittedMember: ConnectedMember? = null
                        try {
                            if (!viaRelay) { TogetherTls.configure(socket); socket.startHandshake() }
                            val wire = Wire(socket)
                            val auth = wire.readJson()
                            val presented = auth.optString("key")
                            val enhanced = auth.optBoolean("enhanced", false)
                            val groupCapable = enhanced && auth.optInt("multiplayer", 0) == 1
                            require(!groupCapable || auth.optInt("groupVersion", 0) == 2) { "房间协议需要更新" }
                            val socialId = if (auth.optInt("companion") == 1 && social != null) TogetherTls.peerId(socket) else ""
                            val resumeToken = auth.optString("resume")
                            val resumedMemberEntry = if (enhanced && resumeToken.length == 43) {
                                memberResumeTokens.entries.firstOrNull { (_, v) ->
                                    secureEquals(resumeToken, v.first) && System.nanoTime() < v.second
                                }
                            } else null
                            val resuming = resumedMemberEntry != null &&
                                memberInviteRoutes[resumedMemberEntry.key]?.let { route -> secureEquals(presented, route) } == true
                            val memberId = when {
                                resumedMemberEntry != null -> resumedMemberEntry.key
                                socialId.isNotEmpty() -> socialId
                                else -> UUID.randomUUID().toString().replace("-", "")
                            }
                            val memberName = auth.optString("name", "朋友 ${memberId.take(4)}")
                            require(auth.optString("type") == "hello" && auth.optInt("version") == 1 && presented.length == 43 &&
                                (resuming || System.nanoTime() < inviteDeadlineNanos && secureEquals(presented, inviteKey)))
                            require(generation == epoch.get() && activeMembers.size < 8)
                            authenticated = true
                            val trusted = trustedPartnerOffer?.let { it.first == socialId && it.second > System.nanoTime() &&
                                companionStore?.partners()?.any { partner -> partner.id == socialId } == true } == true
                            if (trusted) trustedPartnerOffer = null
                            if (approvalRequired && !resuming && !trusted) {
                                val decision = CompletableDeferred<Boolean>()
                                synchronized(lock) {
                                    require(generation == epoch.get() && pendingApproval == null)
                                    pendingApproval = decision
                                }
                                update(generation) { it.copy(status = TogetherStatus.AwaitingApproval, pendingJoin = true,
                                    pendingJoinMemberId = memberId, pendingJoinMemberName = memberName, message = "${memberName} 请求加入，请确认") }
                                try {
                                    if (enhanced) wire.json(JSONObject().put("type", "approval"))
                                    require(withTimeout(if (enhanced) 60_000L else 10_000L) { decision.await() })
                                }
                                finally {
                                    synchronized(lock) {
                                        if (pendingApproval === decision) {
                                            pendingApproval = null
                                            update(generation) { it.copy(pendingJoin = false, pendingJoinMemberId = "", pendingJoinMemberName = "") }
                                        }
                                    }
                                }
                            }
                            val nextResumeKey = newKey()
                            val member = ConnectedMember(memberId, memberName, socket, wire, enhanced, viaRelay, groupCapable,
                                chatReceipts = groupCapable && auth.optInt("companion", 0) == 1 && auth.optInt("chatReceipts", 0) == 1)
                            member.resumeKey = nextResumeKey
                            member.resumeUntil = Long.MAX_VALUE
                            synchronized(lock) {
                                require(generation == epoch.get() && !closed.get() && !activeMembers.containsKey(memberId) && activeMembers.size < 7)
                                require(if (groupCapable) activeMembers.values.none { !it.groupCapable } else activeMembers.isEmpty())
                                require(resuming || System.nanoTime() < inviteDeadlineNanos && secureEquals(presented, inviteKey))
                                require(coordinator.admitVerified(memberId, memberName))
                                admittedId = memberId
                                admittedMember = member
                                memberResumeTokens[memberId] = nextResumeKey to Long.MAX_VALUE
                                memberInviteRoutes[memberId] = presented
                                activeMembers[memberId] = member
                                if (peer == null) {
                                    peer = socket
                                    activeWire = wire
                                    peerEnhanced = enhanced
                                }
                            }
                            wire.json(JSONObject().put("type", "welcome").put("version", 1).put("enhanced", enhanced)
                                .put("companion", if (socialId.isNotEmpty()) 1 else 0)
                                .put("equalControls", enhanced)
                                .also { if (groupCapable) it.put("multiplayer", 1).put("groupVersion", 2).put("roomId", roomConversationId).put("memberId", memberId).put("chatReceipts", if (social != null) 1 else 0) }
                                .also { if (enhanced) it.put("resume", nextResumeKey) })
                            established = true
                            updateMemberList(generation)
                            update(generation) { it.copy(status = TogetherStatus.Connected, peer = if (viaRelay) "加密中继" else socket.inetAddress.hostAddress.orEmpty(),
                                tlsVersion = socket.session.protocol, pendingJoin = false, peerReady = false, collaborative = enhanced,
                                equalControls = enhanced, viaRelay = viaRelay, multiplayer = true,
                                message = "${memberName} 已加入房间") }
                            if (socialId.isNotEmpty()) {
                                attachHostCompanion(generation, member)
                            }
                            broadcastGroupSnapshot(generation)
                            wake.trySend(Unit)
                            scheduleHostChatFlush(generation, state.value.roomSessionId)
                            try {
                                handleMemberWire(generation, member, enhanced)
                            } finally {
                                social?.detach(wire)
                            }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { /* Untrusted handshake errors must not echo capability URLs or peer data. */ }
                        finally {
                            retireSocket(socket)
                            handshakes.release()
                            val removedMember = synchronized(lock) {
                                val removed = generation == epoch.get() && admittedId.isNotEmpty() &&
                                    admittedMember?.let { activeMembers.remove(admittedId, it) } == true
                                if (removed) {
                                    coordinator.leaveVerified(admittedId)
                                    memberResumeTokens[admittedId]?.let {
                                        if (established) memberResumeTokens[admittedId] = it.first to (System.nanoTime() + 45_000_000_000L)
                                        else { memberResumeTokens.remove(admittedId); memberInviteRoutes.remove(admittedId) }
                                    }
                                    coordinator.removeMemberTracks(admittedId) { track -> !isMediaCached(track.id) }
                                }
                                removed
                            }
                            if (removedMember) {
                                updateMemberList(generation)
                                val replacement = activeMembers.values.firstOrNull { it.id.length == 64 && !it.closed }
                                if (replacement != null && social?.state?.value?.connected != true) attachHostCompanion(generation, replacement)
                                broadcast(JSONObject().put("type", "memberLeft").put("memberId", admittedId), generation)
                                broadcastGroupSnapshot(generation)
                            }
                            synchronized(lock) {
                                if (generation == epoch.get()) {
                                    if (peer === socket) {
                                        val nextActive = activeMembers.values.firstOrNull()
                                        peer = nextActive?.socket
                                        activeWire = nextActive?.wire
                                    }
                                    if (activeMembers.isEmpty()) {
                                        update(generation) { it.copy(status = TogetherStatus.Listening, peer = "", tlsVersion = "", pendingJoin = false,
                                            requests = emptyList(), peerReady = false, viaRelay = false, message = "连接已断开，短暂断网时朋友可自动重连") }
                                    }
                                }
                            }
                        }
                    }.invokeOnCompletion { if (!started.get()) { retireSocket(socket); handshakes.release() } }
                }
                fun availableRelayRoutes(): Set<String> = synchronized(lock) {
                    if (generation != epoch.get() || closed.get()) return@synchronized emptySet()
                    val now = System.nanoTime()
                    memberResumeTokens.entries.toList().forEach { entry ->
                        if (entry.value.second <= now && !activeMembers.containsKey(entry.key)) {
                            if (memberResumeTokens.remove(entry.key, entry.value)) memberInviteRoutes.remove(entry.key)
                        }
                    }
                    val routes = memberInviteRoutes.entries.mapNotNull { entry ->
                        if (memberResumeTokens[entry.key]?.second?.let { it > now } == true) entry.value else null
                    }.toMutableSet()
                    if (inviteKey.isNotEmpty() && now < inviteDeadlineNanos) routes.add(inviteKey)
                    routes
                }
                suspend fun waitForRelay(key: String) {
                    var failures = 0
                    while (isActive && generation == epoch.get()) {
                        if (key !in availableRelayRoutes()) break
                        if (activeMembers.size >= 7) { delay(1000); continue }
                        var tunnel: java.net.Socket? = null
                        try {
                            val outer = TogetherRelayTransport.connect(relay.first, key, host = true, token = relay.second) { opened ->
                                tunnel = opened
                                retainSocket(opened, generation)
                                synchronized(lock) {
                                    if (key !in availableRelayRoutes()) throw IOException("Invitation changed")
                                    relayWaiting.add(opened)
                                    relaySocketRoutes[opened] = generation to key
                                }
                            }
                            relayWaiting.remove(outer)
                            relaySocketRoutes.remove(outer)
                            val inner = TogetherRelayTransport.roomSocket(outer, keys, null, companionStore != null)
                            synchronized(lock) {
                                if (generation != epoch.get() || closed.get()) { inner.close(); throw CancellationException("Room closed") }
                                sockets.remove(outer); sockets.add(inner)
                            }
                            tunnel = null
                            failures = 0
                            launch { acceptPeer(inner, true) }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: TogetherRelayTransport.WaitingExpired) { failures = 0 }
                        catch (_: Exception) {
                            failures++
                            if (generation == epoch.get() && activeMembers.isEmpty()) social?.notice(
                                if (failures >= 5) "服务器连接失败，自动重试已停止。请稍后刷新邀请重试。" else "服务器暂不可用，正在有限重试")
                        } finally {
                            tunnel?.let { pending ->
                                relayWaiting.remove(pending)
                                relaySocketRoutes.remove(pending)
                                retireSocket(pending)
                            }
                        }
                        if (failures >= 5) break
                        delay((2_000L shl failures.coerceAtMost(4)).coerceAtMost(30_000L))
                    }
                }
                val relayController = if (relay.first.isNotEmpty()) launch {
                    val routes = mutableMapOf<String, Job>()
                    try {
                        while (isActive && generation == epoch.get()) {
                            val wanted = availableRelayRoutes()
                            routes.keys.filter { it !in wanted }.forEach { expired ->
                                routes.remove(expired)?.cancel()
                                relaySocketRoutes.entries.filter { it.value.first == generation && it.value.second == expired }
                                    .forEach { runCatching { it.key.close() } }
                            }
                            wanted.forEach { key ->
                                if (key !in routes) routes[key] = launch { waitForRelay(key) }
                            }
                            delay(1_000)
                        }
                    } finally {
                        routes.values.forEach { it.cancel() }
                        relaySocketRoutes.entries.filter { it.value.first == generation }.forEach { runCatching { it.key.close() } }
                    }
                } else null
                try {
                    while (isActive && generation == epoch.get()) {
                        val socket = try { listener.accept() as SSLSocket } catch (_: IOException) { break }
                        acceptPeer(socket, false)
                    }
                } finally { relayController?.cancel() }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { fail(generation, "房间未能创建，请检查端口是否被占用或被系统阻止") }
    }

    override fun invitation(address: String, port: Int): String = invitation(address, port, emptyList())

    override fun invitation(address: String, port: Int, candidates: List<String>): String {
        val keys = identity ?: error("请先创建房间")
        check(state.value.role == TogetherRole.Host)
        check(System.nanoTime() < inviteDeadlineNanos) { "邀请已过期，请刷新邀请" }
        val effectiveHost = if (activeRelayUrl.isNotEmpty()) "127.0.0.1" else address.trim().ifBlank { state.value.addresses.firstOrNull().orEmpty() }
        val bounded = (if (activeRelayUrl.isNotEmpty()) emptyList() else candidates).filter { it.removeSurrounding("[", "]") != effectiveHost.removeSurrounding("[", "]") }
            .distinct().take(TogetherInvitation.MAX_HOSTS - 1)
        return TogetherInvitation(effectiveHost, port, keys.pin, inviteKey, bounded, activeRelayUrl).encode()
    }

    override suspend fun join(invitation: String) = joinRoom(invitation, "")
    private suspend fun joinRoom(invitation: String, expectedPartner: String) = withContext(Dispatchers.IO) {
        check(!closed.get())
        leave()
        val generation = epoch.get()
        update(generation) { TogetherState(TogetherRole.Guest, TogetherStatus.Connecting, message = "正在验证邀请并建立加密连接") }
        try {
            val invite = TogetherInvitation.parse(invitation)
            localDevice = companionStore?.let { TogetherTls.identity(it.identity()) }
            val first = connectGuest(generation, invite, false, expectedPartner)
            job = scope.launch {
                var connection = first
                var attempt = 0
                var connectedAt = System.nanoTime()
                while (isActive && generation == epoch.get()) {
                    val retired = connection
                    try { guestPeer(generation, connection.second); break }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) {
                        // Count consecutive unstable attempts, not every interruption over a long session.
                        if (System.nanoTime() - connectedAt >= 10_000_000_000L) attempt = 0
                        if (error !is IOException || error is javax.net.ssl.SSLException || guestResumeKey.isBlank() || attempt >= 3) {
                            fail(generation, "连接已断开或音频校验失败，已停止跟听"); break
                        }
                        runCatching(onGuestStopped)
                        var restored = false
                        while (!restored && attempt < 3 && generation == epoch.get()) {
                            attempt++
                            update(generation) { it.copy(status = TogetherStatus.Reconnecting, reconnectAttempt = attempt,
                                peerReady = false, message = "连接中断，正在重连（$attempt/3）") }
                            delay(1_000L shl (attempt - 1))
                            try {
                                connection = connectGuest(generation, invite, true, expectedPartner)
                                connectedAt = System.nanoTime()
                                restored = true
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (reconnectError: Exception) {
                                if (reconnectError is javax.net.ssl.SSLException || reconnectError !is IOException) break
                            }
                        }
                        if (!restored) { fail(generation, "未能恢复连接，请重新向朋友索取邀请"); break }
                    } finally { social?.detach(retired.second); retireSocket(retired.first) }
                }
            }
        } catch (cancelled: CancellationException) {
            if (generation == epoch.get()) leave()
            throw cancelled
        }
        catch (e: Exception) {
            val friendlyMsg = when {
                e is RelayUnavailableException ->
                    "未能连接一起听服务器，请稍后重试，或请房间发起者检查服务器设置并刷新邀请。"
                e is java.net.SocketTimeoutException ->
                    "未能连接到房主（连接超时）：双方可能不在同一网络。若跨网异地听歌，请确认双方均开启 IPv6；若在同处，建议一人开启手机热点直连。"
                e is java.net.ConnectException ->
                    "未能连接到房主（连接被拒绝）：请确认房主已创建房间且未关闭，或房主电脑防火墙是否放行了端口。"
                e is java.net.NoRouteToHostException ->
                    "未能连接到房主（网络路径不可达）：建议切换至同一 Wi-Fi 或开启手机热点直连。"
                e is javax.net.ssl.SSLException ->
                    "加入失败（房间凭据不匹配）：邀请可能已失效或被房主刷新，请重新索取最新邀请。"
                e.message?.contains("approval", ignoreCase = true) == true || e.message?.contains("未获房主同意") == true ->
                    "房主未同意加入本次一起听。"
                else ->
                    "未能加入：邀请可能过期、未获房主同意或网络无法直达。可重试或更换邀请"
            }
            fail(generation, friendlyMsg)
        }
    }

    private suspend fun connectGuest(generation: Long, invite: TogetherInvitation, resume: Boolean, expectedPartner: String = ""): Pair<SSLSocket, Wire> {
        // Server invitations never dial peer addresses or fall back to a peer after server failure.
        // Invitations without a server remain readable for older clients and explicit local use.
        if (invite.relayUrl.isEmpty()) {
            val hosts = invite.allHosts
            if (hosts.size <= 1) {
                val targetHost = hosts.firstOrNull() ?: invite.host
                return connectSingleGuest(generation, targetHost, invite, resume, expectedPartner)
            }
            return connectRacedGuest(generation, hosts, invite, resume, expectedPartner)
        }
        update(generation) { it.copy(message = "正在连接一起听服务器") }
        var failure: IOException? = null
        repeat(4) { attempt ->
            try { return connectRelayGuest(generation, invite, resume, expectedPartner) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: javax.net.ssl.SSLException) { throw error }
            catch (error: IOException) { failure = error; if (attempt < 3) delay(500L shl attempt) }
        }
        throw RelayUnavailableException(failure)
    }

    private class RelayUnavailableException(cause: IOException?) : IOException("Relay unavailable", cause)

    private suspend fun connectRelayGuest(generation: Long, invite: TogetherInvitation, resume: Boolean, expectedPartner: String): Pair<SSLSocket, Wire> {
        var tunnel: java.net.Socket? = null
        try {
            val outer = TogetherRelayTransport.connect(invite.relayUrl, invite.key, host = false) { opened ->
                tunnel = opened; retainSocket(opened, generation)
            }
            val inner = TogetherRelayTransport.roomSocket(outer, localDevice, invite.pin, false)
            synchronized(lock) {
                if (generation != epoch.get() || closed.get()) { inner.close(); throw CancellationException("Room closed") }
                sockets.remove(outer); sockets.add(inner); peer = inner
            }
            tunnel = null
            return completeGuestHandshake(generation, inner, "加密中继", invite, resume, expectedPartner).also {
                update(generation) { state -> state.copy(viaRelay = true, message = "已通过加密中继连接，正在准备歌曲") }
            }
        } finally { tunnel?.let(::retireSocket) }
    }

    private suspend fun connectSingleGuest(generation: Long, targetHost: String, invite: TogetherInvitation, resume: Boolean, expectedPartner: String): Pair<SSLSocket, Wire> {
        val socket = TogetherTls.client(invite.pin, localDevice).socketFactory.createSocket() as SSLSocket
        try {
            retainSocket(socket, generation)
            TogetherTls.configure(socket)
            synchronized(lock) { check(generation == epoch.get() && !closed.get()); peer = socket }
            val timeout = if (resume) 2_500 else 8_000
            socket.connect(InetSocketAddress(targetHost, invite.port), timeout)
            socket.startHandshake()
            return completeGuestHandshake(generation, socket, targetHost, invite, resume, expectedPartner)
        } catch (error: Exception) {
            retireSocket(socket)
            throw error
        }
    }

    private suspend fun connectRacedGuest(
        generation: Long,
        hosts: List<String>,
        invite: TogetherInvitation,
        resume: Boolean,
        expectedPartner: String
    ): Pair<SSLSocket, Wire> = coroutineScope {
        val resultChannel = Channel<Result<Pair<SSLSocket, String>>>(Channel.BUFFERED)
        val activeSockets = java.util.concurrent.ConcurrentHashMap.newKeySet<java.net.Socket>()
        val winnerChosen = AtomicBoolean(false)
        val errors = java.util.concurrent.CopyOnWriteArrayList<Throwable>()
        val cancellation = launch {
            epochSignal.first { it != generation }
            resultChannel.close(CancellationException("Room already closed"))
        }

        val jobs = hosts.mapIndexed { index, host ->
            launch(Dispatchers.IO) {
                if (index > 0) delay(index * 150L)
                if (winnerChosen.get() || generation != epoch.get() || closed.get()) return@launch

                var s: SSLSocket? = null
                try {
                    val socket = TogetherTls.client(invite.pin, localDevice).socketFactory.createSocket() as SSLSocket
                    s = socket
                    activeSockets.add(socket)
                    retainSocket(socket, generation)
                    TogetherTls.configure(socket)
                    val timeout = if (resume) 2_500 else 6_000
                    socket.connect(InetSocketAddress(host, invite.port), timeout)
                    socket.startHandshake()

                    if (winnerChosen.compareAndSet(false, true)) {
                        resultChannel.trySend(Result.success(socket to host))
                    } else {
                        activeSockets.remove(socket)
                        retireSocket(socket)
                    }
                } catch (t: Throwable) {
                    if (s != null) {
                        activeSockets.remove(s)
                        retireSocket(s)
                    }
                    errors.add(t)
                    if (errors.size >= hosts.size && !winnerChosen.get()) {
                        // A failed certificate check must not be hidden by another candidate's
                        // ordinary connection failure and trigger an automatic relay fallback.
                        val representativeError = errors.firstOrNull { it is javax.net.ssl.SSLException }
                            ?: errors.firstOrNull { it !is SocketTimeoutException } ?: errors.first()
                        resultChannel.trySend(Result.failure(representativeError))
                    }
                }
            }
        }

        try {
            val outcome = resultChannel.receive()
            jobs.forEach { it.cancel() }
            val (socket, winningHost) = outcome.getOrThrow()
            activeSockets.forEach { if (it != socket) retireSocket(it) }

            synchronized(lock) {
                check(generation == epoch.get() && !closed.get())
                peer = socket
            }
            return@coroutineScope completeGuestHandshake(generation, socket, winningHost, invite, resume, expectedPartner)
        } catch (e: Throwable) {
            jobs.forEach { it.cancel() }
            activeSockets.forEach(::retireSocket)
            throw e
        } finally { cancellation.cancel() }
    }

    private suspend fun completeGuestHandshake(
        generation: Long,
        socket: SSLSocket,
        targetHost: String,
        invite: TogetherInvitation,
        resume: Boolean,
        expectedPartner: String
    ): Pair<SSLSocket, Wire> {
        try {
            require(expectedPartner.isEmpty() || TogetherTls.peerId(socket) == expectedPartner) { "Partner device changed" }
            val wire = Wire(socket)
            wire.json(JSONObject().put("type", "hello").put("version", 1).put("key", invite.key).put("enhanced", true)
                .put("companion", if (social != null) 1 else 0)
                .put("multiplayer", 1)
                .put("chatReceipts", if (social != null) 1 else 0)
                .put("groupVersion", 2)
                .put("readiness", readinessTracking).also { if (resume) it.put("resume", guestResumeKey) })
            var welcome = wire.readJson()
            if (welcome.optString("type") == "approval") {
                update(generation) { it.copy(status = TogetherStatus.AwaitingApproval, message = "等待房主确认加入，可随时取消") }
                socket.soTimeout = 65_000
                welcome = wire.readJson()
                socket.soTimeout = 15_000
            }
            require(welcome.optString("type") == "welcome" && welcome.optInt("version") == 1)
            val negotiated = welcome.optBoolean("enhanced", false)
            val nextResumeKey = welcome.optString("resume").also { require(it.isEmpty() || it.matches(KEY)) }
            synchronized(lock) {
                check(generation == epoch.get() && peer === socket && !closed.get())
                peerEnhanced = negotiated
                guestResumeKey = nextResumeKey
                activeWire = wire
            }
            val isMulti = welcome.optInt("multiplayer") == 1
            require(!isMulti || welcome.optInt("groupVersion", 0) == 2) { "房间协议需要更新" }
            update(generation) { it.copy(status = TogetherStatus.Connected, peer = targetHost, port = invite.port,
                tlsVersion = socket.session.protocol, reconnectAttempt = 0, peerReady = false, collaborative = negotiated,
                equalControls = negotiated && welcome.optBoolean("equalControls", false), viaRelay = false,
                multiplayer = isMulti, coordinatorId = if (isMulti) invite.pin else "",
                message = "加密连接已建立，正在准备歌曲", verificationCode = code(invite.pin)) }
            val convId = welcome.optString("roomId").ifEmpty { invite.pin }
            guestMemberId = welcome.optString("memberId")
            require(if (isMulti) convId.matches(Regex("[a-f0-9]{32}")) else convId == invite.pin)
            update(generation) { it.copy(roomSessionId = convId) }
            if (convId.isNotEmpty() && social != null && welcome.optInt("companion") == 1) {
                val peerId = TogetherTls.peerId(socket)
                if (peerId.isNotEmpty()) social.attach(peerId, wire, wire::json, conversationId = convId, name = "一起听房间",
                    isCurrent = { generation == epoch.get() && activeWire === wire && peer === socket && !socket.isClosed })
            }
            return socket to wire
        } catch (error: Exception) {
            retireSocket(socket)
            throw error
        }
    }

    private suspend fun sendMediaToMember(member: ConnectedMember, track: Track, id: String, prefetch: Boolean, retry: Boolean = false) {
        if (member.closed || (!retry && id in member.sentMedia) || !member.pendingMedia.add(id)) return
        try { member.mediaSendLock.withLock {
        if (member.closed) return@withLock
        if (prefetch && (!prefetchEnabled || prefetchNetworkAllowed?.invoke() == false)) return@withLock
        try {
            member.wire.json(JSONObject().put("type", "mediaStart").put("id", id).put("track", metadata(track)).put("prefetch", prefetch))
            val digest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            require(canShareTrack(track))
            val audioStream = openAudio(track)
            audioStream.use { input ->
                val buffer = ByteArray(32_768)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    if (member.closed) return@withLock
                    if (prefetch && (!prefetchEnabled || prefetchNetworkAllowed?.invoke() == false)) {
                        member.wire.json(JSONObject().put("type", "mediaError").put("id", id))
                        return@withLock
                    }
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    size += count
                    require(size <= MAX_AUDIO_BYTES)
                    digest.update(buffer, 0, count)
                    member.wire.binary(buffer, count)
                    yield()
                }
            }
            require(size > 0)
            currentCoroutineContext().ensureActive()
            member.wire.json(JSONObject().put("type", "mediaEnd").put("id", id).put("size", size).put("sha256", hex(digest.digest())))
            member.sentMedia.add(id)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            runCatching { member.wire.json(JSONObject().put("type", "mediaError").put("id", id)) }
        }
        } } finally { member.pendingMedia.remove(id) }
    }

    private suspend fun handleMemberWire(generation: Long, member: ConnectedMember, enhanced: Boolean) = coroutineScope {
        val roomId = roomConversationId
        if (generation != epoch.get() || roomId.isEmpty()) return@coroutineScope
        var lastRequestAt = 0L
        val handledRequests = linkedMapOf<String, Boolean>()
        var guestIncoming: OutputStream? = null
        var guestPartial: Path? = null
        var guestTransferId = ""
        var guestTransferred = 0L
        var guestDigest = MessageDigest.getInstance("SHA-256")
        var guestMetadata: Track? = null

        val snapshot = nativePublished ?: published
        val currentTrack = snapshot.player.currentTrack?.takeIf(::canShareTrack)
        if (currentTrack != null) {
            val mId = mediaKey(currentTrack)
            launch { sendMediaToMember(member, currentTrack, mId, false) }
            val context = publishedLyrics.takeIf { it.first == currentTrack.id }?.second
            val payloadContext = lyricsMessage(mId, context).toString()
            launch { runCatching { member.wire.json(JSONObject(payloadContext)) } }
        }

        try {
            while (isActive && generation == epoch.get() && !member.closed) {
                val packet = member.wire.read()
                ensureActive()
                if (generation != epoch.get() || member.closed) break
                if (packet.first == BINARY) {
                    val out = guestIncoming ?: error("Unexpected binary from member")
                    guestTransferred += packet.second.size
                    require(guestTransferred <= MAX_AUDIO_BYTES)
                    guestDigest.update(packet.second)
                    writeOwned(requireNotNull(guestPartial), out, packet.second, generation)
                    continue
                }
                val message = parseTogetherJson(packet.second)
                if (message.optString("type") in setOf("pairRequest", "pairResult", "partnerContact")) {
                    social?.let { session -> if (session.state.value.peerId == member.id) session.receive(member.wire, message) }
                    continue
                }
                when (message.optString("type")) {
                    "ping" -> {
                        member.pingNanos = System.nanoTime() - message.optLong("stamp", System.nanoTime())
                        member.wire.json(JSONObject().put("type", "pong").put("stamp", message.optLong("stamp", 0L)))
                    }
                    "ready" -> {
                        require(enhanced)
                        val current = (nativePublished ?: published).player.currentTrack ?: continue
                        if (message.optString("id") != mediaKey(current)) continue
                        member.ready = true
                        coordinator.setMemberReady(member.id, true)
                        updateMemberList(generation)
                        broadcastGroupSnapshot(generation)
                    }
                    "need" -> {
                        require(enhanced)
                        val id = message.getString("id")
                        val current = (nativePublished ?: published).player.currentTrack
                        val track = if (current != null && mediaKey(current) == id) current
                            else sharedTracks.firstOrNull { queueIds[it.id] == id || mediaKey(it) == id }
                        if (track != null) {
                            launch { sendMediaToMember(member, track, id, false, retry = true) }
                        }
                    }
                    "action" -> {
                        require(enhanced && member.groupCapable)
                        val requestId = message.getString("id").also { require(it.matches(MEDIA_ID)) }
                        val previousResult = handledRequests[requestId]
                        if (previousResult != null) {
                            member.wire.json(JSONObject().put("type", "resolved").put("id", requestId).put("allowed", previousResult))
                            continue
                        }
                        val sequence = message.getLong("seq").also { require(it > 0 && message.get("seq").toString() == it.toString()) }
                        val actionName = message.getString("action")
                        val publicTrackId = message.optString("trackId")
                        val action = runCatching { TogetherControl.valueOf(actionName) }.getOrNull() ?: continue
                        require(action in setOf(TogetherControl.Append, TogetherControl.Remove, TogetherControl.Move))
                        val groupAction = when (action) {
                            TogetherControl.Play -> TogetherGroupCoordinator.Action.Play
                            TogetherControl.Pause -> TogetherGroupCoordinator.Action.Pause
                            TogetherControl.Next -> TogetherGroupCoordinator.Action.Next
                            TogetherControl.Previous -> TogetherGroupCoordinator.Action.Previous
                            TogetherControl.Seek -> TogetherGroupCoordinator.Action.Seek(publicTrackId.toFloatOrNull() ?: 0f)
                            TogetherControl.Select -> TogetherGroupCoordinator.Action.Select(publicTrackId)
                            TogetherControl.Append -> {
                                val meta = message.optJSONObject("track")
                                if (meta != null) {
                                    val tId = meta.getString("id")
                                    require(tId == publicTrackId)
                                    val title = meta.getString("title").take(128)
                                    val artist = meta.getString("artist").take(80)
                                    val dur = meta.getLong("durationMs").also { require(it in 1L..86_400_000L) }
                                    TogetherGroupCoordinator.Action.Append(TogetherGroupCoordinator.Track(tId, member.id, title, artist, dur))
                                } else null
                            }
                            TogetherControl.Remove -> TogetherGroupCoordinator.Action.Remove(publicTrackId)
                            TogetherControl.Move -> TogetherGroupCoordinator.Action.Move(publicTrackId, message.optInt("destination", 0))
                        }
                        if (groupAction != null) {
                            val fresh = sequence > member.sequenceFloor.get()
                            if (fresh) member.sequenceFloor.set(sequence)
                            val accepted = fresh && applyQueueAction(generation, member.id, localCommandSequence.incrementAndGet(), groupAction)
                            handledRequests[requestId] = accepted
                            if (handledRequests.size > 128) handledRequests.remove(handledRequests.keys.first())
                            if (accepted) {
                                if (groupAction is TogetherGroupCoordinator.Action.Append && !isMediaCached(groupAction.track.id)) {
                                    member.wire.json(JSONObject().put("type", "pullMedia").put("trackId", groupAction.track.id))
                                }
                                member.wire.json(JSONObject().put("type", "resolved").put("id", requestId).put("allowed", true))
                            } else {
                                member.wire.json(JSONObject().put("type", "resolved").put("id", requestId).put("allowed", false))
                            }
                        }
                    }
                    "request" -> {
                        require(enhanced)
                        val id = message.getString("id").also { require(it.matches(MEDIA_ID)) }
                        val action = runCatching { TogetherControl.valueOf(message.getString("action")) }.getOrNull() ?: continue
                        val publicId = message.optString("trackId")
                        val trackId: String? = when (action) {
                            TogetherControl.Select -> hostQueue.value?.tracks?.firstOrNull { queueIds[it.id] == publicId }?.id
                            TogetherControl.Seek -> publicId.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..1f }?.toString()
                            TogetherControl.Play, TogetherControl.Pause, TogetherControl.Next, TogetherControl.Previous -> ""
                            else -> null
                        }
                        val now = System.nanoTime()
                        val accepted = handledRequests[id] ?: run {
                            val allowed = trackId != null && (lastRequestAt == 0L || now - lastRequestAt >= 250_000_000) && actions.subscriptionCount.value > 0 &&
                                actions.tryEmit(TogetherRequest(id, action, trackId, member.id, member.name, null, roomId))
                            if (allowed) lastRequestAt = now
                            handledRequests[id] = allowed
                            if (handledRequests.size > 128) handledRequests.remove(handledRequests.keys.first())
                            allowed
                        }
                        member.wire.json(JSONObject().put("type", "resolved").put("id", id).put("allowed", accepted))
                    }
                    "chat" -> {
                        if (!member.groupCapable) {
                            update(generation) { it.copy(message = "这位朋友使用旧版，暂不支持房间群聊；更新双方软件后再聊天") }
                            member.wire.json(JSONObject().put("type", "resolved").put("allowed", false))
                            continue
                        }
                        val seq = message.optLong("sequence", 0L)
                        val text = message.optString("text")
                        val roomId = roomConversationId
                        if (seq > 0 && text.isNotEmpty()) {
                            require(message.optString("conversationId", roomId) == roomId)
                            require(member.groupCapable && member.chatReceipts)
                            val store = companionStore ?: continue
                            val recipients = activeMembers.values.filter { it.id != member.id && !it.closed }.map { it.id }
                            val targetId = if (activeMembers.size > 1) roomId else member.id
                            val isNew = store.receiveGroup(targetId, seq, text, senderId = member.id, senderName = member.name, recipients = recipients)
                            val stored = store.messages(targetId).firstOrNull { !it.outgoing && it.senderId == member.id && it.sequence == seq }
                            if (stored != null) reportGroupDelivery(generation, roomId, stored)
                            else member.wire.json(JSONObject().put("type", "chatAck").put("sequence", seq))
                            scheduleHostChatFlush(generation, roomId)
                            if (isNew) {
                                social?.notice("")
                                social?.notifyIncomingMessage(roomId)
                            }
                        }
                    }
                    "chatReceipt" -> {
                        require(member.chatReceipts && generation == epoch.get() && activeMembers[member.id] === member)
                        require(message.getString("conversationId") == roomId)
                        val senderId = message.getString("senderId")
                        val seq = message.getLong("sequence").also { require(it > 0) }
                        val targetId = if (activeMembers.size > 1) roomId else member.id
                        val delivered = companionStore?.acknowledgeRecipient(targetId, senderId, seq, member.id)
                            ?: companionStore?.acknowledgeRecipient(roomId, senderId, seq, member.id)
                        if (delivered != null) reportGroupDelivery(generation, roomId, delivered)
                        social?.notice("")
                    }
                    "chatAck" -> {
                        val seq = message.optLong("sequence", 0L)
                        if (seq > 0 && roomConversationId.isNotEmpty()) companionStore?.acknowledge(roomConversationId, seq)
                    }
                    "mediaStart" -> {
                        guestIncoming?.close(); guestIncoming = null
                        guestPartial?.let(::deleteOwned)
                        guestTransferId = message.getString("id").also { id ->
                            require(id.matches(MEDIA_ID) && !isMediaCached(id) && coordinator.snapshot().queue.any { it.id == id && it.sourceMemberId == member.id })
                        }
                        val receivedMetadata = parseMetadata(message.getJSONObject("track"))
                        guestMetadata = receivedMetadata
                        val declared = coordinator.snapshot().queue.single { it.id == guestTransferId && it.sourceMemberId == member.id }
                        require(receivedMetadata.title.take(128) == declared.title && receivedMetadata.artist.take(80) == declared.artist &&
                            durationTextToSeconds(receivedMetadata.durationText) * 1_000L == declared.durationMs)
                        Files.createDirectories(cacheDirectory)
                        guestPartial = Files.createTempFile(cacheDirectory, "guest-$guestTransferId-", ".part").also { own(it, generation) }
                        guestIncoming = Files.newOutputStream(guestPartial)
                        guestTransferred = 0
                        guestDigest = MessageDigest.getInstance("SHA-256")
                    }
                    "mediaError" -> {
                        val id = message.getString("id").also { require(it.matches(MEDIA_ID)) }
                        if (guestTransferId == id) {
                            guestIncoming?.close(); guestIncoming = null
                            guestPartial?.let(::deleteOwned); guestPartial = null
                            guestTransferId = ""
                        }
                        synchronized(lock) {
                            if (generation == epoch.get()) coordinator.removeMemberTracks(member.id) { it.id == id && !isMediaCached(it.id) }
                        }
                        updateMemberList(generation)
                        broadcastGroupSnapshot(generation)
                    }
                    "mediaEnd" -> {
                        require(message.getString("id") == guestTransferId && guestTransferred > 0 && message.getLong("size") == guestTransferred)
                        val expected = message.getString("sha256")
                        require(expected.matches(Regex("[a-f0-9]{64}")) && MessageDigest.isEqual(expected.toByteArray(), hex(guestDigest.digest()).toByteArray()))
                        guestIncoming?.close(); guestIncoming = null
                        val metadata = requireNotNull(guestMetadata)
                        val completed = Files.createTempFile(cacheDirectory, "guest-ready-$guestTransferId-", extension(metadata.mimeType)).also { own(it, generation) }
                        moveOwned(requireNotNull(guestPartial), completed, generation)
                        guestPartial = null
                        val cachedTrack = metadata.copy(id = "together:$generation:$guestTransferId", sourceUri = completed.toString())
                        synchronized(lock) {
                            require(generation == epoch.get() && coordinator.snapshot().queue.any { it.id == guestTransferId && it.sourceMemberId == member.id })
                            sharedTracks = (sharedTracks + cachedTrack).distinctBy { it.id }.take(50)
                            queueIds[cachedTrack.id] = guestTransferId
                            coordinator.markTrackAvailable(guestTransferId)
                            publishHostQueue(generation)
                        }
                        updateMemberList(generation)
                        broadcastGroupSnapshot(generation)
                        val completedId = guestTransferId
                        for (other in activeMembers.values) {
                            if (other.id != member.id && !other.closed) {
                                launch { sendMediaToMember(other, cachedTrack, completedId, false) }
                            }
                        }
                    }
                    "bye" -> {
                        synchronized(lock) {
                            if (generation == epoch.get() && activeMembers[member.id] === member) {
                                memberResumeTokens.remove(member.id)
                                memberInviteRoutes.remove(member.id)
                            }
                        }
                        member.close()
                        break
                    }
                }
            }
        } finally {
            guestIncoming?.close()
            guestPartial?.let(::deleteOwned)
        }
    }

    private data class RemoteFrame(val id: String, val playing: Boolean, val position: Long, val duration: Long, val speed: Float, val at: Long, val sequence: Long)

    private suspend fun guestPeer(generation: Long, wire: Wire) = coroutineScope {
        val conversation = state.value.roomSessionId
        if (generation != epoch.get() || activeWire !== wire) return@coroutineScope
        val coordinatorId = state.value.coordinatorId
        var groupRevision = -1L
        val knownContributions = state.value.sharedQueue.mapTo(mutableSetOf()) { it.id }
        Files.createDirectories(cacheDirectory)
        var incoming: OutputStream? = null
        var partial: Path? = null
        var transferId = ""
        var transferred = 0L
        var retainedBytes = 0L
        var digest = MessageDigest.getInstance("SHA-256")
        var transferMetadata: Track? = null
        val readyMedia = linkedMapOf<String, Track>()
        val readyLyrics = linkedMapOf<String, Lyrics>()
        val requestedMedia = mutableSetOf<String>()
        val evictedMedia = mutableSetOf<String>()
        var prefetch = false
        var latest: RemoteFrame? = null
        var rttNanos = 0L
        val rttSamples = ArrayDeque<Long>()
        var lastSequence = -1L
        var senderBusy = false
        val uploadLock = Mutex()
        fun emit() {
            val frame = latest ?: return
            synchronized(lock) {
                if (generation != epoch.get()) return
                val travel = if (frame.playing) ((System.nanoTime() - frame.at + rttNanos / 2) / 1_000_000.0 * frame.speed).toLong() else 0L
                val progress = if (frame.duration > 0) ((frame.position + travel).toDouble() / frame.duration).toFloat().coerceIn(0f, 1f) else 0f
                _remote.value = TogetherPlayback(readyMedia[frame.id], frame.id, frame.playing, progress, frame.speed, frame.duration, frame.sequence)
                _lyrics.value = readyLyrics[frame.id]?.copy(trackId = readyMedia[frame.id]?.id.orEmpty())
            }
            if (peerEnhanced && !senderBusy && frame.id.isNotEmpty() && frame.id !in readyMedia && frame.id != transferId && frame.id in evictedMedia && requestedMedia.add(frame.id)) {
                evictedMedia.remove(frame.id)
                launch { wire.json(JSONObject().put("type", "need").put("id", frame.id)) }
            }
        }
        val ping = launch {
            while (isActive && generation == epoch.get()) {
                wire.json(JSONObject().put("type", "ping").put("stamp", System.nanoTime()))
                delay(1_000)
            }
        }
        try {
            while (isActive && generation == epoch.get()) {
                val packet = wire.read()
                ensureActive()
                if (generation != epoch.get()) break
                if (packet.first == BINARY) {
                    val output = incoming ?: error("Unexpected audio block")
                    transferred += packet.second.size
                    require(transferred <= MAX_AUDIO_BYTES)
                    require(retainedBytes + transferred <= MAX_AUDIO_BYTES * 2)
                    digest.update(packet.second)
                    writeOwned(requireNotNull(partial), output, packet.second, generation)
                    if (transferred % 524_288 < 32_768) update(generation) { it.copy(receivedBytes = transferred) }
                    continue
                }
                val message = parseTogetherJson(packet.second)
                if (social?.receive(wire, message) == true) continue
                when (message.getString("type")) {
                    "lyrics" -> {
                        require(peerEnhanced)
                        val id = message.getString("id").also { require(it.isEmpty() || it.matches(MEDIA_ID)) }
                        val rows = message.getJSONArray("lines").also { require(it.length() <= 320) }
                        if (rows.length() == 0) readyLyrics.remove(id) else {
                            val lines = (0 until rows.length()).map { index ->
                                val row = rows.getJSONObject(index)
                                val time = if (row.isNull("time")) null else row.getLong("time").also { require(it in 0..86_400_000) }
                                LyricLine(time, row.getString("text").also { require(it.length <= 128) },
                                    row.optString("translation").takeIf { it.isNotEmpty() }?.also { require(it.length <= 128) })
                            }
                            readyLyrics[id] = Lyrics("", lines, message.getBoolean("synchronized"), message.getBoolean("instrumental"),
                                if (message.optBoolean("truncated")) "朋友分享（歌词节选）" else "朋友分享", offsetMs = message.getLong("offset").also { require(it in -600_000..600_000) })
                            while (readyLyrics.size > 2) readyLyrics.remove(readyLyrics.keys.first())
                        }
                        emit()
                    }
                    "pong" -> {
                        val sample = (System.nanoTime() - message.getLong("stamp")).coerceIn(0, 3_000_000_000)
                        rttSamples.addLast(sample)
                        if (rttSamples.size > 7) rttSamples.removeFirst()
                        rttNanos = rttSamples.sorted()[rttSamples.size / 2]
                    }
                    "queue" -> {
                        require(peerEnhanced)
                        val items = message.getJSONArray("items").also { require(it.length() <= 50) }
                        val queue = (0 until items.length()).map { index ->
                            val item = items.getJSONObject(index)
                            TogetherQueueItem(item.getString("id").also { require(it.matches(MEDIA_ID)) },
                                item.getString("title").also { require(it.length <= 128) },
                                item.getString("artist").also { require(it.length <= 80) }, item.getBoolean("current"))
                        }
                        require(queue.distinctBy { it.id }.size == queue.size)
                        update(generation) { it.copy(sharedQueue = queue) }
                    }
                    "groupSnapshot" -> {
                        require(peerEnhanced && state.value.multiplayer && message.getString("roomId") == conversation)
                        val revision = message.getLong("revision").also { require(it >= 0 && message.get("revision").toString() == it.toString()) }
                        if (revision <= groupRevision) continue
                        val membersArray = message.getJSONArray("members").also { require(it.length() in 1..8) }
                        val memberList = (0 until membersArray.length()).map { index ->
                            val item = membersArray.getJSONObject(index)
                            TogetherMember(
                                id = item.getString("id").also { require(it.matches(Regex("[a-f0-9]{32,64}"))) },
                                name = item.getString("name").also { value -> require(value.length in 1..40 && value.none { it.code in 0..31 || it.code in 127..159 }) },
                                ready = item.optBoolean("ready", false),
                                isCoordinator = item.optBoolean("isCoordinator", false)
                            )
                        }
                        require(memberList.map { it.id }.distinct().size == memberList.size && memberList.count { it.isCoordinator } == 1 &&
                            memberList.single { it.isCoordinator }.id == coordinatorId)
                        val queueArray = message.getJSONArray("queue").also { require(it.length() <= 50) }
                        val queue = (0 until queueArray.length()).map { index ->
                            val item = queueArray.getJSONObject(index)
                            TogetherQueueItem(
                                id = item.getString("id").also { require(it.matches(MEDIA_ID)) },
                                title = item.getString("title").also { value -> require(value.length in 1..128 && value.none { it.code in 0..31 || it.code in 127..159 }) },
                                artist = item.getString("artist").also { value -> require(value.length <= 80 && value.none { it.code in 0..31 || it.code in 127..159 }) },
                                current = item.optBoolean("current", false),
                                sourceMemberId = item.getString("sourceMemberId").also { require(it.matches(Regex("[a-f0-9]{32,64}"))) },
                                sourceMemberName = item.optString("sourceMemberName", "成员").also { value -> require(value.length <= 40 && value.none { it.code in 0..31 || it.code in 127..159 }) },
                                playable = item.optBoolean("playable", false)
                            )
                        }
                        val currentId = message.getString("currentTrackId")
                        require(queue.map { it.id }.distinct().size == queue.size && queue.count { it.current } <= 1 &&
                            (if (currentId.isEmpty()) queue.none { it.current } else queue.singleOrNull { it.current }?.id == currentId))
                        val queueIds = queue.mapTo(mutableSetOf()) { it.id }
                        (knownContributions - queueIds).forEach { guestContributedTracks.remove(it) }
                        knownContributions.clear(); knownContributions.addAll(queueIds)
                        groupRevision = revision
                        update(generation) { it.copy(
                            members = memberList,
                            sharedQueue = queue,
                            multiplayer = true,
                            equalControls = true,
                            collaborative = true,
                        ) }
                        social?.updateMembers(memberList, conversation)
                    }
                    "pullMedia" -> {
                        val trackId = message.getString("trackId")
                        val localTrack = guestContributedTracks[trackId]
                        if (localTrack != null) {
                            launch {
                                uploadLock.withLock { try {
                                    wire.json(JSONObject().put("type", "mediaStart").put("id", trackId).put("track", metadata(localTrack)).put("prefetch", false))
                                    val digest = MessageDigest.getInstance("SHA-256")
                                    var size = 0L
                                    openAudio(localTrack).use { input ->
                                        val buffer = ByteArray(32_768)
                                        while (true) {
                                            currentCoroutineContext().ensureActive()
                                            val count = input.read(buffer)
                                            if (count < 0) break
                                            if (count == 0) continue
                                            size += count
                                            require(size <= MAX_AUDIO_BYTES)
                                            digest.update(buffer, 0, count)
                                            wire.binary(buffer, count)
                                            yield()
                                        }
                                    }
                                    require(size > 0)
                                    wire.json(JSONObject().put("type", "mediaEnd").put("id", trackId).put("size", size).put("sha256", hex(digest.digest())))
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) {
                                    runCatching { wire.json(JSONObject().put("type", "mediaError").put("id", trackId)) }
                                } }
                            }
                        } else {
                            wire.json(JSONObject().put("type", "mediaError").put("id", trackId))
                        }
                    }
                    "memberLeft" -> {
                        val memberId = message.optString("memberId")
                        val memberName = state.value.members.firstOrNull { it.id == memberId }?.name ?: "成员"
                        update(generation) { it.copy(message = "$memberName 已离开房间") }
                    }
                    "resolved" -> {
                        require(peerEnhanced)
                        update(generation) { it.copy(message = if (it.equalControls) {
                            if (message.getBoolean("allowed")) "操作已接收，等待播放状态同步" else "操作暂未送达，请稍后重试"
                        } else if (message.getBoolean("allowed")) "对方已接受你的请求" else "对方暂未接受这次请求") }
                    }
                    "mediaStart" -> {
                        incoming?.close(); incoming = null
                        partial?.let(::deleteOwned)
                        retainedBytes = synchronized(lock) { ownedFiles.keys.toList() }
                            .sumOf { runCatching { Files.size(it) }.getOrDefault(0L) }
                        transferId = message.getString("id").also { require(it.matches(MEDIA_ID)) }
                        requestedMedia.remove(transferId)
                        transferMetadata = parseMetadata(message.getJSONObject("track"))
                        prefetch = message.optBoolean("prefetch", false).also { require(!it || peerEnhanced) }
                        partial = Files.createTempFile(cacheDirectory, "incoming-", ".part").also { own(it, generation) }
                        incoming = Files.newOutputStream(partial)
                        transferred = 0
                        digest = MessageDigest.getInstance("SHA-256")
                        update(generation) { if (prefetch) it.copy(prefetching = true, nextTrackTitle = transferMetadata!!.title,
                            message = "正在预备下一首，当前播放不受影响") else it.copy(status = TogetherStatus.Receiving, trackTitle = transferMetadata!!.title,
                            receivedBytes = 0, peerReady = false, message = "正在加密接收当前歌曲，完成后自动对齐房主进度") }
                    }
                    "mediaEnd" -> {
                        require(message.getString("id") == transferId && transferred > 0 && message.getLong("size") == transferred)
                        val expected = message.getString("sha256")
                        require(expected.matches(Regex("[a-f0-9]{64}")) && MessageDigest.isEqual(expected.toByteArray(), hex(digest.digest()).toByteArray()))
                        incoming?.close(); incoming = null
                        val metadata = requireNotNull(transferMetadata)
                        val completed = Files.createTempFile(cacheDirectory, "playing-", extension(metadata.mimeType)).also { own(it, generation) }
                        moveOwned(requireNotNull(partial), completed, generation)
                        partial = null
                        // Retire the predecessor after the player has handed over; live cache is bounded.
                        val replaced = readyMedia.put(transferId, metadata.copy(id = "together:$generation:$transferId", sourceUri = completed.toString(), artworkPath = "embedded:$completed"))
                        evictedMedia.remove(transferId)
                        replaced?.sourceUri?.let { old -> launch { delay(2_000); deleteOwned(Path.of(old)) } }
                        while (readyMedia.size > 2) {
                            val retired = readyMedia.entries.firstOrNull { it.key != latest?.id && it.key != transferId } ?: break
                            readyMedia.remove(retired.key)
                            evictedMedia.add(retired.key)
                            retired.value.sourceUri?.let { old -> launch { delay(2_000); deleteOwned(Path.of(old)) } }
                        }
                        update(generation) { if (prefetch && latest?.id != transferId) it.copy(prefetching = false, nextTrackTitle = metadata.title,
                            message = "下一首已预备") else it.copy(status = if (readinessTracking && peerEnhanced) TogetherStatus.Preparing else TogetherStatus.Connected,
                            trackTitle = metadata.title, receivedBytes = transferred, message = "歌曲已校验，正在准备播放器") }
                        emit()
                    }
                    "mediaError" -> {
                        require(message.getString("id") == transferId)
                        incoming?.close(); incoming = null; partial?.let(::deleteOwned); partial = null
                        evictedMedia.remove(transferId)
                        readyMedia.remove(transferId)?.sourceUri?.let { deleteOwned(java.nio.file.Paths.get(it)) }
                        emit()
                        update(generation) { it.copy(status = TogetherStatus.Connected, prefetching = false,
                            message = if (prefetch) "下一首暂时无法预备" else "房主当前的音频无法共享，请等待换歌") }
                    }
                    "state" -> {
                        val sequence = message.optLong("seq", lastSequence + 1).also { require(it >= 0) }
                        if (sequence <= lastSequence) continue
                        lastSequence = sequence
                        senderBusy = message.optBoolean("sending", false)
                        val id = message.getString("id")
                        require(id.isEmpty() || id.matches(MEDIA_ID))
                        val duration = message.getLong("durationMs").also { require(it in 0..86_400_000) }
                        val position = message.getLong("positionMs").also { require(it in 0..duration) }
                        val speed = message.getDouble("speed").toFloat().also { require(it.isFinite() && it in .25f..4f) }
                        if (id != latest?.id) readyMedia[id]?.let { cached ->
                            update(generation) { it.copy(trackTitle = cached.title, peerReady = false,
                                status = if (readinessTracking && peerEnhanced) TogetherStatus.Preparing else TogetherStatus.Connected,
                                message = "正在切换到已校验的歌曲") }
                        }
                        latest = RemoteFrame(id, message.getBoolean("playing"), position, duration, speed, System.nanoTime(), sequence)
                        emit()
                    }
                    else -> error("Unsupported message")
                }
            }
        } finally {
            ping.cancel()
            incoming?.close()
            partial?.let(::deleteOwned)
        }
    }

    private fun lyricsMessage(mediaId: String, lyrics: Lyrics?): JSONObject {
        val rows = org.json.JSONArray()
        var bytes = 0
        var truncated = false
        for (line in lyrics?.lines.orEmpty()) {
            if (rows.length() >= 320) { truncated = true; break }
            val row = JSONObject().put("time", line.timestampMs?.coerceIn(0, 86_400_000) ?: JSONObject.NULL)
                .put("text", line.text.take(128)).put("translation", line.translation.orEmpty().take(128))
            val cost = row.toString().toByteArray(Charsets.UTF_8).size + 1
            if (bytes + cost > 44_000) { truncated = true; break }
            truncated = truncated || line.text.length > 128 || (line.translation?.length ?: 0) > 128
            rows.put(row); bytes += cost
        }
        return JSONObject().put("type", "lyrics").put("id", mediaId).put("lines", rows)
            .put("synchronized", lyrics?.synchronized == true).put("instrumental", lyrics?.instrumental == true)
            .put("offset", lyrics?.offsetMs?.coerceIn(-600_000, 600_000) ?: 0).put("truncated", truncated)
    }

    override fun leave() {
        val wasGuest = state.value.role == TogetherRole.Guest
        val wasHostingSharedTrack = state.value.role == TogetherRole.Host &&
            (nativePublished ?: published).player.currentTrack?.isTogetherTrack() == true
        val retiredGeneration = epoch.get()
        val retiring: List<java.net.Socket>
        synchronized(lock) {
            epochSignal.value = epoch.incrementAndGet()
            job?.cancel(); job = null
            retiring = sockets.toList()
            sockets.clear()
            peer = null
            runCatching { server?.close() }; server = null
            relayWaiting.clear()
            relaySocketRoutes.clear()
            activeRelayUrl = ""
            roomConversationId = ""
            peerInvitationKey = ""
            identity = null
            nativePublished = null
            activeWire = null
            pendingApproval?.cancel(); pendingApproval = null
            guestResumeKey = ""; guestMemberId = ""; resumeKey = ""; resumeUntil = 0
            trustedPartnerOffer = null
            if (pendingPartnerOffer != null) { pendingPartnerOffer = null; social?.incomingInvitation(null) }
            peerEnhanced = false
            queueIds.clear()
            activeMembers.values.forEach { it.close() }
            activeMembers.clear()
            memberResumeTokens.clear()
            memberInviteRoutes.clear()
            guestContributedTracks.clear()
            mediaCatalog.clear()
            coordinator.reset()
            _hostQueue.value = null
            _hostPlayback.value = null
            awaitingQueueProjection = null
            _remote.value = null
            _lyrics.value = null
            _state.value = TogetherState()
        }
        retiring.forEach { runCatching { it.close() } }
        if (wasGuest || wasHostingSharedTrack) runCatching(onGuestStopped)
        scope.launch { delay(2_000); cleanupThrough(retiredGeneration) }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        partnerHub?.close()
        leave()
        scope.coroutineContext[Job]?.invokeOnCompletion { cleanupOwned() }
        scope.cancel()
        cleanupOwned()
    }

    private fun fail(generation: Long, message: String) {
        synchronized(lock) {
            if (generation != epoch.get()) return
            guestResumeKey = ""
            runCatching { peer?.close() }
            if (_state.value.role == TogetherRole.Guest) runCatching(onGuestStopped)
            _remote.value = null
            _lyrics.value = null
            _state.value = _state.value.copy(role = TogetherRole.None, status = TogetherStatus.Error, message = message)
        }
        scope.launch { delay(2_000); cleanupThrough(generation) }
    }

    private fun update(generation: Long, change: (TogetherState) -> TogetherState) {
        synchronized(lock) { if (generation == epoch.get()) _state.update(change) }
    }
    private fun own(path: Path, generation: Long) {
        synchronized(lock) {
            if (generation != epoch.get() || closed.get()) {
                Files.deleteIfExists(path)
                throw CancellationException("Room already closed")
            }
            val safe = path.toAbsolutePath().normalize()
            val size = Files.size(safe)
            ownedFiles[safe] = generation
            ownedFileBytes[safe] = size
        }
    }
    private fun writeOwned(path: Path, output: OutputStream, bytes: ByteArray, generation: Long) {
        val safe = path.toAbsolutePath().normalize()
        synchronized(lock) {
            check(generation == epoch.get() && ownedFiles[safe] == generation)
            val total = ownedFileBytes.values.sum()
            require(bytes.size.toLong() <= MAX_AUDIO_BYTES * 2 - total) { "房间音频缓存已满" }
            ownedFileBytes[safe] = Math.addExact(ownedFileBytes.getValue(safe), bytes.size.toLong())
        }
        // A failed write may have written a prefix; retain the reservation until deletion.
        output.write(bytes)
    }
    private fun moveOwned(from: Path, to: Path, generation: Long) {
        val source = from.toAbsolutePath().normalize()
        val target = to.toAbsolutePath().normalize()
        synchronized(lock) {
            check(generation == epoch.get() && ownedFiles[source] == generation && ownedFiles[target] == generation)
            Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            ownedFileBytes[target] = ownedFileBytes.getValue(source)
            ownedFileBytes.remove(source)
            ownedFiles.remove(source)
        }
    }
    private fun deleteOwned(path: Path) {
        val safe = path.toAbsolutePath().normalize()
        synchronized(lock) {
            if (safe !in ownedFiles || safe.parent != cacheDirectory.toAbsolutePath().normalize()) return
            if (runCatching { Files.deleteIfExists(safe); true }.getOrDefault(false)) {
                ownedFiles.remove(safe)
                ownedFileBytes.remove(safe)
            }
        }
    }
    private fun cleanupThrough(generation: Long) { synchronized(lock) { ownedFiles.filterValues { it <= generation }.keys.toList() }.forEach(::deleteOwned) }
    private fun cleanupOwned() { synchronized(lock) { ownedFiles.keys.toList() }.forEach(::deleteOwned) }

    private class Wire(socket: SSLSocket) {
        private val input = DataInputStream(BufferedInputStream(socket.inputStream))
        private val output = DataOutputStream(BufferedOutputStream(socket.outputStream))
        private val writing = Mutex()
        suspend fun json(value: JSONObject) = send(JSON, value.toString().toByteArray(Charsets.UTF_8))
        suspend fun binary(value: ByteArray, count: Int) = send(BINARY, value, count)
        private suspend fun send(type: Int, bytes: ByteArray, count: Int = bytes.size) = writing.withLock {
            require(count in 1..MAX_PACKET)
            output.writeByte(type); output.writeInt(count); output.write(bytes, 0, count); output.flush()
        }
        fun read(): Pair<Int, ByteArray> {
            val type = input.readUnsignedByte()
            require(type == JSON || type == BINARY)
            val length = input.readInt().also { require(it in 1..MAX_PACKET) }
            return type to ByteArray(length).also(input::readFully)
        }
        fun readJson(): JSONObject {
            val frame = read(); require(frame.first == JSON)
            return parseTogetherJson(frame.second)
        }
    }

    companion object {
        internal const val MAX_AUDIO_BYTES = 256L * 1024 * 1024
        private const val MAX_PACKET = 65_536
        private const val JSON = 1
        private const val BINARY = 2
        private val KEY = Regex("[A-Za-z0-9_-]{43}")
        private fun newKey(): String = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(32).also(java.security.SecureRandom()::nextBytes))
        private fun secureEquals(left: String, right: String): Boolean = right.isNotEmpty() && MessageDigest.isEqual(
            left.toByteArray(Charsets.US_ASCII), right.toByteArray(Charsets.US_ASCII))
        private val MEDIA_ID = Regex("[a-f0-9-]{36}")
        private fun code(pin: String) = pin.take(12).uppercase().chunked(4).joinToString(" ")
        private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
        private fun metadata(track: Track) = JSONObject().put("title", track.title.take(256)).put("artist", track.artist.take(256))
            .put("album", track.album.take(256)).put("duration", track.durationText.take(16)).put("seed", track.artworkSeed).put("mime", track.mimeType.take(80))
        private fun parseMetadata(value: JSONObject): Track {
            fun text(key: String, max: Int) = value.getString(key).also { require(it.length <= max && it.none { c -> c == '\u0000' }) }
            val duration = text("duration", 16)
            require(durationTextToSeconds(duration) in 0..86_400)
            return Track("", text("title", 256), text("artist", 256), text("album", 256), duration, value.getInt("seed"), mimeType = text("mime", 80))
        }
        private fun extension(mime: String) = when (mime) { "audio/flac", "audio/x-flac" -> ".flac"; "audio/ogg", "audio/opus" -> ".ogg"
            "audio/mp4", "audio/x-m4a" -> ".m4a"; "audio/wav", "audio/x-wav" -> ".wav"; else -> ".mp3" }
        private fun isVirtualInterface(nif: NetworkInterface): Boolean {
            val name = (nif.name + " " + (nif.displayName ?: "")).lowercase()
            return name.contains("wsl") ||
                name.contains("vethernet") ||
                name.contains("virtual") ||
                name.contains("vmware") ||
                name.contains("vbox") ||
                name.contains("docker") ||
                name.contains("hyper-v") ||
                name.contains("teredo") ||
                name.contains("isatap")
        }

        private fun isGlobalUnicastIpv6(addr: InetAddress): Boolean {
            if (addr !is Inet6Address) return false
            if (addr.isLoopbackAddress || addr.isLinkLocalAddress || addr.isMulticastAddress || addr.isSiteLocalAddress) return false
            val bytes = addr.address
            if (bytes.isEmpty()) return false
            val firstByte = bytes[0].toInt() and 0xFF
            return (firstByte and 0xE0) == 0x20
        }

        private fun localAddresses(): List<String> = runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
            val physical = interfaces.filter { !isVirtualInterface(it) }.ifEmpty { interfaces }
            physical.flatMap { it.inetAddresses.toList() }
                .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress && !it.isMulticastAddress }
                .filter { addr ->
                    if (addr is Inet6Address) {
                        val first = addr.address[0].toInt() and 0xFF
                        (first and 0xFE) != 0xFC
                    } else true
                }
                .sortedWith { a, b ->
                    val scoreA = when {
                        isGlobalUnicastIpv6(a) -> 0
                        a is Inet4Address && a.isSiteLocalAddress -> 1
                        a is Inet4Address -> 2
                        else -> 3
                    }
                    val scoreB = when {
                        isGlobalUnicastIpv6(b) -> 0
                        b is Inet4Address && b.isSiteLocalAddress -> 1
                        b is Inet4Address -> 2
                        else -> 3
                    }
                    scoreA.compareTo(scoreB)
                }
                .mapNotNull { it.hostAddress?.substringBefore('%') }.distinct().take(TogetherInvitation.MAX_HOSTS)
        }.getOrDefault(emptyList())
    }
}
