package com.resonance.player.playback

import com.resonance.player.model.AudioEffectPreset
import com.resonance.player.model.formatListeningSeconds
import java.nio.file.Files
import kotlin.test.*

class ListeningStatsStoreTest {

    @Test
    fun formatsListeningSecondsNicely() {
        assertEquals("未听过", formatListeningSeconds(0))
        assertEquals("45秒", formatListeningSeconds(45))
        assertEquals("5分钟", formatListeningSeconds(300))
        assertEquals("1小时15分钟", formatListeningSeconds(4500))
        assertEquals("2小时", formatListeningSeconds(7200))
    }

    @Test
    fun recordsPlaybackSecondsAndSavesAtomically() {
        val tempDir = Files.createTempDirectory("listening-stats-test")
        try {
            val store = ListeningStatsStore(tempDir)
            assertEquals(0L, store.getAllStats().totalSeconds)
            assertNull(store.getTrackStats("track-1"))

            // 1. 记录 30 秒播放
            store.recordPlayback("track-1", 30, isCompleted = false)
            val stats1 = store.getTrackStats("track-1")
            assertNotNull(stats1)
            assertEquals(30L, stats1.totalSeconds)
            assertEquals(0, stats1.playCount)
            assertEquals(30L, store.getAllStats().totalSeconds)
            assertTrue(stats1.lastPlayedAt > 0)

            // 2. 再记录 40 秒并标记完播
            store.recordPlayback("track-1", 40, isCompleted = true)
            val stats2 = store.getTrackStats("track-1")
            assertNotNull(stats2)
            assertEquals(70L, stats2.totalSeconds)
            assertEquals(1, stats2.playCount)
            assertEquals(70L, store.getAllStats().totalSeconds)

            // 3. 记录另一首歌曲
            store.recordPlayback("track-2", 120, isCompleted = true)
            assertEquals(190L, store.getAllStats().totalSeconds)

            // 4. 重建 Store，验证从磁盘持久化恢复
            val reloadedStore = ListeningStatsStore(tempDir)
            assertEquals(190L, reloadedStore.getAllStats().totalSeconds)
            val reloadedTrack1 = reloadedStore.getTrackStats("track-1")
            assertNotNull(reloadedTrack1)
            assertEquals(70L, reloadedTrack1.totalSeconds)
            assertEquals(1, reloadedTrack1.playCount)

            val reloadedTrack2 = reloadedStore.getTrackStats("track-2")
            assertNotNull(reloadedTrack2)
            assertEquals(120L, reloadedTrack2.totalSeconds)
            assertEquals(1, reloadedTrack2.playCount)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun audioEffectPresetFromIdWorks() {
        assertEquals(AudioEffectPreset.Off, AudioEffectPreset.fromId(null))
        assertEquals(AudioEffectPreset.Off, AudioEffectPreset.fromId("unknown"))
        assertEquals(AudioEffectPreset.Vocal, AudioEffectPreset.fromId("vocal"))
        assertEquals(AudioEffectPreset.BassBoost, AudioEffectPreset.fromId("BASS_BOOST"))
        assertEquals(AudioEffectPreset.Karaoke, AudioEffectPreset.fromId("karaoke"))
        assertEquals(AudioEffectPreset.Surround, AudioEffectPreset.fromId("surround"))
    }
}
