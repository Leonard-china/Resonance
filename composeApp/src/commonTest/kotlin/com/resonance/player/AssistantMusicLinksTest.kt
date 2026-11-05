package com.resonance.player

import com.resonance.player.model.*
import kotlin.test.*

class AssistantMusicLinksTest {
    @Test fun assistantWebLinksOnlyAllowExplicitHttpPages() {
        assertTrue(isAssistantWebLink("https://example.com/programme?episode=1"))
        for (value in listOf("file:///tmp/song", "javascript:alert(1)", "intent://example", "resonance://track/1",
            "https://user:secret@example.com/", "not a URL")) assertFalse(isAssistantWebLink(value), value)
    }
    private fun track(id: String, source: String? = "fixture://$id") =
        Track(id, "Song $id", "Artist", "Album", "3:00", 1, sourceUri = source)

    @Test fun encodedIdsResolveToCurrentLibraryMetadataInReplyOrder() {
        val first = track("汉字 / + % ()")
        val second = track("second")
        val reply = "先听 [旧歌名](${assistantTrackLink(first.id)})，再听 [第二首](${assistantTrackLink(second.id)})。"
        val resolved = resolveAssistantMusic(reply, listOf(second, first).associateBy(Track::id))
        assertEquals(listOf(first, second), resolved.tracks)
        assertEquals("先听 旧歌名，再听 第二首。", resolved.text)
    }

    @Test fun duplicateLinksCollapseButPlainNamesAndExternalLinksNeverBecomePlayable() {
        val song = track("one")
        val reply = "Song one [外链](https://example.com/music) [一](${assistantTrackLink(song.id)}) [再听](${assistantTrackLink(song.id)})"
        val result = resolveAssistantMusic(reply, mapOf(song.id to song))
        assertEquals(listOf(song), result.tracks)
        assertTrue(result.text.contains("[外链](https://example.com/music)"))
    }

    @Test fun staleAndMalformedIdsRemainReadableWithoutInventingTracks() {
        val result = resolveAssistantMusic("[已删除](resonance://track/missing) [错误](resonance://track/%ZZ)", emptyMap())
        assertEquals("已删除 错误", result.text)
        assertTrue(result.tracks.isEmpty())
    }

    @Test fun attachmentsAreBoundedAndQueueKeepsExistingOrder() {
        val songs = (0..30).map { track("$it") }
        val reply = songs.joinToString(" ") { "[${it.title}](${assistantTrackLink(it.id)})" }
        assertEquals(songs.take(20), resolveAssistantMusic(reply, songs.associateBy(Track::id)).tracks)
        assertEquals(listOf(songs[2], songs[0], songs[1]), appendAssistantQueue(listOf(songs[2], songs[0]),
            listOf(songs[0], songs[1], songs[1], track("missing", null), track("blank", " "))))
    }
}
