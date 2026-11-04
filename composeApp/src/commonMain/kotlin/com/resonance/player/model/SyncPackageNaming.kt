package com.resonance.player.model

/** One portable path component, with room for the extension and a collision suffix. */
fun syncPackageFileName(playlistName: String?): String {
    val title = playlistName ?: "整个音乐库"
    var stem = title.replace(Regex("[\\\\/:*?\"<>|\\p{Cc}]"), " ")
        .replace(Regex("\\s+"), " ").trim(' ', '.')
    if (stem.endsWith(".resonance", ignoreCase = true)) stem = stem.dropLast(10).trim(' ', '.')
    stem = stem.take(72)
    if (stem.lastOrNull()?.let { it in '\uD800'..'\uDBFF' } == true) stem = stem.dropLast(1)
    stem = stem.trimEnd(' ', '.').ifBlank { "未命名歌单" }
    if (stem.substringBefore('.').uppercase().matches(Regex("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]"))) stem = "歌单 $stem"
    return "$stem.resonance"
}

fun uniqueSyncPackageFileName(preferred: String, exists: (String) -> Boolean): String {
    val stem = if (preferred.endsWith(".resonance", ignoreCase = true)) preferred.dropLast(10) else preferred
    var candidate = "$stem.resonance"
    var suffix = 2
    while (exists(candidate)) candidate = "$stem (${suffix++}).resonance"
    return candidate
}
