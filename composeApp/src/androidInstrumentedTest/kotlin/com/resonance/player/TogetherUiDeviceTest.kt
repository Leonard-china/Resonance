package com.resonance.player

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    private class Room : ListenTogetherService {
        override val state = MutableStateFlow(TogetherState())
        override val remotePlayback = MutableStateFlow<TogetherPlayback?>(null)
        var hosts = 0; var joins = 0; var leaves = 0
        val incoming = MutableStateFlow<String?>(null)
        var approval: Boolean? = null
        override suspend fun host(port: Int) { hosts++; state.value = TogetherState(TogetherRole.Host, TogetherStatus.Listening, listOf("192.0.2.1"), port, invitationExpiresAtMs = System.currentTimeMillis() + 600_000) }
        override suspend fun join(invitation: String) { joins++; state.value = TogetherState(TogetherRole.Guest, TogetherStatus.Connected) }
        override fun invitation(address: String, port: Int) = "fixture-invitation"
        override fun inspectInvitation(value: String) = if (value == "fixture-invitation") TogetherInvitationPreview("192.0.2.1", 45873) else null
        override fun approveJoin(allow: Boolean) { approval = allow }
        override fun publishPlayback(player: PlayerState) {}
        override fun leave() { leaves++; state.value = TogetherState() }
        override fun close() {}
    }
    private fun show(room: Room) {
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { proxy, method, arguments ->
            when (method.name) {
                "equals" -> proxy === arguments?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "Together UI fixture"
                "getListenTogether" -> room
                "getTogetherInvitations" -> room.incoming
                "dismissTogetherInvitation" -> { room.incoming.value = null; Unit }
                "currentTimeMillis" -> System.currentTimeMillis()
                "automaticRecommendationAllowed" -> true
                else -> null
            }
        } as PlatformServices
        ui.setContent {
            val scope = rememberCoroutineScope()
            val features = remember { MusicFeatureController(services, scope) }
            ResonanceTheme(reducedMotion = true) { CompositionLocalProvider(LocalMusicFeatures provides features) {
                TogetherScreen(true, {})
                TogetherInvitationPrompt(services, {})
            } }
        }
    }

    @Test fun externalInvitationShowsAddressWithoutAutomaticallyConnecting() {
        val room = Room().apply { incoming.value = "fixture-invitation" }
        show(room)
        ui.onNodeWithText("加入朋友的一起听？").assertIsDisplayed()
        ui.runOnIdle { assertEquals(0, room.joins) }
        ui.onNodeWithText("取消").performClick()
        ui.runOnIdle { assertEquals(0, room.joins); room.incoming.value = "fixture-invitation" }
        ui.onNodeWithText("确认加入").performClick()
        ui.runOnIdle { assertEquals(1, room.joins) }
    }

    @Test fun openingOrPastingAnInvitationDoesNotConnectUntilExplicitJoin() {
        val room = Room(); show(room)
        ui.onNodeWithText("加入朋友").performScrollTo().performClick()
        ui.onNode(hasSetTextAction()).performScrollTo().performTextInput("fixture-invitation")
        ui.runOnIdle { assertEquals(0, room.joins); assertEquals(0, room.hosts) }
        ui.onNodeWithText("加密连接并跟听").performScrollTo().performClick()
        ui.runOnIdle { assertEquals(0, room.joins) }
        ui.onNodeWithText("确认加入").performClick()
        ui.onNodeWithText("已连接，播放已暂停").assertExists()
        ui.runOnIdle { assertEquals(1, room.joins) }
        ui.onNodeWithText("退出一起听").performClick()
        ui.runOnIdle { assertEquals(1, room.leaves) }
    }

    @Test fun incomingFriendMustBeApprovedAndControlsRemainAccessible() {
        val room = Room().apply { state.value = TogetherState(TogetherRole.Host, TogetherStatus.AwaitingApproval,
            pendingJoin = true, invitationExpiresAtMs = System.currentTimeMillis() + 600_000) }
        show(room)
        ui.runOnIdle { assertEquals(null, room.approval) }
        ui.onNodeWithText("同意加入").performScrollTo().performClick()
        ui.runOnIdle { assertEquals(true, room.approval) }
        ui.onNodeWithText("结束房间").performClick()
        ui.runOnIdle { assertEquals(1, room.leaves) }
    }

    @Test fun roomCreationAndClosureUseExplicitActionsAndKeepPortVisible() {
        val room = Room(); show(room)
        ui.runOnIdle { assertEquals(0, room.hosts) }
        ui.onNodeWithText("创建房间").performScrollTo().performClick()
        ui.onNodeWithText("复制邀请链接").assertIsDisplayed()
        ui.runOnIdle { assertEquals(1, room.hosts); assertEquals(45873, room.state.value.port) }
        ui.onNodeWithText("结束房间").performClick()
        ui.runOnIdle { assertEquals(1, room.leaves) }
    }
}
