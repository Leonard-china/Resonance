package com.resonance.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.ThemeMode
import com.resonance.player.ui.horizontalPageSwipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HorizontalPageSwipeTest {
    @get:Rule val ui = createComposeRule()
    private val page = mutableIntStateOf(0)
    private val value = mutableFloatStateOf(0.3f)
    private fun show() = ui.setContent {
        ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
            Column(Modifier.width(400.dp).height(500.dp).horizontalPageSwipe(page.intValue, 3) { page.intValue = it }) {
                Box(Modifier.fillMaxWidth().height(80.dp).testTag("swipe-space")) { Text("Page " + page.intValue) }
                Slider(value.floatValue, { value.floatValue = it }, Modifier.fillMaxWidth().testTag("child-slider"))
                LazyColumn(Modifier.weight(1f).testTag("vertical-list")) {
                    items((0..99).toList()) { Text("Row " + it, Modifier.fillMaxWidth().height(48.dp)) }
                }
            }
        }
    }
    @Test fun horizontalSwipesChangeOnePageAndDoNotWrapAtEdges() {
        show()
        repeat(3) {
            ui.onNodeWithTag("swipe-space").performTouchInput {
                swipe(Offset(width * 0.8f, height / 2f), Offset(width * 0.2f, height / 2f))
            }
        }
        ui.runOnIdle { assertEquals(2, page.intValue) }
        ui.onNodeWithTag("swipe-space").performTouchInput {
            swipe(Offset(width * 0.2f, height / 2f), Offset(width * 0.8f, height / 2f))
        }
        ui.runOnIdle { assertEquals(1, page.intValue) }
    }
    @Test fun verticalScrollAndSliderDragDoNotChangePage() {
        show()
        ui.onNodeWithTag("vertical-list").performTouchInput {
            swipe(Offset(width / 2f, height * 0.8f), Offset(width / 2f, height * 0.2f))
        }
        ui.onNodeWithTag("child-slider").performTouchInput {
            swipe(Offset(width * 0.3f, height / 2f), Offset(width * 0.8f, height / 2f))
        }
        ui.runOnIdle { assertEquals(0, page.intValue); assertTrue(value.floatValue > 0.6f) }
    }
}
