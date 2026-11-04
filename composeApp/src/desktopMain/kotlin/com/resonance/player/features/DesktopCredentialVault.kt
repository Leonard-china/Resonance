package com.resonance.player.features

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Windows keeps DPAPI; Linux seals secrets with an owner-only, per-data-directory key. */
internal class DesktopCredentialVault(private val directory: Path) {
    private val windows = System.getProperty("os.name").startsWith("Windows", true)
    private val linux by lazy { LinuxCredentialVault(directory.resolve("credentials")) }

    fun encrypt(value: String): String = when {
        value.isBlank() -> ""
        windows -> "dpapi:" + Base64.getEncoder().encodeToString(
            com.sun.jna.platform.win32.Crypt32Util.cryptProtectData(value.toByteArray(Charsets.UTF_8)),
        )
        else -> linux.encrypt(value)
    }

    fun decrypt(value: String): String = when {
        value.startsWith("dpapi:") -> {
            check(windows) { "此凭据受 Windows 用户保护，请在 Linux 重新填写" }
            String(com.sun.jna.platform.win32.Crypt32Util.cryptUnprotectData(
                Base64.getDecoder().decode(value.substringAfter(':')),
            ), Charsets.UTF_8)
        }
        value.startsWith("linux-aesgcm:") -> {
            check(!windows) { "此凭据受 Linux 本机密钥保护，请重新填写" }
            linux.decrypt(value)
        }
        else -> value // Existing plaintext settings remain readable until explicitly saved again.
    }
}

internal class LinuxCredentialVault(private val directory: Path) {
    fun encrypt(value: String): String {
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(create = true), GCMParameterSpec(128, iv))
        cipher.updateAAD(prefix.toByteArray(Charsets.UTF_8))
        return prefix + Base64.getEncoder().encodeToString(iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)))
    }

    fun decrypt(value: String): String {
        require(value.startsWith(prefix)) { "不支持的 Linux 凭据版本" }
        val bytes = Base64.getDecoder().decode(value.removePrefix(prefix))
        require(bytes.size >= 28) { "Linux 凭据已损坏" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        cipher.updateAAD(prefix.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8)
    }

    private fun key(create: Boolean): SecretKeySpec = synchronized(keyLock) {
        check(!Files.isSymbolicLink(directory)) { "凭据目录不能是符号链接" }
        if (create) Files.createDirectories(directory, PosixFilePermissions.asFileAttribute(directoryPermissions))
        check(Files.isDirectory(directory, NOFOLLOW_LINKS)) { "缺少本机凭据密钥，请重新填写配置" }
        Files.setPosixFilePermissions(directory, directoryPermissions)
        val keyFile = directory.resolve("key.bin")
        // A separate lock serializes key creation across processes; the key itself is atomically installed.
        FileChannel.open(directory.resolve("key.lock"), setOf(CREATE, WRITE, NOFOLLOW_LINKS),
            PosixFilePermissions.asFileAttribute(filePermissions)).use { channel ->
            channel.lock().use {
                check(!Files.isSymbolicLink(keyFile)) { "凭据密钥不能是符号链接" }
                if (!Files.exists(keyFile, NOFOLLOW_LINKS)) {
                    check(create) { "缺少本机凭据密钥，请重新填写配置" }
                    val temporary = Files.createTempFile(directory, ".key-", ".tmp",
                        PosixFilePermissions.asFileAttribute(filePermissions))
                    try {
                        FileChannel.open(temporary, WRITE).use { output ->
                            val bytes = java.nio.ByteBuffer.wrap(ByteArray(32).also(random::nextBytes))
                            while (bytes.hasRemaining()) output.write(bytes)
                            output.force(true)
                        }
                        Files.move(temporary, keyFile, ATOMIC_MOVE)
                    } finally { Files.deleteIfExists(temporary) }
                }
                Files.setPosixFilePermissions(keyFile, filePermissions)
                val bytes = Files.readAllBytes(keyFile)
                check(bytes.size == 32) { "本机凭据密钥已损坏" }
                SecretKeySpec(bytes, "AES")
            }
        }
    }

    private companion object {
        const val prefix = "linux-aesgcm:v1:"
        val random = SecureRandom()
        val keyLock = Any()
        val directoryPermissions = PosixFilePermissions.fromString("rwx------")
        val filePermissions = PosixFilePermissions.fromString("rw-------")
    }
}
