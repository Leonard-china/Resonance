package com.resonance.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.audio.PreampAudioProcessor
import com.resonance.player.audio.AndroidLoudnessAnalyzer
import com.resonance.player.audio.LoudnessAnalysis
import kotlinx.coroutines.runBlocking
import com.resonance.player.playback.PlaybackSnapshotCodec
import com.google.common.util.concurrent.ListenableFuture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AudioSessionIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun commandsAreGrantedAndBackgroundTrackChangesApplyStoredVolumes() {
        val preferences = context.getSharedPreferences("resonance_ui_state", Context.MODE_PRIVATE)
        val original = preferences.all
        preferences.edit().putBoolean("gain.auto_enabled", false).putFloat("vol.track.test-a", 0.2f)
            .putFloat("vol.track.test-b", 0.7f).commit()
        val audio = context.cacheDir.resolve("audio-session-fixture.wav")
        audio.writeBytes(silentWave())
        var future: ListenableFuture<MediaController>? = null
        instrumentation.runOnMainSync {
            future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        }
        val controller = future!!.get(15, TimeUnit.SECONDS)
        try {
            var result: ListenableFuture<SessionResult>? = null
            instrumentation.runOnMainSync {
                assertTrue(controller.isSessionCommandAvailable(AudioSessionCommands.command(AudioSessionCommands.GAIN)))
                assertTrue(controller.isSessionCommandAvailable(AudioSessionCommands.command(AudioSessionCommands.VOLUME)))
                result = controller.sendCustomCommand(AudioSessionCommands.command(AudioSessionCommands.GAIN), Bundle().apply { putFloat("gainDb", 6f) })
            }
            assertEquals(SessionResult.RESULT_SUCCESS, result!!.get(5, TimeUnit.SECONDS).resultCode)
            instrumentation.runOnMainSync {
                result = controller.sendCustomCommand(AudioSessionCommands.command(AudioSessionCommands.VOLUME), Bundle().apply { putFloat("volume", 0.31f) })
            }
            assertEquals(SessionResult.RESULT_SUCCESS, result!!.get(5, TimeUnit.SECONDS).resultCode)
            val ready = CountDownLatch(1)
            val switched = CountDownLatch(1)
            val nextVolume = CountDownLatch(1)
            instrumentation.runOnMainSync {
                controller.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) { if (playbackState == Player.STATE_READY) ready.countDown() }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { if (mediaItem?.mediaId == "test-b") switched.countDown() }
                    override fun onVolumeChanged(volume: Float) { if (kotlin.math.abs(volume - 0.7f) < 0.001f) nextVolume.countDown() }
                })
                controller.setMediaItems(listOf("test-a", "test-b").map {
                    MediaItem.Builder().setMediaId(it).setUri(Uri.fromFile(audio)).build()
                })
                controller.prepare()
            }
            assertTrue(ready.await(15, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { assertEquals(0.2f, controller.volume, 0.001f); controller.seekToNextMediaItem() }
            assertTrue(switched.await(5, TimeUnit.SECONDS))
            assertTrue("Wait for the service volume event, rather than the controller's optimistic seek event", nextVolume.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { assertEquals(0.7f, controller.volume, 0.001f) }
            val snapshot = PlaybackSnapshotCodec.decode(preferences.getString("playback.snapshot", null))
            assertNotNull(snapshot)
            assertEquals("test-b", snapshot!!.trackId)
            assertEquals(listOf("test-a", "test-b"), snapshot.queueIds)
        } finally {
            instrumentation.runOnMainSync { controller.clearMediaItems(); controller.release() }
            audio.delete()
            val editor = preferences.edit()
            for (key in listOf("gain.auto_enabled", "vol.track.test-a", "vol.track.test-b", "playback.snapshot")) {
                editor.remove(key)
                when (val value = original[key]) { is Boolean -> editor.putBoolean(key, value); is Float -> editor.putFloat(key, value); is String -> editor.putString(key, value) }
            }
            editor.commit()
        }
    }

    @Test fun processorAmplifiesActualPcmAndSupportsMono() {
        for (channels in listOf(1, 2)) {
            val processor = PreampAudioProcessor()
            processor.configure(AudioProcessor.AudioFormat(48_000, channels, C.ENCODING_PCM_16BIT))
            processor.setGainDb(6.0206f)
            processor.flush()
            val input = ByteBuffer.allocateDirect(100 * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
            repeat(100 * channels) { input.putShort(8192) }
            input.flip()
            processor.queueInput(input)
            val output = processor.output.order(ByteOrder.LITTLE_ENDIAN)
            assertTrue(kotlin.math.abs(output.short.toInt() - 16384) <= 2)
        }
    }

    @Test fun bundledFfmpegMeasuresAndCachesWholeTrackLoudness() = runBlocking {
        val analyzer = AndroidLoudnessAnalyzer(context)
        val preferences = context.getSharedPreferences("resonance_ui_state", Context.MODE_PRIVATE)
        val ids = listOf("r128-test-quiet", "r128-test-loud", "r128-test-silent")
        val files = ids.map { context.cacheDir.resolve("$it.wav") }
        fun clearCache() {
            val editor = preferences.edit()
            ids.forEach { editor.remove("gain.auto.${LoudnessAnalysis.CACHE_VERSION}.$it") }
            editor.commit()
        }
        clearCache()
        try {
            files[0].writeBytes(wave(0.05))
            files[1].writeBytes(wave(0.75))
            files[2].writeBytes(wave(0.0))
            val quiet = analyzer.analyze(ids[0], files[0].absolutePath)
            val loud = analyzer.analyze(ids[1], files[1].absolutePath)
            assertTrue("Quiet track should receive positive gain: $quiet", quiet > 0f && quiet <= 14f)
            assertTrue("Loud track should be attenuated: $loud", loud < 0f)
            assertEquals(quiet, analyzer.cached(ids[0]))
            analyzer.analyze(ids[2], files[2].absolutePath)
            assertNull("Silence must not be cached as a successful measurement", analyzer.cached(ids[2]))
        } finally { files.forEach { it.delete() }; clearCache() }
    }

    @Test fun r128JsonIsParsedOnTheAndroidRegexEngine() {
        val report = """{
            "input_i" : "-26.75",
            "input_tp" : "-26.02"
        }"""
        assertEquals(12.75f, LoudnessAnalysis.gainFromOutput(report))
    }

    private fun silentWave(): ByteArray = wave(0.0)

    private fun wave(amplitude: Double): ByteArray {
        val size = 48_000 * 4 * 2
        return ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + size); put("WAVEfmt ".toByteArray()); putInt(16)
            putShort(1); putShort(2); putInt(48_000); putInt(48_000 * 4); putShort(4); putShort(16)
            put("data".toByteArray()); putInt(size)
            repeat(size / 4) { frame ->
                val sample = (kotlin.math.sin(2.0 * Math.PI * 440.0 * frame / 48_000) * amplitude * 32767).toInt().toShort()
                putShort(sample); putShort(sample)
            }
        }.array()
    }
}
