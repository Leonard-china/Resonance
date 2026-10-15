package com.resonance.player.features

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.Player
import com.resonance.player.model.*
import com.resonance.player.platform.floatingLyricLines
import com.resonance.player.lyrics.LrclibLyricsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

data class OverlayFrame(val player: PlayerState, val lyrics: LyricsUiState, val settings: MusicFeatureSettings)
object AndroidOverlayBus { val frame = MutableStateFlow(OverlayFrame(PlayerState(), LyricsUiState.Idle, MusicFeatureSettings())) }

/** PlaybackService owns this overlay; it never retains an Activity. */
class AndroidLyricsOverlay(private val context: Context, private val player: Player, private val scope: CoroutineScope) {
    private val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: LinearLayout? = null
    private var primary: TextView? = null
    private var secondary: TextView? = null
    private var loadedId: String? = null
    private var cachedTrack: Track? = null
    private var translationKey: String? = null
    private var translated: LyricsUiState? = null
    private var translationSource: Lyrics? = null
    private var translationTarget: String? = null
    private var loadedLyrics: LyricsUiState = LyricsUiState.Idle
    private var loadJob: Job? = null
    private val cache = MusicJsonStore(context.filesDir.toPath().resolve("music-assistant"))
    private val repository = LrclibLyricsRepository(context.filesDir.toPath().resolve("lyrics-cache"))
    private val preferences = context.getSharedPreferences("resonance_overlay", Context.MODE_PRIVATE)
    init {
        val initial = AndroidOverlayBus.frame.value
        scope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { MusicFeatureCodec.settings(cache.read().optJSONObject("settings"), AndroidCredentialVault::decrypt) }.getOrNull() }
            if (saved != null && AndroidOverlayBus.frame.value === initial) AndroidOverlayBus.frame.value = initial.copy(settings = saved)
        }
    }
    private val params = WindowManager.LayoutParams((context.resources.displayMetrics.widthPixels * 0.9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
        gravity = Gravity.TOP or Gravity.START; x = preferences.getInt("x", 0); y = preferences.getInt("y", 160)
    }
    private val ticker = scope.launch { while (isActive) { render(); delay(250) } }
    private fun render() {
        val frame = AndroidOverlayBus.frame.value
        val settings = frame.settings
        if (!settings.floatingLyrics || !Settings.canDrawOverlays(context)) { remove(); return }
        val item = player.currentMediaItem ?: run { remove(); return }
        val track = if (frame.player.currentTrack?.id == item.mediaId) frame.player.currentTrack!! else cachedTrack?.takeIf { it.id == item.mediaId } ?: run {
            Track(item.mediaId, item.mediaMetadata.title?.toString().orEmpty(), item.mediaMetadata.artist?.toString().orEmpty(), item.mediaMetadata.albumTitle?.toString().orEmpty(), "%d:%02d".format((player.duration.coerceAtLeast(0) / 1000) / 60, (player.duration.coerceAtLeast(0) / 1000) % 60), 0, sourceUri = item.localConfiguration?.uri?.toString())
        }
        cachedTrack = track
        if (loadedId != track.id) {
            loadedId = track.id; loadedLyrics = LyricsUiState.Loading; loadJob?.cancel()
            loadJob = scope.launch {
                val result = repository.fetch(track)
                if (loadedId == track.id) loadedLyrics = when (result) { is LyricsFetchResult.Found -> LyricsUiState.Ready(result.lyrics); is LyricsFetchResult.Unavailable -> LyricsUiState.Unavailable(result.message, result.retryable) }
            }
        }
        var lyrics = if (frame.player.currentTrack?.id == track.id && frame.lyrics is LyricsUiState.Ready) frame.lyrics else loadedLyrics
        (lyrics as? LyricsUiState.Ready)?.lyrics?.let { original ->
            if (original.translationLanguage != settings.translationLanguage) {
                if (translationSource !== original || translationTarget != settings.translationLanguage) {
                    translationSource = original; translationTarget = settings.translationLanguage
                    val key = "translation-v1:${MusicJsonStore.hash(original.lines.joinToString("\n") { it.text })}:${settings.translationLanguage}"
                    translationKey = key; translated = null
                    scope.launch {
                        val found = withContext(Dispatchers.IO) { cache.cache(key)?.optJSONArray("lines") }
                        if (translationKey == key && found?.length() == original.lines.size) translated = runCatching { LyricsUiState.Ready(original.withTranslation((0 until found.length()).map(found::getString), settings.translationLanguage)) }.getOrNull()
                    }
                }
                translated?.let { lyrics = it }
            }
        }
        val progress = if (player.duration > 0) (player.currentPosition.toFloat() / player.duration).coerceIn(0f, 1f) else 0f
        val text = floatingLyricLines(PlayerState(currentTrack = track, progress = progress, isPlaying = player.isPlaying), lyrics, settings, player.currentPosition)
        if (view == null) create()
        primary?.apply { if (this.text.toString() != text.first) this.text = text.first; textSize = settings.floatingLyricsSize.toFloat() }
        secondary?.apply { if (this.text.toString() != text.second) this.text = text.second; textSize = settings.floatingLyricsSize * 0.72f; visibility = if (text.second.isBlank()) View.GONE else View.VISIBLE }
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or (if (settings.floatingLyricsLocked) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0)
        val display = context.resources.displayMetrics
        val width = (display.widthPixels * 0.9f).toInt()
        val x = params.x.coerceIn(0, (display.widthPixels - width).coerceAtLeast(0))
        val y = params.y.coerceIn(0, (display.heightPixels - (view?.height ?: 0)).coerceAtLeast(0))
        if (params.flags != flags || params.width != width || params.x != x || params.y != y) {
            params.flags = flags; params.width = width; params.x = x; params.y = y
            view?.let { runCatching { manager.updateViewLayout(it, params) } }
        }
    }
    @Suppress("ClickableViewAccessibility")
    private fun create() {
        val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(24, 12, 24, 12); setBackgroundColor(0xB0182024.toInt()) }
        primary = TextView(context).apply { setTextColor(0xFFABF1D9.toInt()); gravity = Gravity.CENTER; maxLines = 2 }
        secondary = TextView(context).apply { setTextColor(Color.WHITE); gravity = Gravity.CENTER; maxLines = 2 }
        layout.addView(primary); layout.addView(secondary)
        var initialX = 0; var initialY = 0; var downX = 0f; var downY = 0f
        layout.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; downX = event.rawX; downY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    val display = context.resources.displayMetrics
                    params.x = (initialX + event.rawX - downX).toInt().coerceIn(0, (display.widthPixels - layout.width).coerceAtLeast(0))
                    params.y = (initialY + event.rawY - downY).toInt().coerceIn(0, (display.heightPixels - layout.height).coerceAtLeast(0))
                    manager.updateViewLayout(layout, params); true
                }
                MotionEvent.ACTION_UP -> { preferences.edit().putInt("x", params.x).putInt("y", params.y).apply(); true }
                else -> false
            }
        }
        runCatching { manager.addView(layout, params); view = layout }
    }
    private fun remove() { view?.let { runCatching { manager.removeView(it) } }; view = null; primary = null; secondary = null }
    fun close() { ticker.cancel(); loadJob?.cancel(); remove() }
}
