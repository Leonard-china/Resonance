package com.resonance.player.sources

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.evaluate
import com.resonance.player.features.MusicJsonStore
import com.resonance.player.model.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64

/** MusicFree core protocol. Unsupported npm modules fail explicitly; no host reflection is exposed. */
internal class MusicSourceRegistry(
    private val store: MusicJsonStore,
    val network: SourceNetwork = PublicSourceNetwork(),
) {
    private val directory = store.directory.resolve("sources")
    fun list(): List<MusicSourceInfo> {
        val a = store.read().optJSONArray("musicSources") ?: return emptyList()
        return (0 until a.length()).mapNotNull { index -> a.optJSONObject(index)?.let { j ->
            val id = j.optString("id")
            if (!id.matches(Regex("[a-f0-9]{64}"))) null else MusicSourceInfo(id, j.optString("name"),
                j.optString("version"), j.optBoolean("enabled", true), strings(j.optJSONArray("capabilities")))
        } }
    }
    suspend fun install(text: String): MusicSourceInfo = withContext(Dispatchers.IO) {
        require(text.toByteArray().size in 1..MAX_SCRIPT_BYTES) { "歌源脚本为空或超过 2 MB" }
        val metadata = JSONObject(evaluate(text, "JSON.stringify({name:module.exports.platform,version:module.exports.version||'',capabilities:Object.keys(module.exports).filter(k=>typeof module.exports[k]==='function')})", false))
        val name = metadata.optString("name").trim()
        require(name.isNotBlank() && name.length <= 80 && name != "本地") { "脚本不是有效的 MusicFree 歌源" }
        val capabilities = strings(metadata.optJSONArray("capabilities")).filter { it in METHODS }
        require("search" in capabilities || "getMediaSource" in capabilities) { "歌源缺少搜索或音频解析能力" }
        val id = MusicJsonStore.hash(name)
        val old = list()
        require(old.size < 20 || old.any { it.id == id }) { "最多安装 20 个歌源" }
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, ".source-", ".tmp")
        try {
            Files.write(temporary, text.toByteArray(Charsets.UTF_8))
            Files.move(temporary, scriptPath(id), StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temporary) }
        val info = MusicSourceInfo(id, name, metadata.optString("version").take(40), true, capabilities)
        store.update { it.put("musicSources", JSONArray(old.filterNot { source -> source.id == id }.map(::encode) + encode(info))) }
        info
    }
    suspend fun installUrl(url: String): List<MusicSourceInfo> {
        val response = network.request(JSONObject().put("url", PublicSourceNetwork.publicUrl(url)))
        check(response.optInt("status") in 200..299) { "歌源地址读取失败" }
        val body = response.getString("body")
        if (!body.trimStart().startsWith("{") && !body.trimStart().startsWith("[")) return listOf(install(body))
        val root = runCatching { JSONObject(body) }.getOrNull()
        val entries = root?.optJSONArray("plugins") ?: runCatching { JSONArray(body) }.getOrNull()
            ?: error("订阅 JSON 需要 plugins 数组")
        require(entries.length() in 1..20) { "一次最多导入 20 个歌源" }
        return (0 until entries.length()).map { index ->
            val item = entries.get(index)
            val link = if (item is String) item else (item as JSONObject).getString("url")
            val script = network.request(JSONObject().put("url", PublicSourceNetwork.publicUrl(link)))
            check(script.optInt("status") in 200..299) { "订阅中的歌源读取失败" }
            install(script.getString("body"))
        }
    }
    suspend fun installBuiltinSources(): List<MusicSourceInfo> = withContext(Dispatchers.IO) {
        val installed = mutableListOf<MusicSourceInfo>()
        for (file in BUILTIN_SCRIPTS) {
            val script = loadBuiltinScript(file) ?: continue
            try {
                installed += install(script)
            } catch (_: Exception) {}
        }
        installed
    }
    fun enable(id: String, enabled: Boolean) {
        val all = list(); require(all.any { it.id == id }) { "歌源不存在" }
        store.update { it.put("musicSources", JSONArray(all.map { source -> encode(if (source.id == id) source.copy(enabled = enabled) else source) })) }
    }
    fun remove(id: String) {
        val all = list(); require(all.any { it.id == id }) { "歌源不存在" }
        store.update { it.put("musicSources", JSONArray(all.filterNot { source -> source.id == id }.map(::encode))) }
        Files.deleteIfExists(scriptPath(id))
    }
    suspend fun search(query: String, limit: Int = 40): List<SourceSong> {
        require(query.isNotBlank()) { "请输入歌曲或歌手" }
        val result = mutableListOf<SourceSong>()
        val failures = mutableListOf<String>()
        for (source in list().filter { it.enabled && "search" in it.capabilities }) {
            try { result += parseSongs(source.id, call(source.id, "search", JSONArray().put(query.take(180)).put(1).put("music"))) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failures += source.name }
        }
        if (result.isEmpty() && failures.isNotEmpty()) error("这些歌源查询未完成，请检查插件或网络：" + failures.joinToString("、"))
        return result.distinctBy { it.sourceId + ":" + it.id }.take(limit)
    }
    suspend fun recommendations(artists: List<String>, limit: Int): List<SourceSong> {
        val candidates = mutableListOf<SourceSong>()
        val queryList = if (artists.isEmpty()) listOf("热歌", "流行", "经典") else artists.distinct().take(4)
        for (artist in queryList) candidates += search(artist, limit)
        if (candidates.isEmpty()) {
            for (source in list().filter { it.enabled && "getTopLists" in it.capabilities && "getTopListDetail" in it.capabilities }) {
                val groups = asArray(call(source.id, "getTopLists", JSONArray()))
                val first = groups.optJSONObject(0)?.let { it.optJSONArray("data")?.optJSONObject(0) ?: it }
                if (first != null) candidates += parseSongs(source.id, call(source.id, "getTopListDetail", JSONArray().put(first)))
            }
        }
        return candidates.distinctBy { it.sourceId + ":" + it.id }.take(limit)
    }
    suspend fun details(song: SourceSong): SourceSong {
        val info = list().firstOrNull { it.id == song.sourceId } ?: error("歌源已卸载")
        if ("getMusicInfo" !in info.capabilities) return song
        val result = call(song.sourceId, "getMusicInfo", JSONArray().put(JSONObject(song.raw)))
        val merged = JSONObject(song.raw)
        if (result is JSONObject) result.keys().forEach { merged.put(it, result.get(it)) }
        return parseSong(song.sourceId, merged) ?: error("歌源返回的歌曲信息不完整")
    }
    suspend fun media(song: SourceSong): JSONObject {
        val info = list().firstOrNull { it.id == song.sourceId && it.enabled } ?: error("歌源已停用或卸载")
        val result = if ("getMediaSource" in info.capabilities) call(song.sourceId, "getMediaSource", JSONArray().put(JSONObject(song.raw)).put("high"))
            else JSONObject().put("url", JSONObject(song.raw).optString("url"))
        val resolved = when (result) {
            is String -> JSONObject().put("url", result)
            is JSONObject -> result
            else -> error("歌源未提供可用的完整音频")
        }
        PublicSourceNetwork.publicUrl(resolved.getString("url"))
        return resolved
    }
    suspend fun lyrics(song: SourceSong): String? {
        val info = list().firstOrNull { it.id == song.sourceId } ?: return null
        val result = if ("getLyric" in info.capabilities) call(song.sourceId, "getLyric", JSONArray().put(JSONObject(song.raw))) else JSONObject(song.raw)
        val raw = (result as? JSONObject)?.optString("rawLrc")?.takeIf(String::isNotBlank)
        if (raw != null) return raw.take(300_000)
        val link = (result as? JSONObject)?.optString("lrc")?.takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: return null
        return network.request(JSONObject().put("url", link)).takeIf { it.optInt("status") == 200 }?.optString("body")
    }
    suspend fun call(id: String, method: String, args: JSONArray): Any? {
        require(method in METHODS) { "歌源操作不支持" }
        val source = list().firstOrNull { it.id == id && it.enabled } ?: error("歌源已停用或卸载")
        require(method in source.capabilities) { "歌源未实现该功能：" + method }
        val file = scriptPath(id)
        require(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && Files.size(file) <= MAX_SCRIPT_BYTES) { "歌源文件缺失或过大" }
        val expression = "JSON.stringify(await module.exports[" + JSONObject.quote(method) + "](..." + args + ") ?? null)"
        val result = evaluate(String(Files.readAllBytes(file), Charsets.UTF_8), expression, true)
        return org.json.JSONTokener(result).nextValue().takeUnless { it === JSONObject.NULL }
    }
    private suspend fun evaluate(script: String, expression: String, networkEnabled: Boolean): String = withTimeout(25_000) {
        val js = QuickJs.create(Dispatchers.Default)
        try {
            js.memoryLimit = 32L * 1024 * 1024
            js.maxStackSize = 512L * 1024
            js.evaluationTimeoutMillis = 5_000
            js.asyncFunction<String, String>("__sourceRequest") { options ->
                check(networkEnabled) { "安装检查期间不执行网络请求" }
                network.request(JSONObject(options)).toString()
            }
            js.function<String, String>("__sourceBtoa") { Base64.getEncoder().encodeToString(it.toByteArray(Charsets.ISO_8859_1)) }
            js.function<String, String>("__sourceAtob") { String(Base64.getDecoder().decode(it), Charsets.ISO_8859_1) }
            js.function<Int, String>("__sourceRandom") { size ->
                require(size in 1..4096) { "随机字节请求过大" }
                Base64.getEncoder().encodeToString(ByteArray(size).also { java.security.SecureRandom().nextBytes(it) })
            }
            val crypto = if (script.contains("crypto-js")) "var __cryptoModule={exports:{}};(function(module,exports){\n" +
                cryptoScript + "\n})(__cryptoModule,__cryptoModule.exports);\n" else ""
            js.evaluate<String>(SHIM + "\n" + crypto + script + "\n;" + expression)
        } finally { js.close() }
    }
    private fun scriptPath(id: String): Path {
        require(id.matches(Regex("[a-f0-9]{64}"))) { "歌源标识无效" }
        return directory.resolve(id + ".js")
    }
    companion object {
        const val MAX_SCRIPT_BYTES = 2_000_000
        val METHODS = setOf("search", "getMediaSource", "getMusicInfo", "getLyric", "getAlbumInfo", "getMusicSheetInfo", "getTopLists", "getTopListDetail")
        fun asArray(value: Any?): JSONArray = when (value) {
            is JSONArray -> value
            is JSONObject -> value.optJSONArray("data") ?: value.optJSONArray("musicList") ?: JSONArray()
            else -> JSONArray()
        }
        fun parseSongs(source: String, value: Any?): List<SourceSong> = asArray(value).let { array ->
            (0 until array.length().coerceAtMost(100)).mapNotNull { index -> array.optJSONObject(index)?.let { parseSong(source, it) } }
        }
        fun parseSong(source: String, value: JSONObject): SourceSong? {
            val id = value.optString("id").take(180); val title = value.optString("title").trim().take(180)
            val artist = value.optString("artist").trim().take(180)
            if (id.isBlank() || title.isBlank() || artist.isBlank()) return null
            return SourceSong(source, id, title, artist, value.optString("album").take(180), value.optString("albumId").take(180),
                value.optDouble("duration", 0.0).toInt().coerceIn(0, 86400), value.optString("artwork").take(3000),
                value.optString("versionName", value.optString("version")).take(80), value.toString())
        }
        private fun strings(value: JSONArray?): List<String> = value?.let { (0 until it.length()).map(it::optString) }.orEmpty()
        private fun encode(info: MusicSourceInfo) = JSONObject().put("id", info.id).put("name", info.name)
            .put("version", info.version).put("enabled", info.enabled).put("capabilities", JSONArray(info.capabilities))
        val BUILTIN_SCRIPTS = listOf("netease.js", "kuwo.js", "kugou.js")
        fun loadBuiltinScript(name: String): String? {
            val stream = MusicSourceRegistry::class.java.classLoader?.getResourceAsStream("music-source/builtin/$name")
                ?: return null
            return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        private val cryptoScript: String by lazy {
            val input = MusicSourceRegistry::class.java.classLoader?.getResourceAsStream("music-source/crypto-js-4.2.0.js")
                ?: error("应用缺少 crypto-js 运行资源")
            input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        private val SHIM = """
            var global = typeof globalThis !== 'undefined' ? globalThis : this;
            var module={exports:{}}; var exports=module.exports;
            if(!global.console) global.console={log:function(){},warn:function(){},error:function(){}};
            global.btoa=__sourceBtoa; global.atob=__sourceAtob;
            global.crypto={getRandomValues:function(array){var raw=global.atob(__sourceRandom(array.byteLength));var b=new Uint8Array(array.buffer,array.byteOffset,array.byteLength);for(var i=0;i<b.length;i++)b[i]=raw.charCodeAt(i);return array}};
            async function __sourceRequestFn(config) {
              if(typeof config==='string') config={url:config};
              config=Object.assign({method:'GET'},config||{});
              if(config.params) {
                var q=Object.keys(config.params).map(k=>encodeURIComponent(k)+'='+encodeURIComponent(config.params[k])).join('&');
                config.url+=(config.url.includes('?')?'&':'?')+q;
              }
              if(config.data!==undefined) config.body=typeof config.data==='string'?config.data:JSON.stringify(config.data);
              var r=JSON.parse(await __sourceRequest(JSON.stringify(config)));
              var data=r.body; if(config.responseType!=='text') {try{data=JSON.parse(data)}catch(e){}}
              var result={data:data,status:r.status,headers:r.headers};
              if(r.status<200||r.status>=300) {var e=new Error('歌源 HTTP '+r.status);e.response=result;throw e;}
              return result;
            }
            var __sourceAxiosFn=function(config){return __sourceRequestFn(config)};
            __sourceAxiosFn.request=__sourceRequestFn;
            __sourceAxiosFn.get=(url,config)=>__sourceRequestFn(Object.assign({},config||{},{url:url,method:'GET'}));
            __sourceAxiosFn.post=(url,data,config)=>__sourceRequestFn(Object.assign({},config||{},{url:url,data:data,method:'POST'}));
            __sourceAxiosFn.create=(defaults)=>{var a=c=>__sourceRequestFn(Object.assign({},defaults,c));a.get=(u,c)=>a(Object.assign({},c,{url:u,method:'GET'}));a.post=(u,d,c)=>a(Object.assign({},c,{url:u,data:d,method:'POST'}));return a};
            __sourceAxiosFn.all=Promise.all.bind(Promise);
            global.axios=__sourceAxiosFn;
            var __sourceQs={
              stringify:function(o){return Object.keys(o).map(k=>encodeURIComponent(k)+'='+encodeURIComponent(o[k])).join('&')},
              parse:function(s){return s.split('&').reduce((o,p)=>{var a=p.split('=');o[decodeURIComponent(a.shift())]=decodeURIComponent(a.join('='));return o},{})}
            };
            global.qs=__sourceQs;
            global.require=function(name) {
              if(name==='axios')return global.axios;
              if(name==='crypto-js')return (typeof __cryptoModule!=='undefined'?__cryptoModule.exports:undefined);
              if(name==='qs')return global.qs;
              throw new Error('暂不支持插件依赖 '+name+'；请使用已打包依赖的 MusicFree 源');
            };
            global.fetch=async function(url,options){var r=await __sourceRequestFn(Object.assign({},options||{},{url:url,responseType:'text'}));return {ok:r.status>=200&&r.status<300,status:r.status,text:async()=>r.data,json:async()=>JSON.parse(r.data)}};
        """.trimIndent()
    }
}
