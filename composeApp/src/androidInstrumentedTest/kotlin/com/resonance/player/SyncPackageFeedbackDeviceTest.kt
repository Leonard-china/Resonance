package com.resonance.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.resonance.player.design.*
import com.resonance.player.model.*
import com.resonance.player.ui.*
import com.resonance.player.platform.exportDirectoryUri
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SyncPackageFeedbackDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()

    @Test(timeout = 60_000) fun completionKeepsLocationVisibleWhenFileManagerCannotOpen() {
        val calls = mutableListOf<String>()
        ui.setContent {
            val glass = rememberHazeState()
            var visible by remember { mutableStateOf(true) }
            CompositionLocalProvider(LocalGlassState provides glass) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.matchParentSize().background(ResonanceColors.Canvas).hazeSource(glass))
                        androidx.compose.material3.Text("导出页面")
                        if (visible) SyncPackageResultDialog(
                            SyncPackageOutcome(true, SyncReport(true, "完成", exportedPackage =
                                ExportedPackage("轻快活力.resonance", "内部存储 / Download / Resonance", "打开所在目录", exportDirectoryUri().toString()))),
                            { visible = false }, { calls.add(it); false })
                    }
                }
            }
        }
        ui.onNodeWithText("歌单导出完成").assertIsDisplayed()
        ui.onNodeWithText("轻快活力.resonance").assertIsDisplayed()
        ui.onNodeWithText("打开所在目录").performClick()
        ui.onNodeWithText("无法打开文件管理器，请按上方保存位置手动查找。").assertIsDisplayed()
        ui.runOnIdle { assertEquals(listOf(exportDirectoryUri().toString()), calls) }
        ui.onNodeWithText("复制保存位置").performClick()
        ui.onNodeWithText("已复制保存位置").assertIsDisplayed()
        ui.onNodeWithText("完成").performClick()
        ui.onNodeWithText("歌单导出完成").assertDoesNotExist()
        ui.onNodeWithText("导出页面").assertIsDisplayed()
    }
}
