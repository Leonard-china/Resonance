package com.resonance.player.ui

import com.resonance.player.model.*
import com.resonance.player.platform.MusicAssistantServices
import com.resonance.player.platform.PlatformServices
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsTranslationControllerTest {
    private val track = Track("translation-fixture", "Example", "Artist", "", "0:10", 0)
    private val original = Lyrics(track.id, listOf(LyricLine(1000, "Hello"), LyricLine(2000, "World")), true, false, "Fixture")
    private val translated = original.withTranslation(listOf("你好", "世界"), "中文")

    private class Backend {
        var calls = 0
        var target = ""
        var force = false
        val saved = mutableListOf<MusicFeatureSettings>()
        var translate: suspend () -> Lyrics = { error("No fixture reply") }
        var cache: suspend () -> Lyrics? = { null }
        @Suppress("UNCHECKED_CAST")
        private fun invoke(args: Array<out Any?>, block: suspend () -> Any?): Any? =
            block.startCoroutineUninterceptedOrReturn(args.last() as Continuation<Any?>)
        val assistant = Proxy.newProxyInstance(MusicAssistantServices::class.java.classLoader, arrayOf(MusicAssistantServices::class.java)) { _, method, arguments ->
            val args = arguments.orEmpty()
            when (method.name) {
                "translate" -> invoke(args) { calls++; target = args[2] as String; force = args[3] as Boolean; translate() }
                "cachedTranslation" -> invoke(args) { cache() }
                "saveSettings" -> { saved += args[0] as MusicFeatureSettings; Unit }
                else -> Unit
            }
        } as MusicAssistantServices
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, _ ->
            when (method.name) { "getMusicAssistant" -> assistant; else -> Unit }
        } as PlatformServices
    }

    private fun TestScope.controller(backend: Backend) = MusicFeatureController(backend.services, backgroundScope).apply {
        player = PlayerState(currentTrack = track)
        lyrics = LyricsUiState.Ready(original)
    }

    @Test fun trimmedLanguageAndOriginalOnlyModeShowTranslationWithoutChangingTimeline() = runTest {
        val backend = Backend().apply { translate = { translated } }
        val feature = controller(backend)
        feature.settings = feature.settings.copy(translationLanguage = " 中文 ", lyricDisplay = LyricDisplay.Original)
        feature.translate()
        feature.translate() // A second click in the same frame must not start another paid request.
        runCurrent()
        assertEquals(1, backend.calls)
        assertEquals("中文", backend.target)
        assertEquals(LyricDisplay.Bilingual, feature.settings.lyricDisplay)
        assertEquals(LyricDisplay.Bilingual, backend.saved.single().lyricDisplay)
        val result = assertNotNull(feature.translatedLyrics(original.copy(offsetMs = 500)))
        assertEquals(listOf("你好", "世界"), result.lines.map { it.translation })
        assertEquals(listOf(1000L, 2000L), result.lines.map { it.timestampMs })
        assertEquals(500L, result.offsetMs)
        assertTrue(original.lines.all { it.translation == null })
        assertFalse(feature.translating)
        feature.player = PlayerState(currentTrack = track.copy(id = "next"))
        assertNull(feature.translatedLyrics(original))
    }

    @Test fun failedRetranslationKeepsPreviousTranslationAndCanBeRetried() = runTest {
        val backend = Backend().apply { translate = { error("请先配置音乐助手模型与 API Key") } }
        val feature = controller(backend)
        feature.translated = translated
        feature.translate(true)
        runCurrent()
        assertTrue(feature.translationError.contains("请先配置音乐助手模型"))
        assertEquals(translated, feature.translatedLyrics(original))
        assertFalse(feature.translating)
        backend.translate = { translated }
        feature.translate(true)
        assertEquals("", feature.translationError)
        runCurrent()
        assertTrue(backend.force)
        assertEquals(2, backend.calls)
        assertEquals("", feature.translationError)
    }

    @Test fun slowCacheMissCannotEraseANewManualTranslation() = runTest {
        val cached = CompletableDeferred<Lyrics?>()
        val backend = Backend().apply { cache = { cached.await() }; translate = { translated } }
        val feature = controller(backend)
        backgroundScope.launch { feature.reloadTranslation() }
        runCurrent()
        feature.translate()
        runCurrent()
        assertEquals(translated, feature.translatedLyrics(original))
        cached.complete(null)
        runCurrent()
        assertEquals(translated, feature.translatedLyrics(original))
    }

    @Test fun trackChangeRetiresOldRequestAndAllowsImmediateTranslationOfNewSong() = runTest {
        val oldResult = CompletableDeferred<Lyrics>()
        val backend = Backend().apply { translate = { withContext(NonCancellable) { oldResult.await() } } }
        val feature = controller(backend)
        feature.translate()
        runCurrent()
        val next = original.copy(trackId = "next")
        feature.player = PlayerState(currentTrack = track.copy(id = next.trackId))
        feature.lyrics = LyricsUiState.Ready(next)
        feature.reloadTranslation()
        assertFalse(feature.translating)
        backend.translate = { translated.copy(trackId = next.trackId) }
        feature.translate()
        runCurrent()
        oldResult.complete(translated)
        runCurrent()
        assertEquals(next.trackId, feature.translated?.trackId)
        assertEquals(2, backend.calls)
        assertEquals("", feature.translationError)
        assertFalse(feature.translating)
    }

    @Test fun languageChangeAndReplacedSourceRejectOldResult() = runTest {
        for (changeLanguage in listOf(true, false)) {
            val reply = CompletableDeferred<Lyrics>()
            val backend = Backend().apply { translate = { reply.await() } }
            val feature = controller(backend)
            feature.translate()
            runCurrent()
            if (changeLanguage) feature.settings = feature.settings.copy(translationLanguage = "English")
            else feature.lyrics = LyricsUiState.Ready(original.copy(lines = listOf(LyricLine(null, "Replacement"))))
            reply.complete(translated)
            runCurrent()
            assertNull(feature.translated)
            assertFalse(feature.translating)
        }
    }

    @Test fun blankLanguageAndUnavailableServiceReportAnActionableFailure() = runTest {
        val backend = Backend()
        val feature = controller(backend)
        feature.settings = feature.settings.copy(translationLanguage = " ")
        feature.translate()
        runCurrent()
        assertTrue(feature.translationError.contains("填写翻译目标语言"))
        assertEquals(0, backend.calls)
        val unavailable = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, _, _ -> null } as PlatformServices
        val missing = MusicFeatureController(unavailable, backgroundScope).apply {
            player = feature.player; lyrics = feature.lyrics
        }
        missing.translate()
        runCurrent()
        assertTrue(missing.translationError.contains("翻译服务尚未准备好"))
        assertFalse(missing.translating)
    }
}
