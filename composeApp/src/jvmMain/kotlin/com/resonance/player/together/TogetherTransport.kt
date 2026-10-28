package com.resonance.player.together

import java.net.URI
import java.security.KeyStore
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

internal data class TogetherInvitation(val host: String, val port: Int, val pin: String, val key: String) {
    fun encode(): String {
        require(host.isNotBlank() && host.length <= 253 && host.none(Char::isWhitespace)) { "请输入可直达的 IP 地址或域名" }
        require(port in 1..65535) { "端口需在 1–65535 之间" }
        return URI("resonance-listen", null, host.removeSurrounding("[", "]"), port, "/v1", "pin=$pin&key=$key", null).toASCIIString()
    }

    companion object {
        fun parse(value: String): TogetherInvitation {
            require(value.length <= 4096) { "邀请内容过长" }
            val trimmed = value.trim()
            val uriCandidate = Regex("""resonance-listen://[^\s"'<>\u4e00-\u9fa5，。！？、]+""").find(trimmed)?.value ?: trimmed
            val uri = URI(uriCandidate)
            require(uri.scheme == "resonance-listen" && uri.path == "/v1" && uri.userInfo == null && uri.fragment == null)
            val host = requireNotNull(uri.host).removeSurrounding("[", "]")
            require(host.isNotBlank() && host.length <= 253 && uri.port in 1..65535)
            val parts = uri.rawQuery?.split('&').orEmpty()
            require(parts.size == 2 && parts.map { it.substringBefore('=') }.toSet() == setOf("pin", "key"))
            val fields = parts.associate { it.substringBefore('=') to it.substringAfter('=', "") }
            val pin = fields.getValue("pin")
            val key = fields.getValue("key")
            require(pin.matches(Regex("[a-f0-9]{64}")) && key.matches(Regex("[A-Za-z0-9_-]{43}")))
            require(Base64.getUrlDecoder().decode(key).size == 32)
            return TogetherInvitation(host, uri.port, pin, key)
        }
    }
}

internal object TogetherTls {
    data class Identity(val context: SSLContext, val pin: String, val key: String)

    fun identity(): Identity {
        val random = SecureRandom()
        val now = System.currentTimeMillis()
        val held = HeldCertificate.Builder().ecdsa256().commonName("Resonance temporary listening room")
            .validityInterval(now - 60_000, now + 86_400_000).build()
        val certificate = held.certificate
        val password = ByteArray(32).also(random::nextBytes).let { Base64.getEncoder().encodeToString(it).toCharArray() }
        val store = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry("room", held.keyPair.private, password, arrayOf(certificate))
        }
        val managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(store, password) }
        password.fill('\u0000')
        val context = SSLContext.getInstance("TLS").apply { init(managers.keyManagers, null, random) }
        return Identity(context, fingerprint(certificate), ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) })
    }

    fun client(pin: String): SSLContext {
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
        return SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }
    }

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
