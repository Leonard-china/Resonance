package com.resonance.player.model

/** In-memory projection; disk format remains owned by ListeningStatsStore. */
data class ListeningStatsSnapshot(
    val stats: AllListeningStats = AllListeningStats(),
    val loaded: Boolean = false,
    val error: String? = null,
)
