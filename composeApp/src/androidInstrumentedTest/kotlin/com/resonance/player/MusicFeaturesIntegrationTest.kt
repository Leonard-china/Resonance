package com.resonance.player

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.features.*
import com.resonance.player.model.*
import com.resonance.player.platform.AndroidArtworkResolver
import com.resonance.player.platform.decodeArtwork
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.DataOutputStream
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class MusicFeaturesIntegrationTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun importedAudioHasEditableTagsAndCoverAndDislikeCanPhysicallyDeleteIt() = runBlocking {
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "music-feature-test-")
        val prefix = "music-test-${java.util.UUID.randomUUID()}-"
        val context = object : ContextWrapper(base) {
            override fun getFilesDir() = root.resolve("files").toFile().apply { mkdirs() }
            override fun getExternalFilesDir(type: String?) = root.resolve("music").toFile().apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences(prefix + name, mode)
        }
        val missingThumbnail = "content://media/external/audio/albumart/999999999"
        try {
            val access = AndroidMusicAccess(context)
            val wav = root.resolve("input.wav")
            DataOutputStream(Files.newOutputStream(wav)).use { out ->
                fun int(n: Int) = out.writeInt(Integer.reverseBytes(n))
                fun short(n: Int) = out.writeShort(java.lang.Short.reverseBytes(n.toShort()).toInt())
                val size = 44100 * 2
                out.writeBytes("RIFF"); int(36 + size); out.writeBytes("WAVEfmt "); int(16)
                short(1); short(1); int(44100); int(88200); short(2); short(16); out.writeBytes("data"); int(size)
                repeat(44100) { short((kotlin.math.sin(it * 2.0 * Math.PI * 440 / 44100) * 10000).toInt()) }
            }
            val cover = root.resolve("cover.jpg")
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.GREEN) }
            Files.newOutputStream(cover).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }; bitmap.recycle()
            val encoded = root.resolve("result.mp3")
            val request = VideoImportRequest("", "自定义歌名", "自定义歌手", "自定义专辑")
            access.transcode(wav, encoded, request, cover)
            val track = access.importAudio(encoded, request, cover)
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(track.sourceUri))
                assertEquals(request.title, retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE))
                assertEquals(request.artist, retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST))
                assertEquals(request.album, retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM))
                assertTrue(retriever.embeddedPicture!!.isNotEmpty())
            } finally { retriever.release() }
            AndroidArtworkResolver.application = base.applicationContext as android.app.Application
            AndroidArtworkResolver.audioArtwork[missingThumbnail] = track.sourceUri!!
            assertNotNull("A missing MediaStore thumbnail must fall back to embedded artwork", decodeArtwork(missingThumbnail))
            access.savePlaylists(listOf(Playlist("mine", "测试歌单", "", listOf(track), 1)))
            access.favorite(track, true)
            assertTrue(access.tracks().single().isFavorite)
            access.update(track.copy(title = "修改后的歌名"))
            assertEquals("修改后的歌名", access.playlists().single().tracks.single().title)
            access.favorite(track, false)
            assertTrue(access.delete(track).fileDeleted)
            assertTrue(access.tracks().isEmpty()); assertTrue(access.playlists().single().tracks.isEmpty())
            assertTrue(Files.isRegularFile(wav))
        } finally {
            AndroidArtworkResolver.audioArtwork.remove(missingThumbnail)
            listOf("resonance_favorites", "resonance_playlists", "resonance_playlists_backup").forEach { base.getSharedPreferences(prefix + it, Context.MODE_PRIVATE).edit().clear().commit() }
            Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }

    @Test fun longScreenshotsAreSplitAndCredentialsAreEncrypted() {
        val image = base.cacheDir.resolve("screenshot-test-${java.util.UUID.randomUUID()}.png")
        try {
            val bitmap = Bitmap.createBitmap(1700, 4000, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
            image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
            val pieces = AndroidAssistantImages.slices(base, listOf(Uri.fromFile(image)))
            assertTrue(pieces.size > 1)
            pieces.forEach { part ->
                val decoded = BitmapFactory.decodeByteArray(part.bytes, 0, part.bytes.size)
                assertTrue(decoded.width <= 1600 && decoded.height <= 1800); decoded.recycle()
            }
            val dummy = "test-value-not-a-real-api-key"
            val protected = AndroidCredentialVault.encrypt(dummy)
            assertFalse(protected.contains(dummy)); assertEquals(dummy, AndroidCredentialVault.decrypt(protected))
        } finally { image.delete() }
    }
}
