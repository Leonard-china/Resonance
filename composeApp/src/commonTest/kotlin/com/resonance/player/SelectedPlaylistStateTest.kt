package com.resonance.player

import com.resonance.player.model.Playlist
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectedPlaylistStateTest {
    private val playlists = listOf(
        Playlist("playlist-a", "A", "0 首", emptyList(), 1),
        Playlist("playlist-b", "B", "0 首", emptyList(), 2),
    )

    @Test
    fun restoresExistingPlaylist() {
        assertEquals("playlist-b", resolveSelectedPlaylistId("playlist-b", playlists))
    }

    @Test
    fun fallsBackToFirstPlaylistWhenStoredIdIsMissing() {
        assertEquals("playlist-a", resolveSelectedPlaylistId("deleted", playlists))
    }

    @Test
    fun doesNotRestoreRemovedLocalLibraryCard() {
        assertEquals("playlist-a", resolveSelectedPlaylistId("local-library", playlists))
    }

    @Test
    fun returnsNullForEmptyLibrary() {
        assertNull(resolveSelectedPlaylistId("playlist-a", emptyList()))
    }
}
