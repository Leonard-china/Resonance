package com.resonance.player.features

import java.nio.file.Files
import kotlin.test.*

class MusicJsonStoreTest {
    @Test fun corruptPrimaryRecoversWithoutOverwritingGoodBackupOnNextWrite() {
        val root = Files.createTempDirectory("assistant-recovery-")
        try {
            val store = MusicJsonStore(root)
            store.update { it.put("revision", 1) }
            store.update { it.put("revision", 2) }
            Files.writeString(root.resolve("music-assistant.json"), "broken")
            assertEquals(1, store.read().getInt("revision"))
            store.update { it.put("revision", 3) }
            Files.writeString(root.resolve("music-assistant.json"), "broken again")
            assertEquals(1, store.read().getInt("revision"))
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }
    @Test fun twoCorruptCopiesStopInsteadOfSilentlyResettingSettingsAndHistory() {
        val root = Files.createTempDirectory("assistant-corruption-")
        try {
            val store = MusicJsonStore(root)
            Files.writeString(root.resolve("music-assistant.json"), "broken")
            Files.writeString(root.resolve("music-assistant.json.bak"), "broken")
            assertFailsWith<IllegalStateException> { store.update { it.put("replacement", true) } }
            assertEquals("broken", Files.readString(root.resolve("music-assistant.json")))
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }
}
