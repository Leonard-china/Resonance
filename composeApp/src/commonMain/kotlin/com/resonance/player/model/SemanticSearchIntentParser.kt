package com.resonance.player.model

object SemanticSearchIntentParser {

    private val genreKeywords = listOf(
        "流行", "摇滚", "民谣", "纯音乐", "古典", "说唱", "电音", "爵士", "古风", "国风", "蓝调",
        "金属", "后摇", "轻音乐", "acg", "动漫", "原声", "ost", "电子", "朋克", "乡村", "交响",
    )

    private val moodKeywords = listOf(
        "伤感", "难过", "治愈", "欢快", "快乐", "激动", "放松", "宁静", "安静", "深情", "忧郁",
        "燃", "热血", "浪漫", "温柔", "空灵", "慵懒", "怀旧", "复古", "想念", "思念",
    )

    private val sceneKeywords = listOf(
        "雨天", "下雨", "跑步", "运动", "健身", "写代码", "编程", "工作", "睡眠", "助眠", "失眠",
        "开车", "自驾", "旅行", "散步", "学习", "咖啡", "清晨", "深夜", "黄昏",
    )

    fun parse(rawQuery: String): SemanticSearchQueryIntent {
        val q = rawQuery.trim().lowercase()
        val foundGenres = genreKeywords.filter { q.contains(it) }
        val foundMoods = moodKeywords.filter { q.contains(it) }
        val foundScenes = sceneKeywords.filter { q.contains(it) }

        // BPM resolution
        val bpmRegex = Regex("(\\d{2,3})\\s*bpm", RegexOption.IGNORE_CASE)
        val bpmMatch = bpmRegex.find(q)
        val bpmRange = when {
            bpmMatch != null -> {
                val target = bpmMatch.groupValues[1].toIntOrNull() ?: 120
                (target - 15)..(target + 15)
            }
            q.contains("快歌") || q.contains("超燃") || q.contains("节奏感") || q.contains("跑步") || q.contains("健身") -> 120..190
            q.contains("慢歌") || q.contains("催眠") || q.contains("助眠") || q.contains("安静") || q.contains("轻柔") -> 50..88
            q.contains("轻快") || q.contains("散步") || q.contains("放松") -> 95..128
            else -> null
        }

        // Remaining keywords
        var remainder = rawQuery
        for (w in foundGenres + foundMoods + foundScenes) {
            remainder = remainder.replace(w, " ", ignoreCase = true)
        }
        remainder = remainder.replace(Regex("\\b\\d{2,3}\\s*bpm\\b", RegexOption.IGNORE_CASE), " ")
        remainder = remainder.replace(Regex("[适合的找听些歌一首首曲子音乐想]"), " ")
        val keywords = remainder.split("\\s+".toRegex()).map { it.trim() }.filter { it.length >= 2 }

        return SemanticSearchQueryIntent(
            rawQuery = rawQuery,
            artists = emptyList(),
            genres = foundGenres,
            moods = foundMoods,
            scenes = foundScenes,
            bpmRange = bpmRange,
            keywords = keywords,
        )
    }
}
