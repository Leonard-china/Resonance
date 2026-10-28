package com.resonance.player.playback

import com.resonance.player.model.Track
import com.resonance.player.model.TrackFilterOption
import com.resonance.player.model.TrackSortOption
import com.resonance.player.model.buildPlayNextQueue
import com.resonance.player.model.buildRemoveFromQueue
import com.resonance.player.model.buildReorderedQueue
import com.resonance.player.model.filterAndSortTracks
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackQueueAndFilterTest {

    private fun makeTrack(
        id: String,
        title: String = "Track $id",
        artist: String = "Artist $id",
        album: String = "Album $id",
        durationText: String = "03:30",
        sourceUri: String? = "file:///music/$id.mp3",
        artworkPath: String? = "/covers/$id.jpg",
        mimeType: String = "audio/mpeg",
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationText = durationText,
        artworkSeed = id.hashCode(),
        sourceUri = sourceUri,
        artworkPath = artworkPath,
        mimeType = mimeType,
    )

    @Test
    fun testBuildPlayNextQueue_insertsAfterCurrentTrack() {
        val t1 = makeTrack("1")
        val t2 = makeTrack("2")
        val t3 = makeTrack("3")
        val target = makeTrack("4")

        val queue = listOf(t1, t2, t3)
        val result = buildPlayNextQueue(queue, currentTrack = t1, nextTrack = target)

        assertEquals(listOf("1", "4", "2", "3"), result.map { it.id })
    }

    @Test
    fun testBuildPlayNextQueue_movesExistingTrackWithoutDuplicate() {
        val t1 = makeTrack("1")
        val t2 = makeTrack("2")
        val t3 = makeTrack("3")

        val queue = listOf(t1, t2, t3)
        // t3 is currently at index 2, should be moved to right after t1 (index 1)
        val result = buildPlayNextQueue(queue, currentTrack = t1, nextTrack = t3)

        assertEquals(listOf("1", "3", "2"), result.map { it.id })
    }

    @Test
    fun testBuildPlayNextQueue_insertsAtBeginningWhenCurrentTrackNull() {
        val t1 = makeTrack("1")
        val t2 = makeTrack("2")
        val target = makeTrack("3")

        val queue = listOf(t1, t2)
        val result = buildPlayNextQueue(queue, currentTrack = null, nextTrack = target)

        assertEquals(listOf("3", "1", "2"), result.map { it.id })
    }

    @Test
    fun testBuildPlayNextQueue_ignoresUnplayableOrCurrentTrack() {
        val t1 = makeTrack("1")
        val unplayable = makeTrack("2", sourceUri = null)

        val queue = listOf(t1)
        val result1 = buildPlayNextQueue(queue, currentTrack = t1, nextTrack = unplayable)
        assertEquals(queue, result1)

        val result2 = buildPlayNextQueue(queue, currentTrack = t1, nextTrack = t1)
        assertEquals(queue, result2)
    }

    @Test
    fun testBuildRemoveFromQueue() {
        val t1 = makeTrack("1")
        val t2 = makeTrack("2")
        val t3 = makeTrack("3")

        val queue = listOf(t1, t2, t3)
        val result = buildRemoveFromQueue(queue, t2)

        assertEquals(listOf("1", "3"), result.map { it.id })
    }

    @Test
    fun testBuildReorderedQueue() {
        val t1 = makeTrack("1")
        val t2 = makeTrack("2")
        val t3 = makeTrack("3")

        val queue = listOf(t1, t2, t3)
        // Move t1 from index 0 to index 2
        val result = buildReorderedQueue(queue, 0, 2)
        assertEquals(listOf("2", "3", "1"), result.map { it.id })

        // Out of bounds should return queue unchanged
        val oob = buildReorderedQueue(queue, -1, 5)
        assertEquals(queue, oob)
    }

    @Test
    fun testFilterAndSortTracks_filterQuery() {
        val t1 = makeTrack("1", title = "晴天", artist = "周杰伦", album = "叶惠美")
        val t2 = makeTrack("2", title = "七里香", artist = "周杰伦", album = "七里香")
        val t3 = makeTrack("3", title = "稻香", artist = "周杰伦", album = "魔杰座")
        val tracks = listOf(t1, t2, t3)

        val result = filterAndSortTracks(tracks, filterQuery = "七里香")
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun testFilterAndSortTracks_losslessAndQuality() {
        val flac = makeTrack("1", mimeType = "audio/flac")
        val mp3 = makeTrack("2", mimeType = "audio/mpeg")
        val tracks = listOf(flac, mp3)

        val losslessResult = filterAndSortTracks(tracks, filterOption = TrackFilterOption.Lossless)
        assertEquals(listOf("1"), losslessResult.map { it.id })

        val hqResult = filterAndSortTracks(tracks, filterOption = TrackFilterOption.HighQuality)
        assertEquals(listOf("2"), hqResult.map { it.id })
    }

    @Test
    fun testFilterAndSortTracks_missingCoverAndMetadata() {
        val normal = makeTrack("1", artworkPath = "/path/cover.jpg", artist = "Adele", album = "21")
        val noCover = makeTrack("2", artworkPath = null, artist = "Adele", album = "25")
        val noArtist = makeTrack("3", artworkPath = "/path/cover.jpg", artist = "未知歌手", album = "Album")
        val tracks = listOf(normal, noCover, noArtist)

        val missingCover = filterAndSortTracks(tracks, filterOption = TrackFilterOption.MissingCover)
        assertEquals(listOf("2"), missingCover.map { it.id })

        val missingMeta = filterAndSortTracks(tracks, filterOption = TrackFilterOption.MissingMetadata)
        assertEquals(listOf("3"), missingMeta.map { it.id })
    }

    @Test
    fun testFilterAndSortTracks_sortDuration() {
        val shortTrack = makeTrack("1", durationText = "01:15")
        val midTrack = makeTrack("2", durationText = "03:45")
        val longTrack = makeTrack("3", durationText = "05:10")
        val tracks = listOf(midTrack, longTrack, shortTrack)

        val asc = filterAndSortTracks(tracks, sortOption = TrackSortOption.DurationAsc)
        assertEquals(listOf("1", "2", "3"), asc.map { it.id })

        val desc = filterAndSortTracks(tracks, sortOption = TrackSortOption.DurationDesc)
        assertEquals(listOf("3", "2", "1"), desc.map { it.id })
    }
}
