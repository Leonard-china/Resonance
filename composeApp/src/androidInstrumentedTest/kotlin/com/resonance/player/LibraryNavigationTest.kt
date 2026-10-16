package com.resonance.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.platform.*
import com.resonance.player.ui.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Proxy

/** Exercise the actual shell with a far-away playing row and many library entries. No user files. */
class LibraryNavigationTest {
    @get:Rule val ui = createComposeRule()
    private val songs = (0..299).map { Track("$it", "Song %03d".format(it), "Artist", "Album", "3:00", it, sourceUri = "fixture://$it") }
    private val destination = mutableStateOf(LibraryDestination.Library)
    private fun show(enabled: Boolean = false, reduced: Boolean = true) {
        val state = MutableStateFlow(MusicAssistantState())
        val assistant = Proxy.newProxyInstance(MusicAssistantServices::class.java.classLoader, arrayOf(MusicAssistantServices::class.java)) { _, method, _ ->
            when (method.name) { "getState" -> state; else -> null }
        } as MusicAssistantServices
        val platform = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, _ ->
            when (method.name) {
                "getMusicAssistant" -> assistant
                "getSupportsWebLogin", "getSupportsKgg" -> false
                else -> null
            }
        } as PlatformServices
        ui.setContent {
            val scope = rememberCoroutineScope()
            val feature = remember { MusicFeatureController(platform, scope).apply { settings = MusicFeatureSettings(recommendationsEnabled = enabled); library = songs } }
            feature.onNavigate = { destination.value = it }
            ResonanceTheme(ThemeMode.Light, reducedMotion = reduced) {
                CompositionLocalProvider(LocalMusicFeatures provides feature) {
                    Box(Modifier.width(400.dp).fillMaxHeight()) {
                        LibraryShell(destination.value, { destination.value = it },
                            listOf(Playlist("mine", "My playlist", "", songs, 1)), songs,
                            PlayerState(currentTrack = songs.last()), songs, LyricsUiState.Idle,
                            onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {},
                            selectedPlaylistId = null, userPlaylists = emptyList(), onPlaylistSelected = {}, onPlaylistMembershipChange = { _, _, _ -> },
                            onRenamePlaylist = { _, _ -> }, onDeletePlaylist = {}, onDeleteLocalTrack = {}, onToggleFavorite = {}, onRefreshLyrics = {},
                            libraryLocation = "fixture", onCreatePlaylist = {}, onImport = {}, onExportSync = {}, onImportSync = {}, onExitPreview = {},
                            previewMode = false, message = null, operationInProgress = false)
                    }
                }
            }
        }
    }
    @Test fun enteringSongsDoesNotJumpToPlayingTrackAndTabScrollPositionIsRetained() {
        show()
        ui.onNodeWithText("歌曲").performClick()
        ui.onNodeWithText("Song 000").assertIsDisplayed()
        ui.onNode(hasScrollToIndexAction()).performScrollToIndex(31)
        ui.onNodeWithText("Song 030").assertIsDisplayed()
        ui.onNodeWithText("专辑").performClick()
        ui.onNodeWithText("歌曲").performClick()
        ui.onNodeWithText("Song 030").assertIsDisplayed()
    }
    @Test fun assistantReplacesBottomSearchAndHidesMiniPlayer() {
        show()
        ui.onNodeWithContentDescription("打开正在播放详情与歌词").assertIsDisplayed()
        ui.onNodeWithText("搜索").assertDoesNotExist()
        ui.onNodeWithText("音乐助手").performClick()
        ui.onNodeWithContentDescription("打开正在播放详情与歌词").assertDoesNotExist()
        ui.onNodeWithText("发送").assertIsDisplayed()
    }
    @Test fun disabledRecommendationCardAppearsBeforeUserPlaylistsAndOpensSettings() {
        show()
        val daily = ui.onNodeWithText("每日推荐").fetchSemanticsNode().positionInRoot.y
        val custom = ui.onNodeWithText("My playlist").fetchSemanticsNode().positionInRoot.y
        org.junit.Assert.assertTrue(daily <= custom)
        ui.onNodeWithText("未开启推荐 · 点击设置").performClick()
        ui.onNodeWithText("自动推荐并下载").assertExists()
    }
    @Test fun enabledRecommendationCardOpensItsOwnPlaylistPage() {
        show(enabled = true)
        ui.onNodeWithContentDescription("打开每日推荐").performClick()
        ui.onNodeWithText("更新推荐").assertIsDisplayed()
    }
    @Test fun playerCanExpandAndCloseRepeatedlyWithoutLeavingAnOverlay() {
        show(reduced = false)
        repeat(3) {
            ui.onNodeWithContentDescription("打开正在播放详情与歌词").performClick()
            ui.onNodeWithContentDescription("关闭正在播放").assertIsDisplayed().performClick()
            ui.onNodeWithContentDescription("关闭正在播放").assertDoesNotExist()
            ui.onNodeWithContentDescription("打开正在播放详情与歌词").assertIsDisplayed()
        }
    }
}
