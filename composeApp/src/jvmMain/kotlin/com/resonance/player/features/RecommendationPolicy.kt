package com.resonance.player.features

import com.resonance.player.model.*
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Counts musical identities, not files. Recording qualifiers deliberately remain in the key. */
internal object RecommendationPolicy {
    fun normalize(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)
    fun identity(title: String, artist: String): String = normalize(artist) + ":" + normalize(title)
    fun unique(tracks: List<Track>): List<Track> = tracks.distinctBy { identity(it.title, it.artist) }
    fun recentlyRecommended(history: JSONObject?, song: SongRequest, days: Int, now: Long): Boolean {
        val at = history?.optLong(identity(song.title, song.artist), 0L) ?: 0L
        return at > 0 && (now < at || now - at < days.coerceIn(1, 365) * 86_400_000L)
    }
    fun reason(song: SongRequest, saved: List<Track>, source: String, feedback: Collection<RecommendationFeedback> = emptyList()): String {
        val liked = feedback.firstOrNull { it.vote == RecommendationVote.Like && normalize(it.artist) == normalize(song.artist) }
        if (liked != null) return "你为「" + liked.title + "」点过赞，这首也来自 " + song.artist + "。"
        val artist = saved.firstOrNull { normalize(it.artist) == normalize(song.artist) }
        return if (artist != null) "你保存过「" + artist.title + "」，这首也来自 " + song.artist + "。"
        else "来自「" + source + "」的候选，已排除曲库已有和近期推荐。"
    }
    fun date(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().toString()
}
