package com.resonance.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.resonance.player.design.*
import com.resonance.player.model.Playlist
import com.resonance.player.model.ThemeMode
import com.resonance.player.ui.SyncPackageDialog
import com.resonance.player.ui.SyncScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Real Android host/source and native dialog windows, using synthetic playlists only. */
class SyncPackageDialogDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()

    @Test(timeout = 90_000) fun exportScopesScrollConfirmCancelAndReopenWithoutFreezingHost() {
        val playlists = List(1000) { Playlist("playlist-$it", "导出测试歌单 $it", "0 首", emptyList(), it) }
        val confirmations = mutableListOf<String?>()
        val mode = mutableStateOf(ThemeMode.Light)
        ui.setContent {
            val glass = rememberHazeState()
            var exporting by remember { mutableStateOf<Boolean?>(null) }
            CompositionLocalProvider(LocalGlassState provides glass) {
                ResonanceTheme(mode.value, reducedMotion = true) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.matchParentSize().background(ResonanceColors.Canvas).hazeSource(glass))
                        SyncScreen(compact = true, onBack = {}, message = null,
                            onExportSync = { exporting = true }, onImportSync = { exporting = false })
                        exporting?.let { export ->
                            SyncPackageDialog(export, playlists, { exporting = null }, {
                                confirmations.add(it)
                                exporting = null
                            })
                        }
                    }
                }
            }
        }
        for (theme in listOf(ThemeMode.Light, ThemeMode.Dark)) {
            ui.runOnIdle { mode.value = theme }
            repeat(3) {
                ui.onNodeWithText("导出歌单").performClick()
                ui.onNodeWithText("导出歌单文件").assertIsDisplayed()
                ui.onNodeWithText("整个音乐库").assertIsSelected()
                ui.onNodeWithText("取消").performClick()
                ui.onNodeWithText("导出歌单文件").assertDoesNotExist()
                ui.onNodeWithText("导出歌单").assertIsDisplayed()
            }
        }
        ui.runOnIdle { assertEquals(emptyList<String?>(), confirmations) }
        ui.onNodeWithText("导出歌单").performClick()
        ui.onNode(hasScrollToIndexAction()).performScrollToIndex(1001)
        ui.onNodeWithText("导出测试歌单 999").performClick().assertIsSelected()
        ui.runOnIdle { assertEquals(emptyList<String?>(), confirmations) }
        ui.onNodeWithText("导出").performClick()
        ui.runOnIdle { assertEquals(listOf("playlist-999"), confirmations) }
        ui.onNodeWithText("导出歌单").performClick()
        ui.onNodeWithText("整个音乐库").assertIsSelected()
        ui.onNodeWithText("导出").performClick()
        ui.runOnIdle { assertEquals(listOf("playlist-999", null), confirmations) }
        ui.onNodeWithText("导入歌单").performClick()
        ui.onNodeWithText("选择歌单文件").performClick()
        ui.runOnIdle { assertEquals(listOf("playlist-999", null, null), confirmations) }
        ui.onNodeWithText("导出歌单").assertIsDisplayed()
    }
}
