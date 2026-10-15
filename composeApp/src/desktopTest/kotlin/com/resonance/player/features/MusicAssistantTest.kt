package com.resonance.player.features

import com.resonance.player.ai.*
import com.resonance.player.media.*
import com.resonance.player.model.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class MusicAssistantTest {
    private class Library(private val root: Path) : MusicLibraryAccess {
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
    }
    private class Videos : MusicVideoSource {
        var unavailable = false
        var downloads = 0
        val known = mutableMapOf<String, BilibiliVideo>()
        override suspend fun search(song: SongRequest): List<BilibiliVideo> {
            if (unavailable) return emptyList()
            val id = "BV" + MusicJsonStore.hash(song.title).take(10)
            return listOf(BilibiliVideo(id, id.hashCode().toLong(), song.title, song.artist, "", 180).also { known[id] = it })
        }
        override suspend fun inspect(link: String): BilibiliVideo = known.entries.first { link.contains(it.key) }.value
        override suspend fun download(video: BilibiliVideo, destination: Path, onProgress: (ImportProgress) -> Unit): Path {
            downloads++; Files.write(destination, "ID3-${video.bvid}".toByteArray()); return destination
        }
        override suspend fun cover(url: String, destination: Path): Path? = null
    }
    private class Transport : MusicAiTransport {
        val replies = ArrayDeque<JSONObject>()
        val requests = mutableListOf<JSONArray>()
        override suspend fun complete(connection: AiConnection, messages: JSONArray, tools: JSONArray?, maxTokens: Int): AiReply {
            requests += JSONArray(messages.toString())
            return AiReply(replies.removeFirst(), AssistantUsage(100, 60, 20))
        }
        fun content(value: String) { replies.add(JSONObject().put("role", "assistant").put("content", value)) }
        fun songs(vararg titles: String) = content(JSONArray(titles.map { JSONObject().put("title", it).put("artist", "Artist").put("album", "Album").put("reason", "Taste") }).toString())
        fun call(name: String, args: JSONObject) { replies.add(JSONObject().put("role", "assistant").put("content", "").put("tool_calls", JSONArray().put(JSONObject().put("id", "tool-1").put("type", "function").put("function", JSONObject().put("name", name).put("arguments", args.toString()))))) }
    }
    private suspend fun fixture(block: suspend (JvmMusicAssistant, Library, Videos, Transport, MusicJsonStore) -> Unit) {
        val root = Files.createTempDirectory("resonance-assistant-test-")
        try {
            val library = Library(root); val videos = Videos(); val ai = Transport(); val store = MusicJsonStore(root.resolve("state"))
            val runtime = JvmMusicAssistant(store, root, library, client = ai, videoSource = videos)
            runtime.saveSettings(MusicFeatureSettings(connections = listOf(AiConnection(apiKey = "test-key")), recommendationsEnabled = true))
            runtime.settings()
            block(runtime, library, videos, ai, store)
        } finally { Files.walk(root).use { files -> files.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }

    @Test fun recommendationRolloverDeletesOnlyUnsavedDownloads() = runBlocking {
        fixture { runtime, library, _, ai, _ ->
            ai.songs("KeepFavorite", "KeepPlaylist", "Temporary"); runtime.recommend(true)
            val favorite = library.songs.values.first { it.title == "KeepFavorite" }
            val member = library.songs.values.first { it.title == "KeepPlaylist" }
            val temporary = library.songs.values.first { it.title == "Temporary" }
            library.favorite(favorite, true)
            library.lists = library.lists + Playlist("mine", "Mine", "", listOf(member), 1)
            ai.songs("Next"); runtime.recommend(true)
            assertTrue(Files.exists(Path.of(favorite.sourceUri!!)))
            assertTrue(Files.exists(Path.of(member.sourceUri!!)))
            assertFalse(Files.exists(Path.of(temporary.sourceUri!!)))
            assertEquals(listOf(temporary.id), library.deleted)
            assertEquals(listOf("Next"), library.lists.first { it.id == JvmMusicAssistant.DAILY_ID }.tracks.map(Track::title))
        }
    }
    @Test fun dislikeDeletesEvenFavoriteAndEveryPlaylistMembership() = runBlocking {
        fixture { runtime, library, _, ai, store ->
            ai.songs("Reject"); runtime.recommend(true)
            val song = library.songs.values.single(); library.favorite(song, true)
            library.lists = library.lists + Playlist("mine", "Saved", "", listOf(song), 1)
            runtime.dislike(song, "不喜欢这种风格")
            assertFalse(Files.exists(Path.of(song.sourceUri!!)))
            assertFalse(library.songs.containsKey(song.id))
            assertTrue(library.lists.all { p -> p.tracks.none { it.id == song.id } })
            assertEquals(emptyList(), runtime.state.value.recommendations.trackIds)
            assertEquals("不喜欢这种风格", store.read().getJSONObject("dislikes").getJSONObject(JvmMusicAssistant.songKey(song.title, song.artist)).getString("reason"))
        }
    }
    @Test fun unavailableNewBatchKeepsPreviousAudioAndTimestamp() = runBlocking {
        fixture { runtime, library, videos, ai, _ ->
            ai.songs("First"); runtime.recommend(true)
            val previous = runtime.state.value.recommendations
            videos.unavailable = true; ai.songs("NotFound")
            assertFailsWith<IllegalStateException> { runtime.recommend(true) }
            assertEquals(previous, runtime.state.value.recommendations)
            assertTrue(Files.exists(Path.of(library.songs.values.single().sourceUri!!)))
            assertTrue(library.deleted.isEmpty())
        }
    }
    @Test fun playingOldRecommendationIsDeferredInsteadOfDeletingAnOpenFile() = runBlocking {
        fixture { runtime, library, _, ai, _ ->
            ai.songs("Playing"); runtime.recommend(true)
            library.playing = library.songs.values.single().id
            ai.songs("Next"); runtime.recommend(true)
            assertTrue(library.songs.containsKey(library.playing))
            assertTrue(runtime.state.value.recommendations.trackIds.contains(library.playing))
            library.playing = null
            ai.songs("Third"); runtime.recommend(true)
            assertEquals(listOf("Third"), library.songs.values.map(Track::title))
        }
    }
    @Test fun translationCachesPerTextAndTargetAndPreservesTimestamps() = runBlocking {
        fixture { runtime, _, _, ai, _ ->
            val track = Track("t", "Example", "Artist", "", "0:10", 0)
            val original = Lyrics("t", listOf(LyricLine(1000, "你好"), LyricLine(2000, "世界")), true, false, "Local")
            ai.content("[\"Hello\",\"World\"]")
            val english = runtime.translate(track, original, "English")
            assertEquals(listOf(1000L, 2000L), english.lines.map { it.timestampMs })
            assertEquals(listOf("Hello", "World"), english.lines.map { it.translation })
            assertEquals(english, runtime.translate(track, original, "English"))
            assertEquals(1, ai.requests.size)
            ai.content("[\"こんにちは\",\"世界\"]"); runtime.translate(track, original, "日本語")
            assertEquals(2, ai.requests.size)
            assertNull(runtime.cachedTranslation(original.copy(lines = listOf(LyricLine(1000, "Changed"))), "English"))
            assertEquals(listOf("你好", "世界"), original.lines.map { it.text })
        }
    }
    @Test fun wrongTranslationLineCountDoesNotCacheOrModifyOriginal() = runBlocking {
        fixture { runtime, _, _, ai, _ ->
            val original = Lyrics("t", listOf(LyricLine(1000, "One"), LyricLine(2000, "Two")), true, false, "Local")
            ai.content("[\"只有一行\"]")
            assertFailsWith<IllegalArgumentException> { runtime.translate(Track("t", "Test", "A", "", "0:10", 0), original, "中文") }
            assertNull(runtime.cachedTranslation(original, "中文"))
            assertTrue(original.lines.all { it.translation == null })
        }
    }
    @Test fun screenshotRecognitionIsCachedAndExistingSongsAreNotDownloadedAgain() = runBlocking {
        fixture { runtime, library, videos, ai, _ ->
            ai.content("{\"playlists\":[{\"name\":\"My screenshot\",\"tracks\":[{\"title\":\"Song\",\"artist\":\"Artist\"}]}]}")
            val image = AiImage(byteArrayOf(1, 2, 3))
            runtime.chat("导入歌单", listOf(image)); runtime.chat("再导入一次", listOf(image))
            assertEquals(1, ai.requests.size)
            assertEquals(1, videos.downloads)
            assertEquals(1, library.lists.single().tracks.size)
        }
    }
    @Test fun unsolicitedDeleteToolIsRejectedButExplicitDeleteRuns() = runBlocking {
        fixture { runtime, library, _, ai, _ ->
            val path = Files.createTempFile("assistant-owned-", ".mp3")
            try {
                val track = Track("t", "Test", "A", "", "1:00", 0, sourceUri = path.toString()); library.songs[track.id] = track
                val args = JSONObject().put("ids", JSONArray().put("t"))
                ai.call("delete_tracks", args); ai.content("完成")
                runtime.chat("整理一下歌单")
                assertTrue(Files.exists(path)); assertTrue(library.deleted.isEmpty())
                assertTrue(ai.requests.last().toString().contains("只有用户本轮明确要求"))
                ai.call("delete_tracks", args); ai.content("已删除")
                runtime.chat("删除这首音频")
                assertFalse(Files.exists(path)); assertEquals(listOf("t"), library.deleted)
            } finally { Files.deleteIfExists(path) }
        }
    }
    @Test fun monthlyBudgetRejectsBeforeCallingProvider() = runBlocking {
        fixture { runtime, _, _, ai, _ ->
            runtime.saveSettings(runtime.settings().copy(monthlyTokenLimit = 10_000))
            assertFailsWith<IllegalStateException> { runtime.completeText(listOf(AssistantMessage("user", "A".repeat(20_000))), 3000) }
            assertTrue(ai.requests.isEmpty())
        }
    }
    @Test fun originalCandidatesBeatLiveCoverAndUnrelatedArtist() {
        val song = SongRequest("Example", "Singer")
        assertTrue(BilibiliClient.candidateScore(song, "Singer Example 官方音源", "Singer") > 20)
        assertTrue(BilibiliClient.candidateScore(song, "Singer Example 演唱会 Live", "Singer") < 20)
        assertTrue(BilibiliClient.candidateScore(song, "Example 翻唱 cover", "Other") < 20)
        assertTrue(BilibiliClient.candidateScore(song, "Unrelated", "Singer") < 20)
        assertTrue(BilibiliClient.candidateScore(SongRequest("Example", "Oliver"), "Oliver - Example Official Audio", "Oliver") > 20)
    }
    @Test fun negatedAndPlaylistOnlyRequestsCannotDeleteAudio() {
        assertFalse(JvmMusicAssistant.allowsAudioDeletion("不要删除任何歌曲"))
        assertFalse(JvmMusicAssistant.allowsAudioDeletion("删除这个歌单"))
        assertFalse(JvmMusicAssistant.allowsAudioDeletion("Please do not delete my audio files"))
        assertTrue(JvmMusicAssistant.allowsAudioDeletion("删除这首歌曲的音频文件"))
    }
    @Test fun playlistManagementToolsNeverDeleteAudioAndSnapshotPrefixStaysStable() = runBlocking {
        fixture { runtime, library, _, ai, _ ->
            val track = Track("keep", "Kept", "Artist", "Album", "1:00", 0)
            library.songs[track.id] = track
            library.lists = listOf(Playlist("mine", "Old", "", listOf(track), 1))
            ai.call("rename_playlist", JSONObject().put("playlist_id", "mine").put("name", "New")); ai.content("已重命名")
            runtime.chat("歌单重命名")
            assertEquals("New", library.lists.single().name)
            ai.call("remove_from_playlist", JSONObject().put("playlist_id", "mine").put("ids", JSONArray().put("keep"))); ai.content("已移除")
            runtime.chat("只移出歌单")
            assertTrue(library.lists.single().tracks.isEmpty())
            ai.call("delete_playlist", JSONObject().put("playlist_id", "mine")); ai.content("已删除歌单")
            runtime.chat("删除这个歌单")
            assertTrue(library.lists.isEmpty()); assertTrue(library.songs.containsKey("keep")); assertTrue(library.deleted.isEmpty())
            ai.content("你好"); runtime.chat("你好")
            ai.content("你好"); runtime.chat("再聊一次")
            val last = ai.requests.takeLast(2)
            assertEquals(last.first().getJSONObject(0).toString(), last.last().getJSONObject(0).toString())
            assertEquals(last.first().getJSONObject(1).toString(), last.last().getJSONObject(1).toString())
        }
    }
}
