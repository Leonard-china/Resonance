package com.resonance.player.features

import com.resonance.player.sources.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class BuiltinMusicSourcesTest {
    private class Network(val reply: (String) -> String) : SourceNetwork {
        val requests = java.util.concurrent.CopyOnWriteArrayList<String>()
        override suspend fun request(options: JSONObject): JSONObject {
            val url = options.getString("url"); requests += url
            return JSONObject().put("status", 200).put("body", reply(url))
        }
        override suspend fun download(url: String, target: Path, headers: JSONObject, maxBytes: Long): Long = error("Unexpected download")
    }

    @Test fun qqResolvesFreeMediaAndLyricsButNeverRequestsPaidMedia() = runBlocking {
        val root = Files.createTempDirectory("qq-source-")
        val network = Network { url -> when {
            "/soso/" in url -> """{"data":{"song":{"list":[{"songmid":"free","songname":"Fixture Song","singer":[{"name":"Artist"}],"interval":10,"pay":{"pay_play":0}},{"songmid":"paid","songname":"Paid Song","singer":[{"name":"Artist"}],"pay":{"pay_play":1}}]}}}"""
            "musicu.fcg" in url -> """{"req_0":{"data":{"sip":["https://stream.example.com/"],"midurlinfo":[{"purl":"fixture.mp3?key=public-fixture"}]}}}"""
            "/lyric/" in url -> """{"lyric":"[00:01]原创歌词一\n[00:05]原创歌词二"}"""
            else -> error("Unexpected request")
        } }
        try {
            val sources = MusicSourceRegistry(MusicJsonStore(root), network)
            sources.install(MusicSourceRegistry.loadBuiltinScript("qq.js")!!)
            val songs = sources.search("fixture")
            assertEquals(2, songs.size)
            assertEquals("https://stream.example.com/fixture.mp3?key=public-fixture", sources.media(songs.first()).getString("url"))
            assertTrue(sources.lyrics(songs.first())!!.contains("[00:01]"))
            val count = network.requests.size
            assertFails { sources.media(songs.last()) }
            assertEquals(count, network.requests.size)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun archiveListsIndividualPublicTracksAndSkipsRestrictedItems() = runBlocking {
        val root = Files.createTempDirectory("archive-source-")
        val network = Network { url -> when {
            "advancedsearch.php" in url -> """{"response":{"docs":[{"identifier":"open-fixture"},{"identifier":"restricted-fixture"}]}}"""
            url.endsWith("restricted-fixture") -> """{"metadata":{"access-restricted-item":"true"},"files":[{"name":"hidden.mp3"}]}"""
            url.endsWith("open-fixture") -> """{"metadata":{"title":"Fixture Album","creator":"Fixture Artist"},"files":[{"name":"one song.mp3","title":"Fixture Song","length":"01:02","size":"2048"},{"name":"one song.flac","size":"4096"},{"name":"private.mp3","private":true}]}"""
            else -> error("Unexpected request")
        } }
        try {
            val sources = MusicSourceRegistry(MusicJsonStore(root), network)
            sources.install(MusicSourceRegistry.loadBuiltinScript("archive.js")!!)
            val song = sources.search("fixture").single()
            assertEquals("Fixture Song", song.title); assertEquals("Fixture Artist", song.artist); assertEquals(62, song.durationSeconds)
            assertEquals("https://archive.org/download/open-fixture/one%20song.mp3", sources.media(song).getString("url"))
        } finally { root.toFile().deleteRecursively() }
    }
}
