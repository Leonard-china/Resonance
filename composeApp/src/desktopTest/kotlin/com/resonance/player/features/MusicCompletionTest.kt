package com.resonance.player.features

import com.resonance.player.ai.*
import com.resonance.player.enrich.*
import com.resonance.player.lyrics.LrclibLyricsRepository
import com.resonance.player.media.MusicVideoSource
import com.resonance.player.model.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class MusicCompletionTest {
    private val song = Track("song", "原创歌曲", "原创歌手", "", "0:10", 0, artworkPath = "video.jpg")
    private class Library(var song: Track) : MusicLibraryAccess {
        override suspend fun tracks() = listOf(song)
        override suspend fun playlists() = emptyList<Playlist>()
        override suspend fun savePlaylists(playlists: List<Playlist>) {}
        override suspend fun update(track: Track) { song = track }
        override suspend fun favorite(track: Track, value: Boolean) {}
        override suspend fun delete(track: Track): DeleteTrackReport = error("Must not delete audio")
        override suspend fun importAudio(path: Path, request: VideoImportRequest, cover: Path?) =
            song.copy(title = request.title, artist = request.artist, album = request.album, sourceUri = path.toString()).also { song = it }
        override suspend fun transcode(input: Path, output: Path, request: VideoImportRequest, cover: Path?) { Files.copy(input, output) }
    }
    private val noAi = object : MusicAiTransport {
        override suspend fun complete(connection: AiConnection, messages: org.json.JSONArray, tools: org.json.JSONArray?, maxTokens: Int): AiReply =
            error("Completion must not call a paid model")
    }
    private suspend fun fixture(block: suspend (Path, MusicJsonStore, Library) -> Unit) {
        val root = Files.createTempDirectory("music-completion-")
        try { block(root, MusicJsonStore(root.resolve("music-assistant")), Library(song)) }
        finally { root.toFile().deleteRecursively() }
    }
    private fun MusicJsonStore.videoOrigin(track: Track, kind: String = "视频缩略图") = update { root ->
        root.put("mediaOrigins", JSONObject().put(track.id, MediaOriginCodec.encode(MediaOrigin(sourceId = "bilibili", coverKind = kind))))
    }
    private suspend fun JvmMusicAssistant.installLyrics() {
        sources.install("""module.exports={platform:'原创歌词夹具',search:async()=>[{id:'original',title:'原创歌曲',artist:'原创歌手',duration:10}],getLyric:async()=>({rawLrc:'[00:01]原创第一句'+String.fromCharCode(10)+'[00:05]原创第二句'})};""")
    }

    @Test fun videoArtworkUpgradesWithoutForceAndCustomArtworkSurvivesForce() = runBlocking {
        fixture { root, store, library ->
            store.videoOrigin(song)
            val cover = root.resolve("album.cover"); Files.write(cover, byteArrayOf(1))
            var calls = 0
            val runtime = JvmMusicAssistant(store, root, library, client = noAi,
                artworkSource = MusicArtworkSource { _, _ -> calls++; AlbumCover(cover, "原专辑", "测试目录") })
            val enricher = MetadataEnricher(root.resolve("art"), LrclibLyricsRepository(root.resolve("lyrics-cache")),
                lyricsLookup = runtime::resolveLyrics, needsArtwork = runtime::needsCoverEnrichment, artworkEnrichment = runtime::enrichArtwork)
            val options = BatchEnrichOptions(enrichCover = true, enrichLyrics = false, enrichArtistAndAlbum = false, useAiFallback = false)
            val report = enricher.batchEnrich(options, listOf(song), DeepSeekConfig())
            assertEquals(1, report.coversEnriched)
            assertEquals(cover.toString(), report.updatedTracks.single().artworkPath)
            assertEquals("目录专辑封面", runtime.state.value.origins[song.id]?.coverKind)
            assertFalse(runtime.needsCoverEnrichment(library.song))
            assertEquals(1, calls)
            store.videoOrigin(library.song, "自选封面")
            assertFalse(runtime.needsCoverEnrichment(library.song, true))
            assertNull(runtime.enrichArtwork(library.song, true))
            assertEquals(1, calls)
        }
    }

    @Test fun legacyVideoOriginsUpgradeButLegacyManualFlagRemainsProtected() = runBlocking {
        fixture { root, store, library ->
            store.update { it.put("videoOrigins", JSONObject().put("video:1", song.id)) }
            val runtime = JvmMusicAssistant(store, root, library, client = noAi)
            assertTrue(runtime.needsCoverEnrichment(song))
            store.update { it.put("customCovers", JSONObject().put(song.id, true)) }
            assertFalse(runtime.needsCoverEnrichment(song, true))
        }
    }

    @Test fun batchAndPlaybackShareSourceRecoveryAfterAnOldLrclibMiss() = runBlocking {
        fixture { root, store, library ->
            val repository = LrclibLyricsRepository(root.resolve("lyrics-cache"))
            repository.writeCache(song, LyricsFetchResult.Unavailable("旧未找到", false))
            val runtime = JvmMusicAssistant(store, root, library, client = noAi)
            runtime.installLyrics()
            val enricher = MetadataEnricher(root.resolve("art"), repository, lyricsLookup = runtime::resolveLyrics)
            val report = enricher.batchEnrich(BatchEnrichOptions(enrichCover = false, enrichLyrics = true,
                enrichArtistAndAlbum = false, useAiFallback = false), listOf(song), DeepSeekConfig())
            assertEquals(1, report.lyricsEnriched)
            val playback = assertIs<LyricsFetchResult.Found>(runtime.resolveLyrics(song)).lyrics
            assertTrue(playback.synchronized)
            assertEquals(listOf(1000L, 5000L), playback.lines.map { it.timestampMs })
            assertTrue(playback.fromCache)
            assertEquals("歌源 · 原创歌词夹具", playback.source)
        }
    }

    @Test fun videoDownloadAlsoUsesSourceRecoveryAndNeverRedownloadsForLyrics() = runBlocking {
        fixture { root, store, library ->
            var downloads = 0
            val videos = object : MusicVideoSource {
                override suspend fun search(song: SongRequest) = emptyList<BilibiliVideo>()
                override suspend fun inspect(link: String) = BilibiliVideo("BVfixture001", 1, song.title, song.artist, "", 10)
                override suspend fun download(video: BilibiliVideo, destination: Path, onProgress: (ImportProgress) -> Unit): Path {
                    downloads++; Files.write(destination, "ID3-fixture-audio".toByteArray()); return destination
                }
                override suspend fun cover(url: String, destination: Path): Path? = null
            }
            val repository = LrclibLyricsRepository(root.resolve("lyrics-cache"))
            repository.writeCache(song, LyricsFetchResult.Unavailable("旧未找到", false))
            val runtime = JvmMusicAssistant(store, root, library, client = noAi, videoSource = videos,
                artworkSource = MusicArtworkSource { _, _ -> null })
            runtime.installLyrics()
            runtime.importVideo(VideoImportRequest("https://www.bilibili.com/video/BVfixture001", song.title, song.artist, song.album))
            assertEquals(1, downloads)
            assertIs<LyricsFetchResult.Found>(repository.readCache(song))
            assertEquals(MusicTaskOutcome.Completed, runtime.state.value.tasks.single { it.kind == "lyrics" }.outcome)
        }
    }

    @Test fun cancellationDuringArtworkLookupDoesNotUpdateTheLibrary() = runBlocking {
        fixture { root, store, library ->
            store.videoOrigin(song)
            val entered = CompletableDeferred<Unit>()
            val runtime = JvmMusicAssistant(store, root, library, client = noAi,
                artworkSource = MusicArtworkSource { _, _ -> entered.complete(Unit); awaitCancellation() })
            val job = launch { runtime.enrichArtwork(song, false) }
            entered.await(); job.cancelAndJoin()
            assertEquals(song, library.song)
            assertEquals("视频缩略图", runtime.state.value.origins[song.id]?.coverKind)
        }
    }

    @Test fun liveVideoWithStudioTitleDoesNotUseStudioArtworkOrCachedLyrics() = runBlocking {
        fixture { root, store, library ->
            store.update { state ->
                val live = MediaOrigin(sourceId = "bilibili", evidence = "视频：原创歌曲 (Live)", coverKind = "视频缩略图")
                state.put("mediaOrigins", JSONObject().put(song.id, MediaOriginCodec.encode(live)))
            }
            val repository = LrclibLyricsRepository(root.resolve("lyrics-cache"))
            repository.writeCache(song, LyricsFetchResult.Found(Lyrics(song.id,
                listOf(LyricLine(1000, "录音室第一句"), LyricLine(5000, "录音室第二句")), true, false, "测试歌词库")))
            val runtime = JvmMusicAssistant(store, root, library, client = noAi,
                artworkSource = MusicArtworkSource { _, _ -> error("Do not query studio artwork") })
            val missing = assertIs<LyricsFetchResult.Unavailable>(runtime.resolveLyrics(song))
            assertContains(missing.message, "核对歌名")
            val folder = root.resolve("user-lyrics"); Files.createDirectories(folder)
            Files.writeString(folder.resolve("原创歌曲.lrc"), "[00:01]本地第一句\n[00:05]本地第二句")
            assertEquals("本地第一句", assertIs<LyricsFetchResult.Found>(runtime.resolveLyrics(song, customLyricsFolder = folder.toString())).lyrics.lines.first().text)
            val failure = assertFailsWith<IllegalStateException> { runtime.enrichArtwork(song, false) }
            assertContains(failure.message.orEmpty(), "核对歌名")
            assertEquals(song, library.song)
        }
    }

    @Test fun catalogTitleCleaningPreservesRecordingVersions() {
        assertEquals("原创歌曲", catalogTrackTitle("原创歌手 - 原创歌曲 [320k 无损].mp3", "原创歌手"))
        assertEquals("原创歌曲 (Live 无损)", catalogTrackTitle("原创歌曲 (Live 无损)", "原创歌手"))
        assertEquals("原创歌曲（翻唱高清）", catalogTrackTitle("原创歌曲（翻唱高清）", "原创歌手"))
        assertEquals("Love - Hate", catalogTrackTitle("Love - Hate", "Artist"))
    }

    @Test fun invalidArtworkFallsThroughToAnotherCatalogAndSuccessIsCached() = runBlocking {
        fixture { root, store, _ ->
            var catalogs = 0
            var downloads = 0
            val image = root.resolve("real.cover"); Files.write(image, byteArrayOf(1))
            fun candidate(url: String) = MetadataEnricher.OnlineSearchResult(song.title, song.artist, "原专辑", url, url)
            val repository = AlbumCoverRepository(store, listOf(
                { catalogs++; candidate("https://fixture.invalid/broken") },
                { catalogs++; candidate("https://fixture.invalid/valid") }),
                saveArtwork = { _, url -> downloads++; if (url.endsWith("broken")) null else image.toString() })
            val request = SongRequest(song.title, song.artist)
            assertEquals(image, repository.find(request, false)?.path)
            assertEquals(2, catalogs); assertEquals(2, downloads)
            assertEquals(image, repository.find(request, false)?.path)
            assertEquals(2, catalogs); assertEquals(2, downloads)
        }
    }
}
