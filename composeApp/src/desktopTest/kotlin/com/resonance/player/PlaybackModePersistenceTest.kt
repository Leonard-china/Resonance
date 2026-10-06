package com.resonance.player

import com.resonance.player.model.PlaybackModePreference
import com.resonance.player.model.PlayerState
import com.resonance.player.model.RepeatMode
import com.resonance.player.platform.DesktopPlatformServices
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackModePersistenceTest {
    @Test
    fun defaultPlaybackModePreferenceIsDisabledAndRepeatOff() {
        val pref = PlaybackModePreference()
        assertFalse(pref.shuffleEnabled)
        assertEquals(RepeatMode.Off, pref.repeatMode)
    }

    @Test
    fun playerStatePreservesPlaybackModeWhenResettingCurrentTrack() {
        val original = PlayerState(
            currentTrack = null,
            isPlaying = true,
            progress = 0.5f,
            shuffleEnabled = true,
            repeatMode = RepeatMode.All,
        )
        val reset = PlayerState(
            shuffleEnabled = original.shuffleEnabled,
            repeatMode = original.repeatMode,
        )
        assertTrue(reset.shuffleEnabled)
        assertEquals(RepeatMode.All, reset.repeatMode)
        assertFalse(reset.isPlaying)
        assertEquals(0f, reset.progress)
    }

    @Test
    fun desktopPlatformServicesPersistsAndLoadsPlaybackMode() = runBlocking {
        val tempDir = Files.createTempDirectory("resonance-playback-test-")
        val previousEnv = System.getProperty("RESONANCE_DATA")
        try {
            System.setProperty("RESONANCE_DATA", tempDir.toString())
            val services = DesktopPlatformServices()

            // Initially default
            val initial = services.loadPlaybackMode()
            assertFalse(initial.shuffleEnabled)
            assertEquals(RepeatMode.Off, initial.repeatMode)

            // Save shuffle enabled and repeat all
            services.savePlaybackMode(shuffle = true, repeatMode = RepeatMode.All)
            val updated = services.loadPlaybackMode()
            assertTrue(updated.shuffleEnabled)
            assertEquals(RepeatMode.All, updated.repeatMode)

            // Save shuffle disabled and repeat one
            services.savePlaybackMode(shuffle = false, repeatMode = RepeatMode.One)
            val updated2 = services.loadPlaybackMode()
            assertFalse(updated2.shuffleEnabled)
            assertEquals(RepeatMode.One, updated2.repeatMode)

            // Test setPlaybackMode also persists
            services.setPlaybackMode(shuffle = true, repeatMode = RepeatMode.Off)
            val updated3 = services.loadPlaybackMode()
            assertTrue(updated3.shuffleEnabled)
            assertEquals(RepeatMode.Off, updated3.repeatMode)
        } finally {
            if (previousEnv != null) {
                System.setProperty("RESONANCE_DATA", previousEnv)
            } else {
                System.clearProperty("RESONANCE_DATA")
            }
            tempDir.toFile().deleteRecursively()
        }
    }
}
