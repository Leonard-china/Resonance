package com.resonance.player

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.PlayerState
import com.resonance.player.platform.*
import com.resonance.player.ui.TogetherCompanionPanel
import com.resonance.player.ui.TogetherInvitationPrompt
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Proxy

class TogetherCompanionUiDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    private class Room : ListenTogetherService {
        val id = "a".repeat(64)
        override val state = MutableStateFlow(TogetherState())
        override val remotePlayback = MutableStateFlow<TogetherPlayback?>(null)
        override val companion = MutableStateFlow(TogetherCompanionState(available = true, connected = true, peerId = id, peerName = "测试伙伴"))
        var save = false; var cleared = 0; var invitationResponse: Boolean? = null
        override suspend fun sendChat(text: String, peerId: String): Boolean {
            assertEquals(id, peerId)
            if (!save) return false
            companion.value = companion.value.copy(messages = listOf(TogetherChatMessage(1, true, text, 0, false)))
            return true
        }
        override fun clearChat(peerId: String) { assertEquals(id, peerId); cleared++; companion.value = companion.value.copy(messages = emptyList()) }
        override fun respondPartnerInvitation(accept: Boolean, peerId: String) { assertEquals(id, peerId); invitationResponse = accept; companion.value = companion.value.copy(incomingPartnerId = "", incomingPartnerName = "") }
        override suspend fun host(port: Int) {}
        override suspend fun join(invitation: String) { error("Must not auto-join") }
        override fun invitation(address: String, port: Int) = ""
        override fun publishPlayback(player: PlayerState) {}
        override fun leave() {}
        override fun close() {}
    }
    @Test fun failedLocalSaveKeepsDraftAndClearRequiresConfirmation() {
        val room = Room()
        ui.setContent { ResonanceTheme(reducedMotion = true) { Column(Modifier.verticalScroll(rememberScrollState())) { TogetherCompanionPanel(room) } } }
        ui.onNode(hasSetTextAction()).performTextInput("测试消息")
        ui.onNodeWithText("发送", useUnmergedTree = true).performClick()
        ui.onNode(hasSetTextAction()).assertTextContains("测试消息")
        ui.runOnIdle { room.save = true }
        ui.onNodeWithText("发送", useUnmergedTree = true).performClick()
        ui.waitForIdle()
        ui.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        ui.onNodeWithText("待发送 / 等待送达确认").assertExists()
        ui.onNodeWithText("清空本机记录").performScrollTo().performClick()
        ui.runOnIdle { assertEquals(0, room.cleared) }
        ui.onNodeWithText("清空本机聊天记录？").assertIsDisplayed()
        ui.onAllNodesWithText("清空本机记录")[1].performClick()
        ui.runOnIdle { assertEquals(1, room.cleared) }
    }
    @Test fun pairedInvitationDoesNotJoinUntilRecipientAccepts() {
        val room = Room().apply { companion.value = companion.value.copy(incomingPartnerId = id, incomingPartnerName = "测试伙伴") }
        val empty = MutableStateFlow<String?>(null)
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { proxy, method, arguments ->
            when (method.name) {
                "equals" -> proxy === arguments?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "Companion UI fixture"
                "getListenTogether" -> room; "getTogetherInvitations" -> empty; else -> null
            }
        } as PlatformServices
        ui.setContent { ResonanceTheme(reducedMotion = true) { TogetherInvitationPrompt(services, {}) } }
        ui.onNodeWithText("测试伙伴邀你一起听").assertIsDisplayed()
        ui.runOnIdle { assertNull(room.invitationResponse) }
        ui.onNodeWithText("接受邀请").performClick()
        ui.runOnIdle { assertEquals(true, room.invitationResponse) }
    }
}
