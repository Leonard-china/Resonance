package com.resonance.player.features

import com.resonance.player.tutorial.*
import org.json.JSONArray
import org.json.JSONObject

/** Version-independent local progress; IDs are stable across ordinary app upgrades. */
object TutorialProgressCodec {
    const val KEY = "tutorial.progress.v1"

    fun encode(progress: TutorialProgress): String = JSONObject().apply {
        put("welcomeDismissed", progress.welcomeDismissed)
        put("quickPage", progress.quickPage)
        put("quickCompleted", progress.quickCompleted)
        put("chapters", JSONObject().apply {
            progress.chapters.forEach { (id, item) -> put(id, JSONObject().apply {
                put("nextStep", item.nextStep)
                put("completed", JSONArray(item.completed.sorted()))
                put("skipped", JSONArray(item.skipped.sorted()))
            }) }
        })
    }.toString()

    fun decode(raw: String?): TutorialProgress {
        if (raw.isNullOrBlank() || raw.length > 65536) return TutorialProgress()
        return runCatching {
            val root = JSONObject(raw)
            val saved = root.optJSONObject("chapters")
            TutorialProgress(root.optBoolean("welcomeDismissed"),
                root.optInt("quickPage").coerceIn(TutorialCatalog.quick.indices), root.optBoolean("quickCompleted"),
                TutorialCatalog.chapters.mapNotNull { chapter ->
                    val item = saved?.optJSONObject(chapter.id) ?: return@mapNotNull null
                    val known = chapter.steps.map { it.id }.toSet()
                    fun ids(key: String): Set<String> = item.optJSONArray(key)?.let { array ->
                        (0 until array.length()).map { array.optString(it) }.filter { it in known }.toSet()
                    }.orEmpty()
                    val complete = ids("completed")
                    chapter.id to ChapterProgress(item.optInt("nextStep").coerceIn(0, chapter.steps.size), complete, ids("skipped") - complete)
                }.toMap())
        }.getOrDefault(TutorialProgress())
    }
}
