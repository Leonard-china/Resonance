package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
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
    LaunchedEffect(settings.floatingLyricsSize, locked) {
        state.size = DpSize(state.size.width, (settings.floatingLyricsSize * 4.3 + if (locked) 24 else 64).dp)
    }
    val (first, second) = floatingLyricLines(player, lyrics, settings)
    Window(onCloseRequest = { onSettingsChange(settings.copy(floatingLyrics = false)) }, title = "Resonance 桌面歌词", state = state,
        transparent = true, undecorated = true, alwaysOnTop = true, focusable = !locked, resizable = !locked) {
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
            Column(Modifier.fillMaxSize().background(Color(0xB0182024)).padding(horizontal = 18.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (!locked) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onSettingsChange(settings.copy(floatingLyricsLocked = true)) }) { Text("锁定并穿透", color = Color.White) }
                    IconButton(onClick = { onSettingsChange(settings.copy(floatingLyrics = false)) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, "关闭桌面歌词", tint = Color.White) }
                }
                Text(first, color = Color(0xFFABF1D9), fontSize = settings.floatingLyricsSize.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                if (second.isNotBlank()) Text(second, color = Color.White, fontSize = (settings.floatingLyricsSize * 0.72f).sp, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
        if (locked) content() else WindowDraggableArea { content() }
    }
}
