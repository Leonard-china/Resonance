package com.resonance.player.together

import com.resonance.player.platform.TogetherPartner
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** Small direct invitation listener; only locally paired TLS device keys can submit offers. */
internal class TogetherPartnerHub(
    private val store: TogetherCompanionStore,
    private val scope: CoroutineScope,
    private val addresses: () -> List<String>,
    private val offer: suspend (TogetherPartner, String) -> Boolean,
    private val preferredPort: Int = 45874,
) {
    private val mutex = Mutex()
    private val sockets = ConcurrentHashMap.newKeySet<SSLSocket>()
    @Volatile private var server: SSLServerSocket? = null
    @Volatile private var closed = false
    private var worker: Job? = null
    suspend fun start() = mutex.withLock {
        if (server != null) return@withLock
        check(!closed)
        val identity = TogetherTls.identity(store.identity(), lifetimeMs = 365L * 86_400_000)
        fun listener(port: Int) = (identity.context.serverSocketFactory.createServerSocket() as SSLServerSocket).apply {
            try { reuseAddress = true; TogetherTls.configure(this); needClientAuth = true; bind(InetSocketAddress(port), 8) }
            catch (error: Exception) { close(); throw error }
        }
        val socket = try { listener(preferredPort) } catch (error: java.net.BindException) { listener(0) }
        if (closed) { socket.close(); error("Partner listener closed") }
        server = socket
        worker = scope.launch(Dispatchers.IO) {
            val permits = Semaphore(4)
            while (isActive && !closed) {
                val incoming = try { socket.accept() as SSLSocket } catch (_: java.io.IOException) { break }
                if (!permits.tryAcquire()) { incoming.close(); continue }
                sockets.add(incoming)
                val started = java.util.concurrent.atomic.AtomicBoolean(false)
                launch {
                    started.set(true)
                    try {
                        TogetherTls.configure(incoming); incoming.soTimeout = 5_000
                        incoming.startHandshake()
                        val id = TogetherTls.peerId(incoming)
                        val partner = store.partners().firstOrNull { it.id == id } ?: error("Unknown partner")
                        val json = read(incoming)
                        require(json.getString("type") == "invite" && json.getInt("version") == 1)
                        val invitation = json.getString("invitation").also { TogetherInvitation.parse(it) }
                        // Re-check removal after a bounded read; a revoked partner cannot post an offer.
                        require(store.partners().any { it.id == id })
                        val accepted = offer(partner, invitation)
                        write(incoming, JSONObject().put("type", "offered").put("accepted", accepted))
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* No raw invitations, peer certificates or chat in logs. */ }
                    finally { sockets.remove(incoming); runCatching { incoming.close() }; permits.release() }
                }.invokeOnCompletion { if (!started.get()) { sockets.remove(incoming); runCatching { incoming.close() }; permits.release() } }
            }
        }
    }
    val listeningPort: Int get() = server?.localPort ?: 0
    fun contact(): JSONObject = JSONObject().put("addresses", JSONArray(addresses().take(TogetherInvitation.MAX_HOSTS)))
        .put("port", listeningPort)

    suspend fun invite(partner: TogetherPartner, invitation: String): Boolean = withContext(Dispatchers.IO) {
        require(partner.port in 1..65535 && partner.addresses.isNotEmpty())
        val context = TogetherTls.partnerClient(partner.id, TogetherTls.identity(store.identity()))
        // Each attempt is bounded. Try all known routes without simultaneous duplicate invite offers.
        for (host in partner.addresses.take(TogetherInvitation.MAX_HOSTS)) {
            currentCoroutineContext().ensureActive()
            val socket = context.socketFactory.createSocket() as SSLSocket
            sockets.add(socket)
            try {
                check(!closed)
                TogetherTls.configure(socket); socket.soTimeout = 4_000
                socket.connect(InetSocketAddress(TogetherInvitation.checkedHost(host), partner.port), 2_000)
                socket.startHandshake()
                require(store.partners().any { it.id == partner.id })
                write(socket, JSONObject().put("type", "invite").put("version", 1).put("invitation", invitation))
                val result = read(socket)
                require(result.getString("type") == "offered")
                return@withContext result.getBoolean("accepted")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Try the next authenticated endpoint. */ }
            finally { sockets.remove(socket); runCatching { socket.close() } }
        }
        false
    }
    fun close() {
        closed = true
        runCatching { server?.close() }; server = null
        worker?.cancel()
        sockets.forEach { runCatching { it.close() } }
        sockets.clear()
    }
    private fun read(socket: SSLSocket): JSONObject {
        val input = DataInputStream(socket.inputStream)
        val size = input.readInt().also { require(it in 1..8192) }
        return parseTogetherJson(ByteArray(size).also(input::readFully))
    }
    private fun write(socket: SSLSocket, json: JSONObject) {
        val bytes = json.toString().toByteArray(Charsets.UTF_8).also { require(it.size in 1..8192) }
        DataOutputStream(socket.outputStream).apply { writeInt(bytes.size); write(bytes); flush() }
    }
}
