package com.resonance.player.model

data class MusicSourceInfo(
    val id: String, val name: String, val version: String = "", val enabled: Boolean = true,
    val capabilities: List<String> = emptyList(), val message: String = "",
)

/** A plugin's claims remain claims; neither a URL nor a title certifies the recording. */
data class SourceSong(
    val sourceId: String, val id: String, val title: String, val artist: String,
    val album: String = "", val albumId: String = "", val durationSeconds: Int = 0,
    val artwork: String = "", val version: String = "", val raw: String = "{}",
)

data class MediaOrigin(
    val source: String = "", val sourceId: String = "", val itemId: String = "",
    val album: String = "", val albumId: String = "", val version: String = "版本未确认",
    val evidence: String = "", val coverKind: String = "待匹配", val coverSource: String = "",
    val format: String = "", val preserved: Boolean = false, val importedAt: Long = 0,
    val bitrate: Long = 0, val sampleRate: Int = 0, val channels: Int = 0,
)
data class AudioFileInfo(val bitrate: Long = 0, val sampleRate: Int = 0, val channels: Int = 0)

private val recordingVariant = Regex(
    "(?i)\\b(live|remix|cover|instrumental|remaster(?:ed)?|medley|slowed|sped[ -]?up)\\b|现场|演唱会|翻唱|混音|混剪|伴奏|加速|减速|变速|串烧|合集|循环版|剪辑版",
)

fun hasRecordingVariant(title: String): Boolean = recordingVariant.containsMatchIn(title)

/** Catalog queries can discard quality labels, but must retain recording-version evidence. */
fun catalogTrackTitle(title: String, artist: String): String {
    var clean = title.trim().replace(Regex("(?i)\\.(mp3|flac|m4a|wav|ape|ogg|aac)$"), "")
    if (artist.isNotBlank() && clean.startsWith("$artist - ", ignoreCase = true)) clean = clean.substring(artist.length + 3)
    val quality = Regex("(?i)320k|flac|hi-?res|\\bsq\\b|\\bhq\\b|无损|高音质|高品质|高清|完整版|official audio")
    return Regex("[\\[(【（][^\\])】）]*[\\])】）]").replace(clean) { match ->
        if (quality.containsMatchIn(match.value) && !hasRecordingVariant(match.value)) "" else match.value
    }.trim().ifBlank { title.trim() }
}

fun needsCatalogArtwork(
    track: Track,
    origin: MediaOrigin?,
    forceRefresh: Boolean = false,
    customCover: Boolean = false,
    legacyVideo: Boolean = false,
): Boolean {
    if (customCover || origin?.coverKind == "自选封面") return false
    if (track.isMissingCover || forceRefresh) return true
    if (origin?.coverKind == "视频缩略图") return true
    return (origin?.sourceId == "bilibili" || legacyVideo) &&
        (origin == null || origin.coverKind in setOf("", "待匹配"))
}

data class ManagedAudioEntry(
    val track: Track, val bytes: Long, val saved: Boolean, val playing: Boolean,
    val duplicates: List<String> = emptyList(),
)
data class MusicStorageReport(
    val entries: List<ManagedAudioEntry> = emptyList(), val managedBytes: Long = 0,
    val temporaryBytes: Long = 0, val savedBytes: Long = 0, val freeBytes: Long = 0,
)
data class MusicCleanupReport(val deletedFiles: Int = 0, val freedBytes: Long = 0, val messages: List<String> = emptyList())
data class AssistantAction(val id: String, val kind: String, val target: String, val label: String)
