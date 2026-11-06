package com.resonance.player

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.features.AndroidMusicAccess
import com.resonance.player.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class AudioRelinkDeviceTest {
    @Test fun batchRetainsFavoritesAndPlaylistIdsWithoutRewritingPlaylists() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "relink-test-")
        val prefix = "relink-${java.util.UUID.randomUUID()}-"
        val context = object : ContextWrapper(base) {
            override fun getFilesDir() = root.resolve("files").toFile().apply { mkdirs() }
            override fun getExternalFilesDir(type: String?) = root.resolve("music").toFile().apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences(prefix + name, mode)
        }
        try {
            val access = AndroidMusicAccess(context)
            val old = Track("id", "Song", "Artist", "Album", "3:00", 0, sourceUri = root.resolve("missing.mp3").toString())
            val latest = old.copy(title = "Edited")
            AndroidMusicAccess.writeManaged(context, listOf(latest))
            access.favorite(old, true)
            access.savePlaylists(listOf(Playlist("mine", "Keep", "", listOf(latest), 0)))
            val prefs = context.getSharedPreferences("resonance_playlists", Context.MODE_PRIVATE)
            val before = prefs.all.toMap()
            val file = Files.write(root.resolve("new.mp3"), byteArrayOf(1, 2))
            val match = AudioRelinkMatch(old, AudioRelinkCandidate(file.toString(), "new.mp3", "new.mp3"))
            assertEquals(1, access.replaceAudioBatch(listOf(match), listOf(old)) {}.first)
            assertEquals(before, prefs.all)
            val restarted = AndroidMusicAccess(context)
            val track = restarted.tracks().single()
            assertEquals(latest.copy(isFavorite = true, sourceUri = file.toString()), track)
            assertEquals(track, restarted.playlists().single().tracks.single())
            assertEquals(0, restarted.replaceAudioBatch(listOf(match), listOf(old)) {}.first)
        } finally {
            listOf("resonance_favorites", "resonance_playlists", "resonance_playlists_backup").forEach { base.deleteSharedPreferences(prefix + it) }
            root.toFile().deleteRecursively()
        }
    }
}
