package com.resonance.player.together

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.X509EncodedKeySpec

/**
 * An identity-bound control/chat envelope for a future group transport. Signatures authenticate
 * the sender, but do not encrypt payloads or establish membership. Callers must independently
 * validate room consent, membership, sequence and end-to-end encryption before accepting data.
 */
internal object TogetherGroupEnvelope {
    enum class Kind(val wireTag: Int) { Join(1), Chat(2), Control(3) }

    class Signed private constructor(
        val roomId: String,
        val epoch: Long,
        val senderId: String,
        val sequence: Long,
        val kind: Kind,
        payload: ByteArray,
        publicKey: ByteArray,
        signature: ByteArray,
    ) {
        private val payloadBytes = payload.copyOf()
        private val publicKeyBytes = publicKey.copyOf()
        private val signatureBytes = signature.copyOf()
        fun payload(): ByteArray = payloadBytes.copyOf()
        fun publicKey(): ByteArray = publicKeyBytes.copyOf()
        fun signature(): ByteArray = signatureBytes.copyOf()

        companion object {
            internal fun create(roomId: String, epoch: Long, senderId: String, sequence: Long, kind: Kind,
                                payload: ByteArray, publicKey: ByteArray, signature: ByteArray) =
                Signed(roomId, epoch, senderId, sequence, kind, payload, publicKey, signature)
        }
    }

    fun sign(identity: KeyPair, roomId: String, epoch: Long, sequence: Long, kind: Kind, payload: ByteArray): Signed {
        val publicKey = identity.public
        require(isP256(publicKey))
        require(payload.size <= MAX_PAYLOAD_BYTES)
        val body = payload.copyOf()
        val encoded = publicKey.encoded.copyOf()
        require(encoded.size in 1..MAX_KEY_BYTES)
        val senderId = fingerprint(encoded)
        val message = canonical(roomId, epoch, senderId, sequence, kind, body)
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(identity.private); update(message); sign()
        }
        require(signature.size in 1..MAX_SIGNATURE_BYTES)
        return Signed.create(roomId, epoch, senderId, sequence, kind, body, encoded, signature)
    }

    /** Produces a defensive copy only after verifying the actual P-256 public key and signature. */
    fun verify(roomId: String, epoch: Long, senderId: String, sequence: Long, kind: Kind,
               payload: ByteArray, encodedKey: ByteArray, signature: ByteArray): Signed? = runCatching {
        require(ROOM_ID.matches(roomId) && DEVICE_ID.matches(senderId) && epoch > 0 && sequence > 0)
        require(payload.size <= MAX_PAYLOAD_BYTES && encodedKey.size in 1..MAX_KEY_BYTES && signature.size in 1..MAX_SIGNATURE_BYTES)
        val body = payload.copyOf()
        val keyBytes = encodedKey.copyOf()
        val proof = signature.copyOf()
        val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))
        require(isP256(publicKey) && publicKey.encoded.contentEquals(keyBytes))
        require(MessageDigest.isEqual(fingerprint(keyBytes).toByteArray(Charsets.US_ASCII), senderId.toByteArray(Charsets.US_ASCII)))
        val message = canonical(roomId, epoch, senderId, sequence, kind, body)
        val valid = Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey); update(message); verify(proof)
        }
        require(valid)
        Signed.create(roomId, epoch, senderId, sequence, kind, body, keyBytes, proof)
    }.getOrNull()

    private fun canonical(roomId: String, epoch: Long, senderId: String, sequence: Long, kind: Kind, payload: ByteArray): ByteArray {
        require(ROOM_ID.matches(roomId) && DEVICE_ID.matches(senderId) && epoch > 0 && sequence > 0)
        require(payload.size <= MAX_PAYLOAD_BYTES)
        return ByteArrayOutputStream().also { output ->
            DataOutputStream(output).use { data ->
                data.writeUTF("resonance-group-envelope-v1")
                data.writeUTF(roomId)
                data.writeLong(epoch)
                data.writeUTF(senderId)
                data.writeLong(sequence)
                data.writeInt(kind.wireTag)
                data.writeInt(payload.size)
                data.write(payload)
            }
        }.toByteArray()
    }

    private fun fingerprint(encoded: ByteArray) = MessageDigest.getInstance("SHA-256").digest(encoded)
        .joinToString("") { "%02x".format(it) }

    private fun isP256(key: PublicKey): Boolean {
        val actual = (key as? ECPublicKey)?.params ?: return false
        val expected = AlgorithmParameters.getInstance("EC").run {
            init(ECGenParameterSpec("secp256r1")); getParameterSpec(ECParameterSpec::class.java)
        }
        return actual.curve == expected.curve && actual.generator == expected.generator &&
            actual.order == expected.order && actual.cofactor == expected.cofactor
    }

    private val ROOM_ID = Regex("[a-f0-9]{32}")
    private val DEVICE_ID = Regex("[a-f0-9]{64}")
    private const val MAX_KEY_BYTES = 256
    private const val MAX_SIGNATURE_BYTES = 80
    private const val MAX_PAYLOAD_BYTES = 8192
}
