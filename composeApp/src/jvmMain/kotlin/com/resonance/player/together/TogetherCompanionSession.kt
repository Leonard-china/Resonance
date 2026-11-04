package com.resonance.player.together

import com.resonance.player.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.UUID

/** Chat uses the authenticated room wire, which may traverse the user's optional opaque relay. */
internal class TogetherCompanionSession(private val store: TogetherCompanionStore, private val scope: CoroutineScope,
    private val contact: suspend () -> JSONObject = { JSONObject() }) {
    private val mutable = MutableStateFlow(TogetherCompanionState())
    val state = mutable.asStateFlow()
    private val incomingEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val chatMessageEvents = incomingEvents.asSharedFlow()
    private val mutex = Mutex()
    private var channel: Any? = null
    private var send: (suspend (JSONObject) -> Unit)? = null
    private var channelCurrent: () -> Boolean = { false }
    private var outgoingPair = ""
    private var incomingPair = ""
    private var incomingContact = JSONObject()
    private data class Packet(val key: Any, val json: JSONObject)
    private val packets = Channel<Packet>(256)
    private val queued = mutableSetOf<Long>()
    init {
        act { store.identity(); refresh() }
        scope.launch {
            for (packet in packets) {
                val sender = mutex.withLock {
                    if (packet.key !== channel || !channelCurrent()) null else if (packet.json.optString("type") == "chat" &&
                        store.messages(chatTarget(state.value)).none { it.outgoing && !it.delivered && it.sequence == packet.json.getLong("sequence") }) null else send
                }
                try { sender?.invoke(packet.json) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutex.withLock { if (packet.key === channel) {
                    mutable.value = mutable.value.copy(message = "消息待发送，连接恢复后会继续")
                } } }
            }
        }
    }
    private fun emit(json: JSONObject) {
        val key = channel ?: return
        if (!channelCurrent()) return
        check(packets.trySend(Packet(key, json)).isSuccess) { "Companion send queue full" }
    }
    private fun chatTarget(s: TogetherCompanionState): String =
        if (s.members.size > 2) s.conversationId.ifEmpty { s.peerId } else s.peerId.ifEmpty { s.conversationId }

    private fun refresh(message: String = "") {
        val old = mutable.value
        val partners = store.partners()
        val relay = store.relaySettings()
        val activeChatId = chatTarget(old)
        mutable.value = old.copy(available = true, partners = partners, rendezvousUrl = store.rendezvousUrl(),
            relayUrl = relay.first, relayConfigured = relay.first.isNotEmpty() && (relay.first.startsWith("wss://", ignoreCase = true) || relay.second.isNotEmpty()),
            paired = partners.any { it.id == old.peerId }, messages = if (activeChatId.isEmpty()) emptyList() else store.messages(activeChatId),
            pairingIncoming = incomingPair.isNotEmpty(), pairingRequested = outgoingPair.isNotEmpty(), message = message,
            conversationId = old.conversationId, members = old.members)
    }
    private fun act(block: suspend () -> Unit) { scope.launch {
        try { mutex.withLock { block() } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutable.value = mutable.value.copy(message = "操作未完成，本机数据已保留；请检查连接或存储空间后重试") }
    } }
    suspend fun attach(
        id: String,
        key: Any,
        sender: suspend (JSONObject) -> Unit,
        conversationId: String = id,
        name: String = "",
        members: List<TogetherMember> = emptyList(),
        flushPending: Boolean = true,
        isCurrent: () -> Boolean = { true },
    ) = mutex.withLock {
        require(id.matches(Regex("[a-f0-9]{32,64}")))
        require(conversationId.matches(Regex("[a-f0-9]{32,64}")))
        if (!isCurrent()) return@withLock
        channel = key; send = sender; incomingPair = ""; outgoingPair = ""
        channelCurrent = isCurrent
        queued.clear()
        val displayName = if (name.isNotBlank()) name else (store.partners().firstOrNull { it.id == id }?.name ?: "朋友 ${id.take(4)}")
        mutable.value = mutable.value.copy(
            connected = true,
            peerId = id,
            peerName = displayName,
            conversationId = conversationId,
            members = members,
            unreadCount = if (state.value.conversationId == conversationId) state.value.unreadCount else 0,
        )
        refresh()
        if (flushPending) flush()
        if (state.value.paired) emit(JSONObject().put("type", "partnerContact").put("contact", contact()))
    }
    fun updateMembers(members: List<TogetherMember>, expectedConversation: String = "") = act {
        if (expectedConversation.isNotEmpty() && mutable.value.conversationId != expectedConversation) return@act
        mutable.value = mutable.value.copy(members = members)
    }
    suspend fun detach(key: Any? = null) = withContext(NonCancellable) { mutex.withLock {
        if (key == null || key === channel) {
            channel = null; send = null; incomingPair = ""; outgoingPair = ""
            channelCurrent = { false }
            queued.clear()
            mutable.value = mutable.value.copy(connected = false, pairingIncoming = false, pairingRequested = false, members = emptyList())
        }
    } }
    private suspend fun flush() {
        if (send == null || !channelCurrent()) return
        val currentId = chatTarget(state.value)
        val convId = state.value.conversationId.ifEmpty { state.value.peerId }
        for (message in store.messages(currentId).filter { it.outgoing && !it.delivered }) {
            if (message.sequence !in queued) {
                emit(JSONObject().put("type", "chat").put("sequence", message.sequence).put("text", message.text)
                    .put("senderId", message.senderId).put("senderName", message.senderName).put("conversationId", convId))
                queued.add(message.sequence)
            }
        }
    }
    suspend fun sendText(text: String, expectedPeer: String, localId: String = "", localName: String = ""): Boolean = mutex.withLock {
        try {
            val id = state.value.peerId
            val convId = state.value.conversationId.ifEmpty { id }
            val targetId = chatTarget(state.value)
            require(id.isNotEmpty() && (id == expectedPeer || convId == expectedPeer || targetId == expectedPeer))
            store.enqueue(targetId, text.trim(), senderId = localId, senderName = localName, conversationId = convId)
            refresh()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            mutable.value = mutable.value.copy(message = "消息未能保存，请检查长度、待发数量或存储空间后重试")
            return@withLock false
        }
        runCatching { flush() }
        true
    }
    fun clear(id: String) = act {
        store.clear(id)
        if (id == state.value.peerId && state.value.conversationId.isNotEmpty()) store.clear(state.value.conversationId)
        if (id == state.value.conversationId && state.value.peerId.isNotEmpty()) store.clear(state.value.peerId)
        if (id == state.value.peerId || id == state.value.conversationId) {
            queued.clear(); mutable.value = mutable.value.copy(unreadCount = 0)
        }
        refresh("已清空本机记录，朋友设备上的记录不受影响")
    }
    fun pair(expectedPeer: String) = act {
        require(state.value.members.size <= 2 && state.value.peerId == expectedPeer && state.value.connected && channelCurrent() && !state.value.paired && outgoingPair.isEmpty())
        outgoingPair = UUID.randomUUID().toString()
        emit(JSONObject().put("type", "pairRequest").put("id", outgoingPair).put("contact", contact()))
        refresh()
    }
    fun respond(accept: Boolean, expectedPeer: String) = act {
        require(state.value.peerId == expectedPeer && incomingPair.isNotEmpty() && state.value.connected && channelCurrent())
        if (accept) savePartner(incomingContact)
        emit(JSONObject().put("type", "pairResult").put("id", incomingPair).put("accept", accept).put("contact", if (accept) contact() else JSONObject()))
        incomingPair = ""
        refresh(if (accept) "已记住这位常听伙伴" else "已拒绝本次配对")
    }
    fun remove(id: String) = act { store.removePartner(id); refresh("已从本机移除伙伴") }
    fun notice(message: String) = act { refresh(message) }
    private fun recordIncoming(conversationId: String) {
        val target = chatTarget(state.value)
        if (conversationId != target && conversationId != state.value.conversationId && conversationId != state.value.peerId) return
        mutable.value = mutable.value.copy(unreadCount = (state.value.unreadCount + 1).coerceAtMost(999))
        incomingEvents.tryEmit(Unit)
    }
    fun notifyIncomingMessage(conversationId: String = chatTarget(state.value)) = act { recordIncoming(conversationId) }
    fun markRead(conversationId: String) = act {
        val target = chatTarget(state.value)
        if (conversationId == target || conversationId == state.value.conversationId || conversationId == state.value.peerId) mutable.value = mutable.value.copy(unreadCount = 0)
    }
    fun rendezvousStatus(message: String) = act { mutable.value = mutable.value.copy(rendezvousStatus = message) }
    fun incomingInvitation(partner: TogetherPartner?) = act {
        mutable.value = mutable.value.copy(incomingPartnerId = partner?.id.orEmpty(), incomingPartnerName = partner?.name.orEmpty())
    }
    private fun savePartner(json: JSONObject) {
        val rows = json.optJSONArray("addresses") ?: org.json.JSONArray()
        require(rows.length() <= TogetherInvitation.MAX_HOSTS)
        val hosts = (0 until rows.length()).map { TogetherInvitation.checkedHost(rows.getString(it)) }
        store.pair(state.value.peerId, state.value.peerName, hosts, json.optInt("port", 0))
    }
    suspend fun receive(key: Any, json: JSONObject): Boolean {
        val type = json.optString("type")
        if (type !in setOf("chat", "chatAck", "chatAccepted", "pairRequest", "pairResult", "partnerContact", "chatBroadcast")) return false
        mutex.withLock {
            require(key === channel && state.value.connected && channelCurrent())
            val id = state.value.peerId
            when (type) {
                "chat" -> {
                    val sequence = sequence(json)
                    val senderId = json.optString("senderId", id)
                    val senderName = json.optString("senderName", state.value.peerName)
                    val convId = json.optString("conversationId", id)
                    require(convId.isEmpty() || convId == state.value.conversationId.ifEmpty { id })
                    val targetId = chatTarget(state.value)
                    val isNew = store.receive(targetId, sequence, json.getString("text"), senderId, senderName, convId)
                    refresh()
                    if (isNew) recordIncoming(targetId)
                    // ACK follows the durable encrypted write; replays are ACKed without adding rows.
                    emit(JSONObject().put("type", "chatAck").put("sequence", sequence))
                }
                "chatBroadcast" -> {
                    val sequence = sequence(json)
                    val senderId = json.optString("senderId", id)
                    val senderName = json.optString("senderName", "朋友")
                    val convId = json.optString("conversationId", id)
                    require(convId.isEmpty() || convId == state.value.conversationId.ifEmpty { id })
                    val targetId = chatTarget(state.value)
                    val isNew = store.receiveGroup(targetId, sequence, json.getString("text"), senderId, senderName)
                    refresh()
                    if (isNew) recordIncoming(targetId)
                    if (json.optInt("receiptVersion", 0) == 1) emit(JSONObject().put("type", "chatReceipt")
                        .put("conversationId", convId).put("senderId", senderId).put("sequence", sequence))
                }
                "chatAck" -> {
                    val sequence = sequence(json)
                    val targetId = chatTarget(state.value)
                    store.acknowledge(targetId, sequence)
                    queued.remove(sequence)
                    refresh()
                }
                "pairRequest" -> {
                    val request = json.getString("id").also { require(it.matches(Regex("[a-f0-9-]{36}"))) }
                    require(incomingPair.isEmpty() || incomingPair == request)
                    incomingPair = request
                    incomingContact = json.optJSONObject("contact") ?: JSONObject()
                    refresh()
                }
                "chatAccepted" -> {
                    val sequence = sequence(json)
                    require(json.getString("conversationId") == state.value.conversationId)
                    val recipients = json.getJSONArray("pendingRecipients")
                    require(recipients.length() <= 6)
                    val targetId = chatTarget(state.value)
                    store.acceptGroupRecipients(targetId, json.getString("senderId"), sequence,
                        (0 until recipients.length()).map { recipients.getString(it) }, json.getInt("recipientCount"))
                    queued.remove(sequence)
                    refresh()
                }
                "pairResult" -> {
                    require(outgoingPair.isNotEmpty() && json.getString("id") == outgoingPair)
                    val accept = json.getBoolean("accept")
                    if (accept) savePartner(json.optJSONObject("contact") ?: JSONObject())
                    outgoingPair = ""
                    refresh(if (accept) "已记住这位常听伙伴" else "朋友暂未接受配对")
                }
                "partnerContact" -> {
                    // Removing a partner revokes endpoint updates too; never recreate the relationship.
                    if (store.partners().any { it.id == id }) { savePartner(json.getJSONObject("contact")); refresh() }
                }
            }
        }
        return true
    }
    private fun sequence(json: JSONObject): Long {
        val value = json.get("sequence")
        require(value is Number)
        return value.toString().toLong().also { require(it > 0) }
    }
}
