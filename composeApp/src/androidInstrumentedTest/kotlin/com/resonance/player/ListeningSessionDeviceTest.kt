package com.resonance.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.playback.ListeningStatsRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Requires an explicitly isolated applicationId; never changes an installed user's preferences. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ListeningSessionDeviceTest {
    private suspend fun await(message: String, condition: () -> Boolean) = withTimeout(15_000) {
        while (!withContext(Dispatchers.Main.immediate) { condition() }) delay(30)
        assertTrue(message, withContext(Dispatchers.Main.immediate) { condition() })
    }

    @Test fun activityRecreationBackgroundPauseSeekAndNativeRepeatKeepAccurateStats() = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("isolatedNative") == "1")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.startsWith("com.resonance.player.validation")) { "Use a separate validation applicationId" }
        context.getSharedPreferences("resonance_ui_state", Context.MODE_PRIVATE).edit()
            .putFloat("vol.global", 0f).putBoolean("gain.auto_enabled", false).commit()
        val audio = context.cacheDir.resolve("native-listening-${System.nanoTime()}.wav")
        val byteCount = 48_000 * 60 * 4
        val wave = ByteBuffer.allocate(44 + byteCount).order(ByteOrder.LITTLE_ENDIAN)
        wave.put("RIFF".toByteArray()).putInt(36 + byteCount).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(2).putInt(48_000).putInt(192_000).putShort(4).putShort(16)
            .put("data".toByteArray()).putInt(byteCount)
        audio.writeBytes(wave.array()) // Generated silence still traverses the real decoder/audio sink.
        val id = "native-listening-${System.nanoTime()}"
        val repository = ListeningStatsRepository.get(context.filesDir.toPath())
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val future = withContext(Dispatchers.Main.immediate) {
            MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        }
        val controller = future.get(15, TimeUnit.SECONDS)
        fun seconds() = repository.store.getTrackStats(id)?.totalSeconds ?: 0L
        fun completions() = repository.store.getTrackStats(id)?.playCount ?: 0
        val started = android.os.SystemClock.elapsedRealtime()
        try {
            delay(1000) // Let the fresh empty-library UI finish its initial restoration.
            withContext(Dispatchers.Main.immediate) {
                controller.volume = 0f
                controller.repeatMode = Player.REPEAT_MODE_OFF
                controller.setMediaItem(MediaItem.Builder().setMediaId(id).setUri(Uri.fromFile(audio)).build())
                controller.prepare(); controller.play()
            }
            await("Native service must count without UI bookkeeping") { controller.isPlaying && seconds() >= 2 }
            val beforeRecreate = seconds()
            scenario.recreate()
            await("Recreated Activity must not interrupt service accounting") { controller.isPlaying && seconds() >= beforeRecreate + 2 }
            scenario.moveToState(Lifecycle.State.CREATED)
            val beforeBackground = seconds()
            await("Background playback must continue accounting") { seconds() >= beforeBackground + 2 }
            withContext(Dispatchers.Main.immediate) { controller.pause() }
            await("Native pause must settle") { !controller.isPlaying }
            repository.flush()
            val paused = seconds()
            delay(1200)
            repository.flush()
            assertEquals("Paused time must not be counted", paused, seconds())
            withContext(Dispatchers.Main.immediate) { controller.seekTo(57_000) }
            delay(200)
            assertEquals("A seek alone is not a completed listen", 0, completions())
            withContext(Dispatchers.Main.immediate) { controller.play() }
            await("95% must complete exactly once") { completions() == 1 }
            repeat(2) {
                withContext(Dispatchers.Main.immediate) { controller.pause() }
                delay(100)
                withContext(Dispatchers.Main.immediate) { controller.play() }
                delay(250)
            }
            repository.flush()
            assertEquals("Resume must preserve this pass's completion", 1, completions())
            withContext(Dispatchers.Main.immediate) {
                controller.repeatMode = Player.REPEAT_MODE_ONE
                controller.seekTo(59_700)
            }
            await("Native repeat must start another pass") { controller.isPlaying && controller.currentPosition < 1500 }
            val beforeRepeat = seconds()
            await("Repeated track must count new listening time") { seconds() >= beforeRepeat + 1 }
            withContext(Dispatchers.Main.immediate) { controller.seekTo(57_000) }
            await("New repeat may complete once") { completions() == 2 }
            withContext(Dispatchers.Main.immediate) { controller.pause() }
            repository.flush()
            val elapsedSeconds = (android.os.SystemClock.elapsedRealtime() - started) / 1000
            assertTrue("Seek jumps must not add minutes to statistics", seconds() <= elapsedSeconds + 1)
        } finally {
            withContext(Dispatchers.Main.immediate) { controller.stop(); controller.clearMediaItems(); MediaController.releaseFuture(future) }
            repository.flush()
            scenario.close()
            audio.delete() // This test's uniquely named cache fixture only.
        }
    }
}
