package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.resonance.player.data.PreviewLibrary
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.test.*

/** Real native player, rendering clock and pointer events; no audio, network or user files. */
@OptIn(ExperimentalComposeUiApi::class)
class PlayerExpansionTest {
    private val track = PreviewLibrary.tracks.first()
    private val lyrics = LyricsUiState.Ready(Lyrics(track.id,
        (0..29).map { LyricLine(it * 5000L, "原创测试旋律第 $it 句") }, true, false, "测试夹具"))
    private class PointerInputMode : InputModeManager {
        override var inputMode by mutableStateOf(InputMode.Touch)
        override fun requestInputMode(inputMode: InputMode): Boolean {
            this.inputMode = inputMode
            return true
        }
    }

    private class Harness(val width: Int, val height: Int, reduced: Boolean = false) : AutoCloseable {
        val expanded = mutableStateOf(false)
        val reduceMotion = mutableStateOf(reduced)
        val inputMode = PointerInputMode()
        var time = 0L
        lateinit var scene: ImageComposeScene
        fun frames(count: Int = 1) { repeat(count) { time += 16_666_667L; scene.render(time).close() } }
        fun nodes(): List<SemanticsNode> {
            fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap { walk(it.unmergedRootSemanticsNode) }
        }
        fun tag(value: String): SemanticsNode = nodes().firstOrNull {
            it.config.getOrNull(SemanticsProperties.TestTag) == value
        } ?: error("Missing tag $value")
        fun label(value: String): SemanticsNode = nodes().firstOrNull {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(value) == true ||
                it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == value } == true
        } ?: error("Missing label $value")
        fun click(value: String) { assertNotNull(label(value).config.getOrNull(SemanticsActions.OnClick)?.action).invoke() }
        fun event(type: PointerEventType, position: Offset, elapsedFrames: Int = 2) {
            scene.sendPointerEvent(type, position, timeMillis = time / 1_000_000L)
            frames(elapsedFrames)
        }
        fun capture(name: String) {
            val path = Path.of("build/apple-player-2026-11-05/screenshots/$width-$height-$name.png")
            Files.createDirectories(path.parent)
            scene.render(time).use { image -> image.encodeToData()!!.use { Files.write(path, it.bytes) } }
        }
        override fun close() = scene.close()
    }

    @Composable
    private fun Player(harness: Harness) {
        NowPlayingOverlay(playerState = PlayerState(currentTrack = track, positionMs = 5000L),
            queue = PreviewLibrary.tracks, lyricsState = lyrics, onDismiss = { harness.expanded.value = false },
            onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {},
            onSeek = {}, onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {})
    }

    private fun harness(width: Int = 390, height: Int = 844, reduced: Boolean = false): Harness {
        val harness = Harness(width, height, reduced)
        harness.scene = ImageComposeScene(width, height) {
            CompositionLocalProvider(LocalInputModeManager provides harness.inputMode) {
            ResonanceTheme(ThemeMode.Light, reducedMotion = harness.reduceMotion.value) {
                PlayerExpansionLayout(expanded = harness.expanded.value, track = track,
                    onExpandedChange = { harness.expanded.value = it }, background = {
                        Box(Modifier.fillMaxSize().background(ResonanceColors.Canvas)) {
                            Text("曲库内容保持原位置", Modifier.align(Alignment.Center))
                            Row(Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth().height(64.dp)
                                .playerExpansionSource().testTag("fixture-mini-player")
                                .background(ResonanceColors.CanvasElevated).clickable { harness.expanded.value = true },
                                verticalAlignment = Alignment.CenterVertically) {
                                AlbumArtwork(track.artworkSeed, Modifier.padding(8.dp).size(42.dp)
                                    .playerExpansionSource(artwork = true).testTag("fixture-mini-artwork"), 9.dp)
                                Text("打开测试播放器")
                            }
                        }
                    }, player = { Player(harness) })
            }
            }
        }
        harness.frames(12)
        return harness
    }

    @Test fun coverConnectsMeasuredEndpointsAndRapidReversalKeepsItsPosition() {
        SwingUtilities.invokeAndWait {
            for ((width, height) in listOf(390 to 844, 1440 to 900)) harness(width, height).use { h ->
                val source = h.tag("fixture-mini-artwork").boundsInRoot
                val sourceFrame = h.tag("fixture-mini-player").boundsInRoot
                h.capture("closed")
                h.expanded.value = true
                h.frames(7)
                val intermediate = h.tag("player-transition-artwork").boundsInRoot
                val expansion = 1f - h.tag("player-expansion").boundsInRoot.top / sourceFrame.top
                assertTrue(intermediate.width > source.width)
                assertTrue(intermediate.top < source.top)
                h.capture("opening")
                h.frames(90)
                val target = h.tag("player-artwork").boundsInRoot
                assertTrue(intermediate.width < target.width, "$intermediate -> $target")
                val expectedTop = source.top + (target.top - source.top) * expansion
                assertTrue(abs(intermediate.top - expectedTop) < 3f,
                    "Cover must use stable endpoints, not the translated player coordinates: $intermediate / $expectedTop")
                assertTrue(h.nodes().none { it.config.getOrNull(SemanticsProperties.TestTag) == "player-transition-artwork" })
                h.capture("expanded")

                h.click("关闭正在播放"); h.frames(5)
                val before = h.tag("player-transition-artwork").boundsInRoot
                h.capture("closing")
                h.expanded.value = true; h.frames(1)
                val after = h.tag("player-transition-artwork").boundsInRoot
                h.capture("reopening")
                assertTrue(abs(after.top - before.top) < height * .14f,
                    "$width/$height reversal must start from the visible position: $before -> $after")
                h.frames(90)
                assertTrue(abs(h.tag("player-artwork").boundsInRoot.top - target.top) < 1f)
                h.expanded.value = false; h.frames(90)
                assertTrue(h.nodes().none { it.config.getOrNull(SemanticsProperties.TestTag) == "player-expansion" })
                assertTrue(abs(h.tag("fixture-mini-artwork").boundsInRoot.top - source.top) < 1f)
            }
        }
    }

    @Test fun verticalDragTracksThePointerShortPullReturnsAndLongPullDismisses() {
        SwingUtilities.invokeAndWait {
            harness().use { h ->
                h.expanded.value = true; h.frames(90)
                val handle = h.tag("player-drag-handle").boundsInRoot.center
                h.event(PointerEventType.Press, handle)
                for (step in 1..4) h.event(PointerEventType.Move, handle + Offset(0f, step * 24f), 4)
                val pulled = h.tag("player-expansion").boundsInRoot.top
                assertTrue(pulled in 50f..105f, "Drag must move the player before release: $pulled")
                assertTrue(h.expanded.value)
                h.capture("short-drag")
                h.event(PointerEventType.Release, handle + Offset(0f, 96f), 4)
                h.frames(90)
                assertTrue(h.expanded.value, "Short slow drag must spring back")
                assertTrue(h.tag("player-expansion").boundsInRoot.top < 1f)

                h.event(PointerEventType.Press, handle)
                for (step in 1..10) h.event(PointerEventType.Move, handle + Offset(0f, step * 32f), 4)
                h.capture("long-drag")
                h.event(PointerEventType.Release, handle + Offset(0f, 320f), 4)
                h.frames(90)
                assertFalse(h.expanded.value)
                assertTrue(h.nodes().none { it.config.getOrNull(SemanticsProperties.TestTag) == "player-expansion" })
            }
        }
    }

    @Test fun upwardReversalAndLyricsScrollingKeepThePlayerOpen() {
        SwingUtilities.invokeAndWait {
            harness().use { h ->
                h.expanded.value = true; h.frames(90)
                val handle = h.tag("player-drag-handle").boundsInRoot.center
                h.event(PointerEventType.Press, handle)
                for (step in 1..8) h.event(PointerEventType.Move, handle + Offset(0f, step * 32f))
                for (step in 1..7) h.event(PointerEventType.Move, handle + Offset(0f, 256f - step * 30f))
                h.event(PointerEventType.Release, handle + Offset(0f, 46f)); h.frames(90)
                assertTrue(h.expanded.value)
                assertTrue(h.tag("player-expansion").boundsInRoot.top < 1f)
                h.click("歌词"); h.frames(40)
                val line = h.tag("lyric-line-1").boundsInRoot.center
                h.event(PointerEventType.Press, line)
                for (step in 1..7) h.event(PointerEventType.Move, line - Offset(0f, step * 20f))
                h.event(PointerEventType.Release, line - Offset(0f, 140f)); h.frames(40)
                assertTrue(h.expanded.value)
                assertTrue(h.tag("player-expansion").boundsInRoot.top < 1f)
            }
        }
    }

    @Test fun reducedMotionAndKeyboardKeepTheCoverStationaryAndVisible() {
        SwingUtilities.invokeAndWait {
            for (keyboard in listOf(false, true)) harness(reduced = !keyboard).use { h ->
                if (keyboard) h.inputMode.requestInputMode(InputMode.Keyboard)
                h.expanded.value = true; h.frames(6)
                assertTrue(h.tag("player-expansion").boundsInRoot.top < 1f)
                assertTrue(h.nodes().none { it.config.getOrNull(SemanticsProperties.TestTag) == "player-transition-artwork" })
                h.frames(20); h.capture("reduced-motion")
                assertTrue(h.tag("player-artwork").boundsInRoot.height > 0f)
                h.click("关闭正在播放"); h.frames(20)
                assertFalse(h.expanded.value)
                assertTrue(h.nodes().none { it.config.getOrNull(SemanticsProperties.TestTag) == "player-expansion" })
            }
        }
    }

    @Test fun realShellConnectsItsMiniPlayerAtEveryWindowClass() {
        SwingUtilities.invokeAndWait {
            for ((width, height) in listOf(390 to 844, 720 to 480, 1440 to 900)) {
                Harness(width, height).use { h ->
                    h.scene = ImageComposeScene(width, height) {
                        CompositionLocalProvider(LocalInputModeManager provides remember { PointerInputMode() }) {
                            ResonanceTheme(ThemeMode.Light) {
                                LibraryShell(destination = LibraryDestination.Library, onDestinationChange = {},
                                    playlists = PreviewLibrary.playlists, libraryTracks = PreviewLibrary.tracks,
                                    playerState = PlayerState(currentTrack = track), playbackQueue = PreviewLibrary.tracks,
                                    lyricsState = lyrics, onTogglePlay = {}, onPrevious = {}, onNext = {},
                                    onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {}, selectedPlaylistId = null,
                                    userPlaylists = PreviewLibrary.playlists, onPlaylistSelected = {},
                                    onPlaylistMembershipChange = { _, _, _ -> }, onRenamePlaylist = { _, _ -> },
                                    onDeletePlaylist = {}, onDeleteLocalTrack = {}, onToggleFavorite = {},
                                    onRefreshLyrics = {}, libraryLocation = "测试曲库", onCreatePlaylist = {},
                                    onImport = {}, onExportSync = {}, onImportSync = {}, onExitPreview = {},
                                    previewMode = false, message = null, operationInProgress = false)
                            }
                        }
                    }
                    h.frames(20)
                    h.click("打开正在播放详情与歌词"); h.frames(7)
                    assertTrue(h.tag("player-transition-artwork").boundsInRoot.width > 0f)
                    h.capture("real-shell-opening")
                    h.frames(90)
                    assertTrue(h.tag("player-artwork").boundsInRoot.height > 0f)
                    h.capture("real-shell-expanded")
                    h.click("关闭正在播放"); h.frames(90)
                    assertTrue(h.nodes().none { it.config.getOrNull(SemanticsProperties.TestTag) == "player-expansion" })
                    assertNotNull(h.label("打开正在播放详情与歌词").config.getOrNull(SemanticsActions.OnClick)?.action)
                }
            }
        }
    }
}
