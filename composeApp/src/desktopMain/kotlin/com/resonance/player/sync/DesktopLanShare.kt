package com.resonance.player.sync

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.DatagramSocket
import java.net.NetworkInterface
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DesktopLanShare(private val packagePath: Path, private val passphrase: String) : AutoCloseable {
    private val token = randomToken()
    private val served = AtomicBoolean(false)
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "resonance-lan-expiry").apply { isDaemon = true }
    }
    private val server = HttpServer.create(InetSocketAddress("0.0.0.0", 0), 0).apply {
        executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "resonance-lan-share").apply { isDaemon = true }
        }
        createContext("/sync/$token", ::serve)
        start()
    }
    private var qrPath: Path? = null
    val link: String = "resonance://sync?url=${encode("http://${lanAddress().hostAddress}:${server.address.port}/sync/$token")}&key=${encode(passphrase)}"

    init {
        scheduler.schedule({ close() }, EXPIRY_MINUTES, TimeUnit.MINUTES)
    }

    fun writeQr(target: Path): Path {
        target.parent?.let(Files::createDirectories)
        MatrixToImageWriter.writeToPath(QRCodeWriter().encode(link, BarcodeFormat.QR_CODE, 512, 512), "PNG", target)
        qrPath = target
        return target
    }

    private fun serve(exchange: HttpExchange) {
        try {
            val remote = exchange.remoteAddress.address
            if (!isPrivateAddress(remote) || !served.compareAndSet(false, true) || !Files.isRegularFile(packagePath)) {
                exchange.sendResponseHeaders(403, -1)
                return
            }
            exchange.responseHeaders.add("Content-Type", "application/octet-stream")
            exchange.responseHeaders.add("Cache-Control", "no-store")
            exchange.responseHeaders.add("X-Content-Type-Options", "nosniff")
            exchange.sendResponseHeaders(200, Files.size(packagePath))
            exchange.responseBody.use { output -> Files.newInputStream(packagePath).use { it.copyTo(output) } }
        } finally {
            exchange.close()
            if (served.get()) scheduler.schedule({ close() }, 1, TimeUnit.SECONDS)
        }
    }

    override fun close() {
        runCatching { server.stop(0) }
        scheduler.shutdownNow()
        Files.deleteIfExists(packagePath)
        qrPath?.let(Files::deleteIfExists)
        qrPath = null
    }

    private fun lanAddress(): Inet4Address {
        // Prefer the address selected by the active default route. Adapter
        // enumeration order is unreliable with VPN, Hyper-V and emulators.
        val routed = runCatching {
            DatagramSocket().use { socket ->
                socket.connect(InetSocketAddress("1.1.1.1", 53))
                socket.localAddress as? Inet4Address
            }
        }.getOrNull()?.takeIf(::isPrivateAddress)
        if (routed != null) return routed

        return NetworkInterface.getNetworkInterfaces().toList()
            .asSequence()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual && it.supportsMulticast() }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .filter(::isPrivateAddress)
            .sortedByDescending(::addressScore)
            .firstOrNull()
            ?: error("未找到可用的局域网 IPv4 地址")
    }

    private fun addressScore(address: Inet4Address): Int {
        val bytes = address.address.map { it.toInt() and 0xff }
        return when {
            bytes[0] == 192 && bytes[1] == 168 -> 3
            bytes[0] == 10 -> 2
            bytes[0] == 172 && bytes[1] in 16..31 -> 1
            else -> 0
        }
    }

    private fun isPrivateAddress(address: InetAddress): Boolean {
        if (address.isLoopbackAddress || address.isSiteLocalAddress) return true
        val bytes = address.address
        if (bytes.size != 4) return false
        val first = bytes[0].toInt() and 0xff
        val second = bytes[1].toInt() and 0xff
        return first == 10 || (first == 172 && second in 16..31) || (first == 192 && second == 168)
    }

    private fun encode(value: String) = java.net.URLEncoder.encode(value, Charsets.UTF_8)
    private fun randomToken(): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(ByteArray(24).also(SecureRandom()::nextBytes))

    private companion object {
        const val EXPIRY_MINUTES = 10L
    }
}
