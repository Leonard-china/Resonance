package com.resonance.player.application

import com.resonance.player.model.*
import com.resonance.player.platform.LibraryServices
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException

class AudioRelinkSession(private val services: LibraryServices) {
    suspend fun missing(tracks: List<Track>): List<Track> = tracks.filter { !services.audioFileAvailable(it) }
    suspend fun apply(matches: List<AudioRelinkMatch>): Pair<Int, List<String>> {
        var restored = 0
        val errors = mutableListOf<String>()
        for (match in matches.distinctBy { it.track.id }) {
            currentCoroutineContext().ensureActive()
            try {
                val current = services.loadLibrary().firstOrNull { it.id == match.track.id }
                    ?: error("歌曲已移出曲库")
                check(current.sourceUri == match.track.sourceUri && !services.audioFileAvailable(current)) { "音频来源已变化，请重新扫描" }
                val replacement = current.copy(sourceUri = match.candidate.sourceUri)
                check(services.audioFileAvailable(replacement)) { "新文件已不可读取" }
                services.saveReplacementAudio(replacement)
                restored++
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { errors += "${match.track.title}：${error.message}" }
        }
        return restored to errors
    }
}
