package com.resonance.player.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import com.resonance.player.design.LocalGlassState
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.Playlist
import com.resonance.player.model.ThemeMode
import dev.chrisbanes.haze.rememberHazeState
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class SyncPackageDialogTest {
    private fun ImageComposeScene.nodes(): List<SemanticsNode> {
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
    }

    private fun ImageComposeScene.node(label: String): SemanticsNode = nodes().first {
        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true
    }

    private fun ImageComposeScene.click(label: String) {
        assertTrue(assertNotNull(node(label).config.getOrNull(SemanticsActions.OnClick)?.action).invoke())
    }

    @Test fun openingLargeCatalogOnlyComposesVisiblePlaylistOptions() {
        SwingUtilities.invokeAndWait {
            val playlists = List(300) { Playlist("playlist-$it", "导出测试歌单 $it", "0 首", emptyList(), it) }
            val scene = ImageComposeScene(390, 844) {
                CompositionLocalProvider(LocalGlassState provides rememberHazeState()) {
                    ResonanceTheme(reducedMotion = true) {
                        SyncPackageDialog(true, playlists, {}, { error("Opening must not export") })
                    }
                }
            }
            try {
                val started = System.nanoTime()
                scene.render(0).close()
                val options = scene.nodes().count {
                    it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text.startsWith("导出测试歌单 ") } == true
                }
                println("Export dialog first render: ${(System.nanoTime() - started) / 1_000_000} ms; composed playlist options: $options / ${playlists.size}")
                assertTrue(options in 1..30, "Opening must compose a bounded viewport, found $options playlist options")
            } finally { scene.close() }
        }
    }

    @Test fun lastPlaylistRemainsSelectableAndActionsFitShortLargeTextWindows() {
        SwingUtilities.invokeAndWait {
            val playlists = List(1000) { Playlist("playlist-$it", "测试歌单 $it", "0 首", emptyList(), it) }
            for (mode in listOf(ThemeMode.Light, ThemeMode.Dark)) {
                for ((width, height, scale) in listOf(Triple(390, 844, 1f), Triple(360, 640, 1.5f), Triple(720, 360, 1.5f))) {
                    val confirmed = mutableListOf<String?>()
                    var dismissed = 0
                    val scene = ImageComposeScene(width, height, density = Density(1f, scale)) {
                        ResonanceTheme(mode, reducedMotion = true) {
                            SyncPackageDialog(true, playlists, { dismissed++ }, { confirmed.add(it) })
                        }
                    }
                    try {
                        scene.render(0).close()
                        assertTrue(confirmed.isEmpty())
                        for (label in listOf("导出", "取消")) {
                            val bounds = scene.node(label).touchBoundsInRoot
                            assertTrue(bounds.top >= 0 && bounds.bottom <= height && bounds.height > 0,
                                "$label must fit $width x $height at $scale: $bounds")
                        }
                        val list = scene.nodes().single { it.config.getOrNull(SemanticsActions.ScrollToIndex) != null }
                        assertTrue(assertNotNull(list.config.getOrNull(SemanticsActions.ScrollToIndex)?.action).invoke(1001))
                        repeat(4) { scene.render(500_000_000L + it * 500_000_000L).close() }
                        scene.click("测试歌单 999")
                        scene.render(3_000_000_000).close()
                        assertTrue(confirmed.isEmpty(), "Selecting a scope must not start exporting")
                        scene.click("导出")
                        assertEquals(listOf<String?>("playlist-999"), confirmed)
                        scene.click("取消")
                        assertEquals(1, dismissed)
                    } finally { scene.close() }
                }
            }
        }
    }

    @Test fun emptyLibraryAndImportPreserveNullScopeAndDoNoWorkBeforeConfirmation() {
        SwingUtilities.invokeAndWait {
            for (exporting in listOf(true, false)) {
                val confirmed = mutableListOf<String?>()
                val scene = ImageComposeScene(390, 640) {
                    ResonanceTheme(reducedMotion = true) {
                        SyncPackageDialog(exporting, emptyList(), {}, { confirmed.add(it) })
                    }
                }
                try {
                    scene.render(0).close()
                    assertTrue(confirmed.isEmpty())
                    if (exporting) scene.node("整个音乐库")
                    scene.click(if (exporting) "导出" else "选择歌单文件")
                    assertEquals(listOf<String?>(null), confirmed)
                } finally { scene.close() }
            }
        }
    }
}
