package com.resonance.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.platform.updateNativePlaybackQueue
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Media3 timeline mutations; no audio preparation, user files or service connection. */
@RunWith(AndroidJUnit4::class)
class NativePlaybackQueueDeviceTest {
    private fun items(vararg ids: String) = ids.map { MediaItem.Builder().setMediaId(it).setUri("file:///fixture-$it.wav").build() }

    @Test fun queueEditsPreservePositionAndPlayIntentWithoutReplacingCurrentMedia() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val player = ExoPlayer.Builder(instrumentation.targetContext).build()
            try {
                for (playing in listOf(false, true)) {
                    player.setMediaItems(items("a", "current", "b", "c"), 1, 4321L)
                    player.playWhenReady = playing
                    updateNativePlaybackQueue(player, "current", items("c", "current", "new", "a"))
                    assertEquals(listOf("c", "current", "new", "a"), (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId })
                    assertEquals("current", player.currentMediaItem?.mediaId)
                    assertEquals(4321L, player.currentPosition)
                    assertEquals(playing, player.playWhenReady)
                    updateNativePlaybackQueue(player, "current", items("current"))
                    assertEquals(1, player.mediaItemCount)
                    assertEquals(4321L, player.currentPosition)
                    updateNativePlaybackQueue(player, "pending", items("pending"))
                    assertEquals("current", player.currentMediaItem?.mediaId)
                }
            } finally { player.release() }
        }
    }
}
