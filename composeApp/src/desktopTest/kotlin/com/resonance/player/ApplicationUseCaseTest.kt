package com.resonance.player

import com.resonance.player.application.*
import com.resonance.player.data.PersistenceQueue
import com.resonance.player.model.*
import com.resonance.player.platform.PlatformServices
import java.lang.reflect.Proxy
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationUseCaseTest {
    private fun services(call: (String, Array<out Any?>) -> Any?) = Proxy.newProxyInstance(
        PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, args ->
        call(method.name, args.orEmpty())
    } as PlatformServices
    @Test fun preferenceChangesShareOneCapturedOrderedTransactionStream() = runTest {
        val events = mutableListOf<String>()
        val native = services { name, args -> events += "$name:${args[0]}"; Unit }
        val writes = PersistenceQueue(backgroundScope) { throw it }
        val preferences = ApplicationPreferences(writes, native, native)
        preferences.theme(ThemeMode.Dark)
        preferences.selectedPlaylist("old")
        writes.submit { events += "barrier" }
        preferences.theme(ThemeMode.Light)
        preferences.selectedPlaylist("new")
        preferences.lyricsFolder("folder-a")
        preferences.lyricsFolder("folder-b")
        writes.closeAndJoin()
        assertEquals(listOf("saveThemeMode:Dark", "saveLastSelectedPlaylistId:old", "barrier",
            "saveThemeMode:Light", "saveLastSelectedPlaylistId:new", "saveCustomLyricsFolder:folder-b"), events)
    }
    @Test fun importingPackageReloadsCommittedPlaylistsAfterTheWriteBarrier() = runTest {
        val events = mutableListOf<String>()
        val imported = Playlist("new", "New", "0 首", emptyList(), 1)
        val native = services { name, _ ->
            events += name
            when(name) {
                "importMusicPaths" -> ImportReport(emptyList(), playlists = listOf(imported), importedPackageCount = 1)
                "loadLibrary" -> emptyList<Track>()
                "loadPlaylists" -> listOf(imported)
                else -> error(name)
            }
        }
        val writes = PersistenceQueue(backgroundScope) { throw it }
        writes.submit { delay(20); events += "committed prior write" }
        val outcome = LibraryImportUseCase(native, writes).execute(false, listOf("synthetic.resonance")) { error("Stale UI playlists must not override an imported package") }
        assertEquals(listOf("committed prior write", "importMusicPaths", "loadLibrary", "loadPlaylists"), events)
        assertEquals(listOf(imported), outcome.playlists)
        writes.closeAndJoin()
    }
    @Test fun cancelledEmptyImportDoesNotReloadOrCommitAnOlderUiCatalog() = runTest {
        val native = services { name, _ ->
            assertEquals("importMusic", name)
            ImportReport(emptyList(), cancelled = true)
        }
        val writes = PersistenceQueue(backgroundScope) { throw it }
        val outcome = LibraryImportUseCase(native, writes).execute(true, null) { error("No reload expected") }
        assertTrue(outcome.report.cancelled)
        assertNull(outcome.playlists)
        assertNull(outcome.tracks)
        writes.closeAndJoin()
    }
}
