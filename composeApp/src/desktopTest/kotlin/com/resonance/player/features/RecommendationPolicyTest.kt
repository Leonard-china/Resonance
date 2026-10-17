package com.resonance.player.features

import com.resonance.player.model.*
import org.json.JSONObject
import kotlin.test.*

class RecommendationPolicyTest {
    @Test fun duplicateFilesCountOnceAndVersionsRemainSeparate() {
        val original = Track("1", "勇气", "梁静茹", "勇气", "3:59", 0)
        val duplicates = (1..8).map { original.copy(id = it.toString(), sourceUri = "file:///copy" + it + ".mp3") }
        assertEquals(1, RecommendationPolicy.unique(duplicates).size)
        assertEquals(2, RecommendationPolicy.unique(duplicates + original.copy(id = "live", title = "勇气 (Live)")).size)
    }
    @Test fun cooldownSurvivesFileRemovalAndUsesRollingDays() {
        val now = 1_800_000_000_000L
        val song = SongRequest("勇气", "梁静茹")
        val history = JSONObject().put(RecommendationPolicy.identity(song.title, song.artist), now - 86_400_000)
        assertTrue(RecommendationPolicy.recentlyRecommended(history, song, 7, now))
        assertFalse(RecommendationPolicy.recentlyRecommended(history, song, 7, now + 7 * 86_400_000L))
    }
    @Test fun reasonsOnlyUseRealSavedEvidence() {
        val saved = Track("1", "勇气", "梁静茹", "勇气", "3:59", 0)
        val reason = RecommendationPolicy.reason(SongRequest("宁夏", "梁静茹", reason = "曲库有8首抒情歌"), listOf(saved), "目录")
        assertTrue(reason.contains("勇气"))
        assertFalse(reason.contains("8首"))
        assertFalse(reason.contains("抒情"))
    }
    @Test fun legacySettingsDefaultToSourceModeAndRoundTripNewControls() {
        val legacy = MusicFeatureCodec.settings(JSONObject().put("recommendationsEnabled", true))
        assertEquals(RecommendationMode.Source, legacy.recommendationMode)
        val changed = legacy.copy(recommendationCooldownDays = 30, temporaryStorageMb = 256, preferredBilibiliUids = "123,456")
        assertEquals(changed, MusicFeatureCodec.settings(MusicFeatureCodec.encode(changed)))
    }
}
