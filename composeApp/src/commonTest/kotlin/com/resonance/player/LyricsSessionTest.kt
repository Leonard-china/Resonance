package com.resonance.player

import com.resonance.player.application.LyricsSession
import com.resonance.player.model.*
import com.resonance.player.platform.LyricsServices
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsSessionTest {
    private class Native : LyricsServices {
        val pending = mutableListOf<CompletableDeferred<LyricsFetchResult>>()
        override suspend fun chooseLyricsFolder(): String? = null
        override suspend fun loadLyrics(track: Track, forceRefresh: Boolean): LyricsFetchResult {
            val result = CompletableDeferred<LyricsFetchResult>(); pending += result
            return withContext(NonCancellable) { result.await() }
        }
        override suspend fun requestAiLyrics(track: Track): LyricsFetchResult = error("unused")
        override suspend fun saveLyricsOffset(track: Track, offsetMs: Long): Lyrics? = null
        override suspend fun embedLyricsToAudioFile(track: Track, lyrics: Lyrics): Boolean = false
        override suspend fun editTrackMetadata(track: Track, newTitle: String, newArtist: String, newAlbum: String): Track = track
    }
    private val track = Track("a", "A", "", "", "0:10", 0, sourceUri = "synthetic")
    private fun result(text: String) = LyricsFetchResult.Found(Lyrics("a", listOf(LyricLine(0, text)), true, false, "Fixture"))
    @Test fun manualRefreshWinsOverAnOlderAutomaticLookup() = runTest {
        val native = Native(); var state: LyricsUiState = LyricsUiState.Idle
        val session = LyricsSession(native) { state = it }
        val first = launch { session.load(track) }; runCurrent()
        val refresh = launch { session.load(track, forceRefresh = true) }; runCurrent()
        native.pending[1].complete(result("fresh")); runCurrent()
        native.pending[0].complete(result("stale")); runCurrent()
        first.join(); refresh.join()
        assertEquals("fresh", (state as LyricsUiState.Ready).lyrics.lines.single().text)
    }
    @Test fun cancelledLookupCannotPublishAfterSelectingTheSameTrackAgain() = runTest {
        val native = Native(); var state: LyricsUiState = LyricsUiState.Idle
        val session = LyricsSession(native) { state = it }
        val old = launch { session.load(track) }; runCurrent(); old.cancel()
        val current = launch { session.load(track) }; runCurrent()
        native.pending[1].complete(result("current")); runCurrent()
        native.pending[0].complete(result("cancelled")); runCurrent()
        old.join(); current.join()
        assertEquals("current", (state as LyricsUiState.Ready).lyrics.lines.single().text)
    }
    @Test fun acceptedOffsetEditInvalidatesAnInFlightLookup() = runTest {
        val native = Native(); var state: LyricsUiState = LyricsUiState.Idle
        val session = LyricsSession(native) { state = it }
        val pending = launch { session.load(track) }; runCurrent()
        session.accept(track.id, (result("edited") as LyricsFetchResult.Found).lyrics.copy(offsetMs = 500))
        native.pending.single().complete(result("old offset")); runCurrent(); pending.join()
        assertEquals(500L, (state as LyricsUiState.Ready).lyrics.offsetMs)
    }
}
