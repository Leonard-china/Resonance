package com.resonance.player

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.ui.AssistantAudioDeletionDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AssistantDeletionConfirmationDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    @Test fun concreteFilesAreShownAndNoOperationOccursUntilExplicitConfirmation() {
        var confirmed = 0; var cancelled = 0
        val request = AudioDeletionRequest("fixture", listOf(Track("a", "Generated A", "Fixture", "", "1:00", 0,
            sourceUri = "/generated/a.mp3"), Track("b", "Generated B", "Fixture", "", "1:00", 0, sourceUri = "/generated/b.mp3")), "session", 0)
        ui.setContent { ResonanceTheme { AssistantAudioDeletionDialog(request, { cancelled++ }, { confirmed++ }) } }
        ui.onNodeWithText("a.mp3").assertIsDisplayed(); ui.onNodeWithText("b.mp3").assertIsDisplayed()
        ui.runOnIdle { assertEquals(0, confirmed) }
        ui.onNodeWithText("取消").performClick(); ui.runOnIdle { assertEquals(1, cancelled); assertEquals(0, confirmed) }
        ui.onNodeWithText("确认删除这些文件").performClick(); ui.runOnIdle { assertEquals(1, confirmed) }
    }
}
