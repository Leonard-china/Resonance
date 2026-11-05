package com.resonance.player.model

data class AudioRelinkCandidate(val sourceUri: String, val fileName: String, val relativePath: String)
data class AudioRelinkMatch(val track: Track, val candidate: AudioRelinkCandidate)

/** Only unique filenames are suggested; duplicate copies and versions require manual relinking. */
fun matchAudioRelinks(missing: List<Track>, candidates: List<AudioRelinkCandidate>): List<AudioRelinkMatch> {
    fun name(raw: String) = runCatching { java.net.URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") }.getOrDefault(raw).replace('\\', '/').substringBefore('?').substringAfterLast('/').substringAfterLast(':').lowercase()
    val byName = candidates.distinctBy { it.sourceUri }.groupBy { it.fileName.lowercase() }
    val matches = missing.mapNotNull { track ->
        val original = track.sourceUri?.let(::name).orEmpty()
        byName[original]?.singleOrNull()?.let { AudioRelinkMatch(track, it) }
    }
    val duplicates = matches.groupingBy { it.candidate.sourceUri }.eachCount()
    return matches.filter { duplicates[it.candidate.sourceUri] == 1 }
}
