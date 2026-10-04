package com.resonance.player.model

enum class AccentMode(val label: String) {
    Sage("浅灰绿"), IceBlue("冰蓝银白"), Weather("随天气"),
}

enum class WeatherPalette { Sage, IceBlue }

data class WeatherLocation(val latitude: Double, val longitude: Double, val label: String) {
    val valid: Boolean get() = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}

data class AppearancePreferences(
    val accentMode: AccentMode = AccentMode.Sage,
    val fallbackCity: WeatherLocation? = null,
    val reduceMotion: Boolean = false,
    val retainedPalette: WeatherPalette = WeatherPalette.Sage,
)

data class WeatherSnapshot(
    val code: Int,
    val palette: WeatherPalette,
    val updatedAtMillis: Long,
    val locationLabel: String,
    val locationKey: String,
)

fun weatherPalette(code: Int, previous: WeatherPalette): WeatherPalette = when (code) {
    0, 1, 2 -> WeatherPalette.Sage
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77,
    80, 81, 82, 85, 86, 95, 96, 97, 99 -> WeatherPalette.IceBlue
    else -> previous
}

fun weatherDescription(code: Int): String = when (code) {
    0, 1 -> "晴朗"
    2 -> "局部多云"
    3 -> "阴天"
    45, 48 -> "雾"
    51, 53, 55, 56, 57 -> "毛毛雨"
    61, 63, 65, 66, 67, 80, 81, 82 -> "降雨"
    71, 73, 75, 77, 85, 86 -> "降雪"
    95, 96, 97, 99 -> "雷暴"
    else -> "天气暂不可用"
}
