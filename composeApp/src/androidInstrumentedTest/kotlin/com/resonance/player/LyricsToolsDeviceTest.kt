package com.resonance.player

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.ui.LyricsContent
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class LyricsToolsDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    @Test fun calibrationDialogScrollsAndRemainsAboveCurrentLyrics() {
        var offset = 0L
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                ResonanceTheme(ThemeMode.Dark, reducedMotion = true) {
                    Box(Modifier.width(350.dp).height(320.dp)) {
                        val track = Track("fixture", "Generated", "", "", "4:00", 0)
                        LyricsContent(track, .4f, 80000L, Lyrics(track.id,
                            (0..25).map { LyricLine(it * 10000L, "原创测试歌词 $it") }, true, false, "fixture"),
                            onSeek = {}, onAdjustOffset = { offset += it })
                    }
                }
            }
        }
        ui.onNodeWithTag("lyric-tools").performClick()
        ui.onNodeWithText("关闭").assertIsDisplayed()
        ui.onNodeWithText("歌词落后了 · 提前 0.5 秒").performScrollTo().performClick()
        assertEquals(500L, offset)
        ui.onNodeWithText("关闭").performClick()
        ui.onNodeWithTag("player-lyrics-list").assertExists()
    }
}
