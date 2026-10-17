package com.resonance.player

import com.resonance.player.model.*
import kotlin.test.*

class MusicTaskGroupsTest {
    @Test fun successfulSongHidesRecoveredSourceAttemptButKeepsFailedCoverRetry() {
        val root = MusicTaskEntry("root", "Import")
        val song = MusicTaskEntry("song", "Song", kind = "song", outcome = MusicTaskOutcome.Completed, parentId = "root")
        val source = MusicTaskEntry("source", "Source", kind = "source", outcome = MusicTaskOutcome.Failed, retryable = true, parentId = "song")
        val cover = MusicTaskEntry("cover", "Cover", kind = "cover", outcome = MusicTaskOutcome.Failed, retryable = true, parentId = "song")
        assertEquals(listOf("cover"), groupMusicTasks(listOf(root, song, source, cover)).single().failures.map { it.id })
    }
    @Test fun groupsNestedStagesWithoutCombiningSeparateImports() {
        val root = MusicTaskEntry("root", "Import", createdAt = 10)
        val song = MusicTaskEntry("song", "Song", parentId = "root")
        val cover = MusicTaskEntry("cover", "Cover", outcome = MusicTaskOutcome.Failed, retryable = true, parentId = "song")
        val legacy = MusicTaskEntry("legacy", "Old", createdAt = 1)
        val grouped = groupMusicTasks(listOf(legacy, root, song, cover))
        assertEquals(listOf("root", "legacy"), grouped.map { it.root.id })
        assertEquals(listOf("song", "cover"), grouped.first().steps.map { it.id })
        assertEquals(listOf("cover"), grouped.first().failures.map { it.id })
    }
    @Test fun orphanAndCyclicHistoricalParentsDoNotLoopOrLoseEntries() {
        val entries = listOf(MusicTaskEntry("orphan", "Old", parentId = "missing"),
            MusicTaskEntry("a", "A", parentId = "b"), MusicTaskEntry("b", "B", parentId = "a"))
        val groups = groupMusicTasks(entries)
        assertEquals(entries.size, groups.sumOf { 1 + it.steps.size })
        assertEquals(entries.map { it.id }.toSet(), groups.flatMap { listOf(it.root.id) + it.steps.map { step -> step.id } }.toSet())
    }
}
