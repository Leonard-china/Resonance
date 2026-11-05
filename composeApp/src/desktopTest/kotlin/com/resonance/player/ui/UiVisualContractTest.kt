package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.*
import com.resonance.player.data.PreviewLibrary
import com.resonance.player.design.*
import com.resonance.player.model.*
import com.resonance.player.platform.PlatformServices
import com.resonance.player.platform.MusicAssistantServices
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.reflect.Proxy
import com.resonance.player.ui.common.SoftNeumorphicTokens
import com.resonance.player.ui.common.SoftRefinedPlaybackScrubber
import com.resonance.player.ui.common.SoftRefinedVolumeSlider
import com.resonance.player.ui.common.SoftMiniSkipButton
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.test.*

/** Actual native rendering with fixture data; no platform services, network or user files. */
@OptIn(ExperimentalComposeUiApi::class)
class UiVisualContractTest {
    private val player = PlayerState(currentTrack = PreviewLibrary.tracks.first(), progress = 0.38f)
    private val stageLyrics = LyricsUiState.Ready(Lyrics(
        trackId = player.currentTrack!!.id,
        lines = listOf(LyricLine(0, "把忙碌留在窗外"), LyricLine(80_000, "把此刻交给旋律", "Let this moment belong to music"),
            LyricLine(180_000, "让夜色慢慢流淌")),
        synchronized = true, instrumental = false, source = "原创测试文本",
    ))

    private fun <T> ImageComposeScene.rendering(block: (ImageComposeScene) -> T): T =
        try { block(this) } finally { close() }

    private fun ImageComposeScene.node(label: String): SemanticsNode {
        fun find(node: SemanticsNode): SemanticsNode? {
            if (node.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true ||
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == label } == true) return node
            return node.children.firstNotNullOfOrNull { find(it) }
        }
        return semanticsOwners.firstNotNullOfOrNull { find(it.rootSemanticsNode) }
            ?: error("Missing semantic node: $label")
    }

    @Composable
    private fun FixturePlayer(state: PlayerState = player) {
        NowPlayingOverlay(playerState = state, queue = PreviewLibrary.tracks,
            lyricsState = LyricsUiState.Idle, onDismiss = {}, onTogglePlay = {}, onPrevious = {}, onNext = {},
            onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {}, onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {})
    }

    @Test fun galleryPlayerKeepsTransportReachableWithLargeTextInShortWindow() {
        SwingUtilities.invokeAndWait {
            ImageComposeScene(360, 640, density = Density(1f, 1.5f)) {
                ResonanceTheme(reducedMotion = true) { FixturePlayer() }
            }.rendering { scene ->
                scene.render(0).close()
                scene.render(500_000_000).close()
                for (label in listOf("关闭正在播放", "播放选项", "播放进度", "播放", "上一首", "下一首")) {
                    val bounds = scene.node(label).touchBoundsInRoot
                    assertTrue(bounds.height >= 48f && bounds.width >= 48f, "$label: $bounds")
                    assertTrue(bounds.top >= 0 && bounds.bottom <= 640 && bounds.left >= 0 && bounds.right <= 360, "$label leaves viewport: $bounds")
                }
                assertNotNull(scene.node("封面").config.getOrNull(SemanticsProperties.Selected))
                assertNotNull(scene.node("播放选项").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(600_000_000).close()
                scene.render(900_000_000).close()
                assertNotNull(scene.node("播放设置 · 音量与速度").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(1_000_000_000).close()
                scene.render(1_400_000_000).close()
                assertNotNull(scene.node("音量").config.getOrNull(SemanticsActions.SetProgress)?.action)
                assertNotNull(scene.node("1.0x").config.getOrNull(SemanticsActions.OnClick)?.action)
            }
        }
    }

    @Test fun playbackErrorOffersRetryInBothThemes() {
        SwingUtilities.invokeAndWait {
            for (mode in listOf(ThemeMode.Light, ThemeMode.Dark)) {
                var retried = false
                ImageComposeScene(800, 500) {
                    ResonanceTheme(mode, reducedMotion = true) {
                        PlaybackErrorDialog("测试音频无法读取，请检查文件是否仍然存在。", {}, { retried = true })
                    }
                }.rendering { scene ->
                    scene.render(0).close()
                    scene.render(500_000_000).use { bitmap ->
                        val path = Path.of("build/ui-gallery/screenshots/playback-error-${mode.name.lowercase()}.png")
                        Files.createDirectories(path.parent)
                        assertNotNull(bitmap.encodeToData()).use { Files.write(path, it.bytes) }
                    }
                    assertNotNull(scene.node("重试播放").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                    assertTrue(retried)
                }
            }
        }
    }

    @Test fun bufferingAndLoadingStatesRenderDistinctVisualFeedbackInDarkAndLight() {
        SwingUtilities.invokeAndWait {
            for (mode in listOf(ThemeMode.Dark, ThemeMode.Light)) {
                val bufferingPlayer = player.copy(isBuffering = true)
                ImageComposeScene(1280, 800) {
                    ResonanceTheme(mode, reducedMotion = true) {
                        FixturePlayer(bufferingPlayer)
                    }
                }.rendering { scene ->
                    scene.render(0).use { bitmap ->
                        val path = Path.of("build/ui-gallery/screenshots/buffering-feedback-${mode.name.lowercase()}.png")
                        Files.createDirectories(path.parent)
                        assertNotNull(bitmap.encodeToData()).use { Files.write(path, it.bytes) }
                    }
                    assertNotNull(scene.node("正在缓冲…"))
                    assertNotNull(scene.node("加载中"))
                }
            }
        }
    }

    @Test fun draggingProgressCommitsOnlyOnceOnRelease() {
        SwingUtilities.invokeAndWait {
            for (flat in listOf(false, true)) {
            val seeks = mutableListOf<Float>()
            ImageComposeScene(400, 100) {
                ResonanceTheme(reducedMotion = true) {
                    SoftRefinedPlaybackScrubber(.2f, "4:00", { seeks += it }, { _, _ -> "0:48" }, flat = flat)
                }
            }.rendering { scene ->
                scene.render(0).close()
                val bounds = scene.node("播放进度").boundsInRoot
                val y = bounds.center.y
                fun event(type: androidx.compose.ui.input.pointer.PointerEventType, x: Float, time: Long) {
                    scene.sendPointerEvent(type, androidx.compose.ui.geometry.Offset(x, y), timeMillis = time)
                    scene.render(time * 1_000_000).close()
                }
                event(androidx.compose.ui.input.pointer.PointerEventType.Press, 85f, 100)
                for (i in 1..12) event(androidx.compose.ui.input.pointer.PointerEventType.Move, 85f + i * 20, 100L + i * 20)
                assertTrue(seeks.isEmpty(), "Preview drag must not repeatedly restart audio")
                event(androidx.compose.ui.input.pointer.PointerEventType.Release, 325f, 400)
                assertEquals(1, seeks.size)
                assertTrue(seeks.single() > .7f)
            }
            }
        }
    }

    @Test fun songInformationRemainsReachableFromPlayerMenu() {
        SwingUtilities.invokeAndWait {
            for (mode in listOf(ThemeMode.Light, ThemeMode.Dark)) {
            ImageComposeScene(360, 800) {
                ResonanceTheme(mode, reducedMotion = true) { FixturePlayer() }
            }.rendering { scene ->
                var time = 0L
                fun settle() { repeat(40) { time += 16_666_667; scene.render(time).close() } }
                fun capture(name: String) {
                    settle()
                    val output = Path.of("build/am-reference-ui/dialogs/$name-${mode.name}.png")
                    Files.createDirectories(output.parent)
                    scene.render(time).use { bitmap ->
                        assertNotNull(bitmap.encodeToData()).use { Files.write(output, it.bytes) }
                    }
                }
                settle()
                assertNotNull(scene.node("播放选项").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                capture("playback-menu")
                assertNotNull(scene.node("歌曲与来源").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                capture("track-info")
                assertNotNull(scene.node("关闭").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
            }
            }
        }
    }

    @Test fun wideStageKeepsLyricsQueueAndTransportConnected() {
        SwingUtilities.invokeAndWait {
            var selected: Track? = null
            var seek = 0f
            ImageComposeScene(1280, 800) {
                ResonanceTheme(reducedMotion = true) {
                    NowPlayingOverlay(playerState = player, queue = PreviewLibrary.tracks,
                        lyricsState = stageLyrics, onDismiss = {}, onTogglePlay = {}, onPrevious = {}, onNext = {},
                        onToggleShuffle = {}, onCycleRepeat = {}, onSeek = { seek = it },
                        onTrackSelected = { selected = it }, onToggleFavorite = {}, onRefreshLyrics = {})
                }
            }.rendering { scene ->
                scene.render(0).close()
                scene.render(500_000_000).close()
                for (label in listOf("播放进度", "播放", "上一首", "下一首", "把此刻交给旋律")) {
                    val bounds = scene.node(label).touchBoundsInRoot
                    assertTrue(bounds.left >= 0 && bounds.right <= 1280 && bounds.top >= 0 && bounds.bottom <= 800,
                        "$label leaves stage: $bounds")
                }
                assertNotNull(scene.node("播放进度").config.getOrNull(SemanticsActions.SetProgress)?.action).invoke(.6f)
                assertEquals(.6f, seek, .001f)
                assertNotNull(scene.node("歌词").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(600_000_000).close()
                scene.render(1_000_000_000).close()
                assertEquals(true, scene.node("歌词").config.getOrNull(SemanticsProperties.Selected))
                assertNotNull(scene.node("队列").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(1_100_000_000).close()
                scene.render(1_500_000_000).close()
                assertNotNull(scene.node(PreviewLibrary.tracks[1].title).config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                assertEquals(PreviewLibrary.tracks[1].id, selected?.id)
            }
        }
    }

    @Test fun stageBackdropRemainsStableAcrossPlaybackAndAccessibilityStates() {
        SwingUtilities.invokeAndWait {
            val playing = mutableStateOf(true)
            val foreground = mutableStateOf(true)
            val reduced = mutableStateOf(false)
            ImageComposeScene(360, 300) {
                ResonanceTheme(reducedMotion = reduced.value) {
                    CompositionLocalProvider(LocalAppForeground provides foreground.value) {
                        MusicStageBackdrop(7, playing.value, Modifier.fillMaxSize())
                    }
                }
            }.rendering { scene ->
                fun frame(time: Long): ByteArray = scene.render(time).use { bitmap ->
                    assertNotNull(bitmap.encodeToData()).use { it.bytes }
                }
                frame(0)
                val first = frame(500_000_000)
                assertContentEquals(first, frame(2_000_000_000), "The frosted stage no longer animates decorative particles")
                var time = 2_000_000_000L
                for (stop in listOf<() -> Unit>({ playing.value = false }, { foreground.value = false }, { reduced.value = true })) {
                    playing.value = true; foreground.value = true; reduced.value = false
                    frame(time + 100_000_000)
                    frame(time + 500_000_000)
                    stop()
                    frame(time + 600_000_000)
                    val stopped = frame(time + 900_000_000)
                    assertContentEquals(stopped, frame(time + 2_000_000_000), "Stopped stage must not keep rendering new positions")
                    time += 3_000_000_000L
                }
            }
        }
    }

    @Test fun shortLandscapeKeepsPaneTabsAndTransportInsideViewport() {
        SwingUtilities.invokeAndWait {
            for (scale in listOf(1f, 1.5f)) {
                ImageComposeScene(640, 360, density = Density(1f, scale)) {
                    ResonanceTheme(reducedMotion = true) {
                        FixturePlayer(player.copy(currentTrack = player.currentTrack!!.copy(title = "一首名称很长也不应该挤走播放按钮的测试歌曲")))
                    }
                }.rendering { scene ->
                    scene.render(0).close()
                    scene.render(500_000_000).close()
                    for (label in listOf("封面", "歌词", "队列", "播放进度", "播放", "上一首", "下一首")) {
                        val bounds = scene.node(label).touchBoundsInRoot
                        assertTrue(bounds.width >= 48f && bounds.height >= 48f, "$label: $bounds")
                        assertTrue(bounds.left >= 0 && bounds.right <= 640 && bounds.top >= 0 && bounds.bottom <= 360,
                            "$label leaves landscape: $bounds")
                    }
                }
            }
        }
    }

    @Composable
    private fun FixtureFeatures(conversation: Boolean = false, initialState: MusicAssistantState? = null,
                                notice: String = "", content: @Composable () -> Unit) {
        val services = remember {
            val state = MutableStateFlow(initialState ?: MusicAssistantState(
                messages = if (conversation) listOf(AssistantMessage("user", "推荐几首适合傍晚散步的歌"),
                    AssistantMessage("assistant", "选了三首舒缓的歌，陪你慢慢走。\n\n" + PreviewLibrary.tracks.take(3).joinToString(" · ") {
                        "[${it.title}](${assistantTrackLink(it.id)})"
                    })) else emptyList(),
                recommendations = RecommendationBatch(trackIds = PreviewLibrary.tracks.take(3).map { it.id })))
            val assistant = Proxy.newProxyInstance(MusicAssistantServices::class.java.classLoader,
                arrayOf(MusicAssistantServices::class.java)) { _, method, _ ->
                when (method.name) {
                    "getState" -> state
                    "saveScreenshotImportDraft" -> Unit
                    "feedbackFor" -> null
                    "storageReport" -> MusicStorageReport(entries = PreviewLibrary.tracks.take(2).map {
                        ManagedAudioEntry(it, bytes = 4_000_000, saved = true, playing = it.id == player.currentTrack?.id)
                    }, managedBytes = 8_000_000, savedBytes = 8_000_000, freeBytes = 2_000_000_000)
                    else -> error("Unexpected fixture call: ${method.name}")
                }
            } as MusicAssistantServices
            Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, _ ->
                when (method.name) {
                    "getMusicAssistant" -> assistant
                    "getSupportsWebLogin", "getSupportsKgg", "getSupportsKeyboardShortcuts" -> false
                    "getImportProgress" -> MutableStateFlow<ImportProgress?>(null)
                    "getListeningStats" -> MutableStateFlow(ListeningStatsSnapshot(loaded = true))
                    "isVocalSuppressionEnabled" -> MutableStateFlow(false)
                    "getAllListeningStats" -> AllListeningStats()
                    "getForeground" -> MutableStateFlow(true)
                    "audioFileAvailable" -> true
                    "currentTimeMillis" -> 1_000L
                    else -> error("Unexpected fixture call: ${method.name}")
                }
            } as PlatformServices
        }
        val scope = rememberCoroutineScope()
        val feature = remember { MusicFeatureController(services, scope).apply {
            library = PreviewLibrary.tracks.map { it.copy(sourceUri = "fixture://${it.id}") }; message = notice
        } }
        CompositionLocalProvider(LocalMusicFeatures provides feature, content = content)
    }

    @Test fun nativeSlidersAndTabsExposeWorkingActionsAndMinimumTargets() {
        SwingUtilities.invokeAndWait {
            var seek = 0.2f
            var volume = 0.4f
            val generation = mutableIntStateOf(1)
            var calledGeneration = 0
            var albumSelected = false
            ImageComposeScene(360, 400) {
                var progress by remember { mutableFloatStateOf(seek) }
                var level by remember { mutableFloatStateOf(volume) }
                var selected by remember { mutableStateOf(false) }
                val version = generation.intValue
                ResonanceTheme(reducedMotion = true) {
                    Column {
                        SoftRefinedPlaybackScrubber(progress, "3:00", {
                            seek = it; progress = it; calledGeneration = version
                        }, { _, _ -> "0:36" })
                        SoftRefinedVolumeSlider(level, { volume = it; level = it })
                        ResonanceSelectionTab("专辑", selected, { selected = true; albumSelected = true },
                            Modifier.semantics { contentDescription = "专辑标签" })
                        SoftMiniSkipButton(true, {})
                    }
                }
            }.rendering { scene ->
                scene.render(0).close()
                fun setProgress(label: String, value: Float) {
                    val node = scene.node(label)
                    assertTrue(node.touchBoundsInRoot.height >= 48f, "$label: touch bounds ${node.touchBoundsInRoot}")
                    val action = assertNotNull(node.config.getOrNull(SemanticsActions.SetProgress)?.action)
                    assertTrue(action(value))
                }
                setProgress("播放进度", 0.75f)
                scene.render(100_000_000).close()
                assertEquals(0.75f, seek, 0.001f)
                generation.intValue = 2
                scene.render(200_000_000).close()
                setProgress("播放进度", 0.3f)
                setProgress("音量", 0.6f)
                val tab = scene.node("专辑标签")
                assertTrue(tab.boundsInRoot.height >= 48f)
                assertNotNull(tab.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(300_000_000).close()
                assertTrue(albumSelected)
                assertEquals(true, scene.node("专辑标签").config.getOrNull(SemanticsProperties.Selected))
                assertEquals(2, calledGeneration)
                assertEquals(0.3f, seek, 0.001f)
                assertEquals(0.6f, volume, 0.001f)
                assertTrue(scene.node("下一首").boundsInRoot.height >= 48f)
                assertTrue(scene.node("下一首").boundsInRoot.width >= 48f)
            }
        }
    }

    @Test fun compactAssistantKeepsTitleOnOneLineWithLargeText() {
        SwingUtilities.invokeAndWait {
            for (scale in listOf(1f, 1.5f)) {
                ImageComposeScene(360, 800, density = Density(1f, scale)) {
                    ResonanceTheme(reducedMotion = true) { FixtureFeatures { AssistantScreen(true, {}) } }
                }.rendering { scene ->
                    scene.render(0).close()
                    val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    assertNotNull(scene.node("音乐助手").config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action).invoke(layouts)
                    assertEquals(1, layouts.single().lineCount)
                    for (label in listOf("新对话", "助手选项")) {
                        val bounds = scene.node(label).touchBoundsInRoot
                        assertTrue(bounds.left >= 0 && bounds.right <= 360 && bounds.top >= 0 && bounds.bottom <= 800, "$label: $bounds")
                    }
                    assertNotNull(scene.node("助手选项").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                    scene.render(16_000_000).close()
                    scene.render(300_000_000).close()
                    for (label in listOf("对话历史", "音乐任务", "助手设置")) {
                        assertNotNull(scene.node(label).config.getOrNull(SemanticsActions.OnClick)?.action)
                    }
                }
            }
        }
    }

    @Test fun assistantShowsReportedFailureOnceAndKeepsUnrelatedFeedbackVisible() {
        SwingUtilities.invokeAndWait {
            val error = "检索来源暂不可用，请稍后重试。"
            val reply = "处理未完成：$error"
            for (notice in listOf(error, "设置保存失败：磁盘已满")) {
                ImageComposeScene(390, 900) {
                    ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                      androidx.compose.material3.Surface {
                        FixtureFeatures(initialState = MusicAssistantState(
                            messages = listOf(AssistantMessage("user", "找些英语播客"),
                                AssistantMessage("assistant", reply, "本地执行反馈 · DeepSeek (deepseek-flash)")),
                            task = MusicTaskStatus(message = error)), notice = notice) { AssistantScreen(true, {}) }
                      }
                    }
                }.rendering { scene ->
                    scene.render(0).close(); scene.render(500_000_000).close()
                    fun texts(node: SemanticsNode): List<String> =
                        node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } + node.children.flatMap(::texts)
                    val visible = scene.semanticsOwners.flatMap { texts(it.rootSemanticsNode) }
                    assertEquals(1, visible.count { it == reply })
                    assertFalse(error in visible, "Do not repeat the chat error as a footer")
                    if (notice != error) assertTrue(notice in visible, "Keep unrelated errors visible")
                    val output = Path.of("build/assistant-recovery/screenshots")
                    Files.createDirectories(output)
                    scene.render(600_000_000).use { image ->
                        assertNotNull(image.encodeToData()).use { Files.write(output.resolve(if (notice == error) "single-error.png" else "separate-error.png"), it.bytes) }
                    }
                }
            }
        }
    }

    @Test fun assistantResearchUsesNativeMarkdownAndRevealsRealSteps() {
        SwingUtilities.invokeAndWait {
            val root = "research-root"
            val reply = "## 英语播客\n\n适合 **A2–B1**，提供逐字稿。\n\n[节目官网](https://publisher.example/english)"
            val steps = listOf(
                MusicTaskEntry("search", "搜索网页", kind = "assistant_web", parentId = root,
                    outcome = MusicTaskOutcome.Completed, progress = ImportProgress("搜索网页", "English podcasts A2 B1")),
                MusicTaskEntry("fetch", "读取网页", kind = "assistant_web", parentId = root,
                    outcome = MusicTaskOutcome.Completed, progress = ImportProgress("读取网页", "https://publisher.example/english")))
            ImageComposeScene(390, 900) {
                CompositionLocalProvider(androidx.compose.ui.platform.LocalInspectionMode provides true) {
                    ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                        androidx.compose.material3.Surface {
                            FixtureFeatures(initialState = MusicAssistantState(assistantTaskId = root, tasks = steps,
                                messages = listOf(AssistantMessage("user", "给我找一些适合 A2 到 B1 的英语播客"),
                                    AssistantMessage("assistant", reply, "DeepSeek (deepseek-flash)")))) { AssistantScreen(true, {}) }
                        }
                    }
                }
            }.rendering { scene ->
                scene.render(0).close(); scene.render(500_000_000).close()
                fun texts(node: SemanticsNode): List<androidx.compose.ui.text.AnnotatedString> =
                    node.config.getOrNull(SemanticsProperties.Text).orEmpty() + node.children.flatMap(::texts)
                val rich = scene.semanticsOwners.flatMap { texts(it.rootSemanticsNode) }
                assertTrue(rich.any { it.text == "英语播客" }, "Render Markdown headings as native text")
                assertFalse(rich.any { it.text.contains("## ") || it.text.contains("**") }, "Do not print raw Markdown")
                assertTrue(rich.any { text -> text.getLinkAnnotations(0, text.length).any {
                    (it.item as? androidx.compose.ui.text.LinkAnnotation.Url)?.url == "https://publisher.example/english"
                } }, "Expose the official source as a native clickable web link")
                assertNotNull(scene.node("查看检索过程 · 2 项步骤").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(600_000_000).close(); scene.render(1_000_000_000).close()
                assertNotNull(scene.node("English podcasts A2 B1"))
                assertNotNull(scene.node("https://publisher.example/english"))
                val output = Path.of("build/assistant-harness/screenshots")
                Files.createDirectories(output)
                scene.render(1_200_000_000).use { image ->
                    assertNotNull(image.encodeToData()).use { Files.write(output.resolve("research-and-sources.png"), it.bytes) }
                }
            }
        }
    }

    @Test fun albumAtmosphereFollowsColoredArtworkAndIgnoresTransparentArt() {
        SwingUtilities.invokeAndWait {
            val fallback = albumAtmosphere(7)
            val empty = androidx.compose.ui.graphics.ImageBitmap(32, 32)
            assertEquals(fallback, atmosphereFromBitmap(empty, fallback))
            val art = androidx.compose.ui.graphics.ImageBitmap(32, 32)
            androidx.compose.ui.graphics.Canvas(art).drawRect(androidx.compose.ui.geometry.Rect(0f, 0f, 32f, 32f),
                androidx.compose.ui.graphics.Paint().apply { color = Color(0xFFCA493B) })
            val tone = atmosphereFromBitmap(art, fallback)
            assertTrue(tone.glow.red > tone.glow.green * 1.5f && tone.glow.red > tone.glow.blue * 1.5f)
        }
    }

    @Test fun emptyLibraryKeepsEntryActionsUsableWithLargeText() {
        SwingUtilities.invokeAndWait {
            for (scale in listOf(1f, 1.5f)) {
                var searched = false; var imported = false; var created = false
                ImageComposeScene(360, 800, density = Density(1f, scale)) {
                    ResonanceTheme(reducedMotion = true) { EmptyLibrary({ created = true }, { imported = true }, null, { searched = true }) }
                }.rendering { scene ->
                    scene.render(0).close()
                    for (label in listOf("搜索歌曲", "导入音乐", "新建歌单")) {
                        val node = scene.node(label)
                        assertTrue(node.touchBoundsInRoot.height >= 48)
                        assertNotNull(node.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                    }
                    assertTrue(searched && imported && created)
                }
            }
        }
    }

    @Test fun playbackWaveMovesOnlyWhilePlayingAndKeepsNativeSeek() {
        SwingUtilities.invokeAndWait {
            val playing = mutableStateOf(true)
            var seek = 0f
            ImageComposeScene(360, 100) {
                ResonanceTheme {
                    Box(Modifier.fillMaxSize().background(ResonanceColors.Canvas)) {
                        SoftRefinedPlaybackScrubber(.38f, "4:00", { seek = it }, { _, _ -> "1:31" }, isPlaying = playing.value)
                    }
                }
            }.rendering { scene ->
                fun frame(time: Long) = scene.render(time).use { bitmap -> assertNotNull(bitmap.encodeToData()).use { it.bytes } }
                frame(0)
                val moving = frame(500_000_000)
                assertFalse(moving.contentEquals(frame(1_500_000_000)))
                playing.value = false
                frame(1_600_000_000)
                val stopped = frame(1_900_000_000)
                assertContentEquals(stopped, frame(3_000_000_000))
                assertNotNull(scene.node("播放进度").config.getOrNull(SemanticsActions.SetProgress)?.action).invoke(.8f)
                assertEquals(.8f, seek, .001f)
            }
        }
    }

    @Test fun renderNativeMotionPreviewFrames() {
        org.junit.Assume.assumeTrue("Enable RESONANCE_UI_ARTIFACTS for 360-frame media export", System.getenv("RESONANCE_UI_ARTIFACTS") == "1")
        SwingUtilities.invokeAndWait {
            val output = Path.of("build/ui-stage-motion/frames")
            Files.createDirectories(output)
            val preview = mutableStateOf(player.copy(isPlaying = true))
            ImageComposeScene(720, 1600, density = Density(2f)) {
                ResonanceTheme(ThemeMode.Dark) {
                    NowPlayingOverlay(playerState = preview.value, queue = PreviewLibrary.tracks,
                        lyricsState = stageLyrics, onDismiss = {}, onTogglePlay = { preview.value = preview.value.copy(isPlaying = !preview.value.isPlaying) },
                        onPrevious = {}, onNext = { preview.value = preview.value.copy(currentTrack = PreviewLibrary.tracks[1]) },
                        onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {}, onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {})
                }
            }.rendering { scene ->
                for (frame in 0 until 360) {
                    val action = when (frame) { 90 -> "歌词"; 180 -> "队列"; 240 -> "封面"; 270 -> "下一首"; 330 -> "暂停"; else -> null }
                    if (action != null) assertNotNull(scene.node(action).config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                    scene.render(frame * 16_666_667L).use { bitmap ->
                        assertNotNull(bitmap.encodeToData()).use { data -> Files.write(output.resolve("frame-%04d.png".format(frame)), data.bytes) }
                    }
                }
            }
        }
    }

    @Test fun switchingThemeKeepsFirstFrameReadable() {
        SwingUtilities.invokeAndWait {
            val mode = mutableStateOf(ThemeMode.Light)
            var captured: ResonanceColorTokens? = null
            ImageComposeScene(320, 240) {
                ResonanceTheme(mode.value) {
                    val tokens = LocalResonanceColors.current
                    SideEffect { captured = tokens }
                    Text("音乐库")
                }
            }.rendering { scene ->
                scene.render(0).close()
                for ((index, next) in listOf(ThemeMode.Dark, ThemeMode.Light).withIndex()) {
                    mode.value = next
                    scene.render((index + 1) * 16_000_000L).close()
                    val tokens = assertNotNull(captured)
                    assertTrue(contrast(tokens.TextPrimary, tokens.Canvas) >= 4.5f)
                    assertEquals(next == ThemeMode.Dark, tokens.isDark)
                }
            }
        }
    }

    private fun contrast(foreground: Color, background: Color): Float {
        val a = foreground.compositeOver(background).luminance()
        val b = background.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }

    @Test fun textAndSelectedRolesStayReadableAcrossAllThemes() {
        SwingUtilities.invokeAndWait {
            for (mode in listOf(ThemeMode.Light, ThemeMode.Dark)) {
                for (palette in WeatherPalette.entries) {
                    var tokens: ResonanceColorTokens? = null
                    var selectionForeground = Color.Unspecified
                    var selectionBackground = Color.Unspecified
                    var defaultContentColor = Color.Unspecified
                    var reduced = false
                    ImageComposeScene(320, 240) {
                        ResonanceTheme(mode, palette, reducedMotion = true) {
                            val colors = LocalResonanceColors.current
                            val scheme = MaterialTheme.colorScheme
                            val motion = LocalReducedMotion.current
                            val contentColor = androidx.compose.material3.LocalContentColor.current
                            SideEffect {
                                tokens = colors
                                selectionForeground = scheme.onPrimaryContainer
                                selectionBackground = scheme.primaryContainer.compositeOver(colors.Canvas)
                                reduced = motion
                                defaultContentColor = contentColor
                            }
                            Text("音乐与日常", modifier = Modifier.background(colors.Canvas))
                        }
                    }.rendering { scene ->
                        scene.render().close()
                        val t = assertNotNull(tokens)
                        for (foreground in listOf(t.TextPrimary, t.Muted, t.Dim, t.Dimmer, t.Primary)) {
                            assertTrue(contrast(foreground, t.Canvas) >= 4.5f, "$mode/$palette: text contrast ${contrast(foreground, t.Canvas)}")
                            assertTrue(contrast(foreground, t.CanvasElevated) >= 4.5f, "$mode/$palette: gallery surface contrast ${contrast(foreground, t.CanvasElevated)}")
                        }
                        assertTrue(contrast(selectionForeground, selectionBackground) >= 4.5f, "$mode/$palette: selected label")
                        assertTrue(reduced, "Theme must propagate the reduced-motion preference to descendants")
                        assertEquals(t.TextPrimary, defaultContentColor, "Uncolored titles must follow the theme")
                    }
                }
            }
            for (color in listOf(SoftNeumorphicTokens.HeroGradientStart, SoftNeumorphicTokens.HeroGradientMiddle, SoftNeumorphicTokens.HeroGradientEnd)) {
                assertTrue(contrast(Color.White, color) >= 3f, "Play icon must contrast with its gradient")
            }
        }
    }

    @Test fun renderPhoneDesktopPlayerAndToolsWithFixtureData() {
        SwingUtilities.invokeAndWait {
            val output = Path.of("build/ui-gallery/screenshots")
            Files.createDirectories(output)
            for (dark in listOf(false, true)) {
                val mode = if (dark) ThemeMode.Dark else ThemeMode.Light
                for ((width, height, scale) in listOf(Triple(360, 800, 1f), Triple(1280, 800, 1f), Triple(360, 800, 1.5f), Triple(640, 360, 1f), Triple(640, 360, 1.5f))) {
                    for (page in listOf("library", "empty", "player", "tools", "assistant", "settings")) {
                        ImageComposeScene(width, height, density = Density(1f, scale)) {
                            ResonanceTheme(mode, reducedMotion = true) {
                                FixtureFeatures {
                                Box(Modifier.fillMaxSize().background(ResonanceColors.Canvas)) {
                                    when (page) {
                                        "player" -> NowPlayingOverlay(playerState = player, queue = PreviewLibrary.tracks,
                                            lyricsState = stageLyrics, onDismiss = {}, onTogglePlay = {}, onPrevious = {}, onNext = {},
                                            onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {}, onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {})
                                        "tools" -> ProfileScreen(width < 720, {}, {}, {}, {})
                                        "assistant" -> AssistantScreen(width < 720, {})
                                        else -> LibraryShell(
                                            destination = if (page == "settings") LibraryDestination.Settings else LibraryDestination.Library, onDestinationChange = {},
                                            playlists = if (page == "empty") emptyList() else PreviewLibrary.playlists,
                                            libraryTracks = if (page == "empty") emptyList() else PreviewLibrary.tracks,
                                            playerState = if (page == "empty") PlayerState() else player, playbackQueue = PreviewLibrary.tracks, lyricsState = LyricsUiState.Idle,
                                            onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {},
                                            selectedPlaylistId = null, userPlaylists = emptyList(), onPlaylistSelected = {},
                                            onPlaylistMembershipChange = { _, _, _ -> }, onRenamePlaylist = { _, _ -> }, onDeletePlaylist = {},
                                            onDeleteLocalTrack = {}, onToggleFavorite = {}, onRefreshLyrics = {}, libraryLocation = "fixture",
                                            onCreatePlaylist = {}, onImport = {}, onExportSync = {}, onImportSync = {}, onExitPreview = {},
                                            previewMode = page != "empty", themeMode = mode, message = null, operationInProgress = false,
                                        )
                                    }
                                }
                                }
                            }
                        }.rendering { scene ->
                            scene.render(0).close()
                            scene.render(500_000_000).use { image ->
                                // Keep the full 70-scene render/dimension matrix in normal regression.
                                // PNG export is optional evidence, not an assertion, and is CPU-expensive.
                                if (System.getenv("RESONANCE_UI_ARTIFACTS") == "1") {
                                    val png = assertNotNull(image.encodeToData())
                                    val suffix = if (scale > 1f) "-large-text" else if (height == 360) "-landscape" else ""
                                    png.use { Files.write(output.resolve("$page-${mode.name.lowercase()}-$width$suffix.png"), it.bytes) }
                                }
                                assertEquals(width, image.width)
                                assertEquals(height, image.height)
                            }
                        }
                    }
                }
            }
        }
    }
    @Test fun renderFrostedDestinationsThroughTheRealShell() {
        SwingUtilities.invokeAndWait {
            val output = Path.of("build/macos-layout/screenshots")
            Files.createDirectories(output)
            val destinations = listOf(LibraryDestination.Library, LibraryDestination.Assistant,
                LibraryDestination.Profile, LibraryDestination.Settings,
                LibraryDestination.Recommendations, LibraryDestination.RecommendationHistory,
                LibraryDestination.Tasks, LibraryDestination.Sources, LibraryDestination.Storage,
                LibraryDestination.SmartManager, LibraryDestination.TasteRadar, LibraryDestination.Import,
                LibraryDestination.About)
            for (mode in listOf(ThemeMode.Light, ThemeMode.Dark)) {
                for (destination in destinations) {
                    ImageComposeScene(390, 844) {
                        ResonanceTheme(mode, reducedMotion = true) {
                            FixtureFeatures(conversation = destination == LibraryDestination.Assistant) {
                                LibraryShell(
                                            destination = destination, onDestinationChange = {},
                                            playlists = PreviewLibrary.playlists,
                                            libraryTracks = PreviewLibrary.tracks,
                                            playerState = player, playbackQueue = PreviewLibrary.tracks, lyricsState = LyricsUiState.Idle,
                                            onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {},
                                            selectedPlaylistId = null, userPlaylists = emptyList(), onPlaylistSelected = {},
                                            onPlaylistMembershipChange = { _, _, _ -> }, onRenamePlaylist = { _, _ -> }, onDeletePlaylist = {},
                                            onDeleteLocalTrack = {}, onToggleFavorite = {}, onRefreshLyrics = {}, libraryLocation = "fixture",
                                            onCreatePlaylist = {}, onImport = {}, onExportSync = {}, onImportSync = {}, onExitPreview = {},
                                            previewMode = false, themeMode = mode, message = null, operationInProgress = false,
                                        )
                            }
                        }
                    }.rendering { scene ->
                        scene.render(0).close()
                        scene.render(500_000_000).close()
                        scene.render(1_000_000_000).use { image ->
                            assertNotNull(image.encodeToData()).use {
                                Files.write(output.resolve("${destination.name}-${mode.name}.png"), it.bytes)
                            }
                        }
                        assertTrue(scene.semanticsOwners.isNotEmpty())
                    }
                }
            }
        }
    }


    @Test fun assistantAttachmentsOnlyActOnExplicitClicks() {
        SwingUtilities.invokeAndWait {
            val tracks = PreviewLibrary.tracks.take(3).map { it.copy(sourceUri = "fixture://${it.id}") }
            var played: Track? = null
            var appended: List<Track>? = null
            ImageComposeScene(390, 600) {
                ResonanceTheme(reducedMotion = true) {
                    AssistantMusicAttachment(tracks, { played = it }, { appended = it })
                }
            }.rendering { scene ->
                scene.render(0).close()
                assertNull(played)
                assertNull(appended)
                val play = scene.node("播放 ${tracks[1].title}")
                assertTrue(play.touchBoundsInRoot.height >= 48f)
                assertNotNull(play.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                assertEquals(tracks[1], played)
                assertNull(appended)
                assertNotNull(scene.node("加入播放队列").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                assertEquals(tracks, appended)
            }
        }
    }

    @Test fun renderApprovedPlayerPanes() {
        SwingUtilities.invokeAndWait {
            val output = Path.of("build/light-ui-native/screenshots")
            Files.createDirectories(output)
            for (mode in listOf(ThemeMode.Light, ThemeMode.Dark)) {
              for ((width, height) in listOf(390 to 844, 1440 to 960)) {
                ImageComposeScene(width, height) {
                    ResonanceTheme(mode, reducedMotion = true) {
                        NowPlayingOverlay(playerState = player, queue = PreviewLibrary.tracks, lyricsState = stageLyrics,
                            onDismiss = {}, onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {},
                            onCycleRepeat = {}, onSeek = {}, onTrackSelected = {}, onToggleFavorite = {},
                            onRefreshLyrics = {}, onOpenSleepTimer = {})
                    }
                }.rendering { scene ->
                    scene.render(0).close()
                    scene.render(500_000_000).close()
                    for ((index, label) in listOf("封面", "歌词", "队列").withIndex()) {
                        assertNotNull(scene.node(label).config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                        repeat(30) { frame -> scene.render(1_000_000_000L + index * 1_000_000_000L + frame * 16_000_000L).close() }
                        scene.render(1_500_000_000L + index * 1_000_000_000L).use { image ->
                            assertNotNull(image.encodeToData()).use { Files.write(output.resolve("Player-$index-${mode.name}-$width.png"), it.bytes) }
                        }
                        assertEquals(true, scene.node(label).config.getOrNull(SemanticsProperties.Selected))
                        for (control in listOf("上一首", "下一首", "播放", "音量", "音效", "定时")) {
                            val bounds = scene.node(control).touchBoundsInRoot
                            assertTrue(bounds.top >= 0 && bounds.bottom <= height, "$label / $control: $bounds")
                        }
                    }
                }
              }
            }
        }
    }

    @Composable
    private fun ApprovedShell(destination: LibraryDestination, navigate: (LibraryDestination) -> Unit) {
        val tutorial = remember { TutorialController {}.apply {
            restore(com.resonance.player.tutorial.TutorialProgress(welcomeDismissed = true))
        } }
        SideEffect { tutorial.onNavigate = navigate; tutorial.currentDestination = destination; tutorial.hasTrack = true }
        CompositionLocalProvider(LocalTutorial provides tutorial) {
        FixtureFeatures(conversation = true) {
            LibraryShell(destination = destination, onDestinationChange = navigate,
                playlists = PreviewLibrary.playlists, libraryTracks = PreviewLibrary.tracks,
                playerState = player, playbackQueue = PreviewLibrary.tracks, lyricsState = stageLyrics,
                onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {},
                selectedPlaylistId = null, userPlaylists = PreviewLibrary.playlists, onPlaylistSelected = {},
                onPlaylistMembershipChange = { _, _, _ -> }, onRenamePlaylist = { _, _ -> }, onDeletePlaylist = {},
                onDeleteLocalTrack = {}, onToggleFavorite = {}, onRefreshLyrics = {}, libraryLocation = "演示曲库",
                onCreatePlaylist = {}, onImport = {}, onExportSync = {}, onImportSync = {}, onExitPreview = {},
                previewMode = false, themeMode = ThemeMode.Light, message = null, operationInProgress = false)
        }
        }
    }

    @Test fun screenshotDraftFooterWrapsWithoutHidingActionsAtLargeText() {
        SwingUtilities.invokeAndWait {
            ImageComposeScene(390, 720, density = Density(1f, 1.6f)) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                    FixtureFeatures {
                        ScreenshotImportDialog(ScreenshotImportDraft("layout-fixture", listOf(ScreenshotPlaylistDraft("测试歌单",
                            listOf(SongRequest("原创歌曲", "测试歌手")))), "本地 OCR"))
                    }
                }
            }.rendering { scene ->
                scene.render(0).close(); scene.render(500_000_000).close()
                for (label in listOf("丢弃草稿", "稍后继续", "确认导入 1 首")) {
                    val bounds = scene.node(label).touchBoundsInRoot
                    assertTrue(bounds.left >= 0 && bounds.right <= 390 && bounds.top >= 0 && bounds.bottom <= 720,
                        "Draft action must stay visible at large text: $label $bounds")
                }
            }
        }
    }

    @Test fun lyricToolsStayScrollableAndClickableInShortLargeTextWindow() {
        SwingUtilities.invokeAndWait {
            var adjustment = 0L
            ImageComposeScene(390, 360, density = Density(1f, 1.6f)) {
                ResonanceTheme(ThemeMode.Dark, reducedMotion = true) {
                    LyricsContent(player.currentTrack!!, .4f, 80_000L, stageLyrics.let { (it as LyricsUiState.Ready).lyrics },
                        onSeek = {}, onAdjustOffset = { adjustment += it })
                }
            }.rendering { scene ->
                scene.render(0).close(); scene.render(500_000_000).close()
                assertNotNull(scene.node("歌词工具").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(600_000_000).close(); scene.render(1_000_000_000).close()
                assertNotNull(scene.node("关闭").config.getOrNull(SemanticsActions.OnClick)?.action)
                fun find(node: SemanticsNode): SemanticsNode? = if (node.config.getOrNull(SemanticsProperties.TestTag) == "lyrics-tools-scroll") node
                    else node.children.firstNotNullOfOrNull { find(it) }
                val scroll = assertNotNull(scene.semanticsOwners.firstNotNullOfOrNull { find(it.rootSemanticsNode) })
                assertTrue(assertNotNull(scroll.config.getOrNull(SemanticsActions.ScrollBy)?.action).invoke(0f, 350f))
                repeat(15) { scene.render(1_100_000_000L + it * 50_000_000).close() }
                val button = scene.node("歌词落后了 · 提前 0.5 秒")
                assertTrue(button.boundsInRoot.top >= scroll.boundsInRoot.top && button.boundsInRoot.bottom <= scroll.boundsInRoot.bottom,
                    "Calibration action must become visible inside the independent scroll area")
                assertNotNull(button.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                assertEquals(500L, adjustment)
                val path = Path.of("build/player-improvements/screenshots/lyrics-tools-short-large.png")
                Files.createDirectories(path.parent)
                scene.render(2_000_000_000).use { bitmap -> assertNotNull(bitmap.encodeToData()).use { Files.write(path, it.bytes) } }
                assertNotNull(scene.node("关闭").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(2_100_000_000).close(); scene.render(2_500_000_000).close()
                assertFailsWith<IllegalStateException> { scene.node("关闭") }
            }
        }
    }

    @Test fun threeMainDestinationsRemainReachableWithoutRoomEntry() {
        SwingUtilities.invokeAndWait {
            for (width in listOf(390, 960, 1280)) {
                val destination = mutableStateOf(LibraryDestination.Library)
                ImageComposeScene(width, 844) {
                    ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                        ApprovedShell(destination.value) { destination.value = it }
                    }
                }.rendering { scene ->
                    var time = 0L
                    fun settle() { repeat(12) { time += 16_666_667; scene.render(time).close() } }
                    settle()
                    for ((label, target) in listOf("音乐助手" to LibraryDestination.Assistant,
                        "工具" to LibraryDestination.Profile, "音乐库" to LibraryDestination.Library)) {
                        assertNotNull(scene.node(label).config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                        settle()
                        assertEquals(target, destination.value)
                        assertNotNull(scene.node("播放"))
                        assertFailsWith<IllegalStateException> { scene.node("一起听") }
                    }
                    scene.render(time).use { bitmap ->
                        val path = Path.of("build/remove-together/screenshots/navigation-$width.png")
                        Files.createDirectories(path.parent)
                        assertNotNull(bitmap.encodeToData()).use { Files.write(path, it.bytes) }
                    }
                }
            }
        }
    }

    @Test fun localPlayerMenuHasNoRoomInvitation() {
        SwingUtilities.invokeAndWait {
            ImageComposeScene(390, 844) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                    FixtureFeatures { FixturePlayer() }
                }
            }.rendering { scene ->
                scene.render(0).close()
                scene.render(500_000_000).close()
                assertNotNull(scene.node("播放选项").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(600_000_000).close()
                scene.render(1_000_000_000).close()
                assertNotNull(scene.node("歌曲与来源"))
                assertNotNull(scene.node("播放设置 · 音量与速度"))
                assertFailsWith<IllegalStateException> { scene.node("与朋友一起听") }
            }
        }
    }

    @Test fun renderApprovedLightNativePagesAndSettings() {
        SwingUtilities.invokeAndWait {
            val output = Path.of("build/light-ui-native/screenshots")
            Files.createDirectories(output)
            for ((width, height) in listOf(390 to 844, 1440 to 960)) {
                val destination = mutableStateOf(LibraryDestination.Library)
                ImageComposeScene(width, height) {
                    ResonanceTheme(ThemeMode.Light, reducedMotion = true) { ApprovedShell(destination.value) { destination.value = it } }
                }.rendering { scene ->
                    var time = 0L
                    fun capture(name: String) {
                        // Flush layout/effects, then advance the test clock past residual Material transitions.
                        repeat(4) { time += 16_666_667; scene.render(time).close() }
                        time += 500_000_000
                        scene.render(time).use { image ->
                            assertNotNull(image.encodeToData()).use { Files.write(output.resolve("$name-$width.png"), it.bytes) }
                        }
                    }
                    fun click(label: String) {
                        assertNotNull(scene.node(label).config.getOrNull(SemanticsActions.OnClick)?.action, label).invoke()
                    }
                    capture("library-playlists")
                    for (tab in listOf("歌曲", "专辑", "歌手", "歌单")) {
                        click(tab); capture("library-$tab")
                        assertEquals(true, scene.node(tab).config.getOrNull(SemanticsProperties.Selected))
                    }
                    val playlist = PreviewLibrary.playlists.first()
                    click("打开歌单 ${playlist.name}，${playlist.tracks.size} 首歌曲")
                    capture("library-detail")
                    assertNotNull(scene.node("播放全部"))
                    if (width >= 1100) {
                        val sidebarPlaylist = PreviewLibrary.playlists[1]
                        click(sidebarPlaylist.name); capture("sidebar-playlist")
                        assertNotNull(scene.node(sidebarPlaylist.subtitle))
                        click("我喜欢的音乐"); capture("sidebar-favorites")
                        assertNotNull(scene.node("我的收藏"))
                        assertNotNull(scene.node("${PreviewLibrary.tracks.count { it.isFavorite }} 首歌曲"))
                        click("返回上级"); capture("sidebar-back-to-library")
                        assertNotNull(scene.node("专辑"))
                        assertEquals(LibraryDestination.Library, destination.value)
                    }
                    for (page in listOf(LibraryDestination.Assistant, LibraryDestination.Profile,
                        LibraryDestination.Recommendations, LibraryDestination.RecommendationHistory, LibraryDestination.Tasks,
                        LibraryDestination.Sources, LibraryDestination.Storage, LibraryDestination.SmartManager, LibraryDestination.TasteRadar,
                        LibraryDestination.Import, LibraryDestination.PlaylistArchive, LibraryDestination.About, LibraryDestination.Discover,
                        LibraryDestination.AudioHelp, LibraryDestination.Tutorial, LibraryDestination.Settings)) {
                        destination.value = page; capture(page.name)
                        assertNotNull(scene.node("播放"), "The dock stays available on $page")
                        if (page == LibraryDestination.Tutorial) assertNotNull(scene.node("使用教程"))
                    }
                    for (section in SettingsSection.entries.filter { it != SettingsSection.Overview }) {
                        click(section.title); capture("settings-${section.name}")
                        click(if (width >= 1100) "返回上级" else "返回设置"); capture("settings-overview-return")
                        assertEquals(LibraryDestination.Settings, destination.value)
                        assertNotNull(scene.node("按你的习惯，安排每一次聆听。"))
                    }
                }
            }
        }
    }

    @Test fun shortLandscapeRetainsCollectionViewportAndTransport() {
        SwingUtilities.invokeAndWait {
            val output = Path.of("build/light-ui-native/screenshots")
            Files.createDirectories(output)
            for (scale in listOf(1f, 1.5f)) {
              for (page in listOf(LibraryDestination.Library, LibraryDestination.Settings, LibraryDestination.Assistant,
                  LibraryDestination.Profile)) {
                ImageComposeScene(640, 360, density = Density(1f, scale)) {
                    ResonanceTheme(ThemeMode.Light, reducedMotion = true) { ApprovedShell(page) {} }
                }.rendering { scene ->
                    scene.render(0).close(); scene.render(500_000_000).close()
                    fun findTag(node: SemanticsNode): SemanticsNode? =
                        if (node.config.getOrNull(SemanticsProperties.TestTag) == "library-page-swipe") node
                        else node.children.firstNotNullOfOrNull { findTag(it) }
                    if (page == LibraryDestination.Library) {
                        val viewport = assertNotNull(scene.semanticsOwners.firstNotNullOfOrNull { findTag(it.rootSemanticsNode) }).boundsInRoot
                        assertTrue(viewport.height >= 150f, "The collection must retain a usable scroll viewport: $viewport at $scale")
                    }
                    for (label in listOf("播放", "下一首", "音乐库", "音乐助手", "工具")) {
                        val bounds = scene.node(label).touchBoundsInRoot
                        assertTrue(bounds.top >= 0 && bounds.bottom <= 360 && bounds.left >= 0 && bounds.right <= 640,
                            "$label must remain inside the short window: $bounds")
                    }
                    scene.render(1_000_000_000).use { image ->
                        assertNotNull(image.encodeToData()).use { Files.write(output.resolve("${page.name}-short-$scale.png"), it.bytes) }
                    }
                    if (page == LibraryDestination.Library) {
                        fun scrollNode(node: SemanticsNode): SemanticsNode? =
                            if (node.config.getOrNull(SemanticsActions.ScrollToIndex) != null) node
                            else node.children.firstNotNullOfOrNull { scrollNode(it) }
                        val grid = assertNotNull(scene.semanticsOwners.firstNotNullOfOrNull { scrollNode(it.rootSemanticsNode) })
                        assertTrue(assertNotNull(grid.config.getOrNull(SemanticsActions.ScrollToIndex)?.action).invoke(2))
                        repeat(4) { scene.render(1_100_000_000L + it * 500_000_000).close() }
                        val playlist = PreviewLibrary.playlists.first()
                        val entry = scene.node("打开歌单 ${playlist.name}，${playlist.tracks.size} 首歌曲")
                        assertTrue(entry.boundsInRoot.top < 300 && entry.boundsInRoot.bottom > 100, "Scrolled playlist must be visible")
                        scene.render(3_000_000_000).use { image ->
                            assertNotNull(image.encodeToData()).use { Files.write(output.resolve("Library-short-scrolled-$scale.png"), it.bytes) }
                        }
                        assertNotNull(entry.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                        scene.render(4_000_000_000).close(); scene.render(4_500_000_000).close()
                        assertNotNull(scene.node("播放全部"))
                    }
                }
              }
            }
        }
    }

    @Test fun nativePagesReverseWithoutRecreatingTheDepartingContent() {
        SwingUtilities.invokeAndWait {
            val target = mutableIntStateOf(0)
            val created = mutableMapOf<Int, Int>()
            val mounted = mutableSetOf<Int>()
            var inputManager: androidx.compose.ui.input.InputModeManager? = null
            ImageComposeScene(390, 600) {
                val manager = androidx.compose.ui.platform.LocalInputModeManager.current
                SideEffect { inputManager = manager }
                ResonanceTheme(ThemeMode.Light) {
                    ContinuousPageTransition(target.intValue, Modifier.fillMaxSize(), direction = { from, to -> if (to > from) 1 else -1 }) { page ->
                        remember { created[page] = (created[page] ?: 0) + 1; page }
                        DisposableEffect(page) { mounted += page; onDispose { mounted -= page } }
                        Text("page-$page")
                    }
                }
            }.rendering { scene ->
                var time = 0L
                fun frames(count: Int) { repeat(count) { time += 16_666_667; scene.render(time).close() } }
                frames(3)
                // A real mouse press switches Compose's default keyboard mode to Touch.
                assertTrue(assertNotNull(inputManager).requestInputMode(androidx.compose.ui.input.InputMode.Touch))
                frames(2)
                target.intValue = 1; frames(5)
                assertEquals(setOf(0, 1), mounted, "Both page trees must survive the crossfade")
                target.intValue = 0; frames(2)
                assertEquals(1, created[0], "Reversing must reuse the original page and its local state")
                target.intValue = 2; frames(2); target.intValue = 3; frames(30)
                assertEquals(setOf(3), mounted, "Interrupted destinations must be disposed after settling")
                assertNotNull(scene.node("page-3"))
                assertTrue(assertNotNull(inputManager).requestInputMode(androidx.compose.ui.input.InputMode.Keyboard))
                frames(2)
                target.intValue = 4; frames(3)
                assertEquals(setOf(4), mounted, "Keyboard navigation must settle immediately")
            }
        }
    }

}
