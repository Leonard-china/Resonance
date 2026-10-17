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
    suspend fun confirmScreenshotImport(draftId: String, playlists: List<ScreenshotPlaylistDraft>)
    suspend fun discardScreenshotImport()
    suspend fun inspectVideo(link: String): BilibiliVideo
    suspend fun importVideo(request: VideoImportRequest): Track
    suspend fun translate(track: Track, lyrics: Lyrics, language: String, force: Boolean = false): Lyrics
    suspend fun recommend(force: Boolean = false)
    suspend fun dislike(track: Track, reason: String = "")
    suspend fun recordDislikeReason(track: Track, reason: String)
    fun feedbackFor(track: Track): RecommendationFeedback?
    suspend fun setRecommendationVote(track: Track, vote: RecommendationVote)
    suspend fun saveRecommendationComment(track: Track, comment: String)
    suspend fun retain(trackId: String)
    suspend fun updateLibrarySnapshot(tracks: List<Track>, playlists: List<Playlist>)
    suspend fun retryTask(id: String)
    suspend fun retryFailedTasks(ids: List<String>)
    suspend fun matchCover(track: Track, force: Boolean = true): Track
    suspend fun matchVideoCovers()
    suspend fun matchMissingCovers()
    suspend fun setCover(track: Track, path: String): Track
    suspend fun generateSynchronizedLyrics(track: Track, lyrics: Lyrics? = null): Lyrics
    suspend fun cachedSynchronizedLyrics(track: Track): Lyrics?
    suspend fun createSession(title: String = "新对话"): String
    suspend fun switchSession(sessionId: String)
    suspend fun deleteSession(sessionId: String)
    suspend fun clearCurrentSession()
    suspend fun restoreBuiltinSources(): List<MusicSourceInfo> = emptyList()
    suspend fun installSource(text: String): MusicSourceInfo
    suspend fun installSourceUrl(url: String): List<MusicSourceInfo>
    suspend fun enableSource(id: String, enabled: Boolean)
    suspend fun removeSource(id: String)
    suspend fun searchSources(query: String): List<SourceSong>
    suspend fun importSourceSong(song: SourceSong, playlistId: String? = null): Track
    suspend fun prepareSourcePreview(song: SourceSong): String
    suspend fun clearSourcePreview()
    suspend fun storageReport(checkDuplicates: Boolean = false): MusicStorageReport
    suspend fun cleanupDownloads(ids: List<String>, includeSaved: Boolean = false): MusicCleanupReport
    suspend fun restoreDownload(trackId: String): Track
    suspend fun saveLyricOffset(track: Track, lyrics: Lyrics, deltaMs: Long): Lyrics
    suspend fun acknowledgeAction(id: String)
    suspend fun saveToPlaylist(trackIds: List<String>, playlistId: String? = null, newName: String? = null): String
    suspend fun updateUiContext(player: PlayerState, destination: LibraryDestination)
    fun cancel()
    suspend fun createBilibiliQrSession(): BiliQrSession? = null
    suspend fun pollBilibiliQrStatus(qrcodeKey: String): BiliQrPollResult = BiliQrPollResult(BiliQrPollStatus.ERROR, message = "不支持该操作")
}
