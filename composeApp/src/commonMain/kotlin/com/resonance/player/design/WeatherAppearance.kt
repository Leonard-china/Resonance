package com.resonance.player.design

import androidx.compose.runtime.*
import com.resonance.player.model.*
import com.resonance.player.platform.PlatformServices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class AppearanceController(
    val preferences: AppearancePreferences = AppearancePreferences(),
    val snapshot: WeatherSnapshot? = null,
    val status: String = "",
    val updating: Boolean = false,
    val onPreferencesChange: (AppearancePreferences) -> Unit = {},
    val onRefresh: (Boolean) -> Unit = {},
    val searchCities: suspend (String) -> List<WeatherLocation> = { emptyList() },
    val chooseLyricsFolder: suspend () -> String? = { null },
)

val LocalAppearance = staticCompositionLocalOf { AppearanceController() }
val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalAppForeground = staticCompositionLocalOf { true }

@Composable
fun WeatherAppearanceTheme(services: PlatformServices, themeMode: ThemeMode, content: @Composable () -> Unit) {
    var preferences by remember(services) { mutableStateOf(AppearancePreferences()) }
    var snapshot by remember(services) { mutableStateOf<WeatherSnapshot?>(null) }
    var loaded by remember(services) { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var updating by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var requestPermission by remember { mutableStateOf(false) }
    val foreground by services.foreground.collectAsState()

    LaunchedEffect(services) {
        try {
            preferences = services.loadAppearance()
            snapshot = services.loadWeatherSnapshot()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            status = "外观设置暂时无法读取"
        } finally { loaded = true }
    }
    LaunchedEffect(preferences, loaded) {
        if (!loaded) return@LaunchedEffect
        delay(150)
        try { services.saveAppearance(preferences) }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            status = "外观设置未能保存，请稍后重试"
        }
    }

    LaunchedEffect(loaded, foreground, preferences.accentMode, preferences.fallbackCity, refresh) {
        if (!loaded || !foreground || preferences.accentMode != AccentMode.Weather) return@LaunchedEffect
        while (isActive) {
            val age = snapshot?.let { services.currentTimeMillis() - it.updatedAtMillis }
            val cachedCityMatches = preferences.fallbackCity == null || snapshot?.locationLabel == "当前位置" ||
                snapshot?.locationLabel == preferences.fallbackCity?.label
            if (refresh > 0 || age == null || age !in 0L until 1_800_000L || !cachedCityMatches) {
                updating = true
                status = "正在获取当地天气…"
                try {
                    val askPermission = requestPermission
                    requestPermission = false
                    val deviceLocation = services.weatherLocation(askPermission)
                    val location = deviceLocation ?: preferences.fallbackCity
                    if (location == null) {
                        status = "定位不可用，请选择兜底城市；已保留当前配色"
                    } else {
                        val result = services.fetchWeather(location, preferences.retainedPalette)
                        services.saveWeatherSnapshot(result)
                        snapshot = result
                        preferences = preferences.copy(retainedPalette = result.palette)
                        status = if (deviceLocation == null) "正在使用兜底城市" else "已使用设备大致位置"
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    status = "天气获取失败，已保留上次配色；稍后重试"
                } finally { updating = false }
            }
            delay(1_800_000L)
        }
    }
    val palette = when (preferences.accentMode) {
        AccentMode.Sage -> WeatherPalette.Sage
        AccentMode.IceBlue -> WeatherPalette.IceBlue
        AccentMode.Weather -> preferences.retainedPalette
    }
    val reduced = preferences.reduceMotion || services.systemReduceMotion
    CompositionLocalProvider(
        LocalAppearance provides AppearanceController(preferences, snapshot, status, updating,
            onPreferencesChange = { next ->
                if (next.accentMode == AccentMode.Weather && preferences.accentMode != AccentMode.Weather) {
                    requestPermission = true
                    refresh++
                }
                preferences = next.copy(retainedPalette = when (next.accentMode) {
                    AccentMode.Sage -> WeatherPalette.Sage
                    AccentMode.IceBlue -> WeatherPalette.IceBlue
                    AccentMode.Weather -> palette
                })
            },
            onRefresh = { ask -> requestPermission = ask; refresh++ },
            searchCities = services::searchWeatherCities,
            chooseLyricsFolder = services::chooseLyricsFolder),
        LocalReducedMotion provides reduced,
        LocalAppForeground provides foreground,
    ) {
        ResonanceTheme(themeMode, palette, reduced) {
            val dark = ResonanceColors.isDark
            SideEffect { services.updateSystemBars(dark) }
            content()
        }
    }
}
