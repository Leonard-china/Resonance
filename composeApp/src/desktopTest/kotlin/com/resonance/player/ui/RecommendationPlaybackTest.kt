package com.resonance.player.ui

import com.resonance.player.model.Track
import com.resonance.player.platform.MusicAssistantServices
import com.resonance.player.platform.PlatformServices
import com.resonance.player.resolveTrackPlaybackQueue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationPlaybackTest {
    private fun track(id: String, sourceUri: String? = "file:///$id.mp3") =
        Track(id, id, "Fixture artist", "Fixture album", "3:00", 0, sourceUri = sourceUri)

    private class Backend {
        var restore: suspend (String) -> Track = { error("Unexpected restore: $it") }

        @Suppress("UNCHECKED_CAST")
        private fun invoke(args: Array<out Any?>, operation: suspend () -> Track): Any? =
            operation.startCoroutineUninterceptedOrReturn(args.last() as Continuation<Track>)

        val assistant = Proxy.newProxyInstance(MusicAssistantServices::class.java.classLoader,
            arrayOf(MusicAssistantServices::class.java)) { _, method, arguments ->
            val args = arguments.orEmpty()
            when (method.name) {
                "restoreDownload" -> invoke(args) { restore(args[0] as String) }
                else -> error("Unexpected assistant call: ${method.name}")
            }
        } as MusicAssistantServices

        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader,
            arrayOf(PlatformServices::class.java)) { _, method, _ ->
            when (method.name) {
                "getMusicAssistant" -> assistant
                else -> error("Unexpected platform call: ${method.name}")
            }
        } as PlatformServices
    }

    @Test
    fun recommendationPlaybackUsesBatchOrderAndExcludesUnrelatedLibraryTracks() = runTest {
        val first = track("first")
        val second = track("second")
        val feature = MusicFeatureController(Backend().services, backgroundScope).apply {
            library = listOf(track("unrelated"), second, first)
        }
        var requested = emptyList<Track>()
        var audible: Track? = null
        feature.onPlay = { selected, queue ->
            requested = resolveTrackPlaybackQueue(selected, queue)
            // Android chooses a media item by ID inside the supplied native queue.
            audible = requested[requested.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)]
        }

        feature.playRecommendation(second, listOf(first, second))

        assertEquals(listOf("first", "second"), requested.map(Track::id))
        assertEquals(second, audible)
    }

    @Test
    fun restoredAudioIsPlayableBeforeTheUiLibrarySnapshotUpdates() = runTest {
        val missing = track("missing", null)
        val next = track("next")
        val restored = missing.copy(sourceUri = "file:///restored.mp3")
        val backend = Backend().apply { restore = { restored } }
        val feature = MusicFeatureController(backend.services, backgroundScope).apply {
            library = listOf(track("unrelated"), missing, next)
        }
        var reloads = 0
        feature.onReload = { reloads++ } // Composition has not updated feature.library yet.
        var audible: Track? = null
        var queueIds = emptyList<String>()
        feature.onPlay = { selected, queue ->
            val nativeQueue = resolveTrackPlaybackQueue(selected, queue)
            audible = nativeQueue[nativeQueue.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)]
            queueIds = nativeQueue.map(Track::id)
        }

        feature.playRecommendation(missing, listOf(missing, next))
        runCurrent()

        assertEquals(1, reloads)
        assertEquals(restored, audible)
        assertEquals(listOf("missing", "next"), queueIds)
        assertEquals(null, feature.library.single { it.id == missing.id }.sourceUri)
    }

    @Test
    fun lateRestoreCannotOverrideANewerRecommendationClick() = runTest {
        val missing = track("missing", null)
        val next = track("next")
        val reply = CompletableDeferred<Track>()
        val backend = Backend().apply { restore = { withContext(NonCancellable) { reply.await() } } }
        val feature = MusicFeatureController(backend.services, backgroundScope)
        val played = mutableListOf<String>()
        var reloads = 0
        feature.onPlay = { selected, _ -> played += selected.id }
        feature.onReload = { reloads++ }

        feature.playRecommendation(missing, listOf(missing, next))
        runCurrent()
        feature.playRecommendation(next, listOf(missing, next))
        reply.complete(missing.copy(sourceUri = "file:///late.mp3"))
        runCurrent()

        assertEquals(listOf("next"), played)
        assertEquals(0, reloads)
        assertEquals("", feature.message)
    }

    @Test
    fun failedRestoreKeepsPlaybackAndAllowsRetry() = runTest {
        val missing = track("missing", null)
        val backend = Backend().apply { restore = { error("Fixture download failed") } }
        val feature = MusicFeatureController(backend.services, backgroundScope)
        val played = mutableListOf<Track>()
        feature.onPlay = { selected, _ -> played += selected }

        feature.playRecommendation(missing, listOf(missing))
        runCurrent()
        assertTrue(played.isEmpty())
        assertTrue(feature.message.contains("Fixture download failed"))

        val restored = missing.copy(sourceUri = "file:///retry.mp3")
        backend.restore = { restored }
        feature.playRecommendation(missing, listOf(missing))
        runCurrent()
        assertEquals(listOf(restored), played)
        assertEquals("", feature.message)
    }
}
