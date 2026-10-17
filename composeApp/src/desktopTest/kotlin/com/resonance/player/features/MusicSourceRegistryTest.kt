package com.resonance.player.features

import com.resonance.player.sources.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class MusicSourceRegistryTest {
    private class Network : SourceNetwork {
        val requests = mutableListOf<JSONObject>()
        override suspend fun request(options: JSONObject): JSONObject {
            requests += options
            return JSONObject().put("status", 200).put("headers", JSONObject()).put("body",
                """{"data":[{"id":"song-1","title":"晴天","artist":"周杰伦","album":"叶惠美","albumId":"album-1","duration":269}]}""")
        }
        override suspend fun download(url: String, target: Path, headers: JSONObject, maxBytes: Long): Long {
            val bytes = "ID3-fixture-audio".toByteArray()
            Files.write(target, bytes); return bytes.size.toLong()
        }
    }
    private suspend fun fixture(block: suspend (MusicSourceRegistry, MusicJsonStore, Network) -> Unit) {
        val root = Files.createTempDirectory("resonance-source-test-")
        try {
            val store = MusicJsonStore(root)
            val network = Network()
            block(MusicSourceRegistry(store, network), store, network)
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }
    @Test fun realQuickJsRunsAsyncMusicFreeCoreAndPersistsInstallation() = runBlocking {
        fixture { sources, store, network ->
            val info = sources.install(SOURCE)
            assertEquals("测试目录", info.name)
            val song = sources.search("晴天 周杰伦").single()
            assertTrue(network.requests.single().getString("url").contains("%E6%99%B4"))
            assertEquals("album-1", song.albumId)
            assertEquals("叶惠美", sources.details(song).album)
            assertEquals("https://audio.example.test/song.m4a", sources.media(song).getString("url"))
            assertTrue(sources.lyrics(song)!!.contains("[00:10.00]"))
            assertEquals(info, MusicSourceRegistry(store, network).list().single())
            sources.enable(info.id, false)
            assertTrue(sources.search("晴天").isEmpty())
            sources.remove(info.id)
            assertTrue(sources.list().isEmpty())
        }
    }
    @Test fun runtimeSupportsCryptoJsWithoutExposingHostClasses() = runBlocking {
        fixture { sources, _, _ ->
            val info = sources.install("""
                const CryptoJS=require('crypto-js');
                module.exports={platform:'crypto-test',search:async function(){
                    return {digest:CryptoJS.MD5('abc').toString(),
                      random:CryptoJS.lib.WordArray.random(16).sigBytes,
                      java:typeof Java,process:typeof process,packages:typeof Packages};
                }};
            """.trimIndent())
            val result = sources.call(info.id, "search", JSONArray()) as JSONObject
            assertEquals("900150983cd24fb0d6963f7d28e17f72", result.getString("digest"))
            assertEquals(16, result.getInt("random"))
            listOf("java", "process", "packages").forEach { assertEquals("undefined", result.getString(it)) }
        }
    }
    @Test fun invalidReplacementAndUnsupportedDependencyKeepInstalledSource() = runBlocking {
        fixture { sources, _, _ ->
            val original = sources.install(SOURCE)
            assertFails { sources.install("module.exports={platform:'测试目录'}") }
            assertEquals(original, sources.list().single())
            assertEquals(1, sources.search("test").size)
            val blocked = sources.install("module.exports={platform:'blocked',search:function(){return require('fs')}}")
            assertFails { sources.call(blocked.id, "search", JSONArray()) }
        }
    }
    @Test fun cancellationInterruptsInfiniteScriptAndRuntimeCanBeUsedAgain() = runBlocking {
        fixture { sources, _, _ ->
            assertFailsWith<TimeoutCancellationException> {
                withTimeout(500) { sources.install("while(true){}") }
            }
            assertEquals("测试目录", sources.install(SOURCE).name)
        }
    }
    @Test fun publicTransportRejectsLocalAddressesAndCredentialUrls() {
        listOf("http://example.com", "https://user:password@example.com", "https://localhost/", "file:///tmp/song")
            .forEach { assertFails { PublicSourceNetwork.publicUrl(it) } }
        listOf("127.0.0.1", "10.0.0.1", "192.168.1.1", "169.254.169.254", "100.100.100.200", "::1", "fc00::1")
            .forEach { assertFalse(PublicSourceNetwork.publicAddress(InetAddress.getByName(it)), it) }
        assertTrue(PublicSourceNetwork.publicAddress(InetAddress.getByName("1.1.1.1")))
    }
    @Test fun builtinSourcesInstalledSuccessfullyAndExposeCoreCapabilities() = runBlocking {
        fixture { sources, _, _ ->
            val installed = sources.installBuiltinSources()
            assertEquals(3, installed.size)
            val names = installed.map { it.name }.toSet()
            assertTrue(names.contains("网易云音乐"))
            assertTrue(names.contains("酷我音乐"))
            assertTrue(names.contains("酷狗音乐"))
            installed.forEach { source ->
                assertTrue("search" in source.capabilities, "${source.name} 缺少 search")
                assertTrue("getMediaSource" in source.capabilities, "${source.name} 缺少 getMediaSource")
                assertTrue("getLyric" in source.capabilities, "${source.name} 缺少 getLyric")
                assertTrue(source.enabled, "${source.name} 应当默认启用")
            }
            // 幂等性测试
            val reinstalled = sources.installBuiltinSources()
            assertEquals(3, reinstalled.size)
            assertEquals(3, sources.list().size)
        }
    }
    companion object {
        private val SOURCE = """
            const axios=require('axios');
            module.exports={
              platform:'测试目录',version:'1.0.0',
              search:async function(q,page,type){return (await axios.get('https://catalog.example.test/search',{params:{q:q}})).data},
              getMusicInfo:async function(item){return {album:'叶惠美',albumId:'album-1'}},
              getMediaSource:async function(item,quality){return {url:'https://audio.example.test/song.m4a'}},
              getLyric:async function(item){return {rawLrc:'[00:10.00]第一句\n[00:20.00]第二句'}}
            };
        """.trimIndent()
    }
}
