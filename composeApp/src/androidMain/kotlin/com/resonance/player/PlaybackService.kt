package com.resonance.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.resonance.player.audio.AndroidLoudnessAnalyzer
import com.resonance.player.playback.PlaybackSnapshot
import com.resonance.player.playback.PlaybackSnapshotCodec
import kotlinx.coroutines.*

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val preamp = com.resonance.player.audio.PreampAudioProcessor()
    private var baseMasterVolume = 1f
    private var appliedGainDb = 0f
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var trackAudioJob: Job? = null
    private val handler = Handler(Looper.getMainLooper())
    private val snapshotTick = object : Runnable {
        override fun run() { persistPlayback(); handler.postDelayed(this, 5_000) }
    }

    override fun onCreate() {
        super.onCreate()
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: android.content.Context, enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false).setAudioProcessors(arrayOf(preamp)).build()
        }
        val player = ExoPlayer.Builder(this, renderers).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.let(::updateTrackAudio)
                    persistPlayback()
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) { persistPlayback() }
            })
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        mediaSession = MediaSession.Builder(this, player).setSessionActivity(pendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult =
                    MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(AudioSessionCommands.available()).build()

                override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        AudioSessionCommands.GAIN -> {
                            if (args.getBoolean("userInitiated", false)) trackAudioJob?.cancel()
                            val gain = args.getFloat("gainDb", 0f)
                            if (!gain.isFinite()) return Futures.immediateFuture(SessionResult(androidx.media3.session.SessionError.ERROR_BAD_VALUE))
                            rampAudioGain(player, gain.coerceIn(-60f, 18f))
                        }
                        AudioSessionCommands.VOLUME -> {
                            val volume = args.getFloat("volume", 1f)
                            if (!volume.isFinite()) return Futures.immediateFuture(SessionResult(androidx.media3.session.SessionError.ERROR_BAD_VALUE))
                            baseMasterVolume = volume.coerceIn(0f, 1f)
                            applyAudioGain(player, appliedGainDb)
                        }
                        else -> return super.onCustomCommand(session, controller, customCommand, args)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }).build()
        handler.post(snapshotTick)
    }

    /** Also follows track changes when no Activity/controller is connected. */
    private fun updateTrackAudio(item: MediaItem) {
        trackAudioJob?.cancel()
        val player = mediaSession?.player as? ExoPlayer ?: return
        val prefs = getSharedPreferences("resonance_ui_state", MODE_PRIVATE)
        val id = item.mediaId
        val playlistId = PlaybackSnapshotCodec.decode(prefs.getString("playback.snapshot", null))?.playlistId
        baseMasterVolume = when {
            prefs.contains("vol.track.$id") -> prefs.getFloat("vol.track.$id", 1f)
            playlistId != null && prefs.contains("vol.playlist.$playlistId") -> prefs.getFloat("vol.playlist.$playlistId", 1f)
            else -> prefs.getFloat("vol.global", 1f)
        }
        val analyzer = AndroidLoudnessAnalyzer(this)
        val calibrated = prefs.contains("gain.calibrated.$id")
        val automatic = prefs.getBoolean("gain.auto_enabled", true)
        val cached = if (automatic) analyzer.cached(id) else null
        val gain = if (calibrated) prefs.getFloat("gain.calibrated.$id", 0f)
            else if (automatic) cached ?: 0f else prefs.getFloat("gain.track.$id", 0f)
        appliedGainDb = gain
        applyAudioGain(player, gain)
        if (!calibrated && automatic && cached == null) {
            val source = item.localConfiguration?.uri?.toString() ?: return
            trackAudioJob = audioScope.launch {
                try {
                    val measured = analyzer.analyze(id, source)
                    if (player.currentMediaItem?.mediaId == id && prefs.getBoolean("gain.auto_enabled", true)
                        && !prefs.contains("gain.calibrated.$id")) rampAudioGain(player, measured)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { Log.w("ResonanceAudio", "Loudness analysis failed", error) }
            }
        }
    }

    private fun persistPlayback() {
        val player = mediaSession?.player ?: return
        val id = player.currentMediaItem?.mediaId ?: return
        val prefs = getSharedPreferences("resonance_ui_state", MODE_PRIVATE)
        val previous = PlaybackSnapshotCodec.decode(prefs.getString("playback.snapshot", null))
        val progress = if (player.duration > 0) (player.currentPosition.toFloat() / player.duration).coerceIn(0f, 1f) else 0f
        val snapshot = PlaybackSnapshot(id, (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId },
            progress, previous?.playlistId)
        prefs.edit().putString("playback.snapshot", PlaybackSnapshotCodec.encode(snapshot)).apply()
    }

    private fun rampAudioGain(player: ExoPlayer, target: Float) {
        appliedGainDb = target
        applyAudioGain(player, target)
    }

    private fun applyAudioGain(player: ExoPlayer, gainDb: Float) {
        preamp.setGainDb(gainDb)
        player.volume = baseMasterVolume
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
    override fun onDestroy() {
        persistPlayback()
        handler.removeCallbacks(snapshotTick)
        audioScope.cancel()
        mediaSession?.run { player.release(); release() }
        mediaSession = null
        super.onDestroy()
    }
}
