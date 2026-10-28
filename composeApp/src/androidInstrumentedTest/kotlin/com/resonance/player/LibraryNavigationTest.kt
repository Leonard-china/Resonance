package com.resonance.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
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
    private var canvas = Color.Black
    private var chatCalls = 0
    private fun show(enabled: Boolean = false, reduced: Boolean = true, mode: ThemeMode = ThemeMode.Light) {
        val state = MutableStateFlow(MusicAssistantState())
        val assistant = Proxy.newProxyInstance(MusicAssistantServices::class.java.classLoader, arrayOf(MusicAssistantServices::class.java)) { _, method, _ ->
            when (method.name) { "getState" -> state; "chat" -> { chatCalls++; null }; else -> null }
        } as MusicAssistantServices
        val platform = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, _ ->
            when (method.name) {
                "getMusicAssistant" -> assistant
                "getSupportsWebLogin", "getSupportsKgg", "getSupportsKeyboardShortcuts" -> false
                "getForeground" -> MutableStateFlow(true)
                "audioFileAvailable" -> true
                else -> null
            }
        } as PlatformServices
        ui.setContent {
            val scope = rememberCoroutineScope()
            val feature = remember { MusicFeatureController(platform, scope).apply { settings = MusicFeatureSettings(recommendationsEnabled = enabled); library = songs } }
            feature.onNavigate = { destination.value = it }
            ResonanceTheme(mode, reducedMotion = reduced) {
                canvas = com.resonance.player.design.ResonanceColors.Canvas
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
    private fun assertTopEdgeRemainsPainted(mode: ThemeMode) {
        ui.mainClock.autoAdvance = false
        show(reduced = false, mode = mode)
        ui.mainClock.advanceTimeBy(400)
        listOf(LibraryDestination.Profile, LibraryDestination.Settings, LibraryDestination.Profile,
            LibraryDestination.Settings, LibraryDestination.Library).forEach { page ->
            ui.runOnIdle { destination.value = page }
            repeat(6) {
                ui.mainClock.advanceTimeBy(16)
                val pixels = ui.onNodeWithTag("main-page-swipe").captureToImage().toPixelMap()
                listOf(0.1f, 0.5f, 0.9f).forEach { x ->
                    val actual = pixels[(pixels.width * x).toInt(), 1]
                    org.junit.Assert.assertEquals(canvas.red, actual.red, 0.035f)
                    org.junit.Assert.assertEquals(canvas.green, actual.green, 0.035f)
                    org.junit.Assert.assertEquals(canvas.blue, actual.blue, 0.035f)
                }
            }
        }
    }
    @Test fun rapidPageChangesKeepTopEdgeCoveredInLightMode() = assertTopEdgeRemainsPainted(ThemeMode.Light)
    @Test fun rapidPageChangesKeepTopEdgeCoveredInDarkMode() = assertTopEdgeRemainsPainted(ThemeMode.Dark)
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
    @Test fun librarySwipesSelectTabsBeforeMainNavigationAndRetainListPosition() {
        show()
        ui.onNodeWithTag("library-page-swipe").performTouchInput {
            swipe(Offset(width * 0.2f, height / 2f), Offset(width * 0.8f, height / 2f))
        }
        ui.onNodeWithText("Song 000").assertIsDisplayed()
        ui.onNode(hasScrollToIndexAction()).performScrollToIndex(31)
        ui.onNodeWithTag("library-page-swipe").performTouchInput {
            swipe(Offset(width * 0.8f, height / 2f), Offset(width * 0.2f, height / 2f))
        }
        ui.onNodeWithText("My playlist").assertIsDisplayed()
        ui.onNodeWithTag("library-page-swipe").performTouchInput {
            swipe(Offset(width * 0.2f, height / 2f), Offset(width * 0.8f, height / 2f))
        }
        ui.onNodeWithText("Song 030").assertIsDisplayed()
        ui.runOnIdle { org.junit.Assert.assertEquals(LibraryDestination.Library, destination.value) }
    }
    @Test fun libraryBoundarySwipeOpensAssistant() {
        show()
        ui.onNodeWithText("歌手").performClick()
        ui.onNodeWithTag("library-page-swipe").performTouchInput {
            swipe(Offset(width * 0.8f, height / 2f), Offset(width * 0.2f, height / 2f))
        }
        ui.onNodeWithText("发送").assertIsDisplayed()
    }
    @Test fun aboutShowsHighlightsForInstalledVersion() {
        show()
        ui.onNodeWithText("工具").performClick()
        ui.onNodeWithText("关于 Resonance").performScrollTo().performClick()
        ui.onNodeWithText("当前版本更新 · v" + APP_VERSION).performScrollTo().assertIsDisplayed()
        APP_RELEASE_HIGHLIGHTS.firstOrNull()?.let { ui.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
    }
    @Test fun assistantReplacesBottomSearchAndHidesMiniPlayer() {
        show()
        ui.onNodeWithContentDescription("打开正在播放详情与歌词").assertIsDisplayed()
        ui.onNodeWithText("搜索").assertDoesNotExist()
        ui.onNodeWithText("音乐助手").performClick()
        ui.onNodeWithContentDescription("打开正在播放详情与歌词").assertDoesNotExist()
        ui.onNodeWithText("发送").assertIsDisplayed()
    }
    @Test fun assistantHeaderKeepsTitleOnOneLineAndActionsVisible() {
        show()
        ui.onNodeWithText("音乐助手").performClick()
        ui.onNodeWithTag("assistant-title").assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { action ->
                val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                org.junit.Assert.assertTrue(action(results))
                org.junit.Assert.assertEquals(1, results.single().lineCount)
            }
        for (label in listOf("新对话", "历史", "任务", "设置")) ui.onNodeWithText(label).assertIsDisplayed()
    }
    @Test fun assistantExampleFillsDraftWithoutSendingRequest() {
        show()
        ui.onNodeWithText("音乐助手").performClick()
        val prompt = "找一些适合夜晚散步的歌"
        ui.onNodeWithText(prompt).performClick()
        ui.onNodeWithTag("assistant-input").assertTextContains(prompt)
        ui.runOnIdle { org.junit.Assert.assertEquals(0, chatCalls) }
    }
    @Test fun disabledRecommendationCardAppearsBeforeUserPlaylistsAndOpensSettings() {
        show()
        val daily = ui.onNodeWithText("每日推荐").fetchSemanticsNode().positionInRoot.y
        val custom = ui.onNodeWithText("My playlist").fetchSemanticsNode().positionInRoot.y
        org.junit.Assert.assertTrue(daily <= custom)
        ui.onNodeWithContentDescription("每日推荐未开启，打开推荐设置").performClick()
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
