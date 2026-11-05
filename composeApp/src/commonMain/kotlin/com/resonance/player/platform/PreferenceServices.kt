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
interface PreferenceServices {
    suspend fun loadThemeMode(): com.resonance.player.model.ThemeMode
    suspend fun saveThemeMode(mode: com.resonance.player.model.ThemeMode)
    suspend fun loadTutorialProgress(): com.resonance.player.tutorial.TutorialProgress = com.resonance.player.tutorial.TutorialProgress()
    suspend fun saveTutorialProgress(progress: com.resonance.player.tutorial.TutorialProgress) {}
    suspend fun loadDeepSeekConfig(): DeepSeekConfig
    suspend fun saveDeepSeekConfig(config: DeepSeekConfig)
    suspend fun testDeepSeek(config: DeepSeekConfig): DeepSeekTestResult
    suspend fun loadCustomLyricsFolder(): String?
    suspend fun saveCustomLyricsFolder(folder: String?)

}
