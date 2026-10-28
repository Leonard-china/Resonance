package com.resonance.player

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.PlayerState
import com.resonance.player.platform.*
import com.resonance.player.ui.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Proxy

class TogetherUiDeviceTest {
    @get:Rule val ui = createComposeRule()
    private class Room : ListenTogetherService {
        override val state = MutableStateFlow(TogetherState())
        override val remotePlayback = MutableStateFlow<TogetherPlayback?>(null)
        var hosts = 0; var joins = 0; var leaves = 0
        override suspend fun host(port: Int) { hosts++; state.value = TogetherState(TogetherRole.Host, TogetherStatus.Listening, listOf("192.0.2.1"), port) }
        override suspend fun join(invitation: String) { joins++; state.value = TogetherState(TogetherRole.Guest, TogetherStatus.Connected) }
        override fun invitation(address: String, port: Int) = "fixture-invitation"
        override fun publishPlayback(player: PlayerState) {}
        override fun leave() { leaves++; state.value = TogetherState() }
        override fun close() {}
    }
    private fun show(room: Room) {
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, _ ->
            if (method.name == "getListenTogether") room else null
        } as PlatformServices
        ui.setContent {
            val scope = rememberCoroutineScope()
            val features = remember { MusicFeatureController(services, scope) }
            ResonanceTheme(reducedMotion = true) { CompositionLocalProvider(LocalMusicFeatures provides features) { TogetherScreen(true, {}) } }
        }
    }

    @Test fun openingOrPastingAnInvitationDoesNotConnectUntilExplicitJoin() {
        val room = Room(); show(room)
        ui.onNodeWithText("加入朋友").performScrollTo().performClick()
        ui.onNode(hasSetTextAction()).performScrollTo().performTextInput("fixture-invitation")
        ui.runOnIdle { assertEquals(0, room.joins); assertEquals(0, room.hosts) }
        ui.onNodeWithText("加密连接并跟听").performScrollTo().performClick()
        ui.onNodeWithText("跟听中 · 由房主控制播放").assertExists()
        ui.runOnIdle { assertEquals(1, room.joins) }
        ui.onNodeWithText("退出一起听").performScrollTo().performClick()
        ui.runOnIdle { assertEquals(1, room.leaves) }
    }

    @Test fun roomCreationAndClosureUseExplicitActionsAndKeepPortVisible() {
        val room = Room(); show(room)
        ui.runOnIdle { assertEquals(0, room.hosts) }
        ui.onNodeWithText("创建双人房间").performScrollTo().performClick()
        ui.onNodeWithText("复制加密邀请").performScrollTo().assertIsDisplayed()
        ui.runOnIdle { assertEquals(1, room.hosts); assertEquals(45873, room.state.value.port) }
        ui.onNodeWithText("结束房间").performScrollTo().performClick()
        ui.runOnIdle { assertEquals(1, room.leaves) }
    }
}
