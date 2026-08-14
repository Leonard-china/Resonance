package com.resonance.player

import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
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
        App(services)
    }
}
