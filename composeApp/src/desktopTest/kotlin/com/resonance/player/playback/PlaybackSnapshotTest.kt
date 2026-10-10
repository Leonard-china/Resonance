package com.resonance.player.playback

import kotlin.test.*

class PlaybackSnapshotTest {
    @Test fun persistsCurrentTrackQueuePositionAndPlaylist() {
        val snapshot = PlaybackSnapshot("b", listOf("a", "b", "c"), 0.42f, "playlist")
        assertEquals(snapshot, PlaybackSnapshotCodec.decode(PlaybackSnapshotCodec.encode(snapshot)))
    }
    @Test fun malformedSnapshotDoesNotPreventStartup() {
        assertNull(PlaybackSnapshotCodec.decode("broken"))
        assertNull(PlaybackSnapshotCodec.decode("{}"))
        assertNull(PlaybackSnapshotCodec.decode(null))
    }
}
