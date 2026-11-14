package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import com.resonance.player.design.*
import com.resonance.player.model.*
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class BatchEnrichDialogTest {
    private val tracks = List(4) { Track("enrich-$it", "补全歌曲 $it", "测试歌手", "测试专辑", "4:01", it) }

    private fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
    private fun ImageComposeScene.nodes() = semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
    private fun SemanticsNode.hasText(text: String) = config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true
    private fun ImageComposeScene.click(text: String) {
        val node = nodes().first { it.hasText(text) }
        assertTrue(assertNotNull(node.config.getOrNull(SemanticsActions.OnClick)?.action).invoke())
    }

    private fun modifiers(root: SemanticsNode): List<String> = descendants(root)
        .flatMap { node -> generateSequence(node.layoutInfo) { it.parentInfo }.toList() }
        .distinct()
        .flatMap { layout -> layout.getModifierInfo().map { it.modifier.javaClass.simpleName } }

    private fun ImageComposeScene.assertWindowIsolation() {
        val host = semanticsOwners.single { descendants(it.rootSemanticsNode).any { node -> node.hasText("宿主操作") } }
        assertTrue(modifiers(host.rootSemanticsNode).any { it == "HazeEffectNodeElement" }, "Host glass must remain enabled")
        val dialogs = semanticsOwners.filter { it !== host }
        assertTrue(dialogs.isNotEmpty(), "A real dialog must be mounted")
        for (dialog in dialogs) {
            assertFalse(modifiers(dialog.rootSemanticsNode).any { it == "HazeEffectNodeElement" },
                "A separate dialog window must not attach effects to the host Haze source")
        }
    }

    @Test fun configProgressAndCompletionKeepGlassInsideTheHostWindow() {
        SwingUtilities.invokeAndWait {
            for (theme in listOf(ThemeMode.Light, ThemeMode.Dark)) {
                val stage = mutableIntStateOf(0)
                var hostLocalPreserved = false
                val scene = ImageComposeScene(500, 1100) {
                    val glass = rememberHazeState()
                    CompositionLocalProvider(LocalGlassState provides glass) {
                        ResonanceTheme(theme, reducedMotion = true) {
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.fillMaxSize().background(ResonanceColors.Canvas).hazeSource(glass))
                                TextButton(onClick = {}, modifier = Modifier.resonanceGlass()) { Text("宿主操作") }
                                BatchEnrichDialog(tracks, stage.intValue == 1,
                                    if (stage.intValue == 1) BatchEnrichProgress(1, 4, tracks.first(), "正在查找歌词") else null,
                                    if (stage.intValue == 2) BatchEnrichReport(4, 0, 4, 0, 0, tracks, "四首已处理") else null,
                                    true, {}, {}, {})
                                hostLocalPreserved = LocalGlassState.current === glass
                            }
                        }
                    }
                }
                try {
                    for (state in 0..2) {
                        stage.intValue = state
                        scene.render((state + 1) * 100_000_000L).close()
                        scene.assertWindowIsolation()
                        assertTrue(hostLocalPreserved)
                    }
                } finally { scene.close() }
            }
        }
    }

    @Test fun startProgressStopCompleteAndReopenLeaveHostResponsive() {
        SwingUtilities.invokeAndWait {
            var starts = 0
            var stops = 0
            var hostClicks = 0
            var options: BatchEnrichOptions? = null
            val running = mutableStateOf(false)
            val visible = mutableStateOf(true)
            val progress = mutableStateOf<BatchEnrichProgress?>(null)
            val report = mutableStateOf<BatchEnrichReport?>(null)
            val scene = ImageComposeScene(500, 1100) {
                val glass = rememberHazeState()
                CompositionLocalProvider(LocalGlassState provides glass) {
                    ResonanceTheme(reducedMotion = true) {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.fillMaxSize().background(ResonanceColors.Canvas).hazeSource(glass))
                            TextButton(onClick = { hostClicks++ }, modifier = Modifier.resonanceGlass()) { Text("宿主操作") }
                            if (visible.value) BatchEnrichDialog(tracks, running.value, progress.value, report.value,
                                false, { visible.value = false }, { starts++; options = it; running.value = true },
                                { stops++; running.value = false; progress.value = null })
                        }
                    }
                }
            }
            var frame = 0L
            fun render() { frame += 100_000_000L; scene.render(frame).close() }
            try {
                render()
                assertEquals(0, starts)
                repeat(3) {
                    scene.click("开始补全"); render()
                    assertEquals(it + 1, starts)
                    assertEquals(BatchEnrichOptions(useAiFallback = false), options)
                    for (index in 1..4) {
                        progress.value = BatchEnrichProgress(index, 4, tracks[index - 1], "正在查找歌词 $index")
                        render()
                        assertTrue(scene.nodes().any { it.hasText("正在查找歌词 $index") })
                        scene.assertWindowIsolation()
                    }
                    scene.click("停止并保留已处理"); render()
                    assertEquals(it + 1, stops)
                }
                scene.click("开始补全"); render()
                running.value = false
                report.value = BatchEnrichReport(4, 0, 4, 0, 0, tracks, "四首已处理")
                render()
                scene.assertWindowIsolation()
                scene.click("完成"); render()
                scene.click("宿主操作"); render()
                assertEquals(1, hostClicks)
                report.value = null; visible.value = true; render()
                scene.click("取消"); render()
                scene.click("宿主操作"); render()
                assertEquals(2, hostClicks)
                assertEquals(4, starts)
            } finally { scene.close() }
        }
    }
}
