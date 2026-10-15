package com.resonance.player.platform

import com.resonance.player.model.*
import kotlinx.coroutines.flow.StateFlow

interface MusicAssistantServices {
    val state: StateFlow<MusicAssistantState>
    suspend fun settings(): MusicFeatureSettings
    suspend fun saveSettings(settings: MusicFeatureSettings)
    suspend fun testConnection(connection: AiConnection): String
    suspend fun completeText(messages: List<AssistantMessage>, maxTokens: Int): String
    suspend fun cachedTranslation(lyrics: Lyrics, language: String): Lyrics?
    suspend fun chat(message: String, images: List<AiImage> = emptyList())
    suspend fun inspectVideo(link: String): BilibiliVideo
    suspend fun importVideo(request: VideoImportRequest): Track
    suspend fun translate(track: Track, lyrics: Lyrics, language: String, force: Boolean = false): Lyrics
    suspend fun recommend(force: Boolean = false)
    suspend fun dislike(track: Track, reason: String = "")
    suspend fun recordDislikeReason(track: Track, reason: String)
    suspend fun retain(trackId: String)
    suspend fun updateLibrarySnapshot(tracks: List<Track>, playlists: List<Playlist>)
    fun cancel()
}
