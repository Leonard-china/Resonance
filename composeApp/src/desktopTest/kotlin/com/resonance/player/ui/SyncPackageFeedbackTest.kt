package com.resonance.player.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import com.resonance.player.design.*
import com.resonance.player.model.*
import dev.chrisbanes.haze.rememberHazeState
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class SyncPackageFeedbackTest {
    private fun ImageComposeScene.nodes(): List<SemanticsNode> {
        fun all(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::all)
        return semanticsOwners.flatMap { all(it.rootSemanticsNode) }
    }
    private fun ImageComposeScene.node(label: String): SemanticsNode = nodes().first {
        it.config.getOrNull(SemanticsProperties.Text)?.any { t -> t.text == label } == true
    }
    private fun ImageComposeScene.click(label: String) {
        assertTrue(assertNotNull(node(label).config.getOrNull(SemanticsActions.OnClick)?.action).invoke())
    }

    @Test fun completedExportShowsActualLocationAndLongFilenameWithReachableActions() {
        SwingUtilities.invokeAndWait {
            val name = "Resonance-Sync-1793797988443-${"长文件名".repeat(12)}.resonance"
            for (theme in listOf(ThemeMode.Light, ThemeMode.Dark)) {
                for ((height, scale) in listOf(844 to 1f, 640 to 1.5f)) {
                    var dismissed = false
                    val scene = ImageComposeScene(390, height, density = Density(1f, scale)) {
                        CompositionLocalProvider(LocalGlassState provides rememberHazeState()) {
                            ResonanceTheme(theme, reducedMotion = true) {
                                SyncPackageResultDialog(SyncPackageOutcome(true, SyncReport(true, "完成",
                                    exportedPackage = ExportedPackage(name, "内部存储 / Download / Resonance", "打开所在目录"))),
                                    { dismissed = true }, { false })
                            }
                        }
                    }
                    try {
                        scene.render(0).close()
                        scene.node("歌单导出完成")
                        scene.node("内部存储 / Download / Resonance")
                        scene.node(name)
                        for (label in listOf("打开所在目录", "完成")) {
                            val bounds = scene.node(label).touchBoundsInRoot
                            assertTrue(bounds.height > 0 && bounds.top >= 0 && bounds.bottom <= height)
                        }
                        val folder = Path.of("build/export-feedback/screenshots")
                        Files.createDirectories(folder)
                        scene.render(100_000_000).use { image ->
                            assertNotNull(image.encodeToData()).use {
                                Files.write(folder.resolve("${theme.name}-$height-$scale.png"), it.bytes)
                            }
                        }
                        scene.click("完成")
                        assertTrue(dismissed)
                    } finally { scene.close() }
                }
            }
        }
    }

    @Test fun failureNeverDisplaysSuccessOrLocationActions() {
        SwingUtilities.invokeAndWait {
            val scene = ImageComposeScene(390, 640) {
                ResonanceTheme(reducedMotion = true) {
                    SyncPackageResultDialog(SyncPackageOutcome(true, SyncReport(false, "导出失败：存储空间不足")), {},
                        { error("Failed export must not open a folder") })
                }
            }
            try {
                scene.render(0).close()
                scene.node("歌单未能导出")
                scene.node("导出失败：存储空间不足")
                assertTrue(scene.nodes().none {
                    it.config.getOrNull(SemanticsProperties.Text)?.any { t -> t.text in listOf("打开所在目录", "歌单导出完成", "复制保存位置") } == true
                })
            } finally { scene.close() }
        }
    }

    @Test fun progressCanBeDismissedAndBusyScreenCannotStartAnotherArchive() {
        SwingUtilities.invokeAndWait {
            var dismissed = false
            var exports = 0
            val scene = ImageComposeScene(390, 844) {
                ResonanceTheme(reducedMotion = true) {
                    SyncScreen(true, {}, { exports++ }, {}, "正在导出歌单文件，请稍候…", operationInProgress = true)
                    SyncPackageProgressDialog(true) { dismissed = true }
                }
            }
            try {
                scene.render(0).close()
                scene.node("正在导出歌单")
                assertNull(scene.node("导出歌单").config.getOrNull(SemanticsActions.OnClick))
                scene.click("继续使用应用")
                assertTrue(dismissed)
                assertEquals(0, exports)
            } finally { scene.close() }
        }
    }
}
