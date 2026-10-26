package com.resonance.player.model

/**
 * 六维音乐品味维度指标 (0 - 100)
 */
data class TasteDimensions(
    val popVibrancy: Int,        // 流行敏锐度
    val retroNostalgia: Int,     // 复古情怀
    val indieExploration: Int,   // 小众探索度
    val emotionalResonance: Int, // 情绪共鸣度
    val rhythmicEnergy: Int,     // 节奏活力感
    val genreDiversity: Int,     // 流派多样性
) {
    companion object {
        val Balanced = TasteDimensions(
            popVibrancy = 60,
            retroNostalgia = 50,
            indieExploration = 55,
            emotionalResonance = 65,
            rhythmicEnergy = 58,
            genreDiversity = 60,
        )
    }
}

/**
 * 听歌性格与音乐品味画像
 */
data class TastePersona(
    val title: String,
    val subtitle: String,
    val summary: String,
    val dimensions: TasteDimensions,
    val topArtists: List<String>,
    val dominantVibe: String,
    val totalTracksAnalyzed: Int,
    val generatedAt: Long = System.currentTimeMillis(),
)
