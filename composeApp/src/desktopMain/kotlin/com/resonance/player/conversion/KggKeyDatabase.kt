package com.resonance.player.conversion

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Reads the KGG encryption key map from the KuGou PC client's local SQLite database
 * `KGMusicV3.db` (which is itself AES-128-CBC encrypted per page).
 *
 * Algorithm ported from FlyMouseFormat kgg-format.js.
 */
internal object KggKeyDatabase {

    private const val DB_PAGE_SIZE = 0x400
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)
    private val MASTER_KEY = byteArrayOf(
        0x1d, 0x61, 0x31, 0x45, 0xb2.toByte(), 0x47, 0xbf.toByte(), 0x7f,
        0x3d, 0x18, 0x96.toByte(), 0x72, 0x14, 0x4f, 0xe4.toByte(), 0xbf.toByte(),
    )

    /** Ordered list of candidate paths where KGMusicV3.db might be found. */
    fun candidateDbPath(): File? {
        val candidates = buildList {
            System.getenv("FLYINGMOUSE_KGG_DB_PATH")?.let { add(File(it)) }
            System.getenv("APPDATA")?.let { add(File(it, "KuGou8/KGMusicV3.db")) }
            System.getenv("LOCALAPPDATA")?.let { add(File(it, "KuGou8/KGMusicV3.db")) }
            add(File(System.getProperty("user.home"), "AppData/Roaming/KuGou8/KGMusicV3.db"))
        }
        return candidates.firstOrNull { it.isFile }
    }

    // ---- Page AES key/IV derivation ----
    private fun nextPageIV(seed: Long): Long {
        val left = ((seed * 0x9ef4L) and 0xFFFFFFFFL)
        val right = ((seed / 0xce26L) * 0x7fffff07L) and 0xFFFFFFFFL
        val value = (left - right) and 0xFFFFFFFFL
        return if (value and 0x80000000L == 0L) value else (value + 0x7fffff07L) and 0xFFFFFFFFL
    }

    private fun derivePageAESKey(pageNumber: Int, master: ByteArray): ByteArray {
        val buf = ByteArray(0x18)
        System.arraycopy(master, 0, buf, 0, 0x10)
        ByteBuffer.wrap(buf, 0x10, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(pageNumber)
        ByteBuffer.wrap(buf, 0x14, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(0x546c4173)
        return MessageDigest.getInstance("MD5").digest(buf)
    }

    private fun derivePageAESIV(pageNumber: Int): ByteArray {
        val iv = ByteArray(0x10)
        var seed = (pageNumber.toLong() + 1L) and 0xFFFFFFFFL
        val bb = ByteBuffer.wrap(iv).order(ByteOrder.LITTLE_ENDIAN)
        repeat(4) {
            seed = nextPageIV(seed)
            bb.putInt(seed.toInt())
        }
        return MessageDigest.getInstance("MD5").digest(iv)
    }

    private fun decryptPage(buffer: ByteArray, pageStart: Int, length: Int, pageNumber: Int) {
        val key = derivePageAESKey(pageNumber, MASTER_KEY)
        val iv = derivePageAESIV(pageNumber)
        val slice = buffer.copyOfRange(pageStart, pageStart + length)
        val decrypted = KggCipher.aesCbcDecrypt(slice, key, iv)
        System.arraycopy(decrypted, 0, buffer, pageStart, decrypted.size)
    }

    private fun validatePage1Header(header: ByteArray): Boolean {
        if (header.size < 0x18) return false
        val o10 = ByteBuffer.wrap(header, 0x10, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
        val o14 = ByteBuffer.wrap(header, 0x14, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
        val v6 = (((o10 and 0xff) shl 8) or ((o10 and 0xff00) shl 16)) and 0xFFFFFFFFL
        return o14 == 0x20204000L && (v6 - 0x200) and 0xFFFFFFFFL <= 0xfe00L && v6 and (v6 - 1) == 0L
    }

    /**
     * Decrypt the KuGou encrypted SQLite database in-place and return it as a byte array
     * ready for loading by the JDBC driver.
     */
    private fun decryptDatabase(raw: ByteArray): ByteArray {
        // If it already has the SQLite header, it's unencrypted
        if (raw.size >= SQLITE_HEADER.size && raw.copyOfRange(0, SQLITE_HEADER.size).contentEquals(SQLITE_HEADER)) {
            return raw
        }
        require(raw.isNotEmpty() && raw.size % DB_PAGE_SIZE == 0) { "KGG db: invalid database size" }

        val firstPage = raw.copyOfRange(0, DB_PAGE_SIZE)
        require(validatePage1Header(firstPage)) { "KGG db: invalid page 1 header" }

        val expectedHdr = firstPage.copyOfRange(0x10, 0x18)
        // Swap trick: page[0x10..0x18] = page[0x08..0x10]
        System.arraycopy(firstPage, 0x08, firstPage, 0x10, 8)
        decryptPage(firstPage, 0x10, DB_PAGE_SIZE - 0x10, 1)

        require(firstPage.copyOfRange(0x10, 0x18).contentEquals(expectedHdr)) {
            "KGG db: page 1 integrity check failed"
        }

        // Write SQLite magic + decrypted first page tail back into raw
        System.arraycopy(SQLITE_HEADER, 0, raw, 0, SQLITE_HEADER.size)
        System.arraycopy(firstPage, 0x10, raw, 0x10, DB_PAGE_SIZE - 0x10)

        val totalPages = raw.size / DB_PAGE_SIZE
        for (pageNo in 2..totalPages) {
            decryptPage(raw, (pageNo - 1) * DB_PAGE_SIZE, DB_PAGE_SIZE, pageNo)
        }
        return raw
    }

    /**
     * Load the audioHash → ekey map from the given database file.
     * Uses the `org.xerial:sqlite-jdbc` driver via JDBC.
     */
    fun loadKeyMap(dbFile: File): Map<String, String> {
        val raw = dbFile.readBytes()
        val decrypted = decryptDatabase(raw)

        // Write to a temp file so JDBC can open it
        val tmp = File.createTempFile("resonance-kgg-", ".db").also { it.deleteOnExit() }
        tmp.writeBytes(decrypted)

        val map = mutableMapOf<String, String>()
        try {
            // Load driver class explicitly (needed for jpackage environments)
            Class.forName("org.sqlite.JDBC")
            java.sql.DriverManager.getConnection("jdbc:sqlite:${tmp.absolutePath}").use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery(
                        "SELECT EncryptionKeyId, EncryptionKey FROM ShareFileItems " +
                            "WHERE EncryptionKeyId IS NOT NULL AND EncryptionKeyId != '' " +
                            "AND EncryptionKey IS NOT NULL AND EncryptionKey != ''"
                    ).use { rs ->
                        while (rs.next()) {
                            val id = rs.getString(1)?.trim() ?: continue
                            val key = rs.getString(2)?.trim() ?: continue
                            if (id.isNotEmpty() && key.isNotEmpty()) {
                                map[id] = key
                                map[id.lowercase()] = key
                                map[id.uppercase()] = key
                            }
                        }
                    }
                }
            }
        } finally {
            tmp.delete()
        }
        return map
    }

    // ---- Singleton cached key map ----
    @Volatile
    private var cachedKeyMap: Map<String, String>? = null

    /**
     * Return the ekey for the given audioHash, loading and caching the key map on first call.
     *
     * @throws IllegalStateException if the database cannot be found or loaded.
     */
    fun getEkey(audioHash: String): String? {
        val hash = audioHash.trim()
        val km = cachedKeyMap ?: synchronized(this) {
            cachedKeyMap ?: run {
                val dbFile = candidateDbPath()
                    ?: error(
                        "找不到酷狗密钥库 KGMusicV3.db（预期在 %APPDATA%\\KuGou8\\ 下）。" +
                            "请确认本机安装过酷狗音乐并下载过歌曲。"
                    )
                loadKeyMap(dbFile).also { cachedKeyMap = it }
            }
        }
        return km[hash] ?: km[hash.lowercase()] ?: km[hash.uppercase()]
    }

    /** Clears the cached key map, forcing a reload on the next call to [getEkey]. */
    fun invalidateCache() {
        cachedKeyMap = null
    }
}