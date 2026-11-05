package com.resonance.player.application

import com.resonance.player.model.*
import com.resonance.player.platform.LyricsServices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Automatic lookup, refresh and edits share one generation; track IDs alone cannot reject A→B→A. */
class LyricsSession(private val services: LyricsServices, private val publish: (LyricsUiState) -> Unit) {
    private var generation = 0L
    private var trackId: String? = null
    suspend fun load(track: Track?, forceRefresh: Boolean = false) {
        val request = ++generation
        trackId = track?.id
        if (track?.sourceUri == null) { publish(LyricsUiState.Idle); return }
        publish(LyricsUiState.Loading)
        try {
            val result = services.loadLyrics(track, forceRefresh)
            currentCoroutineContext().ensureActive()
            if (request != generation) return
            publish(when (result) {
                is LyricsFetchResult.Found -> LyricsUiState.Ready(result.lyrics)
                is LyricsFetchResult.Unavailable -> LyricsUiState.Unavailable(result.message, result.retryable)
            })
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            if (request == generation) publish(LyricsUiState.Unavailable("歌词查询未完成，请检查网络后重试", true))
        }
    }
    fun accept(id: String, lyrics: Lyrics) {
        if (trackId == id) { ++generation; publish(LyricsUiState.Ready(lyrics)) }
    }
}
