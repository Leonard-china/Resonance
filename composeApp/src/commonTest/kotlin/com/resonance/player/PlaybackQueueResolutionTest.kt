package com.resonance.player

import com.resonance.player.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackQueueResolutionTest {
    private val fullLibrary = listOf(
        Track("pop-1", "Pop Song 1", "Pop Artist", "Pop Album", "3:00", 1, sourceUri = "file:///pop1.mp3"),
        Track("pop-2", "Pop Song 2", "Pop Artist", "Pop Album", "3:30", 2, sourceUri = "file:///pop2.mp3"),
        Track("classical-1", "Moonlight Sonata", "Beethoven", "Classical", "6:00", 3, sourceUri = "file:///classic1.mp3"),
        Track("classical-2", "Nocturne Op.9", "Chopin", "Classical", "4:30", 4, sourceUri = "file:///classic2.mp3"),
    )

    private val classicalPlaylist = listOf(
        Track("classical-1", "Moonlight Sonata", "Beethoven", "Classical", "6:00", 3, sourceUri = "file:///classic1.mp3"),
        Track("classical-2", "Nocturne Op.9", "Chopin", "Classical", "4:30", 4, sourceUri = "file:///classic2.mp3"),
    )

    @Test
    fun scopesPlaybackToSpecificPlaylist() {
        val resolved = resolvePlaybackQueue(
            customQueue = classicalPlaylist,
            fullLibraryQueue = fullLibrary,
        )
        assertEquals(2, resolved.size)
        assertEquals(listOf("classical-1", "classical-2"), resolved.map(Track::id))
    }

    @Test
    fun fallsBackToFullLibraryWhenCustomQueueIsNull() {
        val resolved = resolvePlaybackQueue(
            customQueue = null,
            fullLibraryQueue = fullLibrary,
        )
        assertEquals(4, resolved.size)
        assertEquals(fullLibrary.map(Track::id), resolved.map(Track::id))
    }

    @Test
    fun filtersOutUnplayableTracksInCustomQueue() {
        val queueWithMissingFile = listOf(
            Track("classical-1", "Moonlight", "Beethoven", "Classical", "6:00", 3, sourceUri = "file:///classic1.mp3"),
            Track("classical-unmatched", "Nocturne", "Chopin", "Classical", "4:30", 4, sourceUri = null),
        )
        val resolved = resolvePlaybackQueue(
            customQueue = queueWithMissingFile,
            fullLibraryQueue = fullLibrary,
        )
        assertEquals(1, resolved.size)
        assertEquals(listOf("classical-1"), resolved.map(Track::id))
    }

    @Test
    fun fallsBackToFullLibraryWhenAllCustomTracksAreUnplayable() {
        val allUnplayable = listOf(
            Track("unmatched-1", "Song", "Artist", "Album", "3:00", 1, sourceUri = null),
        )
        val resolved = resolvePlaybackQueue(
            customQueue = allUnplayable,
            fullLibraryQueue = fullLibrary,
        )
        assertEquals(4, resolved.size)
        assertEquals(fullLibrary.map(Track::id), resolved.map(Track::id))
    }

    @Test
    fun selectedDownloadIsIncludedEvenBeforeLibraryReloadReachesTheUi() {
        val restored = fullLibrary.first().copy(id = "new-download", sourceUri = "file:///restored.mp3")
        val queue = resolveTrackPlaybackQueue(restored, classicalPlaylist)

        assertEquals(listOf("classical-1", "classical-2", "new-download"), queue.map(Track::id))
        assertEquals(restored, queue.single { it.id == restored.id })
    }

    @Test
    fun nativeQueueUsesTheSelectedAudioInsteadOfAnOlderCopyWithTheSameId() {
        val old = classicalPlaylist.first()
        val restored = old.copy(sourceUri = "file:///new-location.mp3", title = "Restored title")
        val queue = resolveTrackPlaybackQueue(restored, classicalPlaylist + old)

        assertEquals(listOf("classical-1", "classical-2"), queue.map(Track::id))
        assertEquals(restored, queue.first())
    }

    @Test
    fun recommendationQueueKeepsItsOrderAndSkipsEntriesWithoutAudio() {
        val chosen = classicalPlaylist.last()
        val queue = resolveTrackPlaybackQueue(chosen, listOf(
            chosen,
            fullLibrary.first().copy(sourceUri = ""),
            fullLibrary.last().copy(id = "missing", sourceUri = null),
            classicalPlaylist.first(),
        ))

        assertEquals(listOf("classical-2", "classical-1"), queue.map(Track::id))
        assertEquals(chosen, queue.first())
    }
}
