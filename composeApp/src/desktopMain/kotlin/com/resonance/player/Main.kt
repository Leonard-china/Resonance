package com.resonance.player

import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.resonance.player.platform.DesktopPlatformServices
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.resonance.player.application.ApplicationLifetime

fun main() = application {
    val services = remember { DesktopPlatformServices() }
    val lifetime = remember(services) { ApplicationLifetime(services::close) }
    val scope = rememberCoroutineScope()
    val appIcon = painterResource("resonance-icon.png")
    var visible by remember { mutableStateOf(true) }
    fun requestExit() { visible = false; scope.launch { lifetime.close().join(); exitApplication() } }
    if (java.awt.SystemTray.isSupported()) {
        val trayState = rememberTrayState()
        LaunchedEffect(services, trayState) {
            services.listenTogether.chatMessageEvents.collect {
                trayState.sendNotification(Notification("Resonance", "朋友发来一条一起听消息", Notification.Type.Info))
            }
        }
        Tray(icon = appIcon, state = trayState, tooltip = "Resonance · 后台播放与推荐", onAction = { if (!lifetime.isClosing) visible = true }) {
            Item("打开 Resonance", onClick = { if (!lifetime.isClosing) visible = true })
            Item("退出", onClick = { requestExit() })
        }
    }
    App(services, lifetime = lifetime, mainWindow = { content ->
        Window(
            onCloseRequest = { if (java.awt.SystemTray.isSupported()) { visible = false; services.foreground.value = false } else { requestExit() } },
            visible = visible,
            title = "Resonance",
            icon = appIcon,
        ) {
            window.minimumSize = java.awt.Dimension(920, 640)
            DisposableEffect(window, services) {
                services.fileDialogOwner = window
                val listener = object : java.awt.event.WindowAdapter() {
                    override fun windowIconified(event: java.awt.event.WindowEvent?) { services.foreground.value = false }
                    override fun windowDeiconified(event: java.awt.event.WindowEvent?) { services.foreground.value = true }
                    override fun windowActivated(event: java.awt.event.WindowEvent?) { services.foreground.value = true }
                    override fun windowDeactivated(event: java.awt.event.WindowEvent?) { services.foreground.value = false }
                }
                window.addWindowListener(listener)
                onDispose { services.fileDialogOwner = null; window.removeWindowListener(listener) }
            }
            com.resonance.player.platform.DesktopMusicDropHost(services, content)
        }
    })
}
