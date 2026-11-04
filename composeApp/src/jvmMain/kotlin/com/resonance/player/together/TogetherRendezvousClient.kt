package com.resonance.player.together

import com.resonance.player.platform.TogetherPartner
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.security.KeyPair
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** Opt-in reachability only. This API has no room capability, text or audio argument. */
internal class TogetherRendezvousClient(url: String, private val identity: KeyPair, allowHttpLoopback: Boolean = false) {
    private val base = checkedUrl(url, allowHttpLoopback)
    private val mutex = Mutex()
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val public = identity.public.encoded
    private val id = MessageDigest.getInstance("SHA-256").digest(public).joinToString("") { "%02x".format(it.toInt() and 255) }
    suspend fun register(contact: JSONObject, partners: List<TogetherPartner>) = request("register", JSONObject()
        .put("addresses", contact.getJSONArray("addresses")).put("port", contact.getInt("port"))
        .put("partners", JSONArray(partners.map { it.id })))
    suspend fun withdraw() = request("withdraw", JSONObject())
    suspend fun lookup(partner: TogetherPartner): TogetherPartner? {
        val result = request("lookup", JSONObject().put("partner", partner.id))
        if (!result.getBoolean("reachable")) return null
        val rows = result.getJSONArray("addresses").also { require(it.length() in 1..TogetherInvitation.MAX_HOSTS) }
        val hosts = (0 until rows.length()).map { index ->
            val host = TogetherInvitation.checkedHost(rows.getString(index))
            require(host.all { it in "0123456789abcdefABCDEF:." } && (':' in host || host.split('.').let { pieces ->
                pieces.size == 4 && pieces.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }
            }))
            host
        }
        val port = result.get("port").toString().toInt().also { require(it in 1..65535) }
        return partner.copy(addresses = hosts, port = port)
    }
    private suspend fun request(action: String, payload: JSONObject): JSONObject = withContext(Dispatchers.IO) { mutex.withLock {
        currentCoroutineContext().ensureActive()
        val nonce = post("challenge", JSONObject().put("device", id)).getString("challenge")
        require(nonce.matches(Regex("[A-Za-z0-9_-]{43}")))
        val encoded = encoder.encodeToString(payload.toString().toByteArray(Charsets.UTF_8))
        val bytes = "Resonance rendezvous v1\n$action\n$nonce\n$encoded".toByteArray(Charsets.US_ASCII)
        val signature = Signature.getInstance("SHA256withECDSA").run { initSign(identity.private); update(bytes); sign() }
        currentCoroutineContext().ensureActive()
        post(action, JSONObject().put("publicKey", encoder.encodeToString(public)).put("challenge", nonce)
            .put("payload", encoded).put("signature", encoder.encodeToString(signature)))
    } }
    private fun post(action: String, payload: JSONObject): JSONObject {
        val bytes = payload.toString().toByteArray(Charsets.UTF_8).also { require(it.size in 1..16_384) }
        val connection = base.resolve("v1/$action").toURL().openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 3_000; connection.readTimeout = 3_000
            connection.requestMethod = "POST"; connection.doOutput = true; connection.useCaches = false
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Cache-Control", "no-store")
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
            check(connection.responseCode == 200) { "Reachability request rejected" }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(2048)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 16_384)
                    output.write(buffer, 0, count)
                }
            }
            return parseTogetherJson(output.toByteArray())
        } finally { connection.disconnect() }
    }
    companion object {
        fun checkedUrl(value: String, allowHttpLoopback: Boolean = false): URI {
            require(value.length in 1..512)
            val uri = URI(value.trim())
            require(uri.userInfo == null && uri.fragment == null && uri.query == null && uri.host != null &&
                (uri.port == -1 || uri.port in 1..65535))
            require(uri.scheme == "https" || allowHttpLoopback && uri.scheme == "http" &&
                uri.host.removeSurrounding("[", "]") in setOf("127.0.0.1", "::1", "localhost")) { "寻址服务需使用 HTTPS" }
            return URI(uri.toASCIIString().trimEnd('/') + "/")
        }
    }
}
