package com.resonance.player.features

import com.resonance.player.ai.*
import com.resonance.player.lyrics.LyricOffsetStore
import com.resonance.player.media.*
import com.resonance.player.model.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class StorageAndAssistantExtraTest {
    private class TestLibrary(private val root: Path) : MusicLibraryAccess {
        val songs = linkedMapOf<String, Track>()
        var lists = emptyList<Playlist>()
        val deleted = mutableListOf<String>()
        var playing: String? = null
        override suspend fun tracks() = songs.values.toList()
        override suspend fun playlists() = lists
        override suspend fun savePlaylists(playlists: List<Playlist>) { lists = playlists }
        override suspend fun update(track: Track) { songs[track.id] = track }
        override suspend fun favorite(track: Track, value: Boolean) { songs[track.id]?.let { songs[track.id] = it.copy(isFavorite = value) } }
        override suspend fun delete(track: Track): DeleteTrackReport {
            deleted += track.id; songs.remove(track.id)
            val removed = track.sourceUri?.let { Files.deleteIfExists(Path.of(it)) } ?: false
            lists = lists.map { it.copy(tracks = it.tracks.filterNot { song -> song.id == track.id }) }
            return DeleteTrackReport(true, removed, "done")
        }
        override fun playingTrackId() = playing
        override suspend fun importAudio(path: Path, request: VideoImportRequest, cover: Path?): Track {
            val id = MusicJsonStore.hash(request.title + request.artist)
            val target = root.resolve("$id.mp3"); Files.copy(path, target)
            return Track(id, request.title, request.artist, request.album, "3:00", 0, sourceUri = target.toString()).also { songs[id] = it }
        }
        override suspend fun transcode(input: Path, output: Path, request: VideoImportRequest, cover: Path?) { Files.copy(input, output) }
        override suspend fun prepareSpeechAudio(track: Track, output: Path) { Files.write(output, ByteArray(96000)) }
    }

    private class TestVideos : MusicVideoSource {
        val known = mutableMapOf<String, BilibiliVideo>()
        override suspend fun search(song: SongRequest): List<BilibiliVideo> {
            val id = "BV" + MusicJsonStore.hash(song.title).take(10)
            return listOf(BilibiliVideo(id, id.hashCode().toLong(), song.title, song.artist, "", 180).also { known[id] = it })
        }
        override suspend fun inspect(link: String): BilibiliVideo = known.entries.first { link.contains(it.key) }.value
        override suspend fun download(video: BilibiliVideo, destination: Path, onProgress: (ImportProgress) -> Unit): Path {
            Files.write(destination, "ID3-${video.bvid}".toByteArray()); return destination
        }
        override suspend fun cover(url: String, destination: Path): Path? = null
    }

    private class TestTransport : MusicAiTransport {
        val replies = ArrayDeque<JSONObject>()
        val requests = mutableListOf<JSONArray>()
        override suspend fun complete(connection: AiConnection, messages: JSONArray, tools: JSONArray?, maxTokens: Int): AiReply {
            requests += JSONArray(messages.toString())
            return AiReply(replies.removeFirst(), AssistantUsage(100, 60, 20))
        }
        fun content(value: String) { replies.add(JSONObject().put("role", "assistant").put("content", value)) }
        fun songs(vararg titles: String) = content(JSONArray(titles.map { JSONObject().put("title", it).put("artist", "Artist").put("album", "Album").put("reason", "Taste") }).toString())
        fun call(name: String, args: JSONObject) {
            replies.add(JSONObject().put("role", "assistant").put("content", "").put("tool_calls", JSONArray().put(JSONObject().put("id", "tool-1").put("type", "function").put("function", JSONObject().put("name", name).put("arguments", args.toString())))))
        }
    }

    private suspend fun fixture(block: suspend (JvmMusicAssistant, TestLibrary, TestVideos, TestTransport, MusicJsonStore) -> Unit) {
        val root = Files.createTempDirectory("resonance-extra-test-")
        try {
            val library = TestLibrary(root); val videos = TestVideos(); val ai = TestTransport(); val store = MusicJsonStore(root.resolve("state"))
            val runtime = JvmMusicAssistant(store, root, library, client = ai, videoSource = videos,
                artworkSource = com.resonance.player.enrich.MusicArtworkSource { _, _ -> null },
                lyricSource = { LyricsFetchResult.Unavailable("fixture", false) })
            runtime.saveSettings(MusicFeatureSettings(connections = listOf(AiConnection(apiKey = "test-key")),
                recommendationsEnabled = true, recommendationMode = RecommendationMode.Ai, screenshotRecognition = ScreenshotRecognition.CloudApi))
            runtime.settings()
            block(runtime, library, videos, ai, store)
        } finally {
            Files.walk(root).use { files -> files.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }

    @Test
    fun cleanupRollsBackWhenFileDeleteFails() = runBlocking {
        fixture { runtime, library, _, ai, _ ->
            ai.songs("Locked")
            runtime.recommend(true)
            val song = library.songs.values.single()
            val path = Path.of(song.sourceUri!!)
            val file = path.toFile()
            FileOutputStream(file).use {
                val report = runtime.cleanupDownloads(listOf(song.id))
                assertEquals(0, report.deletedFiles)
                assertEquals(song.sourceUri, library.songs.getValue(song.id).sourceUri)
                assertTrue(report.messages.any { it.contains("未能释放") })
            }
            assertTrue(Files.isRegularFile(path))
        }
    }

    @Test
    fun recommendationHistoryPersistsAcrossRestartAndEnforcesRollingDays() = runBlocking {
        fixture { runtime, library, videos, ai, store ->
            ai.songs("Yesterday")
            runtime.recommend(true)
            val first = library.songs.values.single()
            assertEquals("Yesterday", first.title)

            // Simulate app restart with a fresh JvmMusicAssistant using the exact same store
            val runtime2 = JvmMusicAssistant(store, store.directory.parent, library, client = ai, videoSource = videos,
                artworkSource = com.resonance.player.enrich.MusicArtworkSource { _, _ -> null },
                lyricSource = { LyricsFetchResult.Unavailable("fixture", false) })
            runtime2.saveSettings(MusicFeatureSettings(connections = listOf(AiConnection(apiKey = "test-key")),
                recommendationsEnabled = true, recommendationMode = RecommendationMode.Ai,
                recommendationCooldownDays = 7, screenshotRecognition = ScreenshotRecognition.CloudApi))
            runtime2.settings()

            // Next recommendation: AI returns "Yesterday" (cooled down) and "Today" (fresh)
            ai.songs("Yesterday", "Today")
            runtime2.recommend(true)
            val titles = runtime2.state.value.recommendations.trackIds.map { library.songs.getValue(it).title }
            assertEquals(listOf("Today"), titles)
            assertTrue(library.songs.values.none { it.title == "Yesterday" })
        }
    }

    @Test
    fun assistantPlaybackAndNavigationActionsExecuteAndRejectInvalid() = runBlocking {
        fixture { runtime, library, _, ai, _ ->
            val track = Track("track-1", "Test Song", "Test Artist", "Test Album", "3:00", 0)
            library.songs[track.id] = track

            // Valid play_tracks tool call
            ai.call("play_tracks", JSONObject().put("ids", JSONArray().put("track-1")))
            ai.content("已为您开始播放")
            runtime.chat("帮我播放这首歌")
            assertEquals("play", runtime.state.value.actions.last().kind)
            assertEquals("track-1", runtime.state.value.actions.last().target)

            // Invalid play_tracks tool call (track does not exist)
            ai.call("play_tracks", JSONObject().put("ids", JSONArray().put("non-existent")))
            ai.content("未找到该歌曲")
            runtime.chat("播放不存在的歌曲")
            assertFalse(runtime.state.value.actions.any { it.target == "non-existent" })

            // Valid open_page tool call
            ai.call("open_page", JSONObject().put("page", "Sources"))
            ai.content("已为您打开歌源页面")
            runtime.chat("打开歌源")
            assertEquals("navigate", runtime.state.value.actions.last().kind)
            assertEquals("Sources", runtime.state.value.actions.last().target)

            // Valid set_playing (pause)
            ai.call("set_playing", JSONObject().put("playing", false))
            ai.content("已暂停")
            runtime.chat("暂停播放")
            assertEquals("pause", runtime.state.value.actions.last().kind)

            // Valid seek_playback
            ai.call("seek_playback", JSONObject().put("seconds", 42.5))
            ai.content("已调整位置")
            runtime.chat("跳到42秒")
            assertEquals("seek", runtime.state.value.actions.last().kind)
            assertEquals("42.5", runtime.state.value.actions.last().target)
        }
    }

    @Test
    fun lyricOffsetStoreDirectionAndPersistence() = runBlocking {
        fixture { runtime, _, _, _, store ->
            val track = Track("track-offset-1", "Song", "Artist", "Album", "3:00", 0)
            val lyrics = Lyrics("track-offset-1", listOf(LyricLine(10000, "Line 1")), synchronized = true, instrumental = false, source = "test")

            // Adjust -500ms (lyrics were too early -> delay them by subtracting 500ms)
            val adjusted1 = runtime.saveLyricOffset(track, lyrics, -500L)
            assertEquals(-500L, adjusted1.offsetMs)

            // Adjust +1000ms -> -500 + 1000 = +500ms (advance lyrics)
            val adjusted2 = runtime.saveLyricOffset(track, adjusted1, 1000L)
            assertEquals(500L, adjusted2.offsetMs)

            // Verify persistence by reading directly from LyricOffsetStore
            val cacheDir = store.directory.parent.resolve("lyrics-cache")
            val readOffset = LyricOffsetStore.read(cacheDir, track)
            assertEquals(500L, readOffset)

            // Verify apply
            val freshLyrics = Lyrics("track-offset-1", listOf(LyricLine(10000, "Line 1")), synchronized = true, instrumental = false, source = "test", offsetMs = 0L)
            val applied = LyricOffsetStore.apply(cacheDir, track, freshLyrics)
            assertEquals(500L, applied.offsetMs)

            // Verify bounds: clamping at +/- 600,000ms
            val extreme = runtime.saveLyricOffset(track, applied, 1_000_000L)
            assertEquals(600_000L, extreme.offsetMs)
            val extremeNeg = runtime.saveLyricOffset(track, extreme, -2_000_000L)
            assertEquals(-600_000L, extremeNeg.offsetMs)
        }
    }
}
