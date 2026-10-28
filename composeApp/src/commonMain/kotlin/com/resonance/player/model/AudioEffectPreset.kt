package com.resonance.player.model

/**
 * 跨平台音频音效调校预设
 */
enum class AudioEffectPreset(
    val id: String,
    val displayName: String,
    val subtitle: String,
) {
    Off("off", "默认原声", "纯净未调校高保真直通"),
    Vocal("vocal", "纯净人声", "中频温润凸显，人声细节丰富"),
    BassBoost("bass_boost", "澎湃低音", "低频下潜有力，节奏鼓点浑厚"),
    Pop("pop", "现场流行", "均衡微提升，空间明快轻盈"),
    Rock("rock", "震撼摇滚", "动态两端提升，吉他鼓点鲜明"),
    Surround("surround", "3D 环绕", "立体声场扩宽，沉浸空间体验"),
    Karaoke("karaoke", "伴奏消音", "中置人声消除与低频保护");

    companion object {
        fun fromId(id: String?): AudioEffectPreset =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: Off
    }
}
