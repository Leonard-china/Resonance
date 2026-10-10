package com.resonance.player.audio

import org.json.JSONObject

/** Shared whole-track BS.1770/R128 measurements; automatic gain never exceeds true-peak headroom. */
object LoudnessAnalysis {
    const val CACHE_VERSION = "r128-v1"
    const val FILTER = "loudnorm=I=-14:TP=-1:LRA=50:print_format=json"

    fun gainFromOutput(output: String): Float? {
        return try {
            // Android's ICU regex requires both JSON braces to be escaped.
            val match = Regex("\\{[^{}]*\"input_i\"[^{}]*\\}").findAll(output).lastOrNull() ?: return null
            val json = JSONObject(match.value)
            val integrated = json.optString("input_i").toFloatOrNull() ?: return null
            val peak = json.optString("input_tp").toFloatOrNull() ?: return null
            if (!integrated.isFinite() || !peak.isFinite()) return null
            minOf(-14f - integrated, -1f - peak, 14f).coerceAtLeast(-60f)
        } catch (_: Exception) { null }
    }

    fun arguments(input: String): Array<String> = arrayOf(
        "-hide_banner", "-nostdin", "-nostats", "-i", input, "-map", "0:a:0", "-vn", "-sn", "-dn",
        "-af", FILTER, "-f", "null", "-",
    )
}
