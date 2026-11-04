package com.resonance.player.together

import org.json.JSONObject
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.URI
import java.net.Socket
import java.security.MessageDigest
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/** An opt-in outbound TLS tunnel. The existing pinned room TLS remains inside the tunnel. */
internal object TogetherRelayTransport {
    class WaitingExpired : java.io.IOException("等待加入的连接已到期")
    private val capability = Regex("[A-Za-z0-9_-]{43}")

    fun endpoint(raw: String): URI {
        require(raw.length <= 512)
        val uri = URI(raw.trim())
        val publicWebSocket = uri.scheme.equals("wss", ignoreCase = true)
        require((publicWebSocket || uri.scheme.equals("https", ignoreCase = true)) && uri.host != null && uri.userInfo == null &&
            uri.rawQuery == null && uri.fragment == null &&
            (if (publicWebSocket) uri.path == "/resonance-relay/v1" else uri.path.orEmpty() in listOf("", "/")) &&
            (uri.port == -1 || uri.port in 1..65535)) { "中继地址须为 HTTPS 主机或 WSS 一起听服务地址" }
        TogetherInvitation.checkedHost(uri.host)
        return uri
    }

    fun route(invitationKey: String): String {
        require(invitationKey.matches(capability))
        return MessageDigest.getInstance("SHA-256")
            .digest(("resonance-relay-v1:" + invitationKey).toByteArray(Charsets.US_ASCII))
            .joinToString("") { "%02x".format(it) }
    }

    /** Opens an outer server-authenticated TLS connection. Never log request or exception details. */
    fun connect(rawEndpoint: String, invitationKey: String, host: Boolean, token: String = "", onOpen: (Socket) -> Unit = {}): Socket {
        val uri = endpoint(rawEndpoint)
        if (uri.scheme.equals("wss", ignoreCase = true)) {
            val socket = TogetherWebSocket()
            try {
                onOpen(socket)
                socket.open(uri.toASCIIString())
                val hello = JSONObject().put("version", 1).put("role", if (host) "host" else "guest")
                    .put("route", route(invitationKey))
                socket.outputStream.write((hello.toString() + "\n").toByteArray(Charsets.UTF_8))
                if (host && readStatus(socket) != "waiting") throw java.io.IOException("一起听服务器繁忙")
                val status = readStatus(socket)
                if (host && status == "expired") throw WaitingExpired()
                if (status != "paired") throw java.io.IOException("房间暂不可用，请刷新邀请后重试")
                socket.soTimeout = 15_000
                return socket
            } catch (failure: Exception) { socket.close(); throw failure }
        }
        if (host) require(token.matches(capability))
        val socket = SSLSocketFactory.getDefault().createSocket() as SSLSocket
        try {
            onOpen(socket)
            socket.connect(InetSocketAddress(uri.host, if (uri.port == -1) 443 else uri.port), 8_000)
            socket.soTimeout = 100_000
            socket.tcpNoDelay = true
            socket.enabledProtocols = socket.supportedProtocols.filter { it == "TLSv1.3" || it == "TLSv1.2" }.toTypedArray()
            val params = socket.sslParameters
            params.endpointIdentificationAlgorithm = "HTTPS"
            if (!uri.host.contains(':') && !uri.host.matches(Regex("[0-9.]+")))
                params.serverNames = listOf(javax.net.ssl.SNIHostName(uri.host))
            socket.sslParameters = params
            socket.startHandshake()
            val hello = JSONObject().put("version", 1).put("role", if (host) "host" else "guest")
                .put("route", route(invitationKey)).also { if (host) it.put("token", token) }
            val bytes = (hello.toString() + "\n").toByteArray(Charsets.UTF_8)
            require(bytes.size <= 1024)
            socket.outputStream.write(bytes)
            socket.outputStream.flush()
            if (host && readStatus(socket) != "waiting") throw java.io.IOException("中继暂时不可用")
            if (readStatus(socket) != "paired") throw java.io.IOException("中继连接未能建立")
            socket.soTimeout = 15_000
            return socket
        } catch (failure: Exception) {
            runCatching { socket.close() }
            throw failure
        }
    }

    /** Wrap only after both peers are paired; the outer server must never see room plaintext. */
    fun roomSocket(tunnel: Socket, room: TogetherTls.Identity?, peerPin: String?, authenticateClient: Boolean): SSLSocket {
        val context = if (peerPin == null) requireNotNull(room).context else TogetherTls.client(peerPin, room)
        val inner = context.socketFactory.createSocket(tunnel, "resonance-room", 443, true) as SSLSocket
        try {
            inner.useClientMode = peerPin != null
            if (peerPin == null) inner.wantClientAuth = authenticateClient
            TogetherTls.configure(inner)
            inner.startHandshake()
            return inner
        } catch (failure: Exception) {
            runCatching { inner.close() }
            throw failure
        }
    }

    private fun readStatus(socket: Socket): String {
        val buffer = java.io.ByteArrayOutputStream()
        while (true) {
            val value = socket.inputStream.read()
            if (value < 0) throw EOFException("中继连接已关闭")
            if (value == 10) break
            require(buffer.size() < 256) { "中继响应过长" }
            buffer.write(value)
        }
        val json = JSONObject(buffer.toString(Charsets.UTF_8.name()))
        return json.getString("status")
    }
}
