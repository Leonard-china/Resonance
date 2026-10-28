package com.resonance.player.platform

import com.resonance.player.model.PlayerState
import com.resonance.player.model.Track
import kotlinx.coroutines.flow.StateFlow

enum class TogetherRole { None, Host, Guest }
enum class TogetherStatus { Idle, Listening, Connecting, Connected, Receiving, Error }

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
) {
    val isGuest: Boolean get() = role == TogetherRole.Guest && status !in setOf(TogetherStatus.Idle, TogetherStatus.Error)
}

/** A temporary playable file is available only after the authenticated transfer completes. */
data class TogetherPlayback(
    val track: Track?,
    val mediaId: String,
    val playing: Boolean,
    val progress: Float,
    val speed: Float,
    val durationMs: Long,
)

interface ListenTogetherService {
    val state: StateFlow<TogetherState>
    val remotePlayback: StateFlow<TogetherPlayback?>
    suspend fun host(port: Int = 45873)
    suspend fun join(invitation: String)
    fun invitation(address: String, port: Int = state.value.port): String
    fun publishPlayback(player: PlayerState)
    fun leave()
    fun close()
}

fun Track.isTogetherTrack(): Boolean = id.startsWith("together:")
