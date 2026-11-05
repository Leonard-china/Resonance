package com.resonance.player

import com.resonance.player.application.AudioRelinkSession
import com.resonance.player.model.*
import com.resonance.player.platform.LibraryServices
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Proxy
import kotlin.test.*

class AudioRelinkSessionTest {
    @Test fun savingNewSourcePreservesCurrentMetadataAndRejectsChangedSources() = runBlocking {
        val old = Track("id", "Original", "Artist", "Album", "4:00", 0, isFavorite = true, sourceUri = "/old/a.flac")
        var current = old.copy(title = "Recently edited")
        val saved = mutableListOf<Track>()
        val services = Proxy.newProxyInstance(LibraryServices::class.java.classLoader, arrayOf(LibraryServices::class.java)) { _, method, args ->
            when (method.name) {
                "loadLibrary" -> listOf(current)
                "audioFileAvailable" -> (args!![0] as Track).sourceUri?.startsWith("/new/") == true
                "saveReplacementAudio" -> { val track = args!![0] as Track; saved += track; current = track; Unit }
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
