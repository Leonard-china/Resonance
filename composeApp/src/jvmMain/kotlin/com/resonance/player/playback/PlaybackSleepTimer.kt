package com.resonance.player.playback

import com.resonance.player.model.SleepTimerOption
import com.resonance.player.model.SleepTimerState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Owned by the native player, so closing the UI does not cancel a sleep timer. */
class PlaybackSleepTimer(
    private val scope: CoroutineScope,
    private val onExpire: () -> Unit,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000 },
    private val mutableState: MutableStateFlow<SleepTimerState> = MutableStateFlow(SleepTimerState()),
) {
    val state: StateFlow<SleepTimerState> = mutableState
    private var deadline: Long? = null
    private var ticking: Job? = null
    @Synchronized fun set(option: SleepTimerOption) {
        ticking?.cancel()
        deadline = option.minutes?.let { nowMs() + it * 60_000L }
        mutableState.value = SleepTimerState(option, option.minutes?.times(60))
        if (deadline != null) ticking = scope.launch {
            while (isActive) { delay(250); if (tick()) break }
        }
    }
    @Synchronized fun tick(): Boolean {
        val end = deadline ?: return false
        val remaining = ((end - nowMs() + 999) / 1000).coerceAtLeast(0).toInt()
        if (remaining > 0) mutableState.value = state.value.copy(remainingSeconds = remaining)
        else { deadline = null; mutableState.value = SleepTimerState(); onExpire(); return true }
        return false
    }
    @Synchronized fun trackEnded(): Boolean {
        if (state.value.option != SleepTimerOption.EndOfTrack) return false
        set(SleepTimerOption.Off); onExpire(); return true
    }
    fun close() = set(SleepTimerOption.Off)
}
