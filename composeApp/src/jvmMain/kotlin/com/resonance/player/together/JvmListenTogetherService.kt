package com.resonance.player.together

import com.resonance.player.model.*
import com.resonance.player.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.*
import java.net.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

/** One host and one invited friend. Only the host's current audio is ever exposed. */
class JvmListenTogetherService(
    private val cacheDirectory: Path,
    private val openAudio: (Track) -> InputStream,
    private val onGuestStopped: () -> Unit = {},
) : ListenTogetherService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val epoch = AtomicLong()
    private val closed = AtomicBoolean(false)
    private val lock = Any()
    private val _state = MutableStateFlow(TogetherState())
    override val state: StateFlow<TogetherState> = _state.asStateFlow()
    private val _remote = MutableStateFlow<TogetherPlayback?>(null)
    override val remotePlayback: StateFlow<TogetherPlayback?> = _remote.asStateFlow()
    @Volatile private var server: SSLServerSocket? = null
    @Volatile private var peer: SSLSocket? = null
    @Volatile private var job: Job? = null
    @Volatile private var identity: TogetherTls.Identity? = null
    private data class Published(val player: PlayerState, val at: Long = System.nanoTime())
    @Volatile private var published = Published(PlayerState())
    private val ownedFiles = mutableMapOf<Path, Long>()

    override fun publishPlayback(player: PlayerState) { published = Published(player) }

    override suspend fun host(port: Int) = withContext(Dispatchers.IO) {
        check(!closed.get())
        require(port in 0..65535) { "端口需在 1–65535 之间" }
        leave()
        val generation = epoch.get()
        try {
            val keys = TogetherTls.identity()
            val listener = (keys.context.serverSocketFactory.createServerSocket() as SSLServerSocket).apply {
                reuseAddress = true
                TogetherTls.configure(this)
                try { bind(InetSocketAddress(port), 8) } catch (error: Exception) { close(); throw error }
            }
            synchronized(lock) {
                if (generation != epoch.get() || closed.get()) { listener.close(); return@withContext }
                identity = keys
                server = listener
            }
            update(generation) { TogetherState(TogetherRole.Host, TogetherStatus.Listening,
                addresses = localAddresses(), port = listener.localPort, message = "等待朋友通过邀请加入", verificationCode = code(keys.pin)) }
            val busy = AtomicBoolean(false)
            val handshakes = Semaphore(4)
            job = scope.launch {
                while (isActive && generation == epoch.get()) {
                    val socket = try { listener.accept() as SSLSocket } catch (_: IOException) { break }
                    if (!handshakes.tryAcquire()) { socket.close(); continue }
                    launch {
                        var authenticated = false
                        try {
                            TogetherTls.configure(socket)
                            socket.startHandshake()
                            val wire = Wire(socket)
                            val auth = wire.readJson()
                            val presented = auth.optString("key")
                            require(auth.optString("type") == "hello" && auth.optInt("version") == 1 && presented.length == 43 &&
                                MessageDigest.isEqual(presented.toByteArray(Charsets.US_ASCII), keys.key.toByteArray(Charsets.US_ASCII)))
                            require(generation == epoch.get() && busy.compareAndSet(false, true))
                            authenticated = true
                            synchronized(lock) {
                                check(generation == epoch.get())
                                peer = socket
                            }
                            wire.json(JSONObject().put("type", "welcome").put("version", 1))
                            update(generation) { it.copy(status = TogetherStatus.Connected, peer = socket.inetAddress.hostAddress.orEmpty(),
                                tlsVersion = socket.session.protocol, message = "朋友已加入，正在共享当前歌曲") }
                            hostPeer(generation, wire)
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { /* Reject unauthenticated peers without disclosing room state. */ }
                        finally {
                            runCatching { socket.close() }
                            handshakes.release()
                            if (authenticated) {
                                busy.set(false)
                                synchronized(lock) { if (peer === socket) peer = null }
                                update(generation) { it.copy(status = TogetherStatus.Listening, peer = "", tlsVersion = "", message = "朋友已离开，可以重新邀请") }
                            }
                        }
                    }
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { fail(generation, "房间未能创建，请检查端口是否被占用或被系统阻止") }
    }

    override fun invitation(address: String, port: Int): String {
        val keys = identity ?: error("请先创建房间")
        check(state.value.role == TogetherRole.Host)
        return TogetherInvitation(address.trim(), port, keys.pin, keys.key).encode()
    }

    override suspend fun join(invitation: String) = withContext(Dispatchers.IO) {
        check(!closed.get())
        leave()
        val generation = epoch.get()
        update(generation) { TogetherState(TogetherRole.Guest, TogetherStatus.Connecting, message = "正在验证邀请并建立加密连接") }
        var socket: SSLSocket? = null
        try {
            val invite = TogetherInvitation.parse(invitation)
            socket = TogetherTls.client(invite.pin).socketFactory.createSocket() as SSLSocket
            TogetherTls.configure(socket)
            synchronized(lock) { check(generation == epoch.get() && !closed.get()); peer = socket }
            socket.connect(InetSocketAddress(invite.host, invite.port), 8_000)
            socket.startHandshake()
            val wire = Wire(socket)
            wire.json(JSONObject().put("type", "hello").put("version", 1).put("key", invite.key))
            val welcome = wire.readJson()
            require(welcome.optString("type") == "welcome" && welcome.optInt("version") == 1)
            check(generation == epoch.get())
            update(generation) { it.copy(status = TogetherStatus.Connected, peer = invite.host, port = invite.port,
                tlsVersion = socket.session.protocol, message = "加密连接已建立，等待房主的歌曲", verificationCode = code(invite.pin)) }
            job = scope.launch {
                try { guestPeer(generation, wire) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { fail(generation, "连接已断开或音频校验失败，已停止跟听") }
                finally { runCatching { socket.close() } }
            }
        } catch (cancelled: CancellationException) {
            socket?.close()
            if (generation == epoch.get()) leave()
            throw cancelled
        }
        catch (_: Exception) {
            runCatching { socket?.close() }
            fail(generation, "无法建立加密直连，请检查完整邀请、可达地址和端口。异地需要公网 IPv6 或端口映射")
        }
    }

    private suspend fun hostPeer(generation: Long, wire: Wire) = coroutineScope {
        val ticker = launch {
            var sourceKey: Pair<String, String?>? = null
            var mediaId = ""
            var transfer: Job? = null
            try {
                while (isActive && generation == epoch.get()) {
                    val snapshot = published
                    val track = snapshot.player.currentTrack?.takeIf { !it.sourceUri.isNullOrBlank() && !it.isTogetherTrack() }
                    val nextKey = track?.let { it.id to it.sourceUri }
                    if (nextKey != sourceKey) {
                        transfer?.cancelAndJoin()
                        sourceKey = nextKey
                        mediaId = if (track == null) "" else UUID.randomUUID().toString()
                        if (track != null) {
                            val metadata = metadata(track)
                            wire.json(JSONObject().put("type", "mediaStart").put("id", mediaId).put("track", metadata))
                            val currentId = mediaId
                            transfer = launch {
                                try {
                                    val digest = MessageDigest.getInstance("SHA-256")
                                    var size = 0L
                                    openAudio(track).use { input ->
                                        val buffer = ByteArray(32_768)
                                        while (true) {
                                            ensureActive()
                                            val count = input.read(buffer)
                                            if (count < 0) break
                                            if (count == 0) continue
                                            size += count
                                            require(size <= MAX_AUDIO_BYTES)
                                            digest.update(buffer, 0, count)
                                            wire.binary(buffer, count)
                                        }
                                    }
                                    require(size > 0)
                                    ensureActive()
                                    wire.json(JSONObject().put("type", "mediaEnd").put("id", currentId).put("size", size)
                                        .put("sha256", hex(digest.digest())))
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) {
                                    wire.json(JSONObject().put("type", "mediaError").put("id", currentId))
                                    update(generation) { it.copy(message = "当前音频无法共享，或超过 256 MB；请选择另一首本机歌曲") }
                                }
                            }
                        }
                    }
                    val elapsed = if (snapshot.player.isPlaying) (System.nanoTime() - snapshot.at) / 1_000_000.0 * snapshot.player.playbackSpeed else 0.0
                    val duration = track?.let { durationTextToSeconds(it.durationText).toLong() * 1000 } ?: 0
                    val position = snapshot.player.positionMs?.toDouble() ?: (snapshot.player.progress * duration).toDouble()
                    wire.json(JSONObject().put("type", "state").put("id", mediaId).put("playing", snapshot.player.isPlaying)
                        .put("positionMs", (position + elapsed).coerceIn(0.0, duration.toDouble()).toLong())
                        .put("durationMs", duration).put("speed", snapshot.player.playbackSpeed.toDouble()))
                    delay(500)
                }
            } finally { transfer?.cancel() }
        }
        try {
            while (isActive && generation == epoch.get()) {
                val message = wire.readJson()
                require(message.optString("type") == "ping")
                wire.json(JSONObject().put("type", "pong").put("stamp", message.getLong("stamp")))
            }
        } finally { ticker.cancelAndJoin() }
    }

    private data class RemoteFrame(val id: String, val playing: Boolean, val position: Long, val duration: Long, val speed: Float, val at: Long)

    private suspend fun guestPeer(generation: Long, wire: Wire) = coroutineScope {
        Files.createDirectories(cacheDirectory)
        var incoming: OutputStream? = null
        var partial: Path? = null
        var transferId = ""
        var transferred = 0L
        var retainedBytes = 0L
        var digest = MessageDigest.getInstance("SHA-256")
        var transferMetadata: Track? = null
        var readyId = ""
        var readyTrack: Track? = null
        var latest: RemoteFrame? = null
        var rttNanos = 0L
        fun emit() {
            val frame = latest ?: return
            if (generation != epoch.get()) return
            val travel = if (frame.playing) ((System.nanoTime() - frame.at + rttNanos / 2) / 1_000_000.0 * frame.speed).toLong() else 0L
            val progress = if (frame.duration > 0) ((frame.position + travel).toDouble() / frame.duration).toFloat().coerceIn(0f, 1f) else 0f
            _remote.value = TogetherPlayback(readyTrack.takeIf { readyId == frame.id }, frame.id, frame.playing, progress, frame.speed, frame.duration)
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
                    output.write(packet.second)
                    if (transferred % 524_288 < 32_768) update(generation) { it.copy(receivedBytes = transferred) }
                    continue
                }
                val message = parseTogetherJson(packet.second)
                when (message.getString("type")) {
                    "pong" -> rttNanos = (System.nanoTime() - message.getLong("stamp")).coerceIn(0, 3_000_000_000)
                    "mediaStart" -> {
                        incoming?.close(); incoming = null
                        partial?.let(::deleteOwned)
                        retainedBytes = synchronized(lock) { ownedFiles.keys.toList() }
                            .sumOf { runCatching { Files.size(it) }.getOrDefault(0L) }
                        transferId = message.getString("id").also { require(it.matches(MEDIA_ID)) }
                        transferMetadata = parseMetadata(message.getJSONObject("track"))
                        partial = Files.createTempFile(cacheDirectory, "incoming-", ".part").also { own(it, generation) }
                        incoming = Files.newOutputStream(partial)
                        transferred = 0
                        digest = MessageDigest.getInstance("SHA-256")
                        update(generation) { it.copy(status = TogetherStatus.Receiving, trackTitle = transferMetadata!!.title,
                            receivedBytes = 0, message = "正在加密接收当前歌曲，完成后自动对齐房主进度") }
                    }
                    "mediaEnd" -> {
                        require(message.getString("id") == transferId && transferred > 0 && message.getLong("size") == transferred)
                        val expected = message.getString("sha256")
                        require(expected.matches(Regex("[a-f0-9]{64}")) && MessageDigest.isEqual(expected.toByteArray(), hex(digest.digest()).toByteArray()))
                        incoming?.close(); incoming = null
                        val metadata = requireNotNull(transferMetadata)
                        val completed = Files.createTempFile(cacheDirectory, "playing-", extension(metadata.mimeType)).also { own(it, generation) }
                        Files.move(requireNotNull(partial), completed, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                        partial?.let { synchronized(lock) { ownedFiles.remove(it) } }; partial = null
                        // Retire the predecessor after the player has handed over; live cache is bounded.
                        val oldTrack = readyTrack
                        readyId = transferId
                        readyTrack = metadata.copy(id = "together:$generation:$readyId", sourceUri = completed.toString())
                        emit()
                        oldTrack?.sourceUri?.let { old -> launch { delay(2_000); deleteOwned(Path.of(old)) } }
                        update(generation) { it.copy(status = TogetherStatus.Connected, receivedBytes = transferred, message = "跟听中，播放由房主控制") }
                    }
                    "mediaError" -> {
                        require(message.getString("id") == transferId)
                        incoming?.close(); incoming = null; partial?.let(::deleteOwned); partial = null
                        readyId = ""; readyTrack = null; emit()
                        update(generation) { it.copy(status = TogetherStatus.Connected, message = "房主当前的音频无法共享，请等待换歌") }
                    }
                    "state" -> {
                        val id = message.getString("id")
                        require(id.isEmpty() || id.matches(MEDIA_ID))
                        val duration = message.getLong("durationMs").also { require(it in 0..86_400_000) }
                        val position = message.getLong("positionMs").also { require(it in 0..duration) }
                        val speed = message.getDouble("speed").toFloat().also { require(it.isFinite() && it in .25f..4f) }
                        latest = RemoteFrame(id, message.getBoolean("playing"), position, duration, speed, System.nanoTime())
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

    override fun leave() {
        val wasGuest = state.value.role == TogetherRole.Guest
        val retiredGeneration = epoch.get()
        synchronized(lock) {
            epoch.incrementAndGet()
            job?.cancel(); job = null
            runCatching { peer?.close() }; peer = null
            runCatching { server?.close() }; server = null
            identity = null
        }
        _remote.value = null
        _state.value = TogetherState()
        if (wasGuest) runCatching(onGuestStopped)
        scope.launch { delay(2_000); cleanupThrough(retiredGeneration) }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        leave()
        scope.coroutineContext[Job]?.invokeOnCompletion { cleanupOwned() }
        scope.cancel()
        cleanupOwned()
    }

    private fun fail(generation: Long, message: String) {
        if (generation != epoch.get()) return
        if (state.value.role == TogetherRole.Guest) runCatching(onGuestStopped)
        _remote.value = null
        update(generation) { it.copy(role = TogetherRole.None, status = TogetherStatus.Error, message = message) }
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
            ownedFiles[path.toAbsolutePath().normalize()] = generation
        }
    }
    private fun deleteOwned(path: Path) {
        val safe = path.toAbsolutePath().normalize()
        synchronized(lock) {
            if (safe !in ownedFiles || safe.parent != cacheDirectory.toAbsolutePath().normalize()) return
            if (runCatching { Files.deleteIfExists(safe); true }.getOrDefault(false)) ownedFiles.remove(safe)
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
        private fun localAddresses(): List<String> = runCatching {
            NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }.flatMap { it.inetAddresses.toList() }
                .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress && !it.isMulticastAddress }
                .sortedBy { if (it is Inet4Address && it.isSiteLocalAddress) 0 else if (it is Inet6Address) 1 else 2 }
                .mapNotNull { it.hostAddress?.substringBefore('%') }.distinct()
        }.getOrDefault(emptyList())
    }
}
