package com.resonance.player.model

object SmartLibraryAuditEngine {

    private val trashPatterns = listOf(
        Regex("^\\[[^\\]]+\\]\\s*"), // [mqms2], [320k]
        Regex("^【[^】]+】\\s*"),     // 【无损音质】
        Regex("^(kw|kg|wy|tx|128k|320k|flac|sq|hq)_+", RegexOption.IGNORE_CASE),
        Regex("\\.(mp3|flac|m4a|wav|aac|ogg|opus)$", RegexOption.IGNORE_CASE),
        Regex("_+(128|320)k$", RegexOption.IGNORE_CASE),
    )

    fun audit(tracks: List<Track>): SmartLibraryAuditReport {
        val tagDrafts = mutableListOf<CleanTagDraft>()
        val groupedByKey = mutableMapOf<String, MutableList<Track>>()

        for (track in tracks) {
            // Check Tag Issues
            var cleanTitle = track.title.trim()
            var cleanArtist = track.artist.trim()
            var cleanAlbum = track.album.trim()
            var issueFound = false
            val reasons = mutableListOf<String>()

            // 1. Strip trash patterns
            for (pattern in trashPatterns) {
                if (pattern.containsMatchIn(cleanTitle)) {
                    cleanTitle = cleanTitle.replace(pattern, "").trim()
                    issueFound = true
                    reasons.add("清除垃圾前缀或扩展名")
                }
            }

            // 2. Parse "Artist - Title" format in title
            if (cleanTitle.contains(" - ") && (track.isMissingArtist || cleanArtist == "未知歌手")) {
                val parts = cleanTitle.split(" - ", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    cleanArtist = parts[0].trim()
                    cleanTitle = parts[1].trim()
                    issueFound = true
                    reasons.add("分离标题中混杂的歌手名")
                }
            }

            // 3. Unknown artist / album cleanup
            if (track.isMissingArtist && cleanArtist.isBlank()) {
                cleanArtist = "未知歌手"
            }
            if (track.isMissingAlbum) {
                cleanAlbum = if (cleanTitle.isNotBlank()) "单曲 - $cleanTitle" else "未知专辑"
                issueFound = true
                reasons.add("规范未知专辑")
            }

            if (issueFound && (cleanTitle != track.title || cleanArtist != track.artist || cleanAlbum != track.album)) {
                tagDrafts.add(
                    CleanTagDraft(
                        trackId = track.id,
                        originalTitle = track.title,
                        originalArtist = track.artist,
                        originalAlbum = track.album,
                        suggestedTitle = cleanTitle,
                        suggestedArtist = cleanArtist,
                        suggestedAlbum = cleanAlbum,
                        reason = reasons.distinct().joinToString("，"),
                        selected = true,
                    )
                )
            }

            // Group for duplicate check
            val normKey = normalizeTrackKey(cleanArtist, cleanTitle)
            groupedByKey.getOrPut(normKey) { mutableListOf() }.add(track)
        }

        // Duplicate groups
        val duplicateGroups = mutableListOf<DuplicateGroup>()
        for ((key, groupTracks) in groupedByKey) {
            if (groupTracks.size > 1) {
                // Find best track to keep (prefer FLAC > M4A > MP3, prefer favorite, prefer with cover)
                val sorted = groupTracks.sortedWith(
                    compareByDescending<Track> { it.mimeType.contains("flac", ignoreCase = true) }
                        .thenByDescending { it.isFavorite }
                        .thenByDescending { !it.isMissingCover }
                        .thenByDescending { !it.isMissingAlbum }
                )
                val keepId = sorted.first().id
                val toClean = sorted.drop(1).map { it.id }.toSet()
                duplicateGroups.add(
                    DuplicateGroup(
                        key = key,
                        tracks = groupTracks,
                        recommendedToKeepId = keepId,
                        selectedToCleanIds = toClean,
                    )
                )
            }
        }

        return SmartLibraryAuditReport(
            totalScanned = tracks.size,
            tagIssueCount = tagDrafts.size,
            duplicateGroupCount = duplicateGroups.size,
            tagDrafts = tagDrafts,
            duplicateGroups = duplicateGroups,
        )
    }

    private fun normalizeTrackKey(artist: String, title: String): String {
        val a = artist.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
        val t = title.lowercase().replace(Regex("\\(.*?\\)|（.*?）|\\[.*?\\]"), "").replace(Regex("[^\\p{L}\\p{N}]"), "")
        return "$a::$t"
    }
}
