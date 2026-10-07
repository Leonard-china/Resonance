package com.resonance.player

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentGainDb: Float = 0f
    private var baseMasterVolume: Float = 1.0f

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
            addListener(object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    updateLoudnessEnhancer(audioSessionId)
                }
            })
        }
        val sessionActivityIntent = packageManager?.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle,
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == "SET_AUDIO_GAIN") {
                        val gainDb = args.getFloat("gainDb", 0f)
                        currentGainDb = gainDb
                        applyAudioGain(player, gainDb)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    if (customCommand.customAction == "SET_MASTER_VOLUME") {
                        val vol = args.getFloat("volume", 1.0f)
                        baseMasterVolume = vol.coerceIn(0f, 1f)
                        applyAudioGain(player, currentGainDb)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
            .build()
    }

    private fun updateLoudnessEnhancer(audioSessionId: Int) {
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                mediaSession?.player?.let { applyAudioGain(it as ExoPlayer, currentGainDb) }
            }
        } catch (_: Throwable) {}
    }

    private fun applyAudioGain(player: ExoPlayer, gainDb: Float) {
        try {
            if (loudnessEnhancer == null) {
                val sid = player.audioSessionId
                if (sid != C.AUDIO_SESSION_ID_UNSET && sid != 0) {
                    loudnessEnhancer = LoudnessEnhancer(sid)
                }
            }
            if (gainDb >= 0f) {
                player.volume = baseMasterVolume
                val gainMb = (gainDb * 100).toInt().coerceIn(0, 2000)
                loudnessEnhancer?.setTargetGain(gainMb)
                loudnessEnhancer?.enabled = (gainMb > 0)
            } else {
                loudnessEnhancer?.enabled = false
                val factor = Math.pow(10.0, (gainDb / 20.0).toDouble()).toFloat().coerceIn(0.01f, 1.0f)
                player.volume = (baseMasterVolume * factor).coerceIn(0f, 1f)
            }
        } catch (_: Throwable) {}
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        } catch (_: Throwable) {}
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
