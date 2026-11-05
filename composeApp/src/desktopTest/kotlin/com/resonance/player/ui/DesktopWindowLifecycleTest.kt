@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.resonance.player.ui

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.resonance.player.App
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.platform.DesktopPlatformServices
import com.resonance.player.platform.FloatingLyricsHost
import java.awt.GraphicsEnvironment
import java.awt.Window as AwtWindow
import java.nio.file.Files
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import kotlin.test.*

/** Real native windows: offscreen lyric-text tests cannot catch a paused parent window. */
class DesktopWindowLifecycleTest {
    private fun requireDesktop() = assumeTrue("Opt in with a graphical session",
        System.getenv("RESONANCE_DESKTOP_WINDOW_TEST") == "1" && !GraphicsEnvironment.isHeadless())

    private fun lyricWindow(): ComposeWindow? = AwtWindow.getWindows().filterIsInstance<ComposeWindow>()
        .singleOrNull { it.isDisplayable && it.title == "Resonance 桌面歌词" }

    private fun nodes(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::nodes)
    private fun ComposeWindow.nodes() = semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
    private fun ComposeWindow.hasText(text: String) = nodes().any {
        it.config.getOrNull(SemanticsProperties.Text)?.any { value -> value.text == text } == true
    }
    private fun ComposeWindow.click(label: String) {
        val node = nodes().first {
            it.config.getOrNull(SemanticsProperties.Text)?.any { value -> value.text == label } == true ||
                it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true
        }
        assertTrue(assertNotNull(node.config.getOrNull(SemanticsActions.OnClick)?.action).invoke())
    }
    private suspend fun eventually(condition: suspend () -> Boolean) = withTimeout(5_000) {
        while (!condition()) delay(25)
    }

    private suspend fun drag(window: ComposeWindow) {
        val start = window.locationOnScreen.let { java.awt.Point(it.x + window.width / 2, it.y + window.height - 20) }
        withContext(Dispatchers.IO) {
            val robot = java.awt.Robot()
            robot.mouseMove(start.x, start.y); robot.delay(80)
            robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK); robot.delay(80)
            repeat(10) { step -> robot.mouseMove(start.x - (step + 1) * 10, start.y - (step + 1) * 12); robot.delay(30) }
            robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
        }
        delay(200)
    }

    @Test fun linuxLyricsCanBeDraggedUnlockedAndStayInPlaceDuringUpdates() {
        requireDesktop()
        assumeTrue(System.getProperty("os.name").startsWith("Linux"))
        var failure: Throwable? = null
        application(exitProcessOnExit = false) {
            var settings by remember { mutableStateOf(MusicFeatureSettings(floatingLyrics = true)) }
            var player by remember { mutableStateOf(PlayerState(currentTrack = Track("drag-fixture", "拖动夹具", "测试", "", "0:10", 0))) }
            val lyrics = remember { LyricsUiState.Ready(Lyrics("drag-fixture",
                listOf(LyricLine(0, "拖动第一行"), LyricLine(2_000, "拖动第二行")), true, false, "Fixture")) }
            ResonanceTheme { FloatingLyricsHost(player, lyrics, settings, { settings = it }, {}, {}, {}) }
            LaunchedEffect(Unit) {
                try {
                    eventually { lyricWindow()?.hasText("拖动第一行") == true }
                    delay(250)
                    val window = assertNotNull(lyricWindow())
                    window.extendedState = java.awt.Frame.NORMAL
                    window.toFront(); window.requestFocus()
                    delay(400)
                    var mouseEvents = 0
                    val listener = java.awt.event.AWTEventListener { event ->
                        if (event is java.awt.event.MouseEvent && javax.swing.SwingUtilities.getWindowAncestor(event.component) == window) mouseEvents++
                    }
                    java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(listener, java.awt.AWTEvent.MOUSE_EVENT_MASK or java.awt.AWTEvent.MOUSE_MOTION_EVENT_MASK)
                    val before = window.location
                    drag(window)
                    java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
                    assertTrue(window.x < before.x - 40 && window.y < before.y - 40,
                        "Native drag did not move window: $before -> ${window.location}; native events=$mouseEvents, showing=${window.isShowing}, state=${window.extendedState}, pointer=${java.awt.MouseInfo.getPointerInfo()?.location}")
                    val moved = window.location
                    player = player.copy(positionMs = 2500)
                    settings = settings.copy(floatingLyricsSize = 32)
                    eventually { window.hasText("拖动第二行") }
                    delay(250)
                    assertTrue(window.location.distance(moved) < 4, "Lyric update or resize reset the dragged position")
                    window.click("锁定位置")
                    eventually { settings.floatingLyricsLocked && window.hasText("解锁位置") }
                    val locked = window.location
                    drag(window)
                    assertEquals(locked, window.location, "Locked lyrics moved")
                    // Linux retains a clickable unlock control even while its window is non-focusable.
                    window.click("解锁位置")
                    eventually { !settings.floatingLyricsLocked && window.isFocusableWindow }
                    drag(window)
                    assertTrue(window.location.distance(locked) > 40, "Unlocked lyrics could not move again")
                } catch (error: Throwable) { failure = error }
                finally { exitApplication() }
            }
        }
        failure?.let { throw it }
    }

    @Test fun lyricsUpdateLockCloseAndReopenWhileMainWindowIsHidden() {
        requireDesktop()
        var failure: Throwable? = null
        application(exitProcessOnExit = false) {
            var visible by remember { mutableStateOf(true) }
            var settings by remember { mutableStateOf(MusicFeatureSettings(floatingLyrics = true)) }
            var player by remember { mutableStateOf(PlayerState(
                currentTrack = Track("window-fixture", "窗口夹具", "测试", "", "0:10", 0), positionMs = 0)) }
            val lyrics = remember { LyricsUiState.Ready(Lyrics("window-fixture",
                listOf(LyricLine(0, "第一行夹具"), LyricLine(2_000, "第二行夹具")), true, false, "Fixture")) }
            Window(onCloseRequest = {}, visible = visible, title = "Resonance window fixture") {}
            ResonanceTheme {
                FloatingLyricsHost(player, lyrics, settings, { settings = it }, {}, {}, {})
            }
            LaunchedEffect(Unit) {
                try {
                    eventually { lyricWindow()?.hasText("第一行夹具") == true }
                    if (System.getProperty("os.name").startsWith("Linux")) assertFalse(lyricWindow()!!.isTransparent)
                    visible = false
                    delay(150)
                    repeat(5) {
                        player = player.copy(positionMs = 2_500)
                        eventually { lyricWindow()?.hasText("第二行夹具") == true }
                        lyricWindow()!!.click(if (System.getProperty("os.name").startsWith("Windows")) "锁定并穿透" else "锁定位置")
                        eventually { settings.floatingLyricsLocked && lyricWindow()?.isFocusableWindow == false }
                        settings = settings.copy(floatingLyricsLocked = false, floatingLyricsSize = 32)
                        eventually { lyricWindow()?.isFocusableWindow == true }
                        lyricWindow()!!.click("关闭桌面歌词")
                        eventually { !settings.floatingLyrics && lyricWindow() == null }
                        player = player.copy(positionMs = 0)
                        settings = settings.copy(floatingLyrics = true)
                        eventually { lyricWindow()?.hasText("第一行夹具") == true }
                    }
                    visible = true
                    eventually { AwtWindow.getWindows().filterIsInstance<ComposeWindow>()
                        .any { it.title == "Resonance window fixture" && it.isShowing } }
                } catch (error: Throwable) { failure = error }
                finally { exitApplication() }
            }
        }
        failure?.let { throw it }
    }

    @Test fun productionAppCanCloseLyricsWithoutShowingMainWindow() {
        requireDesktop()
        val directory = Files.createTempDirectory("resonance-window-test-")
        val oldData = System.getProperty("RESONANCE_DATA")
        val oldLibrary = System.getProperty("RESONANCE_LIBRARY")
        var services: DesktopPlatformServices? = null
        var failure: Throwable? = null
        try {
            System.setProperty("RESONANCE_DATA", directory.resolve("data").toString())
            System.setProperty("RESONANCE_LIBRARY", directory.resolve("music").toString())
            val native = DesktopPlatformServices().also { services = it }
            runBlocking { native.musicAssistant.saveSettings(MusicFeatureSettings(floatingLyrics = true)) }
            val lifetime = com.resonance.player.application.ApplicationLifetime(native::close)
            application(exitProcessOnExit = false) {
                App(native, lifetime = lifetime, mainWindow = { content ->
                    Window(onCloseRequest = {}, visible = false, title = "Resonance hidden fixture") { content() }
                })
                LaunchedEffect(Unit) {
                    try {
                        eventually { lyricWindow()?.hasText("Resonance") == true }
                        lyricWindow()!!.click("关闭桌面歌词")
                        eventually { lyricWindow() == null }
                        // PersistenceQueue must commit the close while the host window stays hidden.
                        eventually { !native.musicAssistant.settings().floatingLyrics }
                        assertFalse(AwtWindow.getWindows().filterIsInstance<ComposeWindow>()
                            .single { it.title == "Resonance hidden fixture" }.isShowing)
                    } catch (error: Throwable) { failure = error }
                    finally { lifetime.close().join(); exitApplication() }
                }
            }
            failure?.let { throw it }
        } finally {
            services?.close()
            if (oldData == null) System.clearProperty("RESONANCE_DATA") else System.setProperty("RESONANCE_DATA", oldData)
            if (oldLibrary == null) System.clearProperty("RESONANCE_LIBRARY") else System.setProperty("RESONANCE_LIBRARY", oldLibrary)
            directory.toFile().deleteRecursively()
        }
    }
}
