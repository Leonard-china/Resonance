package com.resonance.player

import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.test.core.app.ActivityScenario
import com.resonance.player.platform.AndroidPlatformServices
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AudioControllerLifecycleDeviceTest {
    private fun services(activity: MainActivity) = MainActivity::class.java.getDeclaredField("services").run {
        isAccessible = true; get(activity) as AndroidPlatformServices
    }
    private fun controller(services: AndroidPlatformServices) = AndroidPlatformServices::class.java.getDeclaredField("player").run {
        isAccessible = true; get(services) as? MediaController
    }
    private fun service() = PlaybackService::class.java.getDeclaredField("activeService").run {
        isAccessible = true; get(null) as PlaybackService
    }
    private fun sessionOf(service: PlaybackService) = PlaybackService::class.java.getDeclaredField("mediaSession").run {
        isAccessible = true; get(service) as MediaSession
    }
    private fun gain(service: PlaybackService) = PlaybackService::class.java.getDeclaredField("appliedGainDb").run {
        isAccessible = true; getFloat(service)
    }
    private suspend fun until(condition: () -> Boolean) = withTimeout(10_000) {
        while (!withContext(Dispatchers.Main.immediate) { condition() }) delay(25)
    }

    @Test fun revokedGainDefersUntilGrantedAndDisposedActivityCannotChangeNewController() = runBlocking<Unit> {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var old: AndroidPlatformServices
        scenario.onActivity { old = services(it) }
        var heldSession: MediaSession? = null
        try {
            until { controller(old)?.isConnected == true }
            val native = withContext(Dispatchers.Main.immediate) { service() }
            val activeSession = withContext(Dispatchers.Main.immediate) { sessionOf(native) }
            heldSession = activeSession
            withContext(Dispatchers.Main.immediate) {
                assertTrue(controller(old)!!.isSessionCommandAvailable(AudioSessionCommands.command(AudioSessionCommands.GAIN)))
                val withoutGain = AudioSessionCommands.available().buildUpon().remove(AudioSessionCommands.command(AudioSessionCommands.GAIN)).build()
                activeSession.connectedControllers.forEach { activeSession.setAvailableCommands(it, withoutGain, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS) }
            }
            until { controller(old)?.isSessionCommandAvailable(AudioSessionCommands.command(AudioSessionCommands.GAIN)) == false }
            withContext(Dispatchers.Main.immediate) { old.setTrackGain(6f, true) }
            delay(150)
            withContext(Dispatchers.Main.immediate) {
                assertFalse(kotlin.math.abs(gain(native) - 6f) < .01f)
                activeSession.connectedControllers.forEach { activeSession.setAvailableCommands(it, AudioSessionCommands.available(), MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS) }
            }
            until { kotlin.math.abs(gain(native) - 6f) < .01f }
            scenario.recreate()
            lateinit var fresh: AndroidPlatformServices
            scenario.onActivity { fresh = services(it); old.close(); old.setTrackGain(-12f, true); old.setVolume(.99f); old.close() }
            until { controller(fresh)?.isConnected == true }
            val freshNative = withContext(Dispatchers.Main.immediate) { service() }
            withContext(Dispatchers.Main.immediate) { fresh.setTrackGain(0f, true) }
            until { kotlin.math.abs(gain(freshNative)) < .01f }
            withContext(Dispatchers.Main.immediate) { old.setTrackGain(12f, true) }
            delay(150)
            withContext(Dispatchers.Main.immediate) { assertEquals(0f, gain(freshNative), .01f) }
        } finally {
            withContext(Dispatchers.Main.immediate) {
                heldSession?.let { active -> active.connectedControllers.forEach { active.setAvailableCommands(it, AudioSessionCommands.available(), MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS) } }
            }
            scenario.close()
        }
    }
}
