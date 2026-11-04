package com.resonance.player.platform

import com.resonance.player.model.PlayerState
import com.resonance.player.model.Track
import com.resonance.player.model.Lyrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

enum class TogetherRole { None, Host, Guest }
enum class TogetherStatus { Idle, Listening, Connecting, AwaitingApproval, Preparing, Connected, Receiving, Reconnecting, Error }

data class TogetherMember(
    val id: String,
    val name: String,
    val ready: Boolean = false,
    val isCoordinator: Boolean = false,
    val pingMs: Long = 0,
)

data class TogetherQueueItem(
    val id: String,
    val title: String,
    val artist: String,
    val current: Boolean = false,
    val sourceMemberId: String = "",
    val sourceMemberName: String = "",
    val playable: Boolean = true,
)
enum class TogetherControl { Play, Pause, Next, Select, Previous, Seek, Append, Remove, Move }
data class TogetherRequest(
    val id: String,
    val action: TogetherControl,
    val trackId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val track: Track? = null,
    val roomSessionId: String = "",
)
data class TogetherInvitationPreview(val host: String, val port: Int, val candidates: List<String> = emptyList(), val relayHost: String = "")

/** Local playable projection; paths never enter a wire snapshot. */
data class TogetherHostQueue(val roomSessionId: String, val revision: Long, val tracks: List<Track>, val currentTrackId: String?,
                             val publicIds: Map<String, String> = emptyMap())
data class TogetherQr(val size: Int, val bits: List<Boolean>)

data class TogetherChatMessage(
    val sequence: Long,
    val outgoing: Boolean,
    val text: String,
    val timeMs: Long,
    val delivered: Boolean,
    val senderId: String = "",
    val senderName: String = "",
    val conversationId: String = "",
    val pendingRecipients: List<String> = emptyList(),
    val recipientCount: Int = 0,
)
data class TogetherPartner(val id: String, val name: String, val addresses: List<String> = emptyList(), val port: Int = 0)
data class TogetherCompanionState(
    val available: Boolean = false,
    val connected: Boolean = false,
    val peerId: String = "",
    val peerName: String = "朋友",
    val paired: Boolean = false,
    val pairingRequested: Boolean = false,
    val pairingIncoming: Boolean = false,
    val messages: List<TogetherChatMessage> = emptyList(),
    val partners: List<TogetherPartner> = emptyList(),
    val message: String = "",
    val incomingPartnerId: String = "",
    val incomingPartnerName: String = "",
    val rendezvousUrl: String = "",
    val rendezvousStatus: String = "",
    val relayUrl: String = "",
    val relayConfigured: Boolean = false,
    val conversationId: String = "",
    val members: List<TogetherMember> = emptyList(),
    val unreadCount: Int = 0,
)

data class TogetherState(
    val role: TogetherRole = TogetherRole.None,
    val status: TogetherStatus = TogetherStatus.Idle,
    val addresses: List<String> = emptyList(),
    val port: Int = 0,
    val peer: String = "",
    val trackTitle: String = "",
    val receivedBytes: Long = 0,
    val tlsVersion: String = "",
    val message: String = "",
    val verificationCode: String = "",
    val pendingJoin: Boolean = false,
    val peerReady: Boolean = false,
    val collaborative: Boolean = false,
    val reconnectAttempt: Int = 0,
    val invitationExpiresAtMs: Long = 0,
    val sharedQueue: List<TogetherQueueItem> = emptyList(),
    val requests: List<TogetherRequest> = emptyList(),
    val prefetching: Boolean = false,
    val nextTrackTitle: String = "",
    val localAudioPaused: Boolean = false,
    val audioPauseReason: String = "",
    /** New peers may send controls without an approval round trip. */
    val equalControls: Boolean = false,
    val viaRelay: Boolean = false,
    val members: List<TogetherMember> = emptyList(),
    val pendingJoinMemberId: String = "",
    val pendingJoinMemberName: String = "",
    val coordinatorId: String = "",
    val multiplayer: Boolean = false,
    val roomSessionId: String = "",
) {
    val isGuest: Boolean get() = role == TogetherRole.Guest && status !in setOf(TogetherStatus.Idle, TogetherStatus.Error)
    val memberCount: Int get() = if (role == TogetherRole.None) 0 else maxOf(1, members.size)
    val isMultiplayer: Boolean get() = multiplayer || members.size > 2
}

/** A temporary playable file is available only after the authenticated transfer completes. */
data class TogetherPlayback(
    val track: Track?,
    val mediaId: String,
    val playing: Boolean,
    val progress: Float,
    val speed: Float,
    val durationMs: Long,
    val sequence: Long = 0,
)

interface ListenTogetherService {
    val hostQueue: StateFlow<TogetherHostQueue?> get() = MutableStateFlow(null)
    val hostPlayback: StateFlow<com.resonance.player.model.PlayerState?> get() = MutableStateFlow(null)
    val state: StateFlow<TogetherState>
    val remotePlayback: StateFlow<TogetherPlayback?>
    val companion: StateFlow<TogetherCompanionState> get() = MutableStateFlow(TogetherCompanionState())
    /** Transient signal after a new incoming message is saved; no message content is exposed. */
    val chatMessageEvents: Flow<Unit> get() = emptyFlow()
    /** True only after this device durably accepted the outgoing message. */
    suspend fun sendChat(text: String, peerId: String = companion.value.peerId): Boolean = false
    fun clearChat(peerId: String) {}
    fun markChatRead(conversationId: String) {}
    fun requestPairing(peerId: String = companion.value.peerId) {}
    fun respondPairing(accept: Boolean, peerId: String = companion.value.peerId) {}
    fun removePartner(peerId: String) {}
    fun invitePartner(peerId: String) {}
    fun respondPartnerInvitation(accept: Boolean, peerId: String = companion.value.incomingPartnerId) {}
    fun configureRendezvous(url: String) {}
    fun configureRelay(url: String, secret: String) {}
    val remoteLyrics: StateFlow<Lyrics?> get() = MutableStateFlow(null)
    suspend fun host(port: Int = 45873)
    suspend fun join(invitation: String)
    fun invitation(address: String, port: Int = state.value.port): String
    fun invitation(address: String, port: Int = state.value.port, candidates: List<String>): String = invitation(address, port)
    /** Parses locally, without resolving or connecting to the address. */
    fun inspectInvitation(value: String): TogetherInvitationPreview? = null
    fun invitationQr(value: String): TogetherQr? = null
    fun publishPlayback(player: PlayerState)
    fun publishQueue(tracks: List<Track>) {}
    fun publishLyrics(trackId: String, lyrics: Lyrics?) {}
    fun enablePrefetch(enabled: Boolean) {}
    fun requireJoinApproval(required: Boolean) {}
    fun approveJoin(allow: Boolean) {}
    fun renewInvitation() {}
    fun reportPlaybackReady(mediaId: String) {}
    fun reportPlaybackFailure(mediaId: String) { leave() }
    fun reportHostPlaybackFailure() {}
    fun trackPlayerReadiness(enabled: Boolean) {}
    fun pauseGuestAudio(reason: String) {}
    fun resumeGuestAudio() {}
    fun requestControl(action: TogetherControl, trackId: String = "") {}
    fun resolveRequest(requestId: String, allow: Boolean) {}
    val approvedActions: Flow<TogetherRequest> get() = emptyFlow()
    fun appendSharedTrack(track: Track) {}
    fun removeSharedTrack(trackId: String) {}
    fun moveSharedTrack(trackId: String, destination: Int) {}
    fun leave()
    fun close()
}

fun Track.isTogetherTrack(): Boolean = id.startsWith("together:")

/** Public endpoint owned by the project maintainer; room content remains encrypted. */
object TogetherDefaults { const val RelayUrl = "wss://leonard.asia/resonance-relay/v1" }
