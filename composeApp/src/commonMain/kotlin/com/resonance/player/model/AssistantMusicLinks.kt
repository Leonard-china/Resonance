package com.resonance.player.model

import java.net.URLDecoder
import java.net.URLEncoder

private const val trackLinkPrefix = "resonance://track/"
private val musicLinkPattern = Regex("\\[([^]\\n]{1,240})]\\((resonance://track/[^\\s)]{1,2048})\\)")

/** Model-generated links open only on an explicit click, and only as web pages. */
fun isAssistantWebLink(value: String): Boolean = runCatching {
    val uri = java.net.URI(value)
    uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null
}.getOrDefault(false)

fun assistantTrackLink(trackId: String): String = trackLinkPrefix + URLEncoder.encode(trackId, "UTF-8")

data class AssistantMusicContent(val text: String, val tracks: List<Track>)

/** Resolve only explicit links to current library IDs. Names and remote URLs never authorize playback. */
fun resolveAssistantMusic(content: String, library: Map<String, Track>): AssistantMusicContent {
    val linked = linkedMapOf<String, Track>()
    val text = musicLinkPattern.replace(content) { match ->
        val id = runCatching { URLDecoder.decode(match.groupValues[2].removePrefix(trackLinkPrefix), "UTF-8") }.getOrNull()
        if (linked.size < 20) library[id]?.let { linked[it.id] = it }
        match.groupValues[1]
    }
    return AssistantMusicContent(text, linked.values.toList())
}

/** Append playable entries in reply order; keep the current queue and never start playback implicitly. */
fun appendAssistantQueue(queue: List<Track>, tracks: List<Track>): List<Track> =
    (queue + tracks.filter { !it.sourceUri.isNullOrBlank() }).distinctBy(Track::id)
