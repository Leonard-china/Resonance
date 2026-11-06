package com.resonance.player

import com.resonance.player.application.AudioRelinkSession
import com.resonance.player.model.*
import com.resonance.player.platform.LibraryServices
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Proxy
import kotlin.test.*

class AudioRelinkSessionTest {
    @Test fun largeBatchLoadsCatalogOnceAndSubmitsOneCommit() = runBlocking {
        val tracks = (1..1000).map { Track("$it", "Song $it", "Artist", "Album", "3:00", it, sourceUri = "/old/$it.mp3") }
        var reads = 0
        var commits = 0
        val services = Proxy.newProxyInstance(LibraryServices::class.java.classLoader, arrayOf(LibraryServices::class.java)) { _, method, args ->
            when (method.name) {
                "loadLibrary" -> { reads++; tracks }
                "audioFileAvailable" -> (args!![0] as Track).sourceUri?.startsWith("/new/") == true
                "saveReplacementAudioBatch" -> { commits++; (args!![0] as List<*>).size to emptyList<String>() }
                else -> error("Unexpected ${method.name}")
            }
        } as LibraryServices
        val result = AudioRelinkSession(services).apply(tracks.map { AudioRelinkMatch(it, AudioRelinkCandidate("/new/${it.id}.mp3", "${it.id}.mp3", "${it.id}.mp3")) })
        assertEquals(1000, result.first)
        assertTrue(result.second.isEmpty())
        assertEquals(1, reads)
        assertEquals(1, commits)
    }
    @Test fun savingNewSourcePreservesCurrentMetadataAndRejectsChangedSources() = runBlocking {
        val old = Track("id", "Original", "Artist", "Album", "4:00", 0, isFavorite = true, sourceUri = "/old/a.flac")
        var current = old.copy(title = "Recently edited")
        val saved = mutableListOf<Track>()
        val services = Proxy.newProxyInstance(LibraryServices::class.java.classLoader, arrayOf(LibraryServices::class.java)) { _, method, args ->
            when (method.name) {
                "loadLibrary" -> listOf(current)
                "audioFileAvailable" -> (args!![0] as Track).sourceUri?.startsWith("/new/") == true
                "saveReplacementAudioBatch" -> {
                    @Suppress("UNCHECKED_CAST") val matches = args!![0] as List<AudioRelinkMatch>
                    val (updates, errors) = prepareAudioRelinkUpdates(matches, listOf(current), { it.sourceUri?.startsWith("/new/") == true })
                    saved += updates; updates.lastOrNull()?.let { current = it }
                    updates.size to errors
                }
                else -> error("Unexpected ${method.name}")
            }
        } as LibraryServices
        val match = AudioRelinkMatch(old, AudioRelinkCandidate("/new/a.flac", "a.flac", "a.flac"))
        val session = AudioRelinkSession(services)
        assertEquals(1, session.apply(listOf(match)).first)
        assertEquals(old.id, saved.single().id)
        assertEquals("Recently edited", saved.single().title)
        assertTrue(saved.single().isFavorite)
        assertEquals(0, session.apply(listOf(match)).first)
        assertEquals(1, saved.size, "An outdated preview must not overwrite the newly restored source")
    }
}
