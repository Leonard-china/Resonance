package com.resonance.player.features

import com.resonance.player.model.*
import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class ScreenshotImportRecoveryTest {
    private class Library : MusicLibraryAccess {
        val songs = (1..3).map { Track("$it", "Song $it", "Artist", "Album", "3:00", it, sourceUri = "fixture:$it") }
        var lists = emptyList<Playlist>()
        var writes = 0
        var beforeSave: suspend (Int) -> Unit = {}
        override suspend fun tracks() = songs
        override suspend fun playlists() = lists
        override suspend fun savePlaylists(playlists: List<Playlist>) { beforeSave(++writes); lists = playlists }
        override suspend fun audioAvailable(track: Track) = true
        override suspend fun update(track: Track) = Unit
        override suspend fun favorite(track: Track, value: Boolean) = Unit
        override suspend fun delete(track: Track): DeleteTrackReport = error("No audio deletion in this fixture")
        override suspend fun importAudio(path: Path, request: VideoImportRequest, cover: Path?): Track = error("No downloads in this fixture")
        override suspend fun transcode(input: Path, output: Path, request: VideoImportRequest, cover: Path?) = error("No conversion in this fixture")
    }
    private suspend fun fixture(block: suspend (JvmMusicAssistant, MusicJsonStore, Library, ScreenshotImportDraft) -> Unit) {
        val root = Files.createTempDirectory("resonance-screenshot-recovery-")
        try {
            val store = MusicJsonStore(root.resolve("state"))
            val library = Library()
            val draft = ScreenshotImportDraft("initial", listOf(ScreenshotPlaylistDraft("Draft", library.songs.map { SongRequest(it.title, it.artist) })), "Local OCR")
            store.update { it.put("screenshotDraft", ScreenshotDraftCodec.encode(draft)) }
            val assistant = JvmMusicAssistant(store, root.resolve("music"), library)
            assistant.settings()
            block(assistant, store, library, draft)
        } finally { root.toFile().deleteRecursively() }
    }
    private fun pending(store: MusicJsonStore) = ScreenshotDraftCodec.decode(MusicJsonStore(store.directory).read().optJSONObject("screenshotDraft"))

    @Test fun playlistCreationFailureRetainsConfirmedEditsAndRejectsOldDialogWrites() = runBlocking {
        fixture { assistant, store, library, draft ->
            library.beforeSave = { error("Injected playlist write failure") }
            val confirmed = listOf(draft.playlists.single().copy(name = "Edited", tracks = listOf(SongRequest("Song 2", "Artist"))))
            assertFailsWith<IllegalStateException> { assistant.confirmScreenshotImport(draft.id, confirmed) }
            val saved = assertNotNull(pending(store))
            assertEquals(confirmed, saved.playlists)
            assertNotEquals(draft.id, saved.id)
            assertFailsWith<IllegalArgumentException> { assistant.saveScreenshotImportDraft(draft) }
            assertFailsWith<IllegalArgumentException> { assistant.confirmScreenshotImport(draft.id, confirmed) }
            library.beforeSave = {}
            val reopened = JvmMusicAssistant(MusicJsonStore(store.directory), store.directory.parent.resolve("music"), library)
            reopened.settings()
            assertEquals(saved, reopened.state.value.screenshotDraft)
            reopened.confirmScreenshotImport(saved.id, saved.playlists)
            assertNull(pending(store))
            assertEquals(listOf("2"), library.lists.single().tracks.map { it.id })
            assertEquals("Edited", library.lists.single().name)
        }
    }

    @Test fun failedSongRemainsWhileCompletedSongsAreCheckpointedAndNotReimported() = runBlocking {
        fixture { assistant, store, library, draft ->
            library.beforeSave = { if (it == 3) error("Second song failed") }
            assistant.confirmScreenshotImport(draft.id, draft.playlists)
            val saved = assertNotNull(pending(store))
            assertEquals(listOf("Song 2"), saved.playlists.flatMap { it.tracks }.map { it.title })
            assertEquals(listOf("1", "3"), library.lists.single().tracks.map { it.id })
            library.beforeSave = {}
            val writes = library.writes
            assistant.confirmScreenshotImport(saved.id, saved.playlists)
            assertEquals(writes + 1, library.writes)
            assertNull(pending(store))
            assertEquals(setOf("1", "2", "3"), library.lists.single().tracks.map { it.id }.toSet())
        }
    }

    @Test fun cancellationKeepsCurrentAndUnstartedSongsAcrossRestart() = runBlocking {
        fixture { assistant, store, library, draft ->
            val reached = CompletableDeferred<Unit>()
            library.beforeSave = { if (it == 3) { reached.complete(Unit); awaitCancellation() } }
            val job = launch { assistant.confirmScreenshotImport(draft.id, draft.playlists) }
            withTimeout(5000) { reached.await() }
            job.cancelAndJoin()
            val saved = assertNotNull(pending(store))
            assertEquals(listOf("Song 2", "Song 3"), saved.playlists.flatMap { it.tracks }.map { it.title })
            assertEquals(listOf("1"), library.lists.single().tracks.map { it.id })
            library.beforeSave = {}
            val reopened = JvmMusicAssistant(MusicJsonStore(store.directory), store.directory.parent.resolve("music"), library)
            reopened.settings()
            reopened.confirmScreenshotImport(saved.id, saved.playlists)
            assertNull(pending(store))
            assertEquals(listOf("1", "2", "3"), library.lists.single().tracks.map { it.id })
        }
    }

    @Test fun completedPlaylistsAreRemovedBeforeResumingTheRemainingPlan() = runBlocking {
        fixture { assistant, store, library, draft ->
            val confirmed = listOf(
                ScreenshotPlaylistDraft("First", listOf(SongRequest("Song 1", "Artist"))),
                ScreenshotPlaylistDraft("Second", listOf(SongRequest("Song 2", "Artist"))),
            )
            library.beforeSave = { if (it == 3) error("Cannot create second playlist") }
            assertFailsWith<IllegalStateException> { assistant.confirmScreenshotImport(draft.id, confirmed) }
            val saved = assertNotNull(pending(store))
            assertEquals(listOf(confirmed[1]), saved.playlists)
            library.beforeSave = {}
            val writes = library.writes
            assistant.confirmScreenshotImport(saved.id, saved.playlists)
            assertEquals(writes + 2, library.writes)
            assertNull(pending(store))
            assertEquals(listOf(listOf("1"), listOf("2")), library.lists.map { it.tracks.map { track -> track.id } })
        }
    }
}
