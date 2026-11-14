package com.resonance.player.ui

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class TrackPlaylistDialogTest {
    private val track = Track("song", "原创歌曲", "原创歌手", "", "3:00", 0)
    private fun descendants(n: SemanticsNode): List<SemanticsNode> = listOf(n) + n.children.flatMap(::descendants)
    private fun ImageComposeScene.nodes() = semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
    private fun SemanticsNode.hasText(text: String) = config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true
    private fun ImageComposeScene.click(text: String) {
        assertTrue(assertNotNull(nodes().first { it.hasText(text) }.config.getOrNull(SemanticsActions.OnClick)?.action).invoke())
    }
    private fun ImageComposeScene.input(text: String) {
        val field = nodes().single { it.config.getOrNull(SemanticsActions.SetText) != null }
        assertTrue(assertNotNull(field.config.getOrNull(SemanticsActions.SetText)?.action).invoke(AnnotatedString(text)))
    }
    private fun ImageComposeScene.scroll(index: Int, time: Long) {
        val list = nodes().single { it.config.getOrNull(SemanticsActions.ScrollToIndex) != null }
        assertTrue(assertNotNull(list.config.getOrNull(SemanticsActions.ScrollToIndex)?.action).invoke(index))
        repeat(4) { render(time + it * 100_000_000L).close() }
    }

    @Test fun searchingKeepsHiddenSelectionsAndOnlyCommitsAfterConfirmation() {
        SwingUtilities.invokeAndWait {
            val changes = mutableListOf<Pair<String, Boolean>>()
            val playlists = List(20) { Playlist("p$it", "歌单 $it", "", if (it == 1) listOf(track) else emptyList(), it) }
            val scene = ImageComposeScene(390, 844) { ResonanceTheme(reducedMotion = true) {
                TrackPlaylistDialog(track, playlists, {}, { _, id, include -> changes.add(id to include) })
            } }
            try {
                scene.render(0).close()
                scene.scroll(1, 100_000_000); scene.click("歌单 0")
                scene.scroll(0, 600_000_000); scene.input("19"); scene.render(1_100_000_000).close()
                scene.scroll(1, 1_200_000_000); scene.click("歌单 19")
                assertTrue(changes.isEmpty())
                scene.click("完成")
                assertEquals(listOf("p0" to true, "p19" to true), changes)
            } finally { scene.close() }
        }
    }

    @Test fun emptyLibraryCanCreateAndAddWithoutLeavingThePickerAndCancelDoesNotCreate() {
        SwingUtilities.invokeAndWait {
            val created = mutableListOf<String>()
            val visible = mutableStateOf(true)
            val scene = ImageComposeScene(390, 844) { ResonanceTheme(reducedMotion = true) {
                if (visible.value) TrackPlaylistDialog(track, emptyList(), { visible.value = false },
                    { _, _, _ -> error("No existing membership should change") }, { created.add(it) })
            } }
            try {
                scene.render(0).close(); scene.click("新建歌单"); scene.render(100_000_000).close()
                assertTrue(scene.nodes().first { it.hasText("完成") }.config.contains(SemanticsProperties.Disabled))
                scene.input("  晚间收藏  "); scene.render(200_000_000).close()
                scene.click("取消"); scene.render(300_000_000).close(); assertTrue(created.isEmpty())
                visible.value = true; scene.render(400_000_000).close()
                scene.click("新建歌单"); scene.render(500_000_000).close()
                scene.input("  晚间收藏  "); scene.render(600_000_000).close()
                scene.click("完成"); assertEquals(listOf("晚间收藏"), created)
            } finally { scene.close() }
        }
    }

    @Test fun creatingAtLargeTextKeepsConfirmationReachableInShortWindows() {
        SwingUtilities.invokeAndWait {
            for ((width, height) in listOf(360 to 640, 720 to 360)) {
                val scene = ImageComposeScene(width, height, density = Density(1f, 1.5f)) { ResonanceTheme(reducedMotion = true) {
                    TrackPlaylistDialog(track, emptyList(), {}, { _, _, _ -> }, {})
                } }
                try {
                    scene.render(0).close(); scene.click("新建歌单"); scene.render(100_000_000).close()
                    for (label in listOf("取消", "完成")) {
                        val bounds = scene.nodes().first { it.hasText(label) }.touchBoundsInRoot
                        assertTrue(bounds.top >= 0 && bounds.bottom <= height && bounds.height > 0, "$label clipped: $bounds")
                    }
                } finally { scene.close() }
            }
        }
    }
}
