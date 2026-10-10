package com.resonance.player.util

import com.resonance.player.model.Track
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Immutable index built once per library revision, including cached pinyin initials. */
class LibrarySearchIndex private constructor(private val entries: List<Entry>) {
    private data class Field(val text: String, val initials: String, val compact: String)
    private data class Entry(val track: Track, val fields: List<Field>)

    suspend fun search(query: String): List<Track> {
        val text = query.trim().lowercase()
        if (text.isEmpty()) return emptyList()
        val compact = text.filter(Char::isLetterOrDigit)
        val found = ArrayList<Track>()
        entries.forEachIndexed { index, entry ->
            if (index % 128 == 0) coroutineContext.ensureActive()
            if (entry.fields.any { text in it.text || text in it.initials ||
                    (compact.isNotEmpty() && compact in it.compact) }) found += entry.track
        }
        return found
    }

    companion object {
        suspend fun build(tracks: List<Track>): LibrarySearchIndex = LibrarySearchIndex(
            tracks.mapIndexed { index, track ->
                if (index % 128 == 0) coroutineContext.ensureActive()
                Entry(track, listOf(track.title, track.artist, track.album).map {
                    Field(it.lowercase(), PinyinUtils.toPinyinInitials(it), it.lowercase().filter(Char::isLetterOrDigit))
                })
            },
        )
    }
}
