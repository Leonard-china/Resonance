package com.resonance.player.application

import com.resonance.player.data.PersistenceQueue
import com.resonance.player.model.ThemeMode
import com.resonance.player.platform.PreferenceServices
import com.resonance.player.platform.LibraryServices

/** All user preference changes use captured values and the application's ordered writer. */
class ApplicationPreferences(
    private val queue: PersistenceQueue,
    private val preferences: PreferenceServices,
    private val library: LibraryServices,
) {
    fun theme(value: ThemeMode) = queue.submitLatest("app.theme") { preferences.saveThemeMode(value) }
    fun selectedPlaylist(id: String?) = queue.submitLatest("library.selected") { library.saveLastSelectedPlaylistId(id) }
    fun lyricsFolder(path: String?) = queue.submitLatest("lyrics.folder") { preferences.saveCustomLyricsFolder(path) }
}
