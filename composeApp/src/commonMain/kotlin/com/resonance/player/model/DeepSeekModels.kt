package com.resonance.player.model

data class DeepSeekConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com/v1",
    val model: String = "deepseek-chat",
    val enabled: Boolean = true,
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank()
}

data class DeepSeekTestResult(
    val success: Boolean,
    val message: String,
)

data class AiEnrichResult(
    val success: Boolean,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val lyrics: String? = null,
    val isInstrumental: Boolean = false,
    val message: String? = null,
)

