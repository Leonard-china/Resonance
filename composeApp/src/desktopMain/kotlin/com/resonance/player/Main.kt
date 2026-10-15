package com.resonance.player

import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.Tray
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.resonance.player.platform.DesktopPlatformServices

fun main() = application {
    val services = remember { DesktopPlatformServices() }
    val appIcon = painterResource("resonance-icon.png")
    var visible by remember { mutableStateOf(true) }
    if (java.awt.SystemTray.isSupported()) {
        Tray(icon = appIcon, tooltip = "Resonance · 后台播放与推荐", onAction = { visible = true }) {
            Item("打开 Resonance", onClick = { visible = true })
            Item("退出", onClick = { services.close(); exitApplication() })
        }
    }
    Window(
        onCloseRequest = { if (java.awt.SystemTray.isSupported()) { visible = false; services.foreground.value = false } else { services.close(); exitApplication() } },
        visible = visible,
        title = "Resonance",
        icon = appIcon,
    ) {
        window.minimumSize = java.awt.Dimension(920, 640)
        DisposableEffect(window, services) {
            val listener = object : java.awt.event.WindowAdapter() {
                override fun windowIconified(event: java.awt.event.WindowEvent?) { services.foreground.value = false }
                override fun windowDeiconified(event: java.awt.event.WindowEvent?) { services.foreground.value = true }
                override fun windowActivated(event: java.awt.event.WindowEvent?) { services.foreground.value = true }
                override fun windowDeactivated(event: java.awt.event.WindowEvent?) { services.foreground.value = false }
            }
            window.addWindowListener(listener)
            onDispose { window.removeWindowListener(listener) }
        }
        App(services)
    }
}
