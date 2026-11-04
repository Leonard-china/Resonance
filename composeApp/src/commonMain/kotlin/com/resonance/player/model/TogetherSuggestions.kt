package com.resonance.player.model

/** The model can only reference this captured list; text never becomes a command or file path. */
fun togetherSuggestionIndices(answer: String, candidateCount: Int): List<Int> {
    val text = answer.trim()
    require(text.length <= 64 && text.matches(Regex("[0-9]+(\\s*,\\s*[0-9]+){0,4}"))) { "AI 返回的选歌格式无效，请重试" }
    val values = text.split(',').map { it.trim().toIntOrNull()?.minus(1) ?: -1 }
    require(values.all { it in 0 until candidateCount } && values.distinct().size == values.size) { "AI 返回了候选以外的歌曲，请重试" }
    return values
}
