package com.resonance.player.ui

import androidx.compose.runtime.*
import com.resonance.player.model.ListeningStatsSnapshot
import com.resonance.player.platform.PlaybackServices
import kotlinx.coroutines.*

/** Native adapters expose snapshots. Compatibility adapters are read strictly off the UI thread. */
@Composable
internal fun rememberListeningStats(services: PlaybackServices?): ListeningStatsSnapshot {
    val snapshots = services?.listeningStats
    if (snapshots != null) return snapshots.collectAsState().value
    return produceState(ListeningStatsSnapshot(), services) {
        while (isActive && services != null) {
            value = withContext(Dispatchers.IO) {
                try { ListeningStatsSnapshot(services.getAllListeningStats(), loaded = true) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { ListeningStatsSnapshot(error = "听歌记录暂不可用，原有记录已保留") }
            }
            delay(1_000)
        }
    }.value
}
