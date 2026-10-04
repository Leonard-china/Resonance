package com.resonance.player.ui

internal actual fun formatWeatherUpdate(timestamp: Long): String =
    java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
