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

/** Compatibility facade composed from focused native capabilities. */
interface PlatformServices : PlaybackServices, LibraryServices, PreferenceServices, AppearanceServices, LyricsServices {
    val supportsKgg: Boolean get() = false
    val supportsWebLogin: Boolean get() = false
    val musicAssistant: MusicAssistantServices? get() = null
    suspend fun chooseAssistantImages(): List<com.resonance.player.model.AiImage> = emptyList()
    suspend fun chooseCoverImage(): String? = null
    suspend fun configureBackgroundRecommendations(settings: com.resonance.player.model.MusicFeatureSettings) {}
    val supportsKeyboardShortcuts: Boolean get() = false
    suspend fun automaticRecommendationAllowed(unmeteredOnly: Boolean): Boolean = true
    fun formatDateTime(epochMillis: Long): String = epochMillis.toString()
    suspend fun chooseMusicSourceFile(): String? = null
    suspend fun setTrackFavorite(trackId: String, favorite: Boolean)
    fun close()
}
