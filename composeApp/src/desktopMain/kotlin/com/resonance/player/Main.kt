package com.resonance.player

import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.resonance.player.platform.DesktopPlatformServices

fun main() = application {
    val services = remember { DesktopPlatformServices() }
    val appIcon = painterResource("resonance-icon.png")
    Window(
        onCloseRequest = ::exitApplication,
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
