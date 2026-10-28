package com.resonance.player

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.ui.NowPlayingOverlay
import com.resonance.player.ui.LibraryShell
import com.resonance.player.ui.LocalMusicFeatures
import com.resonance.player.ui.MusicFeatureController
import com.resonance.player.platform.PlatformServices
import com.resonance.player.platform.MusicAssistantServices
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.reflect.Proxy
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Real Choreographer frames: no Compose test clock, real audio, user library or network. */
class MusicStageFrameMetricsTest {
    private data class Frame(val total: Long, val deadline: Long, val draw: Long)

    @Composable
    private fun NavigationFixture(player: PlayerState, destination: MutableState<LibraryDestination>, tracks: List<Track>) {
        val services = remember {
            val assistantState = MutableStateFlow(MusicAssistantState())
            val assistant = Proxy.newProxyInstance(MusicAssistantServices::class.java.classLoader,
                arrayOf(MusicAssistantServices::class.java)) { _, method, _ -> if (method.name == "getState") assistantState else null } as MusicAssistantServices
            Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, _ ->
                when (method.name) {
                    "getMusicAssistant" -> assistant
                    "getSupportsWebLogin", "getSupportsKgg", "getSupportsKeyboardShortcuts" -> false
                    "getForeground" -> MutableStateFlow(true)
                    "audioFileAvailable" -> true
                    else -> null
                }
            } as PlatformServices
        }
        val scope = rememberCoroutineScope()
        val feature = remember { MusicFeatureController(services, scope).apply { library = tracks } }
        feature.onNavigate = { destination.value = it }
        CompositionLocalProvider(LocalMusicFeatures provides feature) {
            LibraryShell(destination.value, { destination.value = it }, listOf(Playlist("frame-playlist", "夜间电台", "", tracks, 7)), tracks,
                player.copy(isPlaying = false), tracks, LyricsUiState.Idle,
                onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {},
                selectedPlaylistId = null, userPlaylists = emptyList(), onPlaylistSelected = {}, onPlaylistMembershipChange = { _, _, _ -> },
                onRenamePlaylist = { _, _ -> }, onDeletePlaylist = {}, onDeleteLocalTrack = {}, onToggleFavorite = {}, onRefreshLyrics = {},
                libraryLocation = "fixture", onCreatePlaylist = {}, onImport = {}, onExportSync = {}, onImportSync = {}, onExitPreview = {},
                previewMode = true, message = null, operationInProgress = false)
        }
    }

    @Test fun playingStageReportsActualWindowFrameDurations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val label = InstrumentationRegistry.getArguments().getString("metricsLabel") ?: "sample"
        val navigation = InstrumentationRegistry.getArguments().getString("metricsScenario") == "navigation"
        val warmNavigation = navigation && InstrumentationRegistry.getArguments().getString("metricsWarmup") == "true"
        require(label.matches(Regex("[a-z-]+")))
        val tracks = listOf(Track("frame-a", "夜色里的电台", "测试音乐", "动效测量", "4:00", 7),
            Track("frame-b", "流动的光", "测试音乐", "动效测量", "3:00", 2))
        val state = mutableStateOf(PlayerState(currentTrack = tracks.first(), isPlaying = true, progress = .38f))
        val destination = mutableStateOf(LibraryDestination.Library)
        val frames = ConcurrentLinkedQueue<Frame>()
        val collect = AtomicBoolean(false)
        val droppedCallbacks = AtomicInteger(0)
        val listenerThread = HandlerThread("stage-frame-metrics").apply { start() }
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, dropped ->
            if (collect.get() && metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 0L) {
                val total = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
                if (total > 0) frames.add(Frame(total,
                    if (Build.VERSION.SDK_INT >= 31) metrics.getMetric(FrameMetrics.DEADLINE) else -1L,
                    metrics.getMetric(FrameMetrics.DRAW_DURATION)))
                droppedCallbacks.addAndGet(dropped)
            }
        }
        var refreshRate = 0f
        var activityWindow: Window? = null
        var collectedMillis = 0L
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val pages = listOf(LibraryDestination.Profile, LibraryDestination.Settings, LibraryDestination.Profile,
            LibraryDestination.Library, LibraryDestination.Assistant, LibraryDestination.Library,
            LibraryDestination.Profile, LibraryDestination.Library)
        fun navigateJourney() {
            for (page in pages) {
                instrumentation.runOnMainSync { destination.value = page }
                Thread.sleep(1_000)
            }
        }
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        try {
            scenario.onActivity { activity ->
                activityWindow = activity.window
                @Suppress("DEPRECATION")
                refreshRate = activity.windowManager.defaultDisplay.refreshRate
                activity.window.addOnFrameMetricsAvailableListener(listener, Handler(listenerThread.looper))
                activity.setContent {
                    ResonanceTheme(ThemeMode.Dark) {
                        if (navigation) NavigationFixture(state.value, destination, tracks) else NowPlayingOverlay(playerState = state.value, queue = tracks,
                            lyricsState = LyricsUiState.Ready(Lyrics(state.value.currentTrack!!.id,
                                listOf(LyricLine(0, "把忙碌留在窗外"), LyricLine(50_000, "把此刻交给旋律"), LyricLine(150_000, "让夜色慢慢流淌")),
                                true, false, "原创测试文本")),
                            onDismiss = {}, onTogglePlay = { state.value = state.value.copy(isPlaying = !state.value.isPlaying) },
                            onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {},
                            onSeek = { state.value = state.value.copy(progress = it) }, onTrackSelected = {},
                            onToggleFavorite = {}, onRefreshLyrics = {})
                    }
                }
            }
            Thread.sleep(1_500)
            // Distinguish first-use composition/JIT from revisiting the same pages.
            // This remains a Debug diagnostic, not a production performance benchmark.
            if (warmNavigation) {
                navigateJourney()
                Thread.sleep(1_000)
            }
            val started = android.os.SystemClock.elapsedRealtime()
            collect.set(true)
            if (navigation) {
                navigateJourney()
            } else {
                Thread.sleep(3_000)
                instrumentation.runOnMainSync { state.value = state.value.copy(currentTrack = tracks.last()) }
                Thread.sleep(3_000)
                instrumentation.runOnMainSync { state.value = state.value.copy(progress = .65f) }
                Thread.sleep(2_000)
            }
            collect.set(false)
            collectedMillis = android.os.SystemClock.elapsedRealtime() - started
        } finally {
            collect.set(false)
            runCatching { instrumentation.runOnMainSync { activityWindow?.removeOnFrameMetricsAvailableListener(listener) } }
            scenario.close()
            listenerThread.quitSafely()
        }
        val samples = frames.toList()
        assertTrue("Expected real animated frames, got ${samples.size}", samples.size >= 60)
        val sorted = samples.map { it.total / 1_000_000.0 }.sorted()
        fun percentile(fraction: Double) = sorted[((sorted.size - 1) * fraction).toInt()]
        val deadlines = samples.filter { it.deadline > 0 }
        val report = JSONObject().put("label", label).put("sdk", Build.VERSION.SDK_INT).put("refreshRate", refreshRate)
            .put("scenario", if (navigation) "navigation" else "player")
            .put("warmNavigation", warmNavigation)
            .put("collectionMillis", collectedMillis)
            .put("frames", samples.size).put("p50Ms", percentile(.5)).put("p95Ms", percentile(.95)).put("p99Ms", percentile(.99))
            .put("maxMs", sorted.last()).put("deadlineSamples", deadlines.size)
            .put("missedDeadlines", deadlines.count { it.total >= it.deadline }).put("metricsCallbacksDropped", droppedCallbacks.get())
            .put("note", "Debug-only synthetic playback UI sample, not a release benchmark or an audio test")
        val directory = File(context.getExternalFilesDir(null), "stage-frame-metrics").apply { mkdirs() }
        File(directory, "$label.json").writeText(report.toString(2))
        File(directory, "$label.csv").writeText("total_ns,deadline_ns,draw_ns\n" + samples.joinToString("\n") { "${it.total},${it.deadline},${it.draw}" })
        println("STAGE_FRAME_METRICS=$report")
    }
}
