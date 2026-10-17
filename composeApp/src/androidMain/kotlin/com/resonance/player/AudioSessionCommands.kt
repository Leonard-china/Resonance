package com.resonance.player

import android.os.Bundle
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.MediaSession

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object AudioSessionCommands {
    const val GAIN = "SET_AUDIO_GAIN"
    const val VOLUME = "SET_MASTER_VOLUME"
    const val SLEEP_TIMER = "SET_SLEEP_TIMER"
    fun command(action: String) = SessionCommand(action, Bundle.EMPTY)
    fun available(): SessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
        .add(command(GAIN)).add(command(VOLUME)).add(command(SLEEP_TIMER)).build()
}
