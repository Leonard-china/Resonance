package com.resonance.player

import com.resonance.player.platform.PlaybackIssue
import kotlin.test.*

class PlaybackIssueTest {
    @Test fun oldSameTrackAttemptIsRejectedAfterAtoBtoA() {
        val old = PlaybackIssue("decode failed", trackId = "a", requestId = 1)
        assertTrue(old.matches("a", 1))
        assertFalse(old.matches("b", 2))
        assertFalse(old.matches("a", 3))
    }
    @Test fun previewAndStorageFailureNeverInterruptMainPlayback() {
        for (source in PlaybackIssue.Source.entries.filter { it != PlaybackIssue.Source.Playback }) {
            val issue = PlaybackIssue("failure", source, "preview", 1)
            assertFalse(issue.interruptsPlayback)
            assertTrue(issue.matches("main", 3))
        }
    }
}
