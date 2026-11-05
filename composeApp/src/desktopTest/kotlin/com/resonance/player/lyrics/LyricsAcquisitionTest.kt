package com.resonance.player.lyrics

import com.resonance.player.ai.DeepSeekClient
import com.resonance.player.features.MusicJsonStore
import com.resonance.player.model.*
import com.resonance.player.sources.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class LyricsAcquisitionTest {
    private val track = Track("lyrics-fixture", "Fixture Song", "Fixture Artist", "Album", "0:10", 0)

    @Test fun sourceLyricsMatchRecordingAndNeverFetchAudio() = runBlocking {
        val root = Files.createTempDirectory("source-lyrics-")
        val paths = java.util.concurrent.CopyOnWriteArrayList<String>()
        val network = object : SourceNetwork {
            override suspend fun request(options: JSONObject): JSONObject {
                val url = options.getString("url"); paths += url
                val body = if (url.endsWith("/search")) """[
                    {"id":"cover","title":"Fixture Song","artist":"Different Artist","duration":10},
                    {"id":"long","title":"Fixture Song","artist":"Fixture Artist","duration":100},
                    {"id":"correct","title":"Fixture Song","artist":"Fixture Artist","duration":10}
                ]""" else { assertTrue(url.endsWith("/correct")); "[00:01]原创第一句\n[00:05]原创第二句" }
                return JSONObject().put("status", 200).put("body", body)
            }
            override suspend fun download(url: String, target: Path, headers: JSONObject, maxBytes: Long): Long = error("Lyrics must not download audio")
        }
        try {
            val sources = MusicSourceRegistry(MusicJsonStore(root), network)
            sources.install("""module.exports={platform:'无匹配歌词夹具',search:async()=>[],getLyric:async()=>({rawLrc:''})};""")
            sources.install("""const axios=require('axios');module.exports={platform:'歌词夹具',search:async()=> (await axios.get('https://fixture.invalid/search')).data,getLyric:async item=>({rawLrc:(await axios.get('https://fixture.invalid/'+item.id,{responseType:'text'})).data})};""")
            val result = assertIs<LyricsFetchResult.Found>(sources.findLyrics(track)).lyrics
            assertEquals(listOf(1000L, 5000L), result.lines.map { it.timestampMs })
            assertEquals("歌源 · 歌词夹具", result.source)
            assertEquals(2, paths.size)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun cachedLrclibMissDoesNotBlockSourceFallbackAndSuccessPersists() = runBlocking {
        val root = Files.createTempDirectory("lyrics-miss-recovery-")
        try {
            val repository = LrclibLyricsRepository(root, URI("http://127.0.0.1:1"))
            repository.writeCache(track, LyricsFetchResult.Unavailable("旧的未找到结果", false))
            val expected = Lyrics(track.id, listOf(LyricLine(1000, "原创第一句"), LyricLine(5000, "原创第二句")), true, false, "歌源 · 夹具")
            var calls = 0
            val result = repository.fetch(track, sourceFallback = { calls++; LyricsFetchResult.Found(expected) })
            assertEquals(expected, assertIs<LyricsFetchResult.Found>(result).lyrics)
            assertEquals(1, calls)
            val cached = assertIs<LyricsFetchResult.Found>(repository.fetch(track)).lyrics
            assertTrue(cached.fromCache); assertEquals(expected.lines, cached.lines); assertEquals(expected.source, cached.source)
        } finally { root.toFile().deleteRecursively() }
    }

    private val synced = Lyrics(track.id, listOf(LyricLine(1000, "原创第一句"), LyricLine(5000, "原创第二句")), true, false, "歌源 · 同步夹具")
    private val plain = Lyrics(track.id, synced.lines.map { it.copy(timestampMs = null) }, false, false, "纯文本夹具")

    private suspend fun repositoryScenario(
        exact: String = """{"plainLyrics":"原创第一句\n原创第二句"}""",
        search: String = "[]",
        block: suspend (Path, LrclibLyricsRepository) -> Unit,
    ) {
        val root = Files.createTempDirectory("lyrics-timeline-recovery-")
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        for ((path, response) in listOf("/api/get" to exact, "/api/search" to search)) {
            server.createContext(path) { exchange ->
                val bytes = response.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        }
        server.start()
        try { block(root, LrclibLyricsRepository(root.resolve("lyrics-cache"), URI("http://127.0.0.1:${server.address.port}"))) }
        finally { server.stop(0); root.toFile().deleteRecursively() }
    }

    @Test fun freshAndCachedPlainLyricsContinueToSynchronizedSource() = runBlocking {
        for (cached in listOf(false, true)) repositoryScenario { _, repository ->
            if (cached) repository.writeCache(track, LyricsFetchResult.Found(plain))
            var calls = 0
            val result = repository.fetch(track, sourceFallback = { calls++; LyricsFetchResult.Found(synced) })
            assertEquals(synced, assertIs<LyricsFetchResult.Found>(result).lyrics)
            assertEquals(1, calls)
            val restored = assertIs<LyricsFetchResult.Found>(repository.fetch(track, sourceFallback = { error("A synchronized cache must be reused") })).lyrics
            assertTrue(restored.fromCache)
            assertEquals(synced.lines, restored.lines)
            assertEquals(synced.source, restored.source)
        }
    }

    @Test fun cachedPlainLyricsCanRecoverFromLrclibWithoutInstalledSources() = runBlocking {
        repositoryScenario(exact = """{"syncedLyrics":"[00:01]原创第一句\n[00:05]原创第二句"}""") { _, repository ->
            repository.writeCache(track, LyricsFetchResult.Found(plain))
            val result = assertIs<LyricsFetchResult.Found>(repository.fetch(track)).lyrics
            assertTrue(result.synchronized)
            assertEquals(synced.lines, result.lines)
        }
    }

    @Test fun localPlainLyricsDoNotBlockSynchronizedSourceOrLoseTextOnFailure() = runBlocking {
        for (available in listOf(true, false)) repositoryScenario { root, repository ->
            val local = root.resolve("fixture.txt"); Files.writeString(local, "原创第一句\n原创第二句")
            val localTrack = track.copy(sourceUri = root.resolve("fixture.mp3").toString())
            val result = assertIs<LyricsFetchResult.Found>(repository.fetch(localTrack, sourceFallback = {
                if (available) LyricsFetchResult.Found(synced) else error("Source is offline")
            })).lyrics
            assertEquals(synced.lines.map { it.text }, result.lines.map { it.text })
            assertEquals(available, result.synchronized)
            assertEquals("原创第一句\n原创第二句", Files.readString(local))
        }
    }

    @Test fun failedOrPlainSourcePreservesCachedTextAndOffsetAndCancellationPropagates() = runBlocking {
        for (outcome in listOf("unavailable", "exception", "plain", "cancel")) repositoryScenario { _, repository ->
            repository.writeCache(track, LyricsFetchResult.Found(plain))
            repository.updateOffset(track, 750)
            val fallback: suspend (Track) -> LyricsFetchResult = {
                when (outcome) {
                    "unavailable" -> LyricsFetchResult.Unavailable("No sync version", false)
                    "exception" -> error("Source offline")
                    "cancel" -> throw kotlinx.coroutines.CancellationException("Cancelled")
                    else -> LyricsFetchResult.Found(plain.copy(lines = listOf(LyricLine(null, "另一份纯文本"))))
                }
            }
            if (outcome == "cancel") {
                assertFailsWith<kotlinx.coroutines.CancellationException> { repository.fetch(track, sourceFallback = fallback) }
            } else {
                val result = assertIs<LyricsFetchResult.Found>(repository.fetch(track, sourceFallback = fallback)).lyrics
                assertFalse(result.synchronized)
                assertEquals(plain.lines, result.lines)
                assertEquals(750L, result.offsetMs)
            }
        }
    }

    @Test fun synchronizedSearchCandidateWinsOverHigherScoringPlainOrInvalidTimeline() = runBlocking {
        repositoryScenario(search = """[
            {"trackName":"Fixture Song","artistName":"Fixture Artist","albumName":"Album","duration":10,"plainLyrics":"高匹配度的纯文本"},
            {"trackName":"Fixture Song","artistName":"Fixture Artist","albumName":"Album","duration":10,"syncedLyrics":"[00:00]无有效时间轴\n[00:00]无有效时间轴"},
            {"trackName":"Fixture Song","artistName":"Fixture Artist","duration":12,"syncedLyrics":"[00:01]原创第一句\n[00:05]原创第二句"}
        ]""") { _, repository ->
            val result = assertIs<LyricsFetchResult.Found>(repository.fetch(track)).lyrics
            assertTrue(result.synchronized)
            assertEquals(synced.lines, result.lines)
        }
    }

    @Test fun sourcesPreferSynchronizedLyricsAcrossSourcesAndWithinMatchingCandidates() = runBlocking {
        for (sameSource in listOf(false, true)) {
            val root = Files.createTempDirectory("source-synchronized-priority-")
            try {
                val sources = MusicSourceRegistry(MusicJsonStore(root))
                val song = """{id:'plain',title:'Fixture Song',artist:'Fixture Artist',duration:10}"""
                val timedSong = """{id:'timed',title:'Fixture Song',artist:'Fixture Artist',duration:11}"""
                sources.install("""module.exports={platform:'纯文本优先夹具',search:async()=>[$song${if (sameSource) ",$timedSong" else ""}],getLyric:async song=>({rawLrc:song.id==='timed'?'[00:01]原创第一句\n[00:05]原创第二句':'原创第一句\n原创第二句'})};""")
                if (!sameSource) sources.install("""module.exports={platform:'同步夹具',search:async()=>[$timedSong],getLyric:async()=>({rawLrc:'[00:01]原创第一句\n[00:05]原创第二句'})};""")
                val result = assertIs<LyricsFetchResult.Found>(sources.findLyrics(track)).lyrics
                assertTrue(result.synchronized)
                assertEquals(synced.lines, result.lines)
            } finally { root.toFile().deleteRecursively() }
        }
    }

    @Test fun sourceKeepsUsablePlainLyricsWhenLaterMatchingCandidateFails() = runBlocking {
        val root = Files.createTempDirectory("source-plain-failure-")
        try {
            val sources = MusicSourceRegistry(MusicJsonStore(root))
            sources.install("""module.exports={platform:'保留纯文本夹具',search:async()=>[
                {id:'plain',title:'Fixture Song',artist:'Fixture Artist',duration:10},
                {id:'failure',title:'Fixture Song',artist:'Fixture Artist',duration:11}
            ],getLyric:async song=>{if(song.id==='failure')throw new Error('Unavailable');return {rawLrc:'原创第一句\n原创第二句'};}};""")
            val result = assertIs<LyricsFetchResult.Found>(sources.findLyrics(track)).lyrics
            assertFalse(result.synchronized)
            assertEquals(plain.lines, result.lines)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun modelUnavailableIsNotReportedAsANetworkOrSearchFailure() = runBlocking {
        val client = DeepSeekClient { _, _ -> """{"status":"unavailable"}""" }
        val result = assertIs<LyricsFetchResult.Unavailable>(client.generateLyrics(DeepSeekConfig(apiKey = "fixture"), track))
        assertTrue(result.message.contains("当前模型")); assertTrue(result.message.contains("查找歌词"))
        assertFalse(result.retryable)
    }

    @Test fun modelTextNeverCreatesASynchronizedTimeline() = runBlocking {
        val client = DeepSeekClient { _, _ -> """{"status":"ok","lines":["[00:01]原创第一句","[00:05]原创第二句"]}""" }
        val result = assertIs<LyricsFetchResult.Found>(client.generateLyrics(DeepSeekConfig(apiKey = "fixture"), track)).lyrics
        assertFalse(result.synchronized); assertTrue(result.lines.all { it.timestampMs == null })
    }

    @Test fun publicSourceFindsUserReportedRecordingWhenExplicitlyEnabled() = runBlocking {
        org.junit.Assume.assumeTrue(System.getenv("RESONANCE_LYRICS_ACCEPTANCE") == "1")
        val root = Files.createTempDirectory("lyrics-public-acceptance-")
        try {
            val sources = MusicSourceRegistry(MusicJsonStore(root))
            sources.install(MusicSourceRegistry.loadBuiltinScript("netease.js")!!)
            sources.install(MusicSourceRegistry.loadBuiltinScript("qq.js")!!)
            val reported = Track("reported-public-metadata", "夏天的风", "温岚", "温式效应", "4:01", 0)
            val result = sources.findLyrics(reported)
            if (result !is LyricsFetchResult.Found) {
                val candidates = sources.search("夏天的风 温岚")
                println("Public metadata diagnostics: " + candidates.map { "${it.title}/${it.artist}/${it.durationSeconds}" })
                candidates.filter { it.artist == "温岚" }.take(2).forEach {
                    println("Public lyric diagnostic: title=${it.title}, chars=${sources.lyrics(it)?.length}")
                }
            }
            val lyrics = assertIs<LyricsFetchResult.Found>(result).lyrics
            assertTrue(lyrics.lines.size > 2); assertTrue(lyrics.synchronized)
            println("Public lyrics acceptance: source=${lyrics.source}, lines=${lyrics.lines.size}, synchronized=${lyrics.synchronized}; content omitted")
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun publicSynchronizedLyricsReplaceReportedRecordingsPlainCacheWhenExplicitlyEnabled() = runBlocking {
        org.junit.Assume.assumeTrue(System.getenv("RESONANCE_LYRICS_ACCEPTANCE") == "1")
        val root = Files.createTempDirectory("lyrics-public-cache-recovery-")
        try {
            val sources = MusicSourceRegistry(MusicJsonStore(root.resolve("assistant")))
            sources.install(MusicSourceRegistry.loadBuiltinScript("netease.js")!!)
            sources.install(MusicSourceRegistry.loadBuiltinScript("qq.js")!!)
            val reported = Track("reported-plain-cache", "双截棍", "周杰伦", "范特西", "3:21", 0)
            val repository = LrclibLyricsRepository(root.resolve("lyrics-cache"))
            repository.writeCache(reported, LyricsFetchResult.Found(plain.copy(trackId = reported.id)))
            val lyrics = assertIs<LyricsFetchResult.Found>(repository.fetch(reported, sourceFallback = sources::findLyrics)).lyrics
            assertTrue(lyrics.synchronized)
            assertTrue(lyrics.lines.size > 2)
            assertTrue(lyrics.lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= 42_000L } >= 0)
            println("Reported plain-cache recovery: source=${lyrics.source}, lines=${lyrics.lines.size}, synchronized=${lyrics.synchronized}; content omitted")
        } finally { root.toFile().deleteRecursively() }
    }
}
