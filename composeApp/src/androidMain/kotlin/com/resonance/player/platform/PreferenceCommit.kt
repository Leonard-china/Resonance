package com.resonance.player.platform

import android.content.SharedPreferences

internal fun SharedPreferences.Editor.commitChecked(message: String): Boolean {
    check(commit()) { message }
    return true
}

/** Attempts to restore in-memory preferences and their on-disk revision after a failed import. */
internal fun <T> withPreferenceRollback(preferences: List<SharedPreferences>, action: () -> T): T {
    val saved = preferences.map { prefs -> prefs to prefs.all.mapValues { (_, value) ->
        if (value is Set<*>) value.toSet() else value
    } }
    return try { action() } catch (error: Throwable) {
        saved.asReversed().forEach { (prefs, values) ->
            try {
                val editor = prefs.edit().clear()
                values.forEach { (key, value) -> when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                } }
                editor.commitChecked("导入失败后无法恢复偏好，请保留原数据并重试")
            } catch (rollback: Throwable) { error.addSuppressed(rollback) }
        }
        throw error
    }
}
