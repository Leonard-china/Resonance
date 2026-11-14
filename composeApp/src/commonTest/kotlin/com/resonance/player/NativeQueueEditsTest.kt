package com.resonance.player

import com.resonance.player.playback.nativeQueueEdits
import kotlin.test.*

class NativeQueueEditsTest {
    @Test fun insertRemoveAndReorderNeverReplaceThePlayingItem() {
        val current = listOf("a", "playing", "b", "c")
        for (desired in listOf(listOf("playing"), listOf("c", "playing", "new", "a"), listOf("a", "playing", "c", "b"))) {
            val actual = current.toMutableList()
            var index = 1
            for (edit in nativeQueueEdits(current, desired, index, String::equals)) {
                assertFalse(index in edit.from until edit.until)
                if (edit.until <= index) index += edit.items.size - (edit.until - edit.from)
                actual.subList(edit.from, edit.until).clear()
                actual.addAll(edit.from, edit.items)
                assertEquals("playing", actual[index])
            }
            assertEquals(desired, actual)
        }
    }
    @Test fun pendingSelectionAndUnchangedQueueIssueNoNativeCommands() {
        assertTrue(nativeQueueEdits(listOf("old"), listOf("new"), 0, String::equals).isEmpty())
        assertTrue(nativeQueueEdits(listOf("a", "b"), listOf("a", "b"), 0, String::equals).isEmpty())
        assertTrue(nativeQueueEdits(emptyList<String>(), listOf("new"), -1, String::equals).isEmpty())
    }
    @Test fun changedSourceOfANextTrackIsReplacedButCurrentSourceIsPreserved() {
        val current = listOf("a" to "a.wav", "b" to "old.wav")
        val desired = listOf("a" to "a.wav", "b" to "new.wav")
        val edits = nativeQueueEdits(current, desired, 0, sameItem = { left, right -> left == right })
        assertEquals(1, edits.size)
        assertEquals(1, edits.single().from)
        assertEquals(listOf("b" to "new.wav"), edits.single().items)
        assertTrue(nativeQueueEdits(current, listOf("a" to "changed.wav"), 0, sameItem = { left, right -> left == right }).isEmpty())
    }
    @Test fun controllerWithoutSourceStillRetainsCurrentMediaById() {
        val edits = nativeQueueEdits(listOf("a" to null, "b" to null), listOf("a" to "a.wav", "c" to "c.wav"), 0,
            sameItem = { left, right -> left == right }, sameCurrentItem = { left, right -> left.first == right.first })
        assertEquals(1, edits.size)
        assertEquals(1, edits.single().from)
        assertEquals("c", edits.single().items.single().first)
    }
}
