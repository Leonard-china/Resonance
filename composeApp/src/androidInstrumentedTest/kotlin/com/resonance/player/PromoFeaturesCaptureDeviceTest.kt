package com.resonance.player

import android.content.ComponentName
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.platform.*
import com.resonance.player.together.AndroidTogetherRuntime
import com.resonance.player.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

/** Production UI and assistant, real model response, real Windows→Android audio/chat. */
class PromoFeaturesCaptureDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    private fun hold(seconds: Int) {
        val until = SystemClock.elapsedRealtime() + seconds * 1000
        while (SystemClock.elapsedRealtime() < until) { ui.mainClock.advanceTimeBy(32); Thread.sleep(24) }
    }
    @Test fun recordAssistantEntryAndRealEncryptedRoom() = runBlocking<Unit> {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("captureFeatures") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        require(context.packageName.endsWith(".codex")) { "Use the isolated camera package" }
        val directory = context.filesDir
        val invitation = File(directory, "native-bridge/invite.txt").readText()
        val runtime = AndroidTogetherRuntime.get(context)
        val marks = StringBuilder("epoch,${SystemClock.elapsedRealtime()}\n")
        val origin = SystemClock.elapsedRealtime()
        fun mark(label: String) { marks.append("$label,${SystemClock.elapsedRealtime() - origin}\n") }
        val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        val native = withContext(Dispatchers.IO) { future.get(15, java.util.concurrent.TimeUnit.SECONDS) }
        val assistant = com.resonance.player.features.AndroidMusicAccess.runtime(context)
        val settings = assistant.settings()
        require(settings.connections.any { it.provider == settings.assistantProvider && it.configured }) { "Configure the private camera model first" }
        assistant.createSession("夜晚的音乐")
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { proxy, method, arguments ->
            when (method.name) {
                "getListenTogether" -> runtime.service
                "getMusicAssistant" -> assistant
                "currentTimeMillis" -> System.currentTimeMillis()
                "equals" -> proxy === arguments?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "Real native room camera"
                else -> error("Unexpected camera platform call ${method.name}")
            }
        } as PlatformServices
        val page = mutableIntStateOf(0)
        ui.mainClock.autoAdvance = false
        try {
            ui.setContent {
                val scope = rememberCoroutineScope()
                    val feature = remember { MusicFeatureController(services, scope).apply { this.settings = settings; loaded = true } }
                val frame by runtime.service.remotePlayback.collectAsState()
                val roomLyrics by runtime.service.remoteLyrics.collectAsState()
                val track = frame?.track
                feature.player = PlayerState(currentTrack = track, progress = frame?.progress ?: 0f, isPlaying = frame?.playing == true)
                ResonanceTheme(ThemeMode.Light, reducedMotion = false) {
                    CompositionLocalProvider(LocalMusicFeatures provides feature) {
                        when {
                            page.intValue == 0 -> AssistantScreen(true, {})
                            page.intValue == 1 -> TogetherScreen(true, {})
                            track != null -> NowPlayingOverlay(playerState = feature.player, queue = listOf(track),
                                lyricsState = roomLyrics?.let { LyricsUiState.Ready(it) } ?: LyricsUiState.Idle,
                                onDismiss = { page.intValue = 1 }, onTogglePlay = {}, onPrevious = {}, onNext = {},
                                onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {}, onTrackSelected = { _ -> },
                                onToggleFavorite = { _ -> }, onRefreshLyrics = {})
                        }
                    }
                }
            }
            hold(2); mark("assistant"); hold(3)
            val prompt = "只从原创演示曲库的《晚风与海》（舒缓氛围）、《城市入夜以后》（轻柔钢琴）、《沿着光慢慢走》（明快电子）中，挑两首适合深夜聊天的歌，各用一句话解释，100字以内，不调用工具。"
            ui.onNodeWithTag("assistant-input").performTextInput(prompt)
            mark("assistant-input"); hold(2)
            ui.onNodeWithText("发送").performClick()
            ui.runOnIdle { (context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .hideSoftInputFromWindow(ui.activity.window.decorView.windowToken, 0) }
            withTimeout(90_000) {
                while (assistant.state.value.messages.none { it.role == "assistant" && !it.content.startsWith("处理未完成") }) { hold(1); delay(1) }
            }
            val answer = assistant.state.value.messages.last { it.role == "assistant" }
            check(!answer.content.startsWith("处理未完成") && answer.model?.contains("DeepSeek") == true)
            File(directory, "promo-ai-proof.json").writeText(org.json.JSONObject().put("prompt", prompt)
                .put("answer", answer.content).put("model", answer.model).put("capturedAt", System.currentTimeMillis()).toString(2))
            mark("assistant-answer"); hold(9)
            // The invite was provided explicitly to this opt-in rig; production entry still asks confirmation.
            runtime.service.join(invitation)
            withTimeout(25_000) { runtime.service.state.first { it.peerReady && it.status == TogetherStatus.Connected } }
            ui.runOnIdle { page.intValue = 1 }
            hold(1); mark("together"); hold(5)
            ui.onNodeWithText("记为常听伙伴").performScrollTo().performClick()
            withTimeout(15_000) { while (!runtime.service.companion.value.paired) { hold(1); delay(1) } }
            ui.onNodeWithText("已记为常听伙伴").performScrollTo()
            mark("paired"); hold(3)
            ui.onNode(hasSetTextAction() and hasText("发给朋友")).performScrollTo().performTextInput("有你在，晚一点也没关系。")
            ui.onNodeWithText("发送").performScrollTo().performClick()
            ui.runOnIdle { (context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .hideSoftInputFromWindow(ui.activity.window.decorView.windowToken, 0) }
            withTimeout(15_000) { while (runtime.service.companion.value.messages.none { it.outgoing && it.delivered && it.text == "有你在，晚一点也没关系。" }) { hold(1); delay(1) } }
            hold(2)
            ui.onNodeWithText("端到端加密 · 仅保存在各自设备").performScrollTo()
            mark("chat"); hold(9)
            ui.runOnIdle { page.intValue = 2 }
            hold(1); mark("native-player"); hold(10)
            File(directory, "promo-features-marks.csv").writeText(marks.toString())
        } finally {
            runtime.service.leave()
            withContext(Dispatchers.Main.immediate) { native.release() }
        }
    }
}
