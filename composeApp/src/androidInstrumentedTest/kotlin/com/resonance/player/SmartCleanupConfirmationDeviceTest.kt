package com.resonance.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SmartCleanupConfirmationDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    @Test fun selectedGroupsProduceOneBatchAfterConfirmationAndCancelProducesNone() {
        val songs = (1..2).flatMap { group -> (1..2).map { copy ->
            Track("$group-$copy", "Generated $group", "Fixture", "Album", "3:00", 0,
                sourceUri = "/generated/$group-$copy.mp3")
        } }
        val batches = mutableListOf<List<Track>>()
        var singles = 0
        ui.setContent { ResonanceTheme(reducedMotion = true) {
            Box(Modifier.fillMaxSize()) {
                LibraryShell(LibraryDestination.SmartManager, {}, emptyList(), songs,
                    PlayerState(), songs, LyricsUiState.Idle,
                    onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {},
                    selectedPlaylistId = null, userPlaylists = emptyList(), onPlaylistSelected = {}, onPlaylistMembershipChange = { _, _, _ -> },
                    onRenamePlaylist = { _, _ -> }, onDeletePlaylist = {}, onDeleteLocalTrack = { singles++ }, onToggleFavorite = {}, onRefreshLyrics = {},
                    libraryLocation = "fixture", onCreatePlaylist = {}, onImport = {}, onExportSync = {}, onImportSync = {}, onExitPreview = {},
                    previewMode = false, message = null, operationInProgress = false, onBatchDelete = { batches += it })
            }
        } }
        ui.onNodeWithText("版本与去重 (2)").performClick()
        ui.onNodeWithText("确认删除所选音频 (0 首)").assertIsNotEnabled()
        repeat(2) { index -> ui.onAllNodes(isToggleable())[index].performScrollTo().performClick() }
        ui.onNodeWithText("确认删除所选音频 (2 首)").performClick()
        ui.onNodeWithText("批量删除 2 首歌曲？").assertIsDisplayed()
        ui.runOnIdle { assertTrue(batches.isEmpty()); assertEquals(0, singles) }
        ui.onNodeWithText("取消").performClick()
        ui.runOnIdle { assertTrue(batches.isEmpty()) }
        ui.onNodeWithText("确认删除所选音频 (2 首)").performClick()
        ui.onNodeWithText("确认批量删除").performClick()
        ui.runOnIdle {
            assertEquals(1, batches.size)
            assertEquals(setOf("1", "2"), batches.single().map { it.id.substringBefore('-') }.toSet())
            assertEquals(2, batches.single().size); assertEquals(0, singles)
        }
    }
}
