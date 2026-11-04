package com.resonance.player.playback

import com.resonance.player.audio.DesktopPcmPlayer
import com.resonance.player.audio.DesktopPlaybackEvent
import com.resonance.player.model.Track
import com.resonance.player.platform.PlaybackIssue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.*

class DesktopPlaybackFailureTest {
    @Test fun actualDecoderFailuresCarryOriginAndRequestAcrossAtoBtoA() {
        val events = LinkedBlockingQueue<DesktopPlaybackEvent>()
        val player = DesktopPcmPlayer({}, {}, {}, {}, { events.add(it) })
        // Missing URI fails before FFmpeg or an audio device is needed.
        val track = Track("a", "fixture", "", "", "1:00", 0, sourceUri = null)
        try {
            fun fail(track: Track): DesktopPlaybackEvent {
                player.play(track)
                val buffering = assertNotNull(events.poll(5, TimeUnit.SECONDS))
                assertEquals(DesktopPlaybackEvent.Kind.Buffering, buffering.kind)
                return assertNotNull(events.poll(5, TimeUnit.SECONDS)).also {
                    assertEquals(DesktopPlaybackEvent.Kind.Error, it.kind)
                    assertEquals(buffering.requestId, it.requestId)
                }
            }
            val first = fail(track)
            fail(track.copy(id = "b"))
            val current = fail(track)
            assertEquals("a", first.snapshot.currentTrack?.id)
            assertNotEquals(first.requestId, current.requestId)
            assertFalse(PlaybackIssue(first.message, trackId = "a", requestId = first.requestId).matches("a", player.currentRequestId))
            assertTrue(PlaybackIssue(current.message, trackId = "a", requestId = current.requestId).matches("a", player.currentRequestId))
        } finally { player.close() }
    }
}
