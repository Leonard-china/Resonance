package com.resonance.player.features

import com.resonance.player.audio.*
import com.resonance.player.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.nio.file.*
import kotlin.test.*

class OfflineRecoveryTest {
    @Test fun editedScreenshotDraftAndEmptySelectionSurviveNewStoreAndConsumeOnce() = runBlocking<Unit> {
        val root = Files.createTempDirectory("resonance-offline-test-")
        try {
            val store = MusicJsonStore(root.resolve("state"))
            val draft = ScreenshotImportDraft("fixture", listOf(ScreenshotPlaylistDraft("Edited", listOf(SongRequest("Corrected", "Artist", "Album")))), "Local", emptySet())
            store.update { it.put("screenshotDraft", ScreenshotDraftCodec.encode(draft)) }
            assertEquals(draft, ScreenshotDraftCodec.decode(MusicJsonStore(store.directory).read().optJSONObject("screenshotDraft")))
            val library = Library()
            val runtime = JvmMusicAssistant(store, root, library)
            runtime.settings()
            assertEquals(draft, runtime.state.value.screenshotDraft)
            runtime.saveScreenshotImportDraft(draft.copy(playlists = draft.playlists.map { it.copy(name = "Renamed") }))
            assertEquals("Renamed", ScreenshotDraftCodec.decode(store.read().optJSONObject("screenshotDraft"))!!.playlists.single().name)
            runtime.discardScreenshotImport()
            assertNull(ScreenshotDraftCodec.decode(MusicJsonStore(store.directory).read().optJSONObject("screenshotDraft")))
            assertFailsWith<IllegalArgumentException> { runtime.saveScreenshotImportDraft(draft) }
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun corruptOrOversizedDraftDoesNotBecomeAnImport() {
        assertNull(ScreenshotDraftCodec.decode(JSONObject("{\"playlists\":[]}")))
        assertFailsWith<IllegalArgumentException> { ScreenshotDraftCodec.encode(ScreenshotImportDraft("id", listOf(ScreenshotPlaylistDraft("x", listOf(SongRequest("x".repeat(181), "a")))), "local")) }
    }
    @Test fun bpmPipelineDecodesLocallyReusesRevisionAndRejectsInventedSilenceTempo() = runBlocking<Unit> {
        val root = Files.createTempDirectory("resonance-offline-test-")
        try {
            val library = Library()
            val store = MusicJsonStore(root.resolve("state")); Files.createDirectories(store.directory)
            val runtime = JvmMusicAssistant(store, root, library)
            runtime.analyzeAudioFeatures(library.tracks())
            assertEquals(1, library.preparations)
            assertTrue(runtime.state.value.audioBpms.isEmpty(), "Silence is not a measured 120 BPM")
            runtime.analyzeAudioFeatures(library.tracks())
            assertEquals(1, library.preparations)
            library.revision = "changed"
            runtime.analyzeAudioFeatures(library.tracks())
            assertEquals(2, library.preparations)
            assertFalse(Files.list(store.directory).use { it.anyMatch { path -> path.fileName.toString().endsWith(".pcm") } })
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun audioFeatureCacheRestoresValidBackupBeforeUpdating() {
        val root = Files.createTempDirectory("resonance-offline-test-")
        try {
            val store = AudioFeatureStore(root)
            store.put(AudioFeatures("a", 120, 50, revision = "first"))
            store.put(AudioFeatures("a", 130, 60, revision = "second"))
            Files.write(root.resolve("audio-features.json"), "broken".toByteArray())
            val restored = AudioFeatureStore(root)
            assertEquals("first", restored.get("a")!!.revision)
            restored.put(AudioFeatures("b", 140, 70, revision = "new"))
            val reopened = AudioFeatureStore(root)
            assertEquals(120, reopened.get("a")!!.bpm)
            assertEquals(140, reopened.get("b")!!.bpm)
        } finally { root.toFile().deleteRecursively() }
    }
    private class Library : MusicLibraryAccess {
        var preparations = 0
        var revision = "initial"
        val song = Track("a", "Song", "Artist", "", "1:00", 0, sourceUri = "fixture")
        override suspend fun tracks() = listOf(song)
        override suspend fun playlists() = emptyList<Playlist>()
        override suspend fun savePlaylists(playlists: List<Playlist>) {}
        override suspend fun update(track: Track) {}
        override suspend fun delete(track: Track) = DeleteTrackReport(false, false, "unused")
        override suspend fun favorite(track: Track, value: Boolean) {}
        override suspend fun importAudio(path: Path, request: VideoImportRequest, cover: Path?): Track = error("No downloads in offline test")
        override suspend fun transcode(input: Path, output: Path, request: VideoImportRequest, cover: Path?) = error("No transcode in fixture")
        override suspend fun audioAvailable(track: Track) = true
        override suspend fun audioRevision(track: Track) = revision
        override suspend fun prepareAnalysisAudio(track: Track, output: Path) { preparations++; Files.write(output, ByteArray(16000 * 2 * 3)) }
    }
}
