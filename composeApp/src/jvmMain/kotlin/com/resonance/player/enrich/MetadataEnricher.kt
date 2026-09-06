package com.resonance.player.enrich

import com.resonance.player.ai.DeepSeekClient
import com.resonance.player.io.readUpTo
import com.resonance.player.lyrics.LrclibLyricsRepository
import com.resonance.player.lyrics.parseLrc
import com.resonance.player.model.AiEnrichResult
import com.resonance.player.model.BatchEnrichOptions
import com.resonance.player.model.BatchEnrichProgress
import com.resonance.player.model.BatchEnrichReport
import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.LyricLine
import com.resonance.player.model.Lyrics
import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class MetadataEnricher(
    private val artworkDirectory: Path,
    private val lyricsRepository: LrclibLyricsRepository,
    private val deepSeekClient: DeepSeekClient = DeepSeekClient(),
) {

    data class OnlineSearchResult(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val artworkUrl: String? = null,
        val source: String = "Online",
    )

    data class SingleEnrichResult(
        val track: Track,
        val modified: Boolean,
        val aiCalled: Boolean = false,
    )

    suspend fun enrichTrack(
        track: Track,
        options: BatchEnrichOptions,
        config: DeepSeekConfig,
        customLyricsFolder: String? = null,
        onStatus: (String) -> Unit = {},
    ): SingleEnrichResult = withContext(Dispatchers.IO) {
        var currentTrack = track
        var modified = false
        var aiCalled = false

        val needCover = options.enrichCover && (options.forceRefresh || currentTrack.isMissingCover)
        val needArtistOrAlbum = options.enrichArtistAndAlbum && (options.forceRefresh || currentTrack.isMissingArtist || currentTrack.isMissingAlbum)
        val needLyrics = options.enrichLyrics

        // 如果该歌曲什么都不缺且非强制刷新，直接跳过
        if (!needCover && !needArtistOrAlbum && !needLyrics) {
            onStatus("元数据完整，跳过")
            return@withContext SingleEnrichResult(currentTrack, false, false)
        }

        // 1. 尝试免费公开接口检索（iTunes Search & 酷狗）
        var onlineResult: OnlineSearchResult? = null
        if (needCover || needArtistOrAlbum) {
            onStatus("正在免费检索 iTunes / 酷狗官方曲库…")
            val cleanTitle = cleanTrackTitle(currentTrack.title)
            val searchArtist = if (currentTrack.isMissingArtist) "" else currentTrack.artist
            onlineResult = searchItunes(cleanTitle, searchArtist)
                ?: searchKugou(cleanTitle, searchArtist)
        }

        // 2. 如果在线免费源检索到了更精准的歌手/专辑/封面
        if (onlineResult != null) {
            var updatedTitle = currentTrack.title
            var updatedArtist = currentTrack.artist
            var updatedAlbum = currentTrack.album

            if (needArtistOrAlbum) {
                if (!onlineResult.artist.isNullOrBlank() && currentTrack.isMissingArtist) {
                    updatedArtist = onlineResult.artist
                    modified = true
                }
                if (!onlineResult.album.isNullOrBlank() && currentTrack.isMissingAlbum) {
                    updatedAlbum = onlineResult.album
                    modified = true
                }
                if (!onlineResult.title.isNullOrBlank() && (currentTrack.title.contains(" - ") || currentTrack.title.startsWith("track", ignoreCase = true))) {
                    updatedTitle = onlineResult.title
                    modified = true
                }
            }

            var updatedArtworkPath = currentTrack.artworkPath
            if (needCover && !onlineResult.artworkUrl.isNullOrBlank()) {
                onStatus("正在下载高分辨率专辑封面…")
                val localCover = downloadAndSaveArtwork(currentTrack.id, onlineResult.artworkUrl)
                if (localCover != null) {
                    updatedArtworkPath = localCover
                    modified = true
                }
            }

            if (modified) {
                currentTrack = currentTrack.copy(
                    title = updatedTitle,
                    artist = updatedArtist,
                    album = updatedAlbum,
                    artworkPath = updatedArtworkPath,
                )
            }
        }

        // 3. 歌词处理（先走本地与 LRCLIB，免 Token）
        var lyricsFound = false
        if (needLyrics) {
            onStatus("正在匹配本地与 LRCLIB 歌词…")
            val existingLyrics = if (!options.forceRefresh) lyricsRepository.readCache(currentTrack) else null
            if (existingLyrics is LyricsFetchResult.Found) {
                lyricsFound = true
            } else {
                val fetched = lyricsRepository.fetch(
                    track = currentTrack,
                    forceRefresh = options.forceRefresh,
                    customLyricsFolder = customLyricsFolder,
                    aiFallback = null, // 先不调用 AI，保持免费
                )
                if (fetched is LyricsFetchResult.Found) {
                    lyricsFound = true
                }
            }
        }

        // 4. DeepSeek AI 兜底（仅在用户开启且有未解决项时调用，节省 Token！）
        val stillNeedsCover = needCover && currentTrack.isMissingCover
        val stillNeedsArtist = needArtistOrAlbum && currentTrack.isMissingArtist
        val stillNeedsLyrics = needLyrics && !lyricsFound

        if (options.useAiFallback && config.isConfigured && config.enabled && (stillNeedsCover || stillNeedsArtist || stillNeedsLyrics)) {
            onStatus("免费源未命中，正在调用 DeepSeek AI 智能推断…")
            val aiResult = deepSeekClient.enrichMetadata(config, currentTrack, needLyrics = stillNeedsLyrics)
            aiCalled = true

            if (aiResult.success) {
                var updatedTitle = currentTrack.title
                var updatedArtist = currentTrack.artist
                var updatedAlbum = currentTrack.album

                if (stillNeedsArtist) {
                    aiResult.artist?.takeIf(String::isNotBlank)?.let {
                        updatedArtist = it
                        modified = true
                    }
                    aiResult.album?.takeIf(String::isNotBlank)?.let {
                        updatedAlbum = it
                        modified = true
                    }
                    aiResult.title?.takeIf(String::isNotBlank)?.let {
                        updatedTitle = it
                        modified = true
                    }
                }

                currentTrack = currentTrack.copy(
                    title = updatedTitle,
                    artist = updatedArtist,
                    album = updatedAlbum,
                )

                // 如果 AI 推断出了新的歌手/歌名，并且仍缺封面，用推断后的信息再在免费官方库搜一次封面
                if (stillNeedsCover) {
                    val secondaryOnline = searchItunes(updatedTitle, updatedArtist)
                        ?: searchKugou(updatedTitle, updatedArtist)
                    if (secondaryOnline?.artworkUrl != null) {
                        val localCover = downloadAndSaveArtwork(currentTrack.id, secondaryOnline.artworkUrl)
                        if (localCover != null) {
                            currentTrack = currentTrack.copy(artworkPath = localCover)
                            modified = true
                        }
                    }
                }

                // 如果 AI 生成了歌词
                if (stillNeedsLyrics && !aiResult.lyrics.isNullOrBlank()) {
                    val cleaned = deepSeekClient.cleanLrcResponse(aiResult.lyrics)
                    val lines = parseLrc(cleaned)
                    val lyricsObj = if (lines.isNotEmpty()) {
                        Lyrics(
                            trackId = currentTrack.id,
                            lines = lines,
                            synchronized = true,
                            instrumental = aiResult.isInstrumental,
                            source = "DeepSeek AI",
                            fromCache = false,
                        )
                    } else {
                        val plainLines = cleaned.lineSequence().map(String::trim).filter(String::isNotEmpty).map { LyricLine(null, it) }.toList()
                        Lyrics(
                            trackId = currentTrack.id,
                            lines = plainLines,
                            synchronized = false,
                            instrumental = aiResult.isInstrumental,
                            source = "DeepSeek AI",
                            fromCache = false,
                        )
                    }
                    lyricsRepository.writeCache(currentTrack, LyricsFetchResult.Found(lyricsObj))
                    lyricsFound = true
                }
            }
        }

        onStatus("处理完成")
        SingleEnrichResult(currentTrack, modified || lyricsFound || aiCalled, aiCalled)
    }

    suspend fun batchEnrich(
        options: BatchEnrichOptions,
        tracks: List<Track>,
        config: DeepSeekConfig,
        customLyricsFolder: String? = null,
        onProgress: (BatchEnrichProgress) -> Unit = {},
    ): BatchEnrichReport = withContext(Dispatchers.IO) {
        var coversEnriched = 0
        var lyricsEnriched = 0
        var metadataEnriched = 0
        var aiCallsCount = 0
        val updatedList = mutableListOf<Track>()

        tracks.forEachIndexed { index, track ->
            val hadCover = !track.isMissingCover
            val hadArtist = !track.isMissingArtist
            val hadAlbum = !track.isMissingAlbum

            onProgress(
                BatchEnrichProgress(
                    current = index + 1,
                    total = tracks.size,
                    currentTrack = track,
                    statusText = "正在分析曲目信息…",
                )
            )

            val enrichResult = enrichTrack(
                track = track,
                options = options,
                config = config,
                customLyricsFolder = customLyricsFolder,
                onStatus = { status ->
                    onProgress(
                        BatchEnrichProgress(
                            current = index + 1,
                            total = tracks.size,
                            currentTrack = track,
                            statusText = status,
                        )
                    )
                }
            )
            val enrichedTrack = enrichResult.track
            if (enrichResult.aiCalled) aiCallsCount++

            if (!hadCover && !enrichedTrack.isMissingCover) coversEnriched++
            if (!hadArtist && !enrichedTrack.isMissingArtist || (!hadAlbum && !enrichedTrack.isMissingAlbum)) metadataEnriched++
            if (options.enrichLyrics && lyricsRepository.readCache(enrichedTrack) is LyricsFetchResult.Found) lyricsEnriched++

            updatedList.add(enrichedTrack)
        }

        val message = buildString {
            append("智能补全完成：处理 ${tracks.size} 首歌曲，")
            if (coversEnriched > 0) append("补全 $coversEnriched 个封面，")
            if (lyricsEnriched > 0) append("获取 $lyricsEnriched 份歌词，")
            if (metadataEnriched > 0) append("修正 $metadataEnriched 条歌手/专辑，")
            append("节省 Token 模式：优先使用免费官方源。")
        }

        BatchEnrichReport(
            totalProcessed = tracks.size,
            coversEnriched = coversEnriched,
            lyricsEnriched = lyricsEnriched,
            metadataEnriched = metadataEnriched,
            aiCallsCount = aiCallsCount,
            updatedTracks = updatedList,
            message = message,
        )
    }

    internal fun searchItunes(title: String, artist: String): OnlineSearchResult? = runCatching {
        val query = if (artist.isNotBlank() && artist != "未知歌手") "$title $artist" else title
        val uri = URI("https://itunes.apple.com/search?term=${encode(query)}&entity=song&limit=5")
        val conn = uri.toURL().openConnection() as HttpURLConnection
        conn.connectTimeout = 6_000
        conn.readTimeout = 8_000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", USER_AGENT)
        val body = conn.inputStream.buffered().use { it.readUpTo(1_000_000).toString(StandardCharsets.UTF_8) }
        val root = JSONObject(body)
        val results = root.optJSONArray("results") ?: return@runCatching null
        if (results.length() == 0) return@runCatching null

        val normTitle = normalize(title)
        val best = (0 until results.length())
            .mapNotNull(results::optJSONObject)
            .firstOrNull { item ->
                val trackName = normalize(item.optString("trackName"))
                trackName.contains(normTitle) || normTitle.contains(trackName)
            } ?: results.optJSONObject(0) ?: return@runCatching null

        val rawArtwork = best.optString("artworkUrl100").takeIf(String::isNotBlank)
        val hdArtwork = rawArtwork?.replace("/100x100bb.jpg", "/600x600bb.jpg")
            ?.replace("/100x100bb.png", "/600x600bb.png")
            ?: rawArtwork

        OnlineSearchResult(
            title = best.optString("trackName").takeIf(String::isNotBlank),
            artist = best.optString("artistName").takeIf(String::isNotBlank),
            album = best.optString("collectionName").takeIf(String::isNotBlank),
            artworkUrl = hdArtwork,
            source = "iTunes",
        )
    }.getOrNull()

    internal fun searchKugou(title: String, artist: String): OnlineSearchResult? = runCatching {
        val query = if (artist.isNotBlank() && artist != "未知歌手") "$title $artist" else title
        val uri = URI("https://complexsearch.kugou.com/v2/search/song?keyword=${encode(query)}&page=1&pagesize=5&plat=0")
        val conn = uri.toURL().openConnection() as HttpURLConnection
        conn.connectTimeout = 6_000
        conn.readTimeout = 8_000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", USER_AGENT)
        val body = conn.inputStream.buffered().use { it.readUpTo(1_000_000).toString(StandardCharsets.UTF_8) }
        val root = JSONObject(body)
        val data = root.optJSONObject("data") ?: return@runCatching null
        val lists = data.optJSONArray("lists") ?: return@runCatching null
        val item = lists.optJSONObject(0) ?: return@runCatching null
        val songName = item.optString("SongName").takeIf(String::isNotBlank)
            ?.replace(Regex("<em>|</em>"), "")
        val singerName = item.optString("SingerName").takeIf(String::isNotBlank)
            ?.replace(Regex("<em>|</em>"), "")
        val albumName = item.optString("AlbumName").takeIf(String::isNotBlank)
            ?.replace(Regex("<em>|</em>"), "")
        val rawImage = item.optString("Image").takeIf(String::isNotBlank)
            ?.replace("{size}", "400")
            ?.replaceFirst("http://", "https://")

        OnlineSearchResult(
            title = songName,
            artist = singerName,
            album = albumName,
            artworkUrl = rawImage,
            source = "Kugou",
        )
    }.getOrNull()

    internal fun downloadAndSaveArtwork(trackId: String, artworkUrl: String): String? = runCatching {
        if (!artworkUrl.startsWith("https://") && !artworkUrl.startsWith("http://")) {
            return artworkUrl
        }
        Files.createDirectories(artworkDirectory)
        val target = artworkDirectory.resolve("$trackId.cover")
        val temporary = target.resolveSibling("$trackId.cover.tmp")

        val connection = URI(artworkUrl).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.inputStream.use { input ->
            Files.newOutputStream(temporary).use { output ->
                input.copyTo(output)
            }
        }
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        target.toAbsolutePath().normalize().toString()
    }.getOrNull()

    internal fun cleanTrackTitle(title: String): String {
        var text = title.replace(Regex("""(?i)\.(mp3|flac|m4a|wav|ape|kgma|kgg|ogg)$"""), "").trim()
        if (text.contains(" - ")) {
            val after = text.substringAfter(" - ").trim()
            if (after.isNotBlank()) text = after
        }
        return text
            .replace(Regex("""(?i)[\[(【（][^\])】）]*(320k|flac|sq|hq|无损|hires|mp3|official|mv|remix|live)[^\])】）]*[\])】）]"""), "")
            .trim()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    private fun normalize(value: String): String = value.lowercase().filter(Char::isLetterOrDigit)

    companion object {
        private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Resonance/${com.resonance.player.model.APP_VERSION}"
    }
}
