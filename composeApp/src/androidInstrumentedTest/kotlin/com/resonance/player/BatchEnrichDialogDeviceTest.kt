package com.resonance.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.resonance.player.design.*
import com.resonance.player.model.*
import com.resonance.player.ui.BatchEnrichDialog
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Native dialog transitions with a live host Haze source; no user music or network requests. */
class BatchEnrichDialogDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()

    @Test(timeout = 90_000) fun startStopCompleteAndReopenKeepHostResponsive() {
        val tracks = List(4) { Track("enrich-$it", "补全歌曲 $it", "测试歌手", "测试专辑", "4:01", it) }
        val visible = mutableStateOf(false)
        val running = mutableStateOf(false)
        val progress = mutableStateOf<BatchEnrichProgress?>(null)
        val report = mutableStateOf<BatchEnrichReport?>(null)
        val theme = mutableStateOf(ThemeMode.Light)
        var starts = 0
        var stops = 0
        var hostClicks = 0
        ui.setContent {
            val glass = rememberHazeState()
            CompositionLocalProvider(LocalGlassState provides glass) {
                ResonanceTheme(theme.value, reducedMotion = true) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().background(ResonanceColors.Canvas).hazeSource(glass))
                        TextButton(onClick = {
                            hostClicks++; visible.value = true; running.value = false; progress.value = null; report.value = null
                        }, modifier = Modifier.resonanceGlass()) { Text("打开补全") }
                        if (visible.value) BatchEnrichDialog(tracks, running.value, progress.value, report.value,
                            false, { visible.value = false }, {
                                assertEquals(BatchEnrichOptions(useAiFallback = false), it)
                                starts++; running.value = true
                            }, { stops++; running.value = false; progress.value = null })
                    }
                }
            }
        }
        for (mode in listOf(ThemeMode.Light, ThemeMode.Dark)) {
            ui.runOnIdle { theme.value = mode }
            ui.onNodeWithText("打开补全").performClick()
            repeat(2) {
                ui.onNodeWithText("开始补全").performClick()
                for (index in 1..4) {
                    ui.runOnIdle { progress.value = BatchEnrichProgress(index, 4, tracks[index - 1], "正在查找歌词 $index") }
                    ui.onNodeWithText("正在查找歌词 $index").assertIsDisplayed()
                }
                ui.onNodeWithText("停止并保留已处理").performClick()
                ui.onNodeWithText("开始补全").assertIsDisplayed()
            }
            ui.onNodeWithText("开始补全").performClick()
            ui.runOnIdle {
                report.value = BatchEnrichReport(4, 0, 4, 0, 0, tracks, "四首已处理")
                running.value = false
            }
            ui.onNodeWithText("补全处理完成").assertIsDisplayed()
            ui.onNodeWithText("完成").performClick()
            ui.onNodeWithText("打开补全").performClick()
            ui.onNodeWithText("取消").performClick()
        }
        ui.runOnIdle { assertEquals(6, starts); assertEquals(4, stops); assertEquals(4, hostClicks) }
    }
}
