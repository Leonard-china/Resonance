package com.resonance.player.platform

import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.DeepSeekTestResult
import com.resonance.player.model.DeleteTrackReport
import com.resonance.player.model.ImportReport
import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.Playlist
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.SyncReport
import com.resonance.player.model.Track
import com.resonance.player.model.CalibrationStatus
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Focused platform contract; implemented by the existing native adapters. */
interface LyricsServices {
    fun updateFloatingLyrics(player: com.resonance.player.model.PlayerState, lyrics: com.resonance.player.model.LyricsUiState, settings: com.resonance.player.model.MusicFeatureSettings) {}
    fun canDrawOverlays(): Boolean = true
    fun requestOverlayPermission(): Boolean = false
    suspend fun chooseLyricsFolder(): String?
    suspend fun loadLyrics(track: Track, forceRefresh: Boolean = false): LyricsFetchResult
    /** Explicit direct model text generation; does not search or upload audio, and returns no verified timeline. */
    suspend fun requestAiLyrics(track: Track): LyricsFetchResult
    suspend fun saveLyricsOffset(track: Track, offsetMs: Long): com.resonance.player.model.Lyrics?
    suspend fun embedLyricsToAudioFile(track: Track, lyrics: com.resonance.player.model.Lyrics): Boolean
    suspend fun editTrackMetadata(track: Track, newTitle: String, newArtist: String, newAlbum: String): Track
}
