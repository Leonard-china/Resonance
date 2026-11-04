package com.resonance.player.together

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lightweight pure-JVM RFC 5389 / RFC 8489 STUN binding client.
 * Discovers server-reflexive public IP candidates using free, public, permanent STUN services
 * (Cloudflare, Syncthing, Google) without any external libraries or recurring costs.
 */
internal object TogetherStunClient {
    private val DEFAULT_SERVERS = listOf(
        "stun.cloudflare.com" to 3478,
        "stun.syncthing.net" to 3478,
        "stun.l.google.com" to 19302,
        "stun1.l.google.com" to 19302,
    )

    private const val MAGIC_COOKIE = 0x2112A442

    suspend fun resolvePublicCandidate(timeoutMs: Int = 1800): String? = withContext(Dispatchers.IO) {
        for ((host, port) in DEFAULT_SERVERS) {
            try {
                val candidate = queryStun(host, port, timeoutMs)
                if (!candidate.isNullOrBlank()) return@withContext candidate
            } catch (_: Exception) {
                // Try next STUN server
            }
        }
        null
    }

    private fun queryStun(host: String, port: Int, timeoutMs: Int): String? {
        val socket = DatagramSocket().apply { soTimeout = timeoutMs }
        return socket.use { s ->
            val random = SecureRandom()
            val transactionId = ByteArray(12).also(random::nextBytes)
            val request = ByteArray(20).apply {
                this[0] = 0x00 // Binding Request (0x0001)
                this[1] = 0x01
                this[2] = 0x00 // Length: 0
                this[3] = 0x00
                this[4] = 0x21 // Magic Cookie 0x2112A442
                this[5] = 0x12
                this[6] = 0xA4.toByte()
                this[7] = 0x42
                System.arraycopy(transactionId, 0, this, 8, 12)
            }
            val address = InetAddress.getByName(host)
            s.send(DatagramPacket(request, request.size, address, port))

            val buffer = ByteArray(512)
            val responsePacket = DatagramPacket(buffer, buffer.size)
            s.receive(responsePacket)
            parseStunResponse(buffer, responsePacket.length, transactionId)
        }
    }

    private fun parseStunResponse(data: ByteArray, length: Int, expectedTransactionId: ByteArray): String? {
        if (length < 20) return null
        val msgType = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        if (msgType != 0x0101) return null // Binding Success Response
        val magic = ((data[4].toInt() and 0xFF) shl 24) or ((data[5].toInt() and 0xFF) shl 16) or
                    ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
        if (magic != MAGIC_COOKIE) return null
        for (i in 0 until 12) {
            if (data[8 + i] != expectedTransactionId[i]) return null
        }

        var offset = 20
        val msgLength = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        val end = minOf(length, 20 + msgLength)

        while (offset + 4 <= end) {
            val attrType = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            val attrLen = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
            offset += 4
            if (offset + attrLen > end) break

            if (attrType == 0x0020 && attrLen >= 8) { // XOR-MAPPED-ADDRESS
                val family = data[offset + 1].toInt() and 0xFF
                if (family == 0x01 && attrLen >= 8) { // IPv4
                    val ip = ByteArray(4)
                    for (b in 0 until 4) {
                        val cookieByte = (MAGIC_COOKIE ushr (24 - b * 8)) and 0xFF
                        ip[b] = (data[offset + 4 + b].toInt() xor cookieByte).toByte()
                    }
                    val inet = InetAddress.getByAddress(ip)
                    val ipStr = inet.hostAddress
                    if (inet.isSiteLocalAddress || inet.isLoopbackAddress || inet.isLinkLocalAddress) return null
                    return ipStr
                } else if (family == 0x02 && attrLen >= 20) { // IPv6
                    val ip = ByteArray(16)
                    val xorPad = ByteArray(16).apply {
                        this[0] = 0x21; this[1] = 0x12; this[2] = 0xA4.toByte(); this[3] = 0x42
                        System.arraycopy(expectedTransactionId, 0, this, 4, 12)
                    }
                    for (b in 0 until 16) {
                        ip[b] = (data[offset + 4 + b].toInt() xor (xorPad[b].toInt() and 0xFF)).toByte()
                    }
                    val inet = InetAddress.getByAddress(ip)
                    val ipStr = inet.hostAddress?.substringBefore('%')
                    return ipStr
                }
            } else if (attrType == 0x0001 && attrLen >= 8) { // MAPPED-ADDRESS
                val family = data[offset + 1].toInt() and 0xFF
                if (family == 0x01) {
                    val ip = ByteArray(4)
                    System.arraycopy(data, offset + 4, ip, 0, 4)
                    val inet = InetAddress.getByAddress(ip)
                    if (!inet.isSiteLocalAddress && !inet.isLoopbackAddress) return inet.hostAddress
                }
            }
            val padding = (4 - (attrLen % 4)) % 4
            offset += attrLen + padding
        }
        return null
    }
}
