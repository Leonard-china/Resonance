package com.resonance.player

import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.platform.*
import com.resonance.player.together.AndroidTogetherRuntime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class TogetherNativePlaybackDeviceTest {
    @Test fun mumuNativeHostKeepsPublishingAfterActivityGoesIntoBackground() = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("nativeHost") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "native-bridge").apply { mkdirs() }
        val audio = File(directory, "host.mp3").also { check(it.isFile) }
        val prefs = context.getSharedPreferences("resonance_ui_state", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString("playback.snapshot", null)
        val runtime = AndroidTogetherRuntime.get(context)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var controller: MediaController? = null
        try {
            val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
            controller = withContext(Dispatchers.IO) { future.get(15, java.util.concurrent.TimeUnit.SECONDS) }
            val native = controller
            val track = com.resonance.player.model.Track("native-host-original", "同频 · 原创演示", "Resonance Studio", "一起听", "20:00", 7, sourceUri = audio.absolutePath, mimeType = "audio/mpeg")
            withContext(Dispatchers.Main.immediate) {
                native.setMediaItem(androidx.media3.common.MediaItem.Builder().setMediaId(track.id).setUri(audio.toURI().toString())
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).build()).build())
                native.prepare(); native.pause(); native.volume = .08f
            }
            runtime.service.publishPlayback(com.resonance.player.model.PlayerState(currentTrack = track))
            runtime.service.publishQueue(listOf(track))
            runtime.service.requireJoinApproval(false)
            runtime.service.host(0)
            File(directory, "host-port.txt").writeText(runtime.service.state.value.port.toString())
            val forwardPort = InstrumentationRegistry.getArguments().getString("nativeForwardPort")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 45993
            File(directory, "host-invite.txt").writeText(runtime.service.invitation("127.0.0.1", forwardPort))
            withTimeout(120_000) { runtime.service.state.first { it.peerReady } }
            withContext(Dispatchers.Main.immediate) { native.play() }
            withTimeout(10_000) { while (!withContext(Dispatchers.Main.immediate) { native.isPlaying && native.currentPosition > 500 }) delay(50) }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            repeat(60) { second ->
                if (second == 15) withContext(Dispatchers.Main.immediate) { native.pause() }
                if (second == 18) withContext(Dispatchers.Main.immediate) { native.seekTo(120_000) }
                if (second == 20) withContext(Dispatchers.Main.immediate) { native.play() }
                delay(1_000)
                assertEquals(TogetherStatus.Connected, runtime.service.state.value.status)
            }
            withTimeout(30_000) { runtime.service.state.first { it.status == TogetherStatus.Listening } }
        } finally {
            runtime.service.leave()
            withContext(Dispatchers.Main.immediate) { controller?.let { it.pause(); it.clearMediaItems(); it.release() } }
            // Restore only the playback key this fixture owns; no credential/config file is read.
            prefs.edit().apply { if (saved == null) remove("playback.snapshot") else putString("playback.snapshot", saved) }.commit()
            File(directory, "host-invite.txt").delete()
            scenario.close()
        }
    }
    @Test fun checkAssistantAvailabilityForPromoWithoutExportingCredentials() = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("checkAi") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = com.resonance.player.features.AndroidMusicAccess.runtime(context).settings()
        val configured = settings.connections.any { it.provider == settings.assistantProvider && it.configured }
        File(context.getExternalFilesDir(null), "promo-ai-configured.txt").writeText(configured.toString())
    }
    @Test fun servicePlayerFollowsWindowsAfterActivityGoesIntoBackground() = runBlocking<Unit> {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("nativeTogether") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val relayAcceptance = args.getString("nativeRelayAcceptance") == "true"
        if (relayAcceptance) {
            check(context.packageName.startsWith("com.resonance.player.validation")) {
                "Public relay acceptance requires an isolated validation application"
            }
            check(args.getString("nativeCompanions") == "true")
        }
        val directory = File(context.filesDir, "native-bridge").apply { mkdirs() }
        val invitation = File(directory, "invite.txt").readText()
        val runtime = AndroidTogetherRuntime.get(context)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var controller: MediaController? = null
        try {
            val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
            controller = withContext(Dispatchers.IO) { future.get(15, java.util.concurrent.TimeUnit.SECONDS) }
            val native = controller
            runtime.service.join(invitation)
            withTimeout(25_000) { runtime.service.state.first { it.status == TogetherStatus.Connected && it.peerReady } }
            withTimeout(15_000) {
                while (!withContext(Dispatchers.Main.immediate) { native.isPlaying && native.currentPosition > 500 }) delay(50)
            }
            // Move the UI to background; synchronization remains owned by PlaybackService.
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            val seconds = args.getString("nativeSeconds")?.toIntOrNull()?.coerceIn(30, 8_000) ?: 60
            val companions = args.getString("nativeCompanions") == "true"
            if (companions) {
                withTimeout(15_000) { runtime.service.companion.first { it.connected } }
                assertTrue(runtime.service.sendChat("Android 已建立原生跟听与加密聊天"))
                withTimeout(15_000) { runtime.service.companion.first { it.messages.any { message -> message.outgoing && message.delivered } } }
            }
            val log = StringBuilder("elapsed_ms,target_ms,native_ms,playing,status\n")
            val start = android.os.SystemClock.elapsedRealtime()
            var sawPause = false; var sawResume = false; var sawSeek = false
            repeat(seconds - 2) { second ->
                if (companions && second == 40) {
                    runtime.service.pauseGuestAudio("原生回归：暂停本机声音，保持房间")
                    assertTrue(runtime.service.sendChat("本机暂停声音时仍可聊天"))
                }
                if (companions && second in 41..42) assertFalse(withContext(Dispatchers.Main.immediate) { native.isPlaying })
                if (companions && second == 43) runtime.service.resumeGuestAudio()
                delay(1_000)
                val frame = runtime.service.remotePlayback.value
                val position = withContext(Dispatchers.Main.immediate) { native.currentPosition to native.isPlaying }
                val elapsed = android.os.SystemClock.elapsedRealtime() - start
                val status = runtime.service.state.value.status
                assertTrue("Room must survive Activity backgrounding", status == TogetherStatus.Connected)
                if (companions) assertTrue("Chat stays authenticated while backgrounded", runtime.service.companion.value.connected)
                if (elapsed > 16_000 && elapsed < 19_000 && !position.second) sawPause = true
                if (elapsed > 21_000 && position.second) sawResume = true
                if (position.first > 90_000) sawSeek = true
                log.append("$elapsed,${frame?.let { (it.progress * it.durationMs).toLong() }},${position.first},${position.second},$status\n")
            }
            File(directory, "android-native.csv").writeText(log.toString())
            assertTrue("Native output advanced through pause and seek", sawPause && sawResume && sawSeek)
            assertTrue(withContext(Dispatchers.Main.immediate) { native.currentPosition > 90_000 && native.isPlaying })
            if (relayAcceptance) {
                val marker = "[resonance-native-acceptance-complete]"
                assertTrue(runtime.service.sendChat(marker))
                withTimeout(15_000) {
                    runtime.service.companion.first { state ->
                        state.messages.any { it.outgoing && it.text == marker && it.delivered }
                    }
                }
            }
        } finally {
            runtime.service.leave()
            withContext(Dispatchers.Main.immediate) { controller?.release() }
            scenario.close()
        }
    }
}
