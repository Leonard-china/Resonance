package com.resonance.player.together

import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Bounded binary stream for the existing pinned room TLS. WSS is only the outer transport. */
internal class TogetherWebSocket : Socket() {
    private val incoming = ArrayBlockingQueue<ByteArray>(8)
    private val opened = CountDownLatch(1)
    @Volatile private var ended = false
    @Volatile private var failure: IOException? = null
    @Volatile private var timeout = 100_000
    @Volatile private var webSocket: WebSocket? = null
    private var current = byteArrayOf()
    private var position = 0

    fun open(url: String) {
        check(!ended)
        val ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { opened.countDown() }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (bytes.size > 65_536) { fail("中继返回了过大的数据帧"); return }
                try {
                    if (!incoming.offer(bytes.toByteArray(), 15, TimeUnit.SECONDS)) fail("中继接收超时")
                } catch (_: InterruptedException) { Thread.currentThread().interrupt(); fail("中继接收已中断") }
            }
            override fun onMessage(webSocket: WebSocket, text: String) { fail("中继返回了无效数据") }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { fail("无法连接一起听服务器") }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, null) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                ended = true; opened.countDown(); incoming.offer(byteArrayOf())
            }
        })
        webSocket = ws
        if (ended) ws.cancel()
        if (!opened.await(10, TimeUnit.SECONDS)) { close(); throw SocketTimeoutException("连接一起听服务器超时") }
        failure?.let { throw it }
        check(!ended) { "中继连接已关闭" }
    }

    private fun fail(message: String) {
        failure = IOException(message)
        ended = true
        opened.countDown()
        incoming.offer(byteArrayOf())
        webSocket?.cancel()
    }

    private val input = object : InputStream() {
        override fun read(): Int {
            if (!next()) return -1
            return current[position++].toInt() and 255
        }
        private fun next(): Boolean {
            while (position >= current.size) {
                if (ended && incoming.isEmpty()) { failure?.let { throw it }; return false }
                current = if (timeout == 0) incoming.take() else incoming.poll(timeout.toLong(), TimeUnit.MILLISECONDS)
                    ?: throw SocketTimeoutException("等待中继数据超时")
                position = 0
            }
            return true
        }
        override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
            require(offset >= 0 && length >= 0 && offset <= bytes.size - length)
            if (length == 0) return 0
            if (!next()) return -1
            val count = minOf(length, current.size - position)
            current.copyInto(bytes, offset, position, position + count)
            position += count
            return count
        }
    }
    private val output = object : OutputStream() {
        override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)
        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            require(offset >= 0 && length >= 0 && offset <= bytes.size - length)
            val ws = webSocket ?: throw IOException("中继尚未连接")
            var cursor = offset
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
            while (cursor < offset + length) {
                while (!ended && ws.queueSize() > 262_144) {
                    if (System.nanoTime() >= deadline) { fail("中继发送超时"); break }
                    Thread.sleep(5)
                }
                if (ended) throw failure ?: IOException("中继连接已关闭")
                val count = minOf(16_384, offset + length - cursor)
                if (!ws.send(bytes.toByteString(cursor, count))) throw IOException("中继发送失败")
                cursor += count
            }
        }
    }

    override fun getInputStream(): InputStream = input
    override fun getOutputStream(): OutputStream = output
    override fun isConnected(): Boolean = webSocket != null && !ended
    override fun isClosed(): Boolean = ended
    override fun getSoTimeout(): Int = timeout
    override fun setSoTimeout(value: Int) { require(value >= 0); timeout = value }
    override fun setTcpNoDelay(on: Boolean) = Unit
    override fun close() { ended = true; opened.countDown(); incoming.clear(); incoming.offer(byteArrayOf()); webSocket?.cancel() }

    companion object {
        private val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    }
}
