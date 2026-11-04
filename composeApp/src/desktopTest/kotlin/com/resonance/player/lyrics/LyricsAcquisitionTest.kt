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
}
