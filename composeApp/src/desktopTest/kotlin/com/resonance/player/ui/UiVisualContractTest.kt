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
    private fun FixturePlayer() {
        NowPlayingOverlay(playerState = player, queue = PreviewLibrary.tracks,
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

    @Test fun songInformationRemainsReachableFromPlayerMenu() {
        SwingUtilities.invokeAndWait {
            ImageComposeScene(360, 800) {
                ResonanceTheme(reducedMotion = true) { FixturePlayer() }
            }.rendering { scene ->
                scene.render(0).close()
                assertNotNull(scene.node("播放选项").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(100_000_000).close()
                scene.render(500_000_000).close()
                assertNotNull(scene.node("歌曲与来源").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(600_000_000).close()
                scene.render(1_000_000_000).close()
                assertNotNull(scene.node("关闭").config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
            }
        }
    }

    @Composable
    private fun FixtureFeatures(content: @Composable () -> Unit) {
        val services = remember {
            val state = MutableStateFlow(MusicAssistantState())
            val assistant = Proxy.newProxyInstance(MusicAssistantServices::class.java.classLoader,
                arrayOf(MusicAssistantServices::class.java)) { _, method, _ ->
                when (method.name) { "getState" -> state; else -> error("Unexpected fixture call: ${method.name}") }
            } as MusicAssistantServices
            Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, _ ->
                when (method.name) {
                    "getMusicAssistant" -> assistant
                    "getSupportsWebLogin", "getSupportsKgg", "getSupportsKeyboardShortcuts" -> false
                    "getForeground" -> MutableStateFlow(true)
                    "audioFileAvailable" -> true
                    else -> error("Unexpected fixture call: ${method.name}")
                }
            } as PlatformServices
        }
        val scope = rememberCoroutineScope()
        val feature = remember { MusicFeatureController(services, scope).apply { library = PreviewLibrary.tracks } }
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
                for ((width, height, scale) in listOf(Triple(360, 800, 1f), Triple(1280, 800, 1f), Triple(360, 800, 1.5f), Triple(640, 360, 1f))) {
                    for (page in listOf("library", "player", "tools", "assistant", "settings")) {
                        ImageComposeScene(width, height, density = Density(1f, scale)) {
                            ResonanceTheme(mode, reducedMotion = true) {
                                FixtureFeatures {
                                Box(Modifier.fillMaxSize().background(ResonanceColors.Canvas)) {
                                    when (page) {
                                        "player" -> NowPlayingOverlay(playerState = player, queue = PreviewLibrary.tracks,
                                            lyricsState = LyricsUiState.Idle, onDismiss = {}, onTogglePlay = {}, onPrevious = {}, onNext = {},
                                            onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {}, onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {})
                                        "tools" -> ProfileScreen(width < 720, {}, {}, {}, {})
                                        "assistant" -> AssistantScreen(width < 720, {})
                                        else -> LibraryShell(
                                            destination = if (page == "settings") LibraryDestination.Settings else LibraryDestination.Library, onDestinationChange = {},
                                            playlists = PreviewLibrary.playlists, libraryTracks = PreviewLibrary.tracks,
                                            playerState = player, playbackQueue = PreviewLibrary.tracks, lyricsState = LyricsUiState.Idle,
                                            onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {},
                                            selectedPlaylistId = null, userPlaylists = emptyList(), onPlaylistSelected = {},
                                            onPlaylistMembershipChange = { _, _, _ -> }, onRenamePlaylist = { _, _ -> }, onDeletePlaylist = {},
                                            onDeleteLocalTrack = {}, onToggleFavorite = {}, onRefreshLyrics = {}, libraryLocation = "fixture",
                                            onCreatePlaylist = {}, onImport = {}, onExportSync = {}, onImportSync = {}, onExitPreview = {},
                                            previewMode = true, themeMode = mode, message = null, operationInProgress = false,
                                        )
                                    }
                                }
                                }
                            }
                        }.rendering { scene ->
                            scene.render(0).close()
                            scene.render(500_000_000).use { image ->
                                val png = assertNotNull(image.encodeToData())
                                val suffix = if (scale > 1f) "-large-text" else if (height == 360) "-landscape" else ""
                                png.use { Files.write(output.resolve("$page-${mode.name.lowercase()}-$width$suffix.png"), it.bytes) }
                                assertEquals(width, image.width)
                                assertEquals(height, image.height)
                            }
                        }
                    }
                }
            }
        }
    }
}
