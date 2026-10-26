package com.resonance.player.model

data class CleanTagDraft(
    val trackId: String,
    val originalTitle: String,
    val originalArtist: String,
    val originalAlbum: String,
    val suggestedTitle: String,
    val suggestedArtist: String,
    val suggestedAlbum: String,
    val reason: String,
    var selected: Boolean = true,
)

data class DuplicateGroup(
    val key: String, // Normalized artist + title
    val tracks: List<Track>,
    val recommendedToKeepId: String,
    val canReclaimBytesEstimate: Long = 0L,
    var selectedToCleanIds: Set<String> = emptySet(),
)

data class SmartLibraryAuditReport(
    val totalScanned: Int,
    val tagIssueCount: Int,
    val duplicateGroupCount: Int,
    val tagDrafts: List<CleanTagDraft>,
    val duplicateGroups: List<DuplicateGroup>,
    val auditTimestamp: Long = System.currentTimeMillis(),
)
