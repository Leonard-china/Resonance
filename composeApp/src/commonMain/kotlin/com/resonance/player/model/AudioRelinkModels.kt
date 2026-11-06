package com.resonance.player.model

data class AudioRelinkCandidate(val sourceUri: String, val fileName: String, val relativePath: String)
data class AudioRelinkMatch(val track: Track, val candidate: AudioRelinkCandidate)

/** Called under the platform's library lock; only the source changes, using current metadata. */
fun prepareAudioRelinkUpdates(
    matches: List<AudioRelinkMatch>,
    currentTracks: List<Track>,
    available: (Track) -> Boolean,
    checkActive: () -> Unit = {},
): Pair<List<Track>, List<String>> {
    val currentById = currentTracks.associateBy { it.id }
    val updates = mutableListOf<Track>()
    val errors = mutableListOf<String>()
    for (match in matches.distinctBy { it.track.id }) {
        checkActive()
        try {
            val current = currentById[match.track.id] ?: error("歌曲已移出曲库")
            check(current.sourceUri == match.track.sourceUri && !available(current)) { "音频来源已变化，请重新扫描" }
            val replacement = current.copy(sourceUri = match.candidate.sourceUri)
            check(available(replacement)) { "新文件已不可读取" }
            updates += replacement
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { errors += "${match.track.title}：${error.message}" }
    }
    return updates to errors
}

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
