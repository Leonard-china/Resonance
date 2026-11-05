package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.resonance.player.model.*
import com.resonance.player.platform.floatingLyricLines

@Composable
fun DesktopFloatingLyricsWindow(player: PlayerState, lyrics: LyricsUiState, settings: MusicFeatureSettings,
    onSettingsChange: (MusicFeatureSettings) -> Unit, onTogglePlay: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) {
    val state = rememberWindowState(width = 760.dp, height = 140.dp, position = WindowPosition.Aligned(Alignment.BottomCenter))
    val locked = settings.floatingLyricsLocked
    // XWayland/OpenGL can present an empty black ARGB surface despite a valid Compose tree.
    // This small text window uses an opaque surface on Linux; the main window is unaffected.
    val transparent = !System.getProperty("os.name").startsWith("Linux", ignoreCase = true)
    val linux = !transparent
    LaunchedEffect(settings.floatingLyricsSize, locked) {
        state.size = DpSize(state.size.width, (settings.floatingLyricsSize * 4.3 + if (locked && !linux) 24 else 64).dp)
    }
    val (first, second) = floatingLyricLines(player, lyrics, settings)
    Window(onCloseRequest = { onSettingsChange(settings.copy(floatingLyrics = false)) }, title = "Resonance 桌面歌词", state = state,
        transparent = transparent, undecorated = true, alwaysOnTop = true, focusable = !locked, resizable = !locked) {
        DisposableEffect(window, locked) {
            if (System.getProperty("os.name").startsWith("Windows")) {
                val hwnd = com.sun.jna.platform.win32.WinDef.HWND(com.sun.jna.Native.getWindowPointer(window))
                val user = com.sun.jna.platform.win32.User32.INSTANCE
                val original = user.GetWindowLong(hwnd, -20)
                user.SetWindowLong(hwnd, -20, if (locked) original or 0x20 or 0x80000 else original and 0x20.inv())
            }
            onDispose {}
        }
        val content: @Composable () -> Unit = {
            val density = LocalDensity.current.density
            val drag = if (linux && !locked) Modifier.pointerInput(window, density) {
                var origin: java.awt.Point? = null
                var pointer: java.awt.Point? = null
                detectDragGestures(
                    onDragStart = { origin = window.location; pointer = java.awt.MouseInfo.getPointerInfo()?.location },
                    onDrag = { change, _ ->
                        val start = origin; val from = pointer; val now = java.awt.MouseInfo.getPointerInfo()?.location
                        if (start != null && from != null && now != null) {
                            val x = start.x + now.x - from.x; val y = start.y + now.y - from.y
                            window.setLocation(x, y)
                            // Absolute coordinates prevent the aligned state from snapping back on updates.
                            state.position = WindowPosition.Absolute((x * window.graphicsConfiguration.defaultTransform.scaleX / density).dp,
                                (y * window.graphicsConfiguration.defaultTransform.scaleY / density).dp)
                        }
                        change.consume()
                    },
                )
            } else Modifier
            Column(Modifier.fillMaxSize().then(drag).testTag("floating-lyrics-drag-area")
                .background(Color(if (transparent) 0xB0182024 else 0xFF182024)).padding(horizontal = 18.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (!locked || linux) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onSettingsChange(settings.copy(floatingLyricsLocked = !locked)) }) { Text(if (locked) "解锁位置" else if (!linux) "锁定并穿透" else "锁定位置", color = Color.White) }
                    IconButton(onClick = { onSettingsChange(settings.copy(floatingLyrics = false)) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, "关闭桌面歌词", tint = Color.White) }
                }
                Text(first, color = Color(0xFFABF1D9), fontSize = settings.floatingLyricsSize.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                if (second.isNotBlank()) Text(second, color = Color.White, fontSize = (settings.floatingLyricsSize * 0.72f).sp, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
        if (locked || linux) content() else WindowDraggableArea { content() }
    }
}
