package com.resonance.player

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.platform.*
import com.resonance.player.tutorial.*
import com.resonance.player.ui.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Proxy
import java.io.File

/** Real shell and real targets, isolated fixtures; never opens a user's library or paid service. */
class TutorialNavigationTest {
    @get:Rule val ui = createComposeRule()
    private val songs = listOf(Track("fixture", "教程测试歌曲", "测试歌手", "测试专辑", "3:00", 1, sourceUri = "fixture://song"))
    private var saved = TutorialProgress()
    private val tutorial = TutorialController { saved = it }
    private lateinit var features: MusicFeatureController
    private fun capture(name: String) {
        val image = ui.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "tutorial-$name.png").outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    private fun show(destination: LibraryDestination = LibraryDestination.Settings, progress: TutorialProgress = TutorialProgress(true), smallViewport: Boolean = false) {
        val state = MutableStateFlow(MusicAssistantState())
        val assistant = Proxy.newProxyInstance(MusicAssistantServices::class.java.classLoader, arrayOf(MusicAssistantServices::class.java)) { _, method, _ ->
            when (method.name) { "getState" -> state; else -> null }
        } as MusicAssistantServices
        val platform = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, _ ->
            when (method.name) { "getMusicAssistant" -> assistant; "getSupportsWebLogin", "getSupportsKgg", "getSupportsKeyboardShortcuts" -> false; else -> null }
        } as PlatformServices
        tutorial.restore(progress)
        ui.setContent {
            val density = LocalDensity.current
            var page by remember { mutableStateOf(destination) }
            val scope = rememberCoroutineScope()
            features = remember { MusicFeatureController(platform, scope).apply { library = songs } }
            features.onNavigate = { page = it }
            tutorial.onNavigate = { page = it }
            tutorial.currentDestination = page
            tutorial.hasTrack = true
            ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                CompositionLocalProvider(LocalMusicFeatures provides features, LocalTutorial provides tutorial,
                    LocalDensity provides Density(density.density, if (smallViewport) 1.5f else density.fontScale)) {
                    Box(Modifier.width(400.dp).then(if (smallViewport) Modifier.height(320.dp) else Modifier.fillMaxHeight())) {
                        LibraryShell(page, { page = it }, emptyList(), songs, PlayerState(currentTrack = songs.first()), songs, LyricsUiState.Idle,
                            onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {},
                            selectedPlaylistId = null, userPlaylists = emptyList(), onPlaylistSelected = {}, onPlaylistMembershipChange = { _, _, _ -> },
                            onRenamePlaylist = { _, _ -> }, onDeletePlaylist = {}, onDeleteLocalTrack = {}, onToggleFavorite = {}, onRefreshLyrics = {},
                            libraryLocation = "fixture", onCreatePlaylist = {}, onImport = {}, onExportSync = {}, onImportSync = {}, onExitPreview = {},
                            previewMode = false, message = null, operationInProgress = false)
                        TutorialDialogs(tutorial, true)
                    }
                }
            }
        }
    }

    @Test fun welcomeCanBeSkippedAndIsNotOfferedAgain() {
        show(progress = TutorialProgress())
        ui.onNodeWithText("欢迎使用 Resonance").assertIsDisplayed()
        ui.onNodeWithText("跳过，稍后再看").performClick()
        ui.runOnIdle { tutorial.offerWelcome(); assertTrue(saved.welcomeDismissed) }
        ui.onNodeWithText("欢迎使用 Resonance").assertDoesNotExist()
        ui.onNodeWithText("使用教程").assertIsDisplayed()
    }

    @Test fun settingsOpensQuickGuideAndAllFourPagesCanBeRead() {
        show()
        ui.onNodeWithText("使用教程").performClick()
        ui.onNodeWithText("开始浏览").performClick()
        ui.onNodeWithText("让音乐来到你的曲库").assertIsDisplayed()
        capture("quick")
        repeat(3) { ui.onNodeWithText("下一步").performScrollTo().performClick(); ui.waitForIdle() }
        ui.onNodeWithText("开始使用").performScrollTo().performClick()
        ui.runOnIdle { assertTrue(saved.quickCompleted) }
        ui.onNodeWithText("你的歌单").assertExists()
    }

    @Test fun spotlightPassesRealClicksAndAdvancesThroughPlayerTabs() {
        show()
        ui.runOnIdle {
            tutorial.start(TutorialCatalog.chapters.first())
            tutorial.advance(skipped = true)
            tutorial.advance(skipped = true)
        }
        ui.onNodeWithContentDescription("打开正在播放详情与歌词").performClick()
        ui.waitForIdle()
        ui.onNodeWithTag("tutorial-target-PlayerLyrics").assertExists().performClick()
        ui.waitForIdle()
        capture("coach")
        ui.onNodeWithTag("tutorial-target-PlayerQueue").assertExists().performClick()
        ui.onNodeWithText("这一章已浏览完").assertIsDisplayed()
        ui.runOnIdle {
            assertEquals(setOf("expand", "lyrics", "queue"), saved.chapters.getValue("first-song").completed)
            assertEquals(setOf("import", "play"), saved.chapters.getValue("first-song").skipped)
        }
    }

    @Test fun pauseAndResumeRetainStepAndDoNotEnableRecommendations() {
        show()
        ui.runOnIdle { tutorial.start(TutorialCatalog.chapter("recommendations")!!) }
        ui.onNodeWithText("已了解，下一步").performClick()
        ui.onNodeWithContentDescription("暂停教学").performClick()
        ui.runOnIdle {
            assertFalse(features.settings.recommendationsEnabled)
            assertEquals(1, saved.chapters.getValue("recommendations").nextStep)
            tutorial.start(TutorialCatalog.chapter("recommendations")!!)
            assertEquals("enable", tutorial.step?.id)
        }
        ui.onNodeWithTag("tutorial-target-RecommendationSettings").assertExists()
        ui.onNodeWithText("跳过本步").performScrollTo().performClick()
        ui.runOnIdle { assertFalse(features.settings.recommendationsEnabled) }
    }

    @Test fun offscreenSettingsTargetIsBroughtIntoView() {
        val progress = TutorialProgress(true, chapters = mapOf("lyrics" to ChapterProgress(2)))
        show(progress = progress)
        ui.runOnIdle { tutorial.start(TutorialCatalog.chapter("lyrics")!!) }
        ui.waitUntil(10000) { runCatching { ui.onNodeWithTag("tutorial-target-SpeechSettings").assertIsDisplayed(); true }.getOrDefault(false) }
        ui.waitForIdle()
        ui.onNodeWithTag("tutorial-target-SpeechSettings").assertIsDisplayed()
        ui.onNodeWithContentDescription("暂停教学").performClick()
        ui.onNodeWithText("使用教程").assertExists()
    }

    @Test fun quickGuideRemainsNavigableWithLargeTextAndShortViewport() {
        show(smallViewport = true)
        ui.runOnIdle { tutorial.quick() }
        repeat(3) { ui.onNodeWithText("下一步").performScrollTo().performClick(); ui.waitForIdle() }
        ui.onNodeWithText("开始使用").performScrollTo().assertIsDisplayed().performClick()
        ui.runOnIdle { assertTrue(saved.quickCompleted) }
    }
}
