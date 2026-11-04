package com.resonance.player.together

import com.resonance.player.io.AtomicPropertiesFile
import com.resonance.player.platform.TogetherChatMessage
import com.resonance.player.platform.TogetherPartner
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Properties
import org.json.JSONArray
import org.json.JSONObject

/** Only app-private encrypted data, never library audio. Call on the service's IO scope. */
class TogetherCompanionStore(
    private val directory: Path,
    private val seal: (String) -> String,
    private val open: (String) -> String,
    private val defaultRelay: String = "",
) {
    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()
    private fun path(id: String): Path {
        require(id.matches(Regex("[a-f0-9]{32,64}")))
        return directory.resolve("chat-$id.properties")
    }
    private fun decode(value: String): JSONObject = JSONObject(open(value)).also { require(it.getInt("format") == 1) }
    private fun read(file: Path): JSONObject {
        val props = AtomicPropertiesFile.read(file) { runCatching { decode(it.getProperty("sealed")); true }.getOrDefault(false) }
        return props.getProperty("sealed")?.let(::decode) ?: JSONObject().put("format", 1)
    }
    private fun write(file: Path, json: JSONObject) = AtomicPropertiesFile.write(file,
        Properties().apply { setProperty("sealed", seal(json.put("format", 1).toString())) }, "Resonance encrypted companion data")

    @Synchronized fun identity(): KeyPair {
        val file = directory.resolve("identity.properties")
        val json = read(file)
        if (json.has("private")) {
            val factory = KeyFactory.getInstance("EC")
            return KeyPair(factory.generatePublic(X509EncodedKeySpec(decoder.decode(json.getString("public")))),
                factory.generatePrivate(PKCS8EncodedKeySpec(decoder.decode(json.getString("private")))))
        }
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        write(file, json.put("public", encoder.encodeToString(pair.public.encoded)).put("private", encoder.encodeToString(pair.private.encoded)))
        return pair
    }

    @Synchronized fun partners(): List<TogetherPartner> {
        val rows = read(directory.resolve("partners.properties")).optJSONArray("partners") ?: return emptyList()
        require(rows.length() <= 32)
        return (0 until rows.length()).map { rows.getJSONObject(it).let { row ->
            val addresses = row.optJSONArray("addresses") ?: JSONArray()
            require(addresses.length() <= TogetherInvitation.MAX_HOSTS)
            TogetherPartner(row.getString("id").also { path(it) }, row.getString("name").take(40),
                (0 until addresses.length()).map { TogetherInvitation.checkedHost(addresses.getString(it)) }, row.optInt("port", 0).also { require(it in 0..65535) })
        } }
    }
    @Synchronized fun pair(id: String, name: String, addresses: List<String> = emptyList(), port: Int = 0) {
        path(id)
        require(addresses.size <= TogetherInvitation.MAX_HOSTS && port in 0..65535)
        val all = partners().filterNot { it.id == id } + TogetherPartner(id, name.take(40).ifBlank { "常听伙伴" }, addresses.map(TogetherInvitation::checkedHost), port)
        require(all.size <= 32) { "最多保存 32 位伙伴" }
        write(directory.resolve("partners.properties"), JSONObject().put("partners", JSONArray(all.map {
            partnerJson(it)
        })))
    }
    @Synchronized fun removePartner(id: String) {
        path(id)
        write(directory.resolve("partners.properties"), JSONObject().put("partners", JSONArray(partners().filterNot { it.id == id }.map {
            partnerJson(it)
        })))
    }
    private fun partnerJson(partner: TogetherPartner) = JSONObject().put("id", partner.id).put("name", partner.name)
        .put("addresses", JSONArray(partner.addresses)).put("port", partner.port)
    @Synchronized fun rendezvousUrl(): String = read(directory.resolve("rendezvous.properties")).optString("url", "")
    @Synchronized fun setRendezvousUrl(url: String) {
        write(directory.resolve("rendezvous.properties"), JSONObject().put("url", url))
    }
    @Synchronized fun relaySettings(): Pair<String, String> = read(directory.resolve("relay.properties")).let {
        it.optString("url", defaultRelay) to it.optString("secret", "")
    }
    @Synchronized fun setRelaySettings(url: String, secret: String) {
        write(directory.resolve("relay.properties"), JSONObject().put("url", url).put("secret", secret))
    }

    private data class History(val incoming: Long, val outgoing: Long, val messages: List<TogetherChatMessage>,
                               val incomingBySender: Map<String, Long>? = null)
    private fun pendingRecipients(row: JSONObject): List<String> {
        val values = row.optJSONArray("pendingRecipients") ?: return emptyList()
        require(values.length() <= 7)
        return (0 until values.length()).map { values.getString(it).also { id -> require(id.matches(Regex("[a-f0-9]{32,64}"))) } }
            .also { require(it.distinct().size == it.size) }
    }
    private fun senderFloors(json: JSONObject): Map<String, Long>? {
        val values = json.optJSONObject("incomingBySender") ?: return null
        require(values.length() <= 128)
        val result = mutableMapOf<String, Long>()
        val keys = values.keys()
        while (keys.hasNext()) {
            val sender = keys.next()
            require(sender.matches(Regex("[a-f0-9]{32,64}")))
            result[sender] = values.getLong(sender).also { require(it >= 0) }
        }
        return result
    }
    private fun history(id: String): History {
        val file = path(id)
        val clear = file.resolveSibling("${file.fileName}.clear")
        // A clear marker has no rollback backup. Corruption fails closed instead of resurrecting history.
        val marker = if (Files.exists(clear)) decode(Files.newBufferedReader(clear).use { it.readText() }) else JSONObject()
        val inFloor = marker.optLong("incoming", 0)
        val outFloor = marker.optLong("outgoing", 0)
        val json = read(file)
        val clearedSenders = senderFloors(marker)
        val storedSenders = senderFloors(json)
        val mergedSenders = if (clearedSenders != null || storedSenders != null) {
            (clearedSenders.orEmpty().keys + storedSenders.orEmpty().keys).associateWith { sender ->
                maxOf(clearedSenders?.get(sender) ?: 0L, storedSenders?.get(sender) ?: 0L)
            }.also { require(it.size <= 128) }
        } else null
        val rows = json.optJSONArray("messages") ?: JSONArray()
        require(rows.length() <= MAX_MESSAGES)
        val messages = (0 until rows.length()).map { rows.getJSONObject(it).let { row ->
            val pending = pendingRecipients(row)
            val recipientCount = row.optInt("recipientCount", 0)
            require(recipientCount in 0..7 && recipientCount >= pending.size)
            if (recipientCount > 0) require(row.getBoolean("delivered") == pending.isEmpty())
            TogetherChatMessage(
                row.getLong("sequence"),
                row.getBoolean("outgoing"),
                checkedText(row.getString("text")),
                row.getLong("time"),
                row.getBoolean("delivered"),
                row.optString("senderId", ""),
                row.optString("senderName", ""),
                row.optString("conversationId", id),
                pending,
                recipientCount,
            )
        } }.filter { row -> row.sequence > if (row.outgoing) outFloor else if (clearedSenders != null && row.senderId.isNotEmpty())
            (clearedSenders[row.senderId] ?: 0L) else inFloor }
        return History(maxOf(inFloor, json.optLong("incoming", 0)), maxOf(outFloor, json.optLong("outgoing", 0)), messages, mergedSenders)
    }
    private fun save(id: String, history: History) {
        val pending = history.messages.filter { it.pendingRecipients.isNotEmpty() || it.outgoing && !it.delivered }
        val retained = (pending + history.messages.filterNot { it.pendingRecipients.isNotEmpty() || it.outgoing && !it.delivered }
            .takeLast(MAX_MESSAGES - pending.size)).toSet()
        write(path(id), JSONObject().put("incoming", history.incoming)
        .put("outgoing", history.outgoing).put("messages", JSONArray(history.messages.filter { it in retained }.map {
            JSONObject().put("sequence", it.sequence).put("outgoing", it.outgoing).put("text", it.text)
                .put("time", it.timeMs).put("delivered", it.delivered)
                .put("senderId", it.senderId)
                .put("senderName", it.senderName)
                .put("conversationId", it.conversationId)
                .put("pendingRecipients", JSONArray(it.pendingRecipients)).put("recipientCount", it.recipientCount)
        })).also { json -> history.incomingBySender?.let { json.put("incomingBySender", JSONObject(it)) } })
    }
    @Synchronized fun messages(id: String): List<TogetherChatMessage> = history(id).messages
    @Synchronized fun enqueue(id: String, text: String, senderId: String = "", senderName: String = "", conversationId: String = id,
                              recipients: List<String> = emptyList()): TogetherChatMessage {
        require(recipients.size <= 7 && recipients.distinct().size == recipients.size && recipients.all { it.matches(Regex("[a-f0-9]{32,64}")) })
        val h = history(id)
        require(h.messages.count { it.outgoing && !it.delivered } < 100) { "待发送消息已满，请等待连接恢复" }
        val message = TogetherChatMessage(
            Math.addExact(h.outgoing, 1),
            true,
            checkedText(text),
            System.currentTimeMillis(),
            false,
            senderId,
            senderName,
            conversationId,
            recipients.toList(),
            recipients.size,
        )
        save(id, h.copy(outgoing = message.sequence, messages = h.messages + message))
        return message
    }
    @Synchronized fun receive(id: String, sequence: Long, text: String, senderId: String = "", senderName: String = "", conversationId: String = id): Boolean {
        require(sequence > 0)
        checkedText(text)
        val h = history(id)
        if (sequence <= h.incoming) return false
        save(id, h.copy(incoming = sequence, messages = h.messages + TogetherChatMessage(
            sequence,
            false,
            text,
            System.currentTimeMillis(),
            true,
            senderId,
            senderName,
            conversationId
        )))
        return true
    }
    /** A group sender has its own sequence space; never compare it with another member's counter. */
    @Synchronized fun receiveGroup(id: String, sequence: Long, text: String, senderId: String, senderName: String,
                                   recipients: List<String> = emptyList()): Boolean {
        require(senderId.matches(Regex("[a-f0-9]{32,64}")) && sequence > 0)
        require(recipients.size <= 6 && recipients.distinct().size == recipients.size && recipients.all { it != senderId && it.matches(Regex("[a-f0-9]{32,64}")) })
        checkedText(text)
        val h = history(id)
        val floors = h.incomingBySender.orEmpty()
        require(senderId in floors || floors.size < 128)
        if (sequence <= (floors[senderId] ?: 0L)) return false
        if (recipients.isNotEmpty()) require(h.messages.count { it.pendingRecipients.isNotEmpty() } < 100) { "房间转发队列已满" }
        save(id, h.copy(incomingBySender = floors + (senderId to sequence), messages = h.messages + TogetherChatMessage(
            sequence, false, text, System.currentTimeMillis(), recipients.isEmpty(), senderId, senderName.take(40), id,
            recipients.toList(), recipients.size)))
        return true
    }
    @Synchronized fun acknowledge(id: String, sequence: Long) {
        val h = history(id)
        if (h.messages.none { it.outgoing && it.sequence == sequence && !it.delivered && it.recipientCount == 0 }) return
        save(id, h.copy(messages = h.messages.map { if (it.outgoing && it.sequence == sequence && it.recipientCount == 0) it.copy(delivered = true) else it }))
    }
    @Synchronized fun acknowledgeRecipient(id: String, senderId: String, sequence: Long, recipient: String): TogetherChatMessage? {
        require(sequence > 0 && senderId.matches(Regex("[a-f0-9]{32,64}")) && recipient.matches(Regex("[a-f0-9]{32,64}")))
        val h = history(id)
        val original = h.messages.firstOrNull { it.senderId == senderId && it.sequence == sequence && recipient in it.pendingRecipients } ?: return null
        val pending = original.pendingRecipients.filterNot { it == recipient }
        val updated = original.copy(pendingRecipients = pending, delivered = pending.isEmpty())
        save(id, h.copy(messages = h.messages.map { message ->
            if (message.senderId == senderId && message.sequence == sequence && message.outgoing == original.outgoing) updated else message
        }))
        return updated
    }
    @Synchronized fun acceptGroupRecipients(id: String, senderId: String, sequence: Long, recipients: List<String>, total: Int) {
        require(sequence > 0 && total in 1..7 && recipients.size < total && recipients.distinct().size == recipients.size &&
            recipients.all { it.matches(Regex("[a-f0-9]{32,64}")) && it != senderId })
        val h = history(id)
        val original = h.messages.firstOrNull { it.outgoing && it.sequence == sequence && it.senderId == senderId } ?: return
        if (original.recipientCount > 0) require(original.recipientCount == total)
        // Status reports can race each other on the coordinator; acknowledgements only remove recipients.
        val pending = if (original.recipientCount == 0) recipients else original.pendingRecipients.filter { it in recipients }
        save(id, h.copy(messages = h.messages.map {
            if (it === original) it.copy(pendingRecipients = pending, recipientCount = total, delivered = pending.isEmpty()) else it
        }))
    }
    @Synchronized fun clear(id: String) {
        val h = history(id)
        val file = path(id)
        val marker = file.resolveSibling("${file.fileName}.clear")
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, "clear-", ".tmp")
        try {
            val clearMarker = JSONObject().put("format", 1).put("incoming", h.incoming).put("outgoing", h.outgoing)
            h.incomingBySender?.let { clearMarker.put("incomingBySender", JSONObject(it)) }
            Files.newBufferedWriter(temporary).use { it.write(seal(clearMarker.toString())) }
            try { Files.move(temporary, marker, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
            catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temporary, marker, StandardCopyOption.REPLACE_EXISTING) }
        } finally { Files.deleteIfExists(temporary) }
        save(id, h.copy(messages = emptyList()))
        Files.deleteIfExists(file.resolveSibling("${file.fileName}.bak"))
    }

    companion object {
        const val MAX_MESSAGES = 1000
        fun checkedText(text: String): String = text.also {
            require(it.isNotBlank() && it.length <= 2000 && it.toByteArray(Charsets.UTF_8).size <= 8000 && '\u0000' !in it) { "消息需为 1–2000 字，且不超过 8 KB" }
        }
    }
}
