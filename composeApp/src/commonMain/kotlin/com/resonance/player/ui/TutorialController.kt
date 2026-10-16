package com.resonance.player.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import com.resonance.player.model.LibraryDestination
import com.resonance.player.tutorial.*

internal data class TutorialAnchor(val owner: Any, val bounds: Rect)

/** UI-only navigation. Advancing never calls music, authentication or paid services. */
class TutorialController(private val persist: (TutorialProgress) -> Unit) {
    var progress by mutableStateOf(TutorialProgress()); private set
    var loaded by mutableStateOf(false); private set
    var showWelcome by mutableStateOf(false); private set
    var quickMode by mutableStateOf(false); private set
    var readingChapterId by mutableStateOf<String?>(null)
    var activeChapterId by mutableStateOf<String?>(null); private set
    var locationRequest by mutableStateOf(0); private set
    var finishedChapterId by mutableStateOf<String?>(null)
    var currentDestination by mutableStateOf(LibraryDestination.Library)
    var hasTrack by mutableStateOf(false)
    var onNavigate: (LibraryDestination) -> Unit = {}
    internal val anchors = mutableStateMapOf<TutorialTarget, TutorialAnchor>()
    private var welcomeChecked = false

    val chapter get() = TutorialCatalog.chapter(activeChapterId)
    val stepIndex get() = progress.chapters[activeChapterId]?.nextStep ?: 0
    val step get() = chapter?.steps?.getOrNull(stepIndex)

    fun restore(value: TutorialProgress) { progress = value; loaded = true }
    fun offerWelcome() {
        if (loaded && !welcomeChecked) {
            welcomeChecked = true
            showWelcome = !progress.welcomeDismissed
        }
    }
    private fun update(value: TutorialProgress) { progress = value; persist(value) }
    fun dismissWelcome() { showWelcome = false; update(progress.copy(welcomeDismissed = true)) }
    fun directory() {
        dismissWelcome(); activeChapterId = null; quickMode = false; readingChapterId = null
        onNavigate(LibraryDestination.Tutorial)
    }
    fun quick(restart: Boolean = false) {
        dismissWelcome(); activeChapterId = null; quickMode = true; readingChapterId = null
        if (restart) update(progress.copy(quickPage = 0))
        onNavigate(LibraryDestination.Tutorial)
    }
    fun quickPage(index: Int) = update(progress.copy(quickPage = index.coerceIn(TutorialCatalog.quick.indices)))
    fun finishQuick() { update(progress.copy(quickCompleted = true)); quickMode = false; onNavigate(LibraryDestination.Library) }
    fun start(chapter: TutorialChapter, restart: Boolean = false) {
        dismissWelcome(); quickMode = false; readingChapterId = null; finishedChapterId = null
        val saved = progress.chapters[chapter.id]
        if (restart || (saved?.nextStep ?: 0) >= chapter.steps.size) {
            update(progress.copy(chapters = progress.chapters + (chapter.id to ChapterProgress())))
        }
        activeChapterId = chapter.id
        locate()
    }
    fun locate() {
        val current = step ?: return
        locationRequest++
        val page = if (current.needsTrack && !hasTrack) LibraryDestination.Library else current.destination
        onNavigate(if (page == LibraryDestination.Playing) LibraryDestination.Library else page)
    }
    fun advance(skipped: Boolean = false) {
        val current = chapter ?: return
        update(progress.record(current, stepIndex, skipped))
        if (stepIndex >= current.steps.size) {
            finishedChapterId = current.id; activeChapterId = null; quickMode = false
            onNavigate(LibraryDestination.Tutorial)
        } else locate()
    }
    fun event(event: TutorialCompletion) { if (step?.completion == event) advance() }
    fun pause() { activeChapterId = null; quickMode = false; readingChapterId = null; onNavigate(LibraryDestination.Tutorial) }
}

val LocalTutorial = staticCompositionLocalOf<TutorialController?> { null }
