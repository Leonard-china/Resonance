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
