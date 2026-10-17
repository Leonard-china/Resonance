package com.resonance.player.features

import com.resonance.player.model.*
import org.json.JSONObject

internal object RecommendationFeedbackCodec {
    fun read(root: JSONObject): Map<String, RecommendationFeedback> {
        val records = root.optJSONObject("recommendationFeedback") ?: return emptyMap()
        return records.keys().asSequence().mapNotNull { key ->
            val item = records.optJSONObject(key) ?: return@mapNotNull null
            val title = item.optString("title")
            val artist = item.optString("artist")
            if (title.isBlank() || artist.isBlank()) return@mapNotNull null
            RecommendationPolicy.identity(title, artist) to RecommendationFeedback(title, artist,
                item.optString("album"),
                RecommendationVote.entries.firstOrNull { it.name == item.optString("vote") } ?: RecommendationVote.Neutral,
                item.optString("comment").take(500), item.optLong("updatedAt"))
        }.toMap()
    }
    fun write(root: JSONObject, feedback: RecommendationFeedback) {
        val records = root.optJSONObject("recommendationFeedback") ?: JSONObject()
        val key = RecommendationPolicy.identity(feedback.title, feedback.artist)
        if (feedback.vote == RecommendationVote.Neutral && feedback.comment.isBlank()) records.remove(key)
        else records.put(key, JSONObject().put("title", feedback.title).put("artist", feedback.artist)
            .put("album", feedback.album).put("vote", feedback.vote.name)
            .put("comment", feedback.comment.take(500)).put("updatedAt", feedback.updatedAt))
        root.put("recommendationFeedback", records)
    }
}

/** Local metadata evidence only. Free-form interpretation is reserved for opt-in AI mode. */
internal object RecommendationFeedbackPolicy {
    private val tags = listOf("抒情", "轻快", "摇滚", "民谣", "电子", "爵士", "古典", "纯音乐", "钢琴", "吉他", "粤语", "日语", "英语", "说唱")
    internal fun commentSignals(comment: String): Map<String, Int> = tags.mapNotNull { tag ->
        val index = comment.lastIndexOf(tag)
        if (index < 0) null else {
            val before = comment.substring(maxOf(0, index - 10), index)
            val negative = Regex("(?:不喜欢|不想|不要|不爱|讨厌|太|少来|少推|别|不是)[^，。！？,;；]{0,6}$").containsMatchIn(before)
            tag to if (negative) -1 else 1
        }
    }.toMap()

    fun seeds(saved: List<Track>, feedback: Collection<RecommendationFeedback>): List<String> {
        val artists = linkedMapOf<String, Pair<String, Int>>()
        fun add(artist: String, score: Int) {
            val key = RecommendationPolicy.normalize(artist)
            if (key.isNotBlank()) artists[key] = artist to ((artists[key]?.second ?: 0) + score)
        }
        RecommendationPolicy.unique(saved).forEach { add(it.artist, if (it.isFavorite) 8 else 4) }
        feedback.sortedByDescending { it.updatedAt }.forEach { item ->
            val commentScore = when {
                listOf("难听", "不好听", "不喜欢", "太吵", "不想再听").any(item.comment::contains) -> -2
                item.comment.isNotBlank() -> 1
                else -> 0
            }
            add(item.artist, when (item.vote) { RecommendationVote.Like -> 3; RecommendationVote.Dislike -> -5; else -> 0 } + commentScore)
        }
        val preferredTags = feedback.flatMap { commentSignals(it.comment).entries }.groupBy({ it.key }, { it.value })
            .mapValues { it.value.sum() }.filterValues { it > 0 }.entries.sortedByDescending { it.value }.map { it.key }
        // Keep space for explicit comment preferences inside the registry's four-query budget.
        val preferredArtists = artists.values.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
        return (preferredArtists.take(if (preferredTags.isEmpty()) 4 else 3) + preferredTags.take(1) + preferredArtists).distinct().take(4)
    }

    fun score(song: SongRequest, feedback: Collection<RecommendationFeedback>): Int {
        val artist = RecommendationPolicy.normalize(song.artist)
        val album = RecommendationPolicy.normalize(song.album)
        val metadata = song.title + " " + song.artist + " " + song.album
        return feedback.sumOf { item ->
            val sameArtist = RecommendationPolicy.normalize(item.artist) == artist
            val sameAlbum = sameArtist && album.isNotBlank() && RecommendationPolicy.normalize(item.album) == album
            val voteScore = when (item.vote) {
                RecommendationVote.Like -> (if (sameArtist) 3 else 0) + (if (sameAlbum) 2 else 0)
                RecommendationVote.Dislike -> (if (sameArtist) -4 else 0) + (if (sameAlbum) -3 else 0)
                else -> 0
            }
            voteScore + commentSignals(item.comment).entries.sumOf { (tag, preference) -> if (metadata.contains(tag)) preference * 3 else 0 }
        }
    }

    fun summary(feedback: Collection<RecommendationFeedback>): String = org.json.JSONArray(
        feedback.sortedByDescending { it.updatedAt }.take(24).map { item ->
            JSONObject().put("title", item.title.take(180)).put("artist", item.artist.take(180)).put("vote", item.vote.name).put("comment", item.comment.take(160))
        }).toString()
}
