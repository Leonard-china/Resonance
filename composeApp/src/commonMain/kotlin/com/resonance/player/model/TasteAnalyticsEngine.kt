package com.resonance.player.model

object TasteAnalyticsEngine {

    fun analyze(
        tracks: List<Track>,
        feedbackLikes: Set<String> = emptySet(),
        feedbackDislikes: Set<String> = emptySet(),
    ): TastePersona {
        if (tracks.isEmpty()) {
            return TastePersona(
                title = "初遇拾音人",
                subtitle = "音乐宇宙尚待探索",
                summary = "曲库还处于纯净状态，导入本地歌曲或搜索心仪旋律后，专属的品味雷达将随旋律绽放。",
                dimensions = TasteDimensions.Balanced,
                topArtists = emptyList(),
                dominantVibe = "静谧",
                totalTracksAnalyzed = 0,
            )
        }

        val total = tracks.size
        // Count artists
        val artistCounts = mutableMapOf<String, Int>()
        var favoriteCount = 0
        var longTracks = 0
        var shortTracks = 0

        val retroKeywords = setOf("经典", "老歌", "80s", "90s", "重金属", "原声", "live", "现场", "交响", "协奏", "古典", "民谣")
        val modernPopKeywords = setOf("remix", "feat", "pop", "流行", "舞曲", "edm", "trap", "hiphop", "说唱", "电音")
        val indieKeywords = setOf("独立", "纯音乐", "instrumental", "ost", "ambient", "氛围", "后摇", "acg", "同人", "东方")
        val emotionalKeywords = setOf("伤感", "温柔", "治愈", "吉他", "钢琴", "安静", "孤独", "晚安", "失眠", "想念", "蓝调")

        var retroHits = 0
        var modernHits = 0
        var indieHits = 0
        var emotionalHits = 0

        for (t in tracks) {
            val a = t.artist.trim()
            if (a.isNotBlank() && !t.isMissingArtist) {
                artistCounts[a] = (artistCounts[a] ?: 0) + 1
            }
            if (t.isFavorite || t.id in feedbackLikes) {
                favoriteCount++
            }

            val text = (t.title + " " + t.artist + " " + t.album).lowercase()
            if (retroKeywords.any { text.contains(it) }) retroHits++
            if (modernPopKeywords.any { text.contains(it) }) modernHits++
            if (indieKeywords.any { text.contains(it) }) indieHits++
            if (emotionalKeywords.any { text.contains(it) }) emotionalHits++
        }

        val topArtists = artistCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        val uniqueArtistsCount = artistCounts.size
        val artistDiversityRatio = if (total > 0) (uniqueArtistsCount.toFloat() / total).coerceIn(0f, 1f) else 0.5f

        // Calculate 6 dimensions (0 - 100)
        val popVibrancy = ((modernHits.toFloat() / total * 150f) + 40f + (topArtists.size * 3)).toInt().coerceIn(30, 95)
        val retroNostalgia = ((retroHits.toFloat() / total * 160f) + 35f).toInt().coerceIn(25, 95)
        val indieExploration = ((indieHits.toFloat() / total * 160f) + (artistDiversityRatio * 40f) + 25f).toInt().coerceIn(30, 96)
        val emotionalResonance = (((favoriteCount.toFloat() / total * 60f) + (emotionalHits.toFloat() / total * 120f) + 45f)).toInt().coerceIn(35, 98)
        val rhythmicEnergy = ((modernHits * 1.2f + total * 0.4f) / total * 70f + 30f).toInt().coerceIn(30, 95)
        val genreDiversity = ((artistDiversityRatio * 70f) + (topArtists.size * 5f) + 15f).toInt().coerceIn(35, 98)

        val dimensions = TasteDimensions(
            popVibrancy = popVibrancy,
            retroNostalgia = retroNostalgia,
            indieExploration = indieExploration,
            emotionalResonance = emotionalResonance,
            rhythmicEnergy = rhythmicEnergy,
            genreDiversity = genreDiversity,
        )

        // Determine Persona
        val (title, subtitle, summary, dominantVibe) = when {
            emotionalResonance >= 78 && retroNostalgia >= 65 -> Quad(
                "光影留声机",
                "在岁月的和弦里寻找心跳共振",
                "你的曲库充满了温热的情感底色与时光故事。比起快餐旋律，你更容易被有年代感与叙事厚度的编曲击中，每一段旋律都是私藏的情绪胶片。",
                "怀旧深情",
            )
            rhythmicEnergy >= 75 && popVibrancy >= 70 -> Quad(
                "都市律动先锋",
                "节拍是你的多巴胺加速器",
                "你热爱充满张力与动态的律动，电子、现代流行与澎湃鼓点是你的能量源泉。只要音乐响起，日常的每个步伐都自带节拍器与电影感。",
                "活力澎湃",
            )
            indieExploration >= 75 && genreDiversity >= 70 -> Quad(
                "隐世声音猎手",
                "在小众与纯粹中捕获独家共鸣",
                "你拥有极其敏锐的听觉雷达与不受潮流束缚的独立品味。小众厂牌、纯乐氛围或冷门原声在你的曲库中闪闪发光，拒绝千篇一律的同质化流行。",
                "灵动纯粹",
            )
            emotionalResonance >= 75 -> Quad(
                "深夜白日梦想家",
                "借音乐构筑属于自己的静谧避难所",
                "你的音乐品味如同一杯温热的咖啡，偏爱能够慰藉心弦的抒情篇章与细腻旋律。在喧嚣的世界中，耳机内流淌的声线是你与内心对话的最佳桥梁。",
                "温柔治愈",
            )
            else -> Quad(
                "全域沉浸听觉家",
                "兼容并蓄的音乐全能杂食家",
                "你的音乐宇宙广阔而自由，流行、古典、独立、律动皆能在你的播放队列中和谐共鸣。不拘泥于特定标签，只要打动双耳便是好歌。",
                "丰富多元",
            )
        }

        return TastePersona(
            title = title,
            subtitle = subtitle,
            summary = summary,
            dimensions = dimensions,
            topArtists = topArtists,
            dominantVibe = dominantVibe,
            totalTracksAnalyzed = total,
        )
    }

    private data class Quad(val first: String, val second: String, val third: String, val fourth: String)
}
