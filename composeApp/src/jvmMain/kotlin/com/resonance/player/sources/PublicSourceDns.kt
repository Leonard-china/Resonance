package com.resonance.player.sources

import com.resonance.player.io.readUpTo
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/** VPN Fake-IP addresses aren't destinations. Resolve them independently, then apply the same public-IP rule. */
internal class PublicSourceDns(
    private val system: Dns = Dns.SYSTEM,
    private val resolveFakeIp: (String) -> List<InetAddress> = ::resolveHttps,
) : Dns {
    private data class Cached(val addresses: List<InetAddress>, val expiresNanos: Long)
    private val cache = LinkedHashMap<String, Cached>()

    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = system.lookup(hostname)
        if (addresses.isNotEmpty() && addresses.all(PublicSourceNetwork::publicAddress)) return addresses
        require(addresses.isNotEmpty() && addresses.all(::isFakeIp) &&
            hostname.any(Char::isLetter) && ':' !in hostname) { "歌源只允许访问公网地址" }
        synchronized(cache) { cache[hostname]?.takeIf { it.expiresNanos > System.nanoTime() }?.let { return it.addresses } }
        val verified = resolveFakeIp(hostname)
        require(verified.isNotEmpty() && verified.all(PublicSourceNetwork::publicAddress)) { "歌源只允许访问公网地址" }
        synchronized(cache) {
            cache[hostname] = Cached(verified.toList(), System.nanoTime() + TimeUnit.SECONDS.toNanos(60))
            while (cache.size > 128) cache.remove(cache.keys.first())
        }
        return verified
    }

    companion object {
        private fun isFakeIp(address: InetAddress): Boolean = address.address.let {
            it.size == 4 && (it[0].toInt() and 255) == 198 && (it[1].toInt() and 255) in 18..19
        }
        private val resolver by lazy {
            OkHttpClient.Builder().dns(Dns.SYSTEM).followRedirects(false).followSslRedirects(false)
                .connectTimeout(4, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).callTimeout(8, TimeUnit.SECONDS).build()
        }
        private fun resolveHttps(hostname: String): List<InetAddress> {
            val url = "https://cloudflare-dns.com/dns-query".toHttpUrl().newBuilder()
                .addQueryParameter("name", hostname).addQueryParameter("type", "A").build()
            val request = Request.Builder().url(url).header("Accept", "application/dns-json").build()
            return resolver.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "VPN 地址解析暂不可用，请稍后重试" }
                val bytes = response.body?.byteStream()?.use { it.readUpTo(64_001) } ?: error("DNS 响应为空")
                require(bytes.size <= 64_000) { "DNS 响应过大" }
                val json = JSONObject(bytes.toString(Charsets.UTF_8))
                check(json.optInt("Status", -1) == 0) { "未能解析歌源地址" }
                val answers = json.optJSONArray("Answer") ?: error("未能解析歌源地址")
                (0 until answers.length().coerceAtMost(32)).mapNotNull { index ->
                    val answer = answers.optJSONObject(index) ?: return@mapNotNull null
                    val value = answer.optString("data")
                    if (answer.optInt("type") != 1 || !value.matches(Regex("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))) null
                    else InetAddress.getByName(value)
                }.distinct()
            }
        }
    }
}
