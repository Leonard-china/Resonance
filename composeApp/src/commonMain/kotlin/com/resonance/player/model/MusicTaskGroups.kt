package com.resonance.player.model

data class MusicTaskGroup(val root: MusicTaskEntry, val steps: List<MusicTaskEntry>) {
    val failures get() = steps.filter { entry ->
        if (!entry.retryable || entry.outcome !in setOf(MusicTaskOutcome.Failed, MusicTaskOutcome.Interrupted, MusicTaskOutcome.Cancelled)) return@filter false
        if (entry.kind !in setOf("source", "video")) return@filter true
        val byId = (steps + root).associateBy { it.id }
        var parent = byId[entry.parentId]
        val seen = mutableSetOf<String>()
        while (parent != null && seen.add(parent.id)) {
            if (parent.kind == "song" && parent.outcome == MusicTaskOutcome.Completed) return@filter false
            parent = byId[parent.parentId]
        }
        // A different source succeeded for this same recording; keep the failed attempt as diagnostics.
        steps.none { it.id != entry.id && it.kind in setOf("source", "video") && it.title == entry.title && it.outcome == MusicTaskOutcome.Completed }
    }
}

/** Legacy entries without a parent remain individual groups. Truncated parents are tolerated. */
fun groupMusicTasks(entries: List<MusicTaskEntry>): List<MusicTaskGroup> {
    val byId = entries.associateBy { it.id }
    fun root(entry: MusicTaskEntry): MusicTaskEntry {
        var current = entry
        val seen = mutableSetOf(current.id)
        while (true) {
            val parent = byId[current.parentId] ?: return current
            if (!seen.add(parent.id)) return byId.getValue(seen.minOrNull()!!)
            current = parent
        }
    }
    return entries.groupBy { root(it).id }.map { (id, group) ->
        MusicTaskGroup(byId.getValue(id), group.filterNot { it.id == id })
    }.sortedByDescending { it.root.createdAt }
}
