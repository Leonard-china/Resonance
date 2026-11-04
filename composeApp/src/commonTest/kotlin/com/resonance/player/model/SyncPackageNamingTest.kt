package com.resonance.player.model

import kotlin.test.*

class SyncPackageNamingTest {
    @Test fun playlistTitleIsTheFilenameWithoutTimestampsAndLibraryHasAClearName() {
        assertEquals("轻快活力.resonance", syncPackageFileName("轻快活力"))
        assertEquals("整个音乐库.resonance", syncPackageFileName(null))
        assertEquals("我的歌单.resonance", syncPackageFileName(" 我的歌单.RESONANCE "))
        assertEquals("未命名歌单.resonance", syncPackageFileName("...  "))
    }

    @Test fun namesAreSinglePortableComponentsIncludingWindowsReservedNames() {
        assertEquals("歌单 CON.resonance", syncPackageFileName("CON"))
        assertEquals("歌单 NUL.mp3.resonance", syncPackageFileName("NUL.mp3"))
        val name = syncPackageFileName("../我的\\歌单:夏日*?\"<>|\n夜晚")
        assertTrue(name.startsWith("我的 歌单 夏日"))
        assertTrue(name.endsWith(".resonance"))
        assertFalse(name.any { it in "/\\:*?\"<>|\n\r" })
    }

    @Test fun longUnicodeNamesStayWithinFilenameLimitsWithoutCuttingEmojiInHalf() {
        val name = syncPackageFileName("春".repeat(71) + "🎵" + "夏".repeat(30))
        val stem = name.removeSuffix(".resonance")
        assertEquals("春".repeat(71), stem)
        assertTrue(name.encodeToByteArray().size < 255)
        assertEquals("夜晚 🎵.resonance", syncPackageFileName("夜晚 🎵"))
    }

    @Test fun repeatedExportsKeepExistingFilesAndUseTheNextFreeSuffix() {
        val existing = setOf("轻快活力.resonance", "轻快活力 (2).resonance", "轻快活力 (3).resonance")
        assertEquals("轻快活力 (4).resonance", uniqueSyncPackageFileName("轻快活力.resonance", existing::contains))
        assertEquals("新歌单.resonance", uniqueSyncPackageFileName("新歌单.resonance", existing::contains))
    }
}
