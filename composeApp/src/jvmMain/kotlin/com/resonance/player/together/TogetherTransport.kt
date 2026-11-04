package com.resonance.player.together

import java.net.URI
import java.security.KeyStore
import java.security.KeyPair
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Base64
import javax.net.ssl.*
import okhttp3.tls.HeldCertificate
import org.json.JSONObject
import org.json.JSONTokener

internal fun parseTogetherJson(bytes: ByteArray): JSONObject {
    val text = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    // Android supplies its own org.json implementation. Validate protocol limits before
    // either platform parser, including escaped duplicate keys and recursion depth.
    val containers = mutableListOf<Pair<Char, MutableSet<String>>>()
    var colonAt = -1
    var index = 0
    while (index < text.length) {
        when (val character = text[index]) {
            '"' -> {
                val start = index++
                var escaped = false
                while (index < text.length) {
                    val value = text[index]
                    require(value.code >= 32)
                    if (!escaped && value == '"') break
                    if (escaped) escaped = false else if (value == '\\') escaped = true
                    index++
                }
                require(index < text.length)
                var after = index + 1
                while (after < text.length && text[after].isWhitespace()) after++
                if (after < text.length && text[after] == ':') {
                    require(containers.lastOrNull()?.first == '{')
                    val key = JSONTokener(text.substring(start, index + 1)).nextValue() as String
                    require(containers.last().second.add(key))
                    colonAt = after
                }
            }
            '{', '[' -> { containers += character to mutableSetOf<String>(); require(containers.size <= 16) }
            '}', ']' -> {
                require(containers.isNotEmpty() && containers.last().first == if (character == '}') '{' else '[')
                containers.removeAt(containers.lastIndex)
            }
            ':' -> { require(colonAt == index); colonAt = -1 }
            ',', ' ', '\t', '\r', '\n' -> Unit
            else -> {
                val start = index
                while (index < text.length && text[index] !in "{}[],: \t\r\n") index++
                val token = text.substring(start, index)
                require(token in setOf("true", "false", "null") || token.matches(Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")))
                index--
            }
        }
        index++
    }
    require(containers.isEmpty())
    val tokener = JSONTokener(text)
    return JSONObject(tokener).also { require(tokener.nextClean() == '\u0000') }
}

internal data class TogetherInvitation(
    val host: String,
    val port: Int,
    val pin: String,
    val key: String,
    val candidates: List<String> = emptyList(),
    val relayUrl: String = "",
) {
    val allHosts: List<String>
        get() = (listOf(host) + candidates).map(::checkedHost).distinct().also { require(it.size <= MAX_HOSTS) { "邀请候选地址过多" } }

    fun encode(): String {
        val primary = checkedHost(host)
        require(port in 1..65535) { "端口需在 1–65535 之间" }
        require(candidates.size <= MAX_HOSTS) { "邀请候选地址过多" }
        val cleanCands = allHosts.filter { it != primary }
        val candsParam = if (cleanCands.isNotEmpty()) "&cands=" + cleanCands.joinToString(",") else ""
        val relayParam = if (relayUrl.isEmpty()) "" else "&relay=" + Base64.getUrlEncoder().withoutPadding().encodeToString(
            TogetherRelayTransport.endpoint(relayUrl).toASCIIString().toByteArray(Charsets.UTF_8))
        return URI("resonance-listen", null, primary, port, "/v1", "pin=$pin&key=$key$candsParam$relayParam", null).toASCIIString()
    }

    companion object {
        const val MAX_HOSTS = 8
        internal fun checkedHost(raw: String): String {
            val value = raw.removeSurrounding("[", "]")
            require(value.isNotBlank() && value.length <= 253 && value.all { it.code in 33..126 } && '%' !in value) { "候选地址格式无效" }
            val parsed = URI("resonance-listen", null, value, 1, "/v1", null, null).host?.removeSurrounding("[", "]")
            require(parsed?.equals(value, ignoreCase = true) == true) { "候选地址格式无效" }
            return value
        }
        fun parse(value: String): TogetherInvitation {
            require(value.length <= 4096) { "邀请内容过长" }
            val trimmed = value.trim()
            val uriCandidate = Regex("""resonance-listen://[^\s"'<>\u4e00-\u9fa5，。！？、]+""").find(trimmed)?.value ?: trimmed
            val uri = URI(uriCandidate)
            require(uri.scheme == "resonance-listen" && uri.path == "/v1" && uri.userInfo == null && uri.fragment == null)
            val host = requireNotNull(uri.host).removeSurrounding("[", "]")
            require(host.isNotBlank() && host.length <= 253 && uri.port in 1..65535)
            val parts = uri.rawQuery?.split('&').orEmpty()
            val keys = parts.map { it.substringBefore('=') }
            require(keys.size == keys.distinct().size) { "邀请参数包含重复键" }
            val fields = parts.associate { it.substringBefore('=') to it.substringAfter('=', "") }
            require(fields.containsKey("pin") && fields.containsKey("key"))
            require(fields.keys.all { it in setOf("pin", "key", "cands", "relay") }) { "包含未知参数" }
            val pin = fields.getValue("pin")
            val key = fields.getValue("key")
            require(pin.matches(Regex("[a-f0-9]{64}")) && key.matches(Regex("[A-Za-z0-9_-]{43}")))
            require(Base64.getUrlDecoder().decode(key).size == 32)
            val cands = fields["cands"]?.split(',')?.also { require(it.size < MAX_HOSTS) { "邀请候选地址过多" } }?.map(::checkedHost).orEmpty()
            val relay = fields["relay"]?.let { encoded ->
                require(encoded.length in 1..684 && encoded.matches(Regex("[A-Za-z0-9_-]+")))
                TogetherRelayTransport.endpoint(String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)).toASCIIString()
            }.orEmpty()
            return TogetherInvitation(checkedHost(host), uri.port, pin, key, cands, relay).also { it.allHosts }
        }
    }
}

internal object TogetherTls {
    data class Identity(val context: SSLContext, val pin: String, val key: String, val keyManagers: Array<javax.net.ssl.KeyManager>)

    fun identity(keyPair: KeyPair? = null, lifetimeMs: Long = 86_400_000): Identity {
        val random = SecureRandom()
        val now = System.currentTimeMillis()
        val held = HeldCertificate.Builder().ecdsa256().commonName("Resonance temporary listening room")
            .validityInterval(now - 60_000, now + lifetimeMs).also { if (keyPair != null) it.keyPair(keyPair) }.build()
        val certificate = held.certificate
        val certificates = okhttp3.tls.HandshakeCertificates.Builder().heldCertificate(held).build()
        val managers = arrayOf<javax.net.ssl.KeyManager>(certificates.keyManager)
        val context = SSLContext.getInstance("TLS").apply { init(managers, arrayOf(deviceTrust()), random) }
        return Identity(context, fingerprint(certificate), ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }, managers)
    }

    fun client(pin: String, identity: Identity? = null): SSLContext {
        val expected = pin.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        require(expected.size == 32)
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) { throw CertificateException("Client certificates are not used") }
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val certificate = chain?.singleOrNull() ?: throw CertificateException("Unexpected certificate chain")
                certificate.checkValidity()
                if (!MessageDigest.isEqual(expected, MessageDigest.getInstance("SHA-256").digest(certificate.encoded))) {
                    throw CertificateException("Room certificate does not match invitation")
                }
                certificate.verify(certificate.publicKey)
            }
        }
        return SSLContext.getInstance("TLS").apply { init(identity?.keyManagers, arrayOf(trust), SecureRandom()) }
    }

    /** TLS proves possession; the room capability and human approval still authorize access. */
    private fun deviceTrust() = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) { checkedDevice(chain) }
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) { checkedDevice(chain) }
    }
    fun partnerClient(id: String, identity: Identity): SSLContext {
        require(id.matches(Regex("[a-f0-9]{64}")))
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) { throw CertificateException("Unexpected role") }
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (publicKeyId(checkedDevice(chain)) != id) throw CertificateException("Partner device does not match")
            }
        }
        return SSLContext.getInstance("TLS").apply { init(identity.keyManagers, arrayOf(trust), SecureRandom()) }
    }
    fun publicKeyId(key: java.security.PublicKey): String = MessageDigest.getInstance("SHA-256").digest(key.encoded)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    fun publicKeyId(cert: X509Certificate): String = publicKeyId(cert.publicKey)
    private fun checkedDevice(chain: Array<out X509Certificate>?): X509Certificate {
        val certificate = chain?.singleOrNull() ?: throw CertificateException("Unexpected device certificate")
        certificate.checkValidity()
        val key = certificate.publicKey as? java.security.interfaces.ECPublicKey ?: throw CertificateException("Expected EC device key")
        if (key.params.curve.field.fieldSize != 256) throw CertificateException("Expected P256 device key")
        certificate.verify(key)
        return certificate
    }
    fun peerId(socket: SSLSocket): String = runCatching {
        val cert = checkedDevice(socket.session.peerCertificates.map { it as X509Certificate }.toTypedArray())
        publicKeyId(cert)
    }.getOrDefault("")

    fun configure(socket: SSLSocket) {
        socket.enabledProtocols = socket.supportedProtocols.filter { it == "TLSv1.3" || it == "TLSv1.2" }.toTypedArray()
        socket.enabledCipherSuites = allowedSuites(socket.supportedCipherSuites)
        socket.soTimeout = 15_000
        socket.tcpNoDelay = true
    }

    fun configure(socket: SSLServerSocket) {
        socket.enabledProtocols = socket.supportedProtocols.filter { it == "TLSv1.3" || it == "TLSv1.2" }.toTypedArray()
        socket.enabledCipherSuites = allowedSuites(socket.supportedCipherSuites)
        socket.needClientAuth = false
    }

    private fun allowedSuites(supported: Array<String>): Array<String> = supported.filter {
        it in setOf("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256")
    }.also { require(it.isNotEmpty()) { "设备缺少安全 TLS 套件" } }.toTypedArray()

    private fun fingerprint(certificate: X509Certificate): String = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
