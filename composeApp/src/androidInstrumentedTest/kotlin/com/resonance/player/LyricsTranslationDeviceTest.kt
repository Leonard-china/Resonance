package com.resonance.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.platform.MusicAssistantServices
import com.resonance.player.platform.PlatformServices
import com.resonance.player.ui.*
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Proxy

/** Real lyric controls with synthetic replies; no device library, credentials or network. */
class LyricsTranslationDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    private val track = Track("translation-ui-fixture", "Example", "Artist", "", "0:10", 0)
    private val original = Lyrics(track.id, listOf(LyricLine(null, "Hello")), false, false, "Fixture")
    private var fail = false

    private fun show() {
        val assistant = Proxy.newProxyInstance(MusicAssistantServices::class.java.classLoader, arrayOf(MusicAssistantServices::class.java)) { _, method, _ ->
            when (method.name) {
                "translate" -> {
                    if (fail) error("请先配置音乐助手模型与 API Key")
                    original.withTranslation(listOf("你好"), "中文")
                }
                else -> Unit
            }
        } as MusicAssistantServices
        val platform = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, _ ->
            when (method.name) { "getMusicAssistant" -> assistant; else -> Unit }
        } as PlatformServices
        ui.setContent {
            val scope = rememberCoroutineScope()
            val features = remember { MusicFeatureController(platform, scope).apply {
                settings = MusicFeatureSettings(translationLanguage = " 中文 ", lyricDisplay = LyricDisplay.Original)
                player = PlayerState(currentTrack = track)
                lyrics = LyricsUiState.Ready(original)
            } }
            ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                CompositionLocalProvider(LocalMusicFeatures provides features) {
                    Box(Modifier.fillMaxSize()) {
                        LyricsContent(track, 0f, lyrics = features.translatedLyrics(original) ?: original, onSeek = {})
                    }
                }
            }
        }
        ui.onNodeWithTag("lyric-tools").performClick()
    }

    @Test fun clickingTranslateShowsBilingualLyricsFromOriginalOnlyMode() {
        show()
        ui.onNodeWithText("翻译为中文").performClick()
        ui.onNodeWithText("你好").assertIsDisplayed()
        ui.onNodeWithText("Hello").assertIsDisplayed()
        ui.onNodeWithText("重译").assertIsDisplayed()
    }

    @Test fun failedRequestShowsErrorBesideControlsAndAllowsRetry() {
        fail = true
        show()
        ui.onNodeWithText("翻译为中文").performClick()
        ui.onNodeWithTag("lyrics-translation-error").assertIsDisplayed().assertTextContains("请先配置音乐助手模型与 API Key", substring = true)
        ui.onNodeWithText("Hello").assertIsDisplayed()
        ui.runOnIdle { fail = false }
        ui.onNodeWithText("翻译为中文").assertIsEnabled().performClick()
        ui.onNodeWithText("你好").assertIsDisplayed()
        ui.onNodeWithTag("lyrics-translation-error").assertDoesNotExist()
    }
}
