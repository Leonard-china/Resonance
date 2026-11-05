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
interface AppearanceServices {
    val foreground: kotlinx.coroutines.flow.StateFlow<Boolean>
    val systemReduceMotion: Boolean get() = false
    fun updateSystemBars(isDark: Boolean) {}
    fun updateWindowBackground(argb: Int) {}
    fun currentTimeMillis(): Long
    suspend fun loadAppearance(): com.resonance.player.model.AppearancePreferences
    suspend fun saveAppearance(preferences: com.resonance.player.model.AppearancePreferences)
    suspend fun loadWeatherSnapshot(): com.resonance.player.model.WeatherSnapshot?
    suspend fun saveWeatherSnapshot(snapshot: com.resonance.player.model.WeatherSnapshot)
    suspend fun weatherLocation(requestPermission: Boolean = false): com.resonance.player.model.WeatherLocation?
    suspend fun searchWeatherCities(query: String): List<com.resonance.player.model.WeatherLocation>
    suspend fun fetchWeather(location: com.resonance.player.model.WeatherLocation, previous: com.resonance.player.model.WeatherPalette): com.resonance.player.model.WeatherSnapshot
}
