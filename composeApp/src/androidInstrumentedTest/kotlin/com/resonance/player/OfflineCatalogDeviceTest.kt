package com.resonance.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.resonance.player.features.AndroidMusicAccess
import com.resonance.player.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class OfflineCatalogDeviceTest {
    @Test fun missingManagedAudioSurvivesAnUnrelatedEditAndReconnect() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val access = AndroidMusicAccess(context)
        val original = AndroidMusicAccess.readManaged(context)
        val playlists = access.playlists()
        val favorites = context.getSharedPreferences("resonance_favorites", Context.MODE_PRIVATE)
        val originalFavorites = favorites.getStringSet("ids", emptySet()).orEmpty().toSet()
        val folder = Files.createTempDirectory(context.cacheDir.toPath(), "offline-catalog-")
        val file = Files.write(folder.resolve("generated.mp3"), byteArrayOf(1, 2, 3))
        val offline = Track("qa-offline-a", "Generated A", "Fixture", "Album", "1:00", 0,
            sourceUri = file.toUri().toString())
        val other = offline.copy(id = "qa-offline-b", title = "Generated B", sourceUri = null)
        try {
            AndroidMusicAccess.writeManaged(context, listOf(offline, other))
            access.favorite(offline, true)
            access.savePlaylists(listOf(Playlist("qa-offline-list", "Generated", "", listOf(offline), 0)))
            val parked = file.resolveSibling("parked.mp3")
            Files.move(file, parked)
            assertFalse(access.audioAvailable(offline))
            access.update(other.copy(title = "Edited B"))
            assertEquals(setOf(offline.id, other.id), AndroidMusicAccess.readManaged(context).map { it.id }.toSet())
            assertTrue(access.tracks().first { it.id == offline.id }.isFavorite)
            assertEquals(offline.id, access.playlists().single().tracks.single().id)
            Files.move(parked, file)
            assertTrue(access.audioAvailable(access.tracks().first { it.id == offline.id }))
        } finally {
            AndroidMusicAccess.writeManaged(context, original)
            access.savePlaylists(playlists)
            check(favorites.edit().putStringSet("ids", originalFavorites).commit())
            folder.toFile().deleteRecursively()
        }
    }
}
