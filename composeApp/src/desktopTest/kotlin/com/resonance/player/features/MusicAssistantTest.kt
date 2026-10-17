package com.resonance.player.features

import com.resonance.player.ai.*
import com.resonance.player.media.*
import com.resonance.player.model.*
import com.resonance.player.lyrics.LyricOffsetStore
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
        var speechPreparations = 0
        var networkAllowed = true
        override suspend fun automaticDownloadsAllowed(unmeteredOnly: Boolean) = networkAllowed
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
        override suspend fun prepareSpeechAudio(track: Track, output: Path) { speechPreparations++; Files.write(output, ByteArray(96000)) }
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
            val runtime = JvmMusicAssistant(store, root, library, client = ai, videoSource = videos, artworkSource = com.resonance.player.enrich.MusicArtworkSource { _, _ -> null }, lyricSource = { LyricsFetchResult.Unavailable("fixture", false) })
            runtime.saveSettings(MusicFeatureSettings(connections = listOf(AiConnection(apiKey = "test-key")), recommendationsEnabled = true, recommendationMode = com.resonance.player.model.RecommendationMode.Ai, screenshotRecognition = ScreenshotRecognition.CloudApi))
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
            runtime.chat("导入歌单", listOf(image))
            assertEquals(0, videos.downloads)
            assertTrue(library.lists.isEmpty())
            val firstDraft = runtime.state.value.screenshotDraft!!
            runtime.confirmScreenshotImport(firstDraft.id, firstDraft.playlists)
            runtime.chat("导入歌单", listOf(image))
            val secondDraft = runtime.state.value.screenshotDraft!!
            runtime.confirmScreenshotImport(secondDraft.id, secondDraft.playlists)
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
    @Test fun localScreenshotImportNeedsNoApiKeyAndCompletesTitlesBeforeDownload() = runBlocking {
        fixture { _, library, videos, ai, store ->
            var reads = 0
            val runtime = JvmMusicAssistant(store, store.directory.parent, library, client = ai, videoSource = videos,
                artworkSource = com.resonance.player.enrich.MusicArtworkSource { _, _ -> null },
                lyricSource = { LyricsFetchResult.Unavailable("fixture", false) },
                screenshotOcr = com.resonance.player.ocr.ScreenshotOcr { _, _ ->
                    reads++
                    listOf(com.resonance.player.ocr.OcrPage(900, 900, listOf(
                        com.resonance.player.ocr.OcrLine("Long Song...", 90, 100, 600, 135),
                        com.resonance.player.ocr.OcrLine("Artist - Album", 90, 145, 600, 172))))
                },
                songMetadata = com.resonance.player.media.SongMetadataLookup { listOf(SongRequest("Long Song Full Title (feat. Guest)", "Artist", "Album")) })
            runtime.saveSettings(MusicFeatureSettings())
            val image = AiImage(byteArrayOf(1, 2, 3))
            runtime.chat("导入", listOf(image))
            assertEquals(0, videos.downloads)
            assertTrue(library.lists.isEmpty())
            val firstDraft = runtime.state.value.screenshotDraft!!
            runtime.confirmScreenshotImport(firstDraft.id, firstDraft.playlists)
            runtime.chat("导入", listOf(image))
            val secondDraft = runtime.state.value.screenshotDraft!!
            runtime.confirmScreenshotImport(secondDraft.id, secondDraft.playlists)
            assertEquals(1, reads)
            assertTrue(ai.requests.isEmpty())
            assertEquals(1, videos.downloads)
            assertEquals("Long Song Full Title (feat. Guest)", library.songs.values.single().title)
            assertEquals("Long Song Full Title (feat. Guest)", library.lists.single().tracks.single().title)
            assertEquals("Long Song Full Title", videos.known.values.single().title)
            assertEquals(0L, runtime.state.value.usage.total)
            assertEquals("本地 OCR · PP-OCRv5", runtime.state.value.messages.last().model)
        }
    }
    @Test fun localRecognitionFailureNeverFallsBackToPaidApi() = runBlocking {
        fixture { _, library, videos, ai, store ->
            val runtime = JvmMusicAssistant(store, store.directory.parent, library, client = ai, videoSource = videos,
                screenshotOcr = com.resonance.player.ocr.ScreenshotOcr { _, _ -> error("fixture OCR failure") })
            runtime.saveSettings(MusicFeatureSettings(connections = listOf(AiConnection(apiKey = "fixture"))))
            assertFailsWith<IllegalStateException> { runtime.chat("导入", listOf(AiImage(byteArrayOf(1)))) }
            assertTrue(ai.requests.isEmpty())
            assertEquals(0, videos.downloads)
            assertTrue(library.lists.isEmpty())
        }
    }

    @Test fun retryCoverDoesNotDownloadAudioAgainAndHistorySurvivesRestart() = runBlocking {
        val root = Files.createTempDirectory("resonance-cover-retry-")
        try {
            val store = MusicJsonStore(root.resolve("state")); val library = Library(root); val videos = Videos(); val ai = Transport()
            var available = false
            val cover = root.resolve("album.jpg"); Files.write(cover, byteArrayOf(1))
            val source = com.resonance.player.enrich.MusicArtworkSource { _, _ -> if (available) com.resonance.player.enrich.AlbumCover(cover, "Real Album", "fixture") else null }
            val runtime = JvmMusicAssistant(store, root, library, client = ai, videoSource = videos, artworkSource = source, lyricSource = { LyricsFetchResult.Unavailable("no lyrics", false) })
            videos.search(SongRequest("Song", "Artist"))
            val video = videos.known.values.single()
            val track = runtime.importVideo(VideoImportRequest(video.bvid, "Song", "Artist", "单曲"))
            assertEquals(1, videos.downloads)
            val failed = runtime.state.value.tasks.single { it.kind == "cover" && it.outcome == MusicTaskOutcome.Failed }
            assertTrue(failed.retryable)
            assertTrue(Files.exists(Path.of(track.sourceUri!!)))
            available = true
            runtime.retryFailedTasks(listOf(failed.id))
            assertEquals(1, videos.downloads)
            assertEquals(cover.toString(), library.songs[track.id]?.artworkPath)
            assertEquals("Real Album", library.songs[track.id]?.album)
            val restarted = JvmMusicAssistant(store, root, library, client = ai, videoSource = videos)
            restarted.settings()
            assertEquals(MusicTaskOutcome.Completed, restarted.state.value.tasks.first { it.id == failed.id }.outcome)
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }

    @Test fun generatedLyricsUseAudioTimestampsAndCacheWithoutChargingTwice() = runBlocking {
        val root = Files.createTempDirectory("resonance-generated-test-")
        try {
            val library = Library(root); val store = MusicJsonStore(root.resolve("state")); var calls = 0
            val transport = com.resonance.player.lyrics.SpeechLyricsTransport { _, _, _, progress ->
                calls++; progress(ImportProgress("fixture recognition"))
                com.resonance.player.lyrics.SpeechRecognition(listOf(LyricLine(500, "你好"), LyricLine(1500, "世界")), listOf(
                    com.resonance.player.lyrics.SpeechWord("你好", 500, 1000), com.resonance.player.lyrics.SpeechWord("世界", 1500, 2500)), 3)
            }
            val runtime = JvmMusicAssistant(store, root, library, speechClient = transport)
            runtime.saveSettings(MusicFeatureSettings(speechApiKey = "fake-speech-key", monthlySpeechSeconds = 60))
            val track = Track("sync", "Song", "Artist", "Album", "0:03", 0, sourceUri = root.resolve("song.mp3").toString())
            library.songs[track.id] = track
            val original = Lyrics(track.id, listOf(LyricLine(null, "你好"), LyricLine(null, "世界")), false, false, "plain")
            val first = runtime.generateSynchronizedLyrics(track, original)
            assertEquals(listOf(500L, 1500L), first.lines.map { it.timestampMs })
            assertTrue(first.synchronized)
            val second = runtime.generateSynchronizedLyrics(track, original)
            assertTrue(second.fromCache)
            assertEquals(1, calls); assertEquals(1, library.speechPreparations)
            assertEquals(3, runtime.state.value.speechSeconds)
            assertEquals(first.lines, runtime.cachedSynchronizedLyrics(track)?.lines)
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }

    @Test fun interruptedTaskIsRecoveredAndSpeechBudgetRejectsBeforeUpload() = runBlocking {
        val root = Files.createTempDirectory("resonance-interrupted-test-")
        try {
            val store = MusicJsonStore(root.resolve("state")); val library = Library(root)
            val journal = MusicTaskJournal(store); val id = journal.begin("封面", "cover", "song")
            var uploads = 0
            val runtime = JvmMusicAssistant(store, root, library, speechClient = com.resonance.player.lyrics.SpeechLyricsTransport { _, _, _, _ -> uploads++; error("must not upload") })
            runtime.saveSettings(MusicFeatureSettings(speechApiKey = "fake", monthlySpeechSeconds = 60))
            runtime.settings()
            assertEquals(MusicTaskOutcome.Interrupted, runtime.state.value.tasks.first { it.id == id }.outcome)
            store.update { it.put("speech.${java.time.YearMonth.now()}", 59) }
            val track = Track("song", "Song", "Artist", "", "0:03", 0)
            assertFailsWith<IllegalStateException> { runtime.generateSynchronizedLyrics(track) }
            assertEquals(0, uploads)
            assertTrue(runtime.state.value.tasks.any { it.kind == "syncLyrics" && it.outcome == MusicTaskOutcome.Failed })
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }
    @Test fun cancellationAfterAudioImportKeepsItInPendingRecommendationCleanup() = runBlocking {
        val root = Files.createTempDirectory("resonance-cancel-metadata-")
        try {
            val store = MusicJsonStore(root.resolve("state")); val library = Library(root); val videos = Videos(); val ai = Transport()
            val entered = CompletableDeferred<Unit>(); var block = true
            val runtime = JvmMusicAssistant(store, root, library, client = ai, videoSource = videos,
                artworkSource = com.resonance.player.enrich.MusicArtworkSource { _, _ -> null },
                lyricSource = { if (block) { entered.complete(Unit); awaitCancellation() } else LyricsFetchResult.Unavailable("fixture", false) })
            runtime.saveSettings(MusicFeatureSettings(connections = listOf(AiConnection(apiKey = "test")), recommendationsEnabled = true, recommendationMode = com.resonance.player.model.RecommendationMode.Ai, recommendationCount = 1))
            ai.songs("Interrupted")
            val work = launch { runtime.recommend(true) }
            withTimeout(5000) { entered.await() }
            runtime.cancel(); work.join()
            val first = library.songs.values.single()
            assertEquals(first.id, store.read().getJSONArray("recommendationPending").getString(0))
            assertTrue(Files.exists(Path.of(first.sourceUri!!)))
            block = false; ai.songs("Next"); runtime.recommend(true)
            assertTrue(first.id in library.deleted)
            assertFalse(Files.exists(Path.of(first.sourceUri!!)))
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }
    @Test fun chatWithMultipleSessionsMaintainsIsolationAndAttribution() = runBlocking {
        fixture { runtime, _, _, ai, _ ->
            ai.content("First session reply")
            runtime.chat("Hello session 1")
            assertEquals("Hello session 1", runtime.state.value.sessions.first().title)
            assertEquals("First session reply", runtime.state.value.messages.last().content)
            assertEquals("通义千问 (qwen3-vl-flash)", runtime.state.value.messages.last().model)

            val session1Id = runtime.state.value.currentSessionId
            val session2Id = runtime.createSession("Second Session")
            assertEquals(session2Id, runtime.state.value.currentSessionId)
            assertTrue(runtime.state.value.messages.isEmpty())

            ai.content("Second session reply")
            runtime.chat("Hello session 2")
            assertEquals(2, runtime.state.value.messages.size)
            assertEquals("Second session reply", runtime.state.value.messages.last().content)

            // Switch back to session 1
            runtime.switchSession(session1Id)
            assertEquals(session1Id, runtime.state.value.currentSessionId)
            assertEquals("First session reply", runtime.state.value.messages.last().content)

            // Delete session 2
            runtime.deleteSession(session2Id)
            assertEquals(1, runtime.state.value.sessions.size)
            assertEquals(session1Id, runtime.state.value.sessions.single().id)
        }
    }
    @Test fun recentlyRemovedRecommendationIsNotDownloadedAgain() = runBlocking {
        fixture { runtime, library, _, ai, _ ->
            ai.songs("Yesterday"); runtime.recommend(true)
            ai.songs("Today"); runtime.recommend(true)
            assertTrue(library.songs.values.none { it.title == "Yesterday" })
            ai.songs("Yesterday", "Fresh"); runtime.recommend(true)
            assertEquals(listOf("Fresh"), runtime.state.value.recommendations.trackIds.map { library.songs.getValue(it).title })
        }
    }
    @Test fun saveRecommendationToNewPlaylistSurvivesRolloverAndReload() = runBlocking {
        fixture { runtime, library, _, ai, _ ->
            ai.songs("Saved"); runtime.recommend(true)
            val song = library.songs.values.single()
            runtime.saveToPlaylist(listOf(song.id), newName = "My Music")
            ai.songs("Fresh"); runtime.recommend(true)
            assertTrue(Files.isRegularFile(Path.of(song.sourceUri!!)))
            assertEquals(listOf(song.id), library.lists.first { it.name == "My Music" }.tracks.map(Track::id))
        }
    }
    @Test fun builtInHelpDoesNotSendAnyModelRequest() = runBlocking {
        fixture { runtime, _, _, ai, _ ->
            runtime.chat("怎么把每日推荐加入歌单", emptyList())
            assertTrue(ai.requests.isEmpty())
            assertTrue(runtime.state.value.messages.last().model!!.contains("内置"))
            assertTrue(runtime.state.value.actions.any { it.kind == "suggest_page" })
        }
    }
    @Test fun cleanupReleasesOnlyOwnedTemporaryAudioAndCanRestoreIt() = runBlocking {
        fixture { runtime, library, _, ai, _ ->
            ai.songs("Temporary"); runtime.recommend(true)
            val song = library.songs.values.single()
            val size = Files.size(Path.of(song.sourceUri!!))
            val report = runtime.cleanupDownloads(listOf(song.id))
            assertEquals(1, report.deletedFiles)
            assertEquals(size, report.freedBytes)
            assertNull(library.songs.getValue(song.id).sourceUri)
            assertTrue(library.lists.first { it.id == JvmMusicAssistant.DAILY_ID }.tracks.any { it.id == song.id })
            val restored = runtime.restoreDownload(song.id)
            assertEquals(song.id, restored.id)
            assertTrue(Files.isRegularFile(Path.of(restored.sourceUri!!)))
        }
    }
    @Test fun cleanupProtectsSavedAndPlayingAudio() = runBlocking {
        fixture { runtime, library, _, ai, _ ->
            ai.songs("Saved", "Playing"); runtime.recommend(true)
            val saved = library.songs.values.first { it.title == "Saved" }
            val playing = library.songs.values.first { it.title == "Playing" }
            runtime.saveToPlaylist(listOf(saved.id), newName = "Keep")
            library.playing = playing.id
            assertEquals(0, runtime.cleanupDownloads(listOf(saved.id, playing.id)).deletedFiles)
            assertTrue(Files.exists(Path.of(saved.sourceUri!!)))
            assertTrue(Files.exists(Path.of(playing.sourceUri!!)))
        }
    }
    @Test fun recommendationVotesAndCommentsDoNotFavoriteOrDeleteAudioOrCallModel() = runBlocking {
        fixture { runtime, library, _, ai, store ->
            ai.songs("Good", "Bad"); runtime.recommend(true)
            val good = library.songs.values.first { it.title == "Good" }
            val bad = library.songs.values.first { it.title == "Bad" }
            val requests = ai.requests.size
            runtime.setRecommendationVote(good, RecommendationVote.Like)
            runtime.saveRecommendationComment(good, "喜欢轻快的旋律")
            runtime.setRecommendationVote(bad, RecommendationVote.Dislike)
            assertEquals(requests, ai.requests.size)
            assertFalse(library.songs.getValue(good.id).isFavorite)
            assertTrue(listOf(good, bad).all { Files.isRegularFile(Path.of(it.sourceUri!!)) })
            assertEquals(RecommendationVote.Like, runtime.feedbackFor(good.copy(id = "duplicate-file"))?.vote)
            assertEquals(2, RecommendationFeedbackCodec.read(store.read()).size)
            runtime.setRecommendationVote(good, RecommendationVote.Neutral)
            assertEquals("喜欢轻快的旋律", runtime.feedbackFor(good)?.comment)
            runtime.saveRecommendationComment(good, "")
            assertNull(runtime.feedbackFor(good))
        }
    }
    @Test fun screenshotSelectionCanBeEditedAndDiscardedWithoutDownloading() = runBlocking {
        fixture { runtime, library, videos, ai, _ ->
            ai.content("""{"playlists":[{"name":"Draft","tracks":[{"title":"Wrong","artist":"Artist"},{"title":"Skip","artist":"Artist"}]}]}""")
            runtime.chat("识别", listOf(AiImage(byteArrayOf(9))))
            val draft = runtime.state.value.screenshotDraft!!
            assertTrue(library.songs.isEmpty())
            assertFailsWith<IllegalArgumentException> { runtime.confirmScreenshotImport("expired", draft.playlists) }
            assertEquals(0, videos.downloads)
            runtime.confirmScreenshotImport(draft.id, listOf(ScreenshotPlaylistDraft("Edited", listOf(SongRequest("Correct", "Artist")))))
            assertEquals(listOf("Correct"), library.songs.values.map { it.title })
            assertEquals("Edited", library.lists.single().name)
            assertNull(runtime.state.value.screenshotDraft)
            assertFailsWith<IllegalArgumentException> { runtime.confirmScreenshotImport(draft.id, draft.playlists) }
            assertEquals(1, videos.downloads)
            runtime.chat("识别", listOf(AiImage(byteArrayOf(9))))
            runtime.discardScreenshotImport()
            assertNull(runtime.state.value.screenshotDraft)
            assertEquals(1, videos.downloads)
            assertTrue(runtime.state.value.tasks.any { it.parentId.isNotBlank() && it.kind == "song" })
        }
    }
    @Test fun automaticRecommendationWaitsForNetworkButManualRequestCanProceed() = runBlocking {
        fixture { runtime, library, videos, ai, _ ->
            runtime.saveSettings(runtime.settings().copy(recommendationsUnmeteredOnly = true))
            library.networkAllowed = false
            runtime.recommend()
            assertTrue(ai.requests.isEmpty())
            assertEquals(0, videos.downloads)
            ai.songs("Manual")
            runtime.recommend(true)
            assertEquals(1, videos.downloads)
            assertEquals("Manual", runtime.state.value.recommendationHistory.single().title)
        }
    }
    @Test fun networkSettingRoundTripsAndLegacySettingsKeepPreviousBehavior() {
        assertFalse(MusicFeatureCodec.settings(JSONObject()).recommendationsUnmeteredOnly)
        val settings = MusicFeatureSettings(recommendationsUnmeteredOnly = true)
        assertTrue(MusicFeatureCodec.settings(MusicFeatureCodec.encode(settings)).recommendationsUnmeteredOnly)
    }
    @Test fun sourcePreviewUsesBoundedTemporaryFileWithoutImportingOrCallingAi() = runBlocking {
        fixture { runtime, library, _, ai, store ->
            var downloads = 0
            var allowedBytes = 0L
            val network = object : com.resonance.player.sources.SourceNetwork {
                override suspend fun request(options: JSONObject) = error("unexpected network request")
                override suspend fun download(url: String, target: Path, headers: JSONObject, maxBytes: Long): Long {
                    downloads++; allowedBytes = maxBytes
                    Files.write(target, "ID3-preview".toByteArray()); return Files.size(target)
                }
            }
            runtime.sources = com.resonance.player.sources.MusicSourceRegistry(store, network)
            runtime.installSource("""module.exports={platform:'Preview',search:async()=>[{id:'one',title:'Song',artist:'Artist'}],getMediaSource:async()=>({url:'https://audio.example.test/song.mp3'})};""")
            val song = runtime.searchSources("Song").single()
            val preview = Path.of(runtime.prepareSourcePreview(song))
            assertEquals(64L * 1024 * 1024, allowedBytes)
            assertTrue(Files.isRegularFile(preview))
            assertTrue(library.songs.isEmpty())
            assertTrue(ai.requests.isEmpty())
            runtime.clearSourcePreview()
            assertFalse(Files.exists(preview))
            val trial = song.copy(raw = JSONObject(song.raw).put("isTrial", true).toString())
            assertFailsWith<IllegalArgumentException> { runtime.prepareSourcePreview(trial) }
            assertEquals(1, downloads)
        }
    }
    @Test fun batchRetryNeverRunsPaidLyricsAndUndoClearsOldRejection() = runBlocking {
        fixture { runtime, _, _, ai, store ->
            val journal = MusicTaskJournal(store)
            val paid = journal.begin("Paid", "syncLyrics")
            journal.update(paid, outcome = MusicTaskOutcome.Failed)
            assertFailsWith<IllegalArgumentException> { runtime.retryFailedTasks(listOf(paid)) }
            assertTrue(ai.requests.isEmpty())
            val track = Track("feedback-only", "Song", "Artist", "", "0:00", 0)
            val originalKey = JvmMusicAssistant.songKey(track.title, track.artist)
            store.update { it.put("dislikes", JSONObject().put(originalKey, JSONObject())) }
            runtime.setRecommendationVote(track, RecommendationVote.Dislike)
            runtime.setRecommendationVote(track, RecommendationVote.Neutral)
            assertFalse(store.read().getJSONObject("dislikes").has(originalKey))
        }
    }
    @Test fun feedbackSurvivesRolloverCleanupAndIsPassedToNextAiRecommendation() = runBlocking {
        fixture { runtime, library, _, ai, store ->
            ai.songs("Yesterday"); runtime.recommend(true)
            val old = library.songs.values.single()
            runtime.setRecommendationVote(old, RecommendationVote.Like)
            runtime.saveRecommendationComment(old, "喜欢轻快，不要太吵的摇滚")
            ai.songs("Next"); runtime.recommend(true)
            assertFalse(Files.exists(Path.of(old.sourceUri!!)))
            runtime.settings()
            assertEquals(RecommendationVote.Like, runtime.feedbackFor(old)?.vote)
            assertTrue(ai.requests.last().toString().contains("喜欢轻快"))
            assertTrue(RecommendationFeedbackCodec.read(store.read()).values.any { it.title == old.title })
            assertTrue(runtime.state.value.recommendationHistory.any { it.title == old.title })
            val next = library.songs.values.single()
            assertTrue(runtime.state.value.recommendations.reasons.getValue(next.id).contains("点过赞"))
        }
    }
    @Test fun localAudioOutsideManagedRootCannotBecomeCleanupTarget() = runBlocking {
        fixture { runtime, library, _, _, store ->
            val outside = Files.createTempFile("resonance-external-audio-", ".mp3")
            try {
                val track = Track("external", "External", "Artist", "Album", "3:00", 0, sourceUri = outside.toString())
                library.songs[track.id] = track
                store.update { it.put("videoOrigins", JSONObject().put("old-index", track.id)) }
                assertTrue(runtime.storageReport().entries.none { it.track.id == track.id })
                assertEquals(0, runtime.cleanupDownloads(listOf(track.id), true).deletedFiles)
                assertTrue(Files.isRegularFile(outside))
            } finally { Files.deleteIfExists(outside) }
        }
    }

}
