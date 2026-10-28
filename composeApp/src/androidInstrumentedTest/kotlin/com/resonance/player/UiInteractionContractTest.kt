package com.resonance.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.ui.ResonanceSelectionTab
import com.resonance.player.ui.common.SoftRefinedPlaybackScrubber
import com.resonance.player.ui.common.SoftRefinedVolumeSlider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UiInteractionContractTest {
    @get:Rule val ui = createComposeRule()

    @Test fun tabsExposeSelectionAndKeepTouchTargetsInReducedMotion() {
        ui.setContent {
            var selected by remember { mutableStateOf("歌曲") }
            ResonanceTheme(reducedMotion = true) {
                Row(Modifier.width(360.dp)) {
                    listOf("歌曲", "歌单", "专辑", "歌手").forEach { label ->
                        ResonanceSelectionTab(label, label == selected, { selected = label })
                    }
                }
            }
        }
        ui.onNodeWithText("歌曲").assertIsSelected().assertHeightIsAtLeast(48.dp)
        ui.onNodeWithText("专辑").performClick().assertIsSelected()
        ui.onNodeWithText("歌曲").assertIsNotSelected()
    }

    @Test fun seekAndVolumeExposeNativeProgressActionsAndFreshCallbacks() {
        var seek = 0.2f
        var volume = 0.4f
        val generation = mutableIntStateOf(1)
        var calledGeneration = 0
        ui.setContent {
            var progress by remember { mutableFloatStateOf(seek) }
            var level by remember { mutableFloatStateOf(volume) }
            val currentGeneration = generation.intValue
            ResonanceTheme(reducedMotion = true) {
                Column(Modifier.width(360.dp)) {
                    SoftRefinedPlaybackScrubber(progress, "3:00", {
                        seek = it; progress = it; calledGeneration = currentGeneration
                    }, { _, _ -> "0:36" })
                    SoftRefinedVolumeSlider(level, { volume = it; level = it })
                }
            }
        }
        assertTrue(ui.onNodeWithContentDescription("播放进度").fetchSemanticsNode().touchBoundsInRoot.height >= with(ui.density) { 48.dp.toPx() })
        ui.onNodeWithContentDescription("播放进度").performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }
        ui.runOnIdle { assertEquals(0.75f, seek, 0.001f); generation.intValue = 2 }
        ui.onNodeWithContentDescription("播放进度").performSemanticsAction(SemanticsActions.SetProgress) { it(0.3f) }
        assertTrue(ui.onNodeWithContentDescription("音量").fetchSemanticsNode().touchBoundsInRoot.height >= with(ui.density) { 48.dp.toPx() })
        ui.onNodeWithContentDescription("音量").performSemanticsAction(SemanticsActions.SetProgress) { it(0.6f) }
        ui.runOnIdle {
            assertEquals(2, calledGeneration)
            assertEquals(0.3f, seek, 0.001f)
            assertEquals(0.6f, volume, 0.001f)
        }
    }
}
