package com.resonance.player.features

import com.resonance.player.model.LibraryDestination
import com.resonance.player.platform.DesktopLibraryStore
import com.resonance.player.tutorial.*
import com.resonance.player.ui.TutorialController
import java.nio.file.Files
import kotlin.test.*

class TutorialProgressTest {
    @Test fun welcomeIsOnlyOfferedUntilDismissedAndSurvivesRestart() {
        var saved = TutorialProgress()
        val tutorial = TutorialController { saved = it }
        tutorial.restore(saved)
        tutorial.offerWelcome()
        assertTrue(tutorial.showWelcome)
        tutorial.dismissWelcome()
        val restarted = TutorialController { saved = it }
        restarted.restore(TutorialProgressCodec.decode(TutorialProgressCodec.encode(saved)))
        restarted.offerWelcome()
        assertFalse(restarted.showWelcome)
    }

    @Test fun pauseRestoresExactStepAndOtherChaptersRemainIndependent() {
        var saved = TutorialProgress()
        val chapter = TutorialCatalog.chapters.first()
        val tutorial = TutorialController { saved = it }.apply { restore(saved) }
        tutorial.start(chapter)
        tutorial.event(TutorialCompletion.MusicAvailable)
        tutorial.advance(skipped = true)
        tutorial.pause()
        val restored = TutorialController { saved = it }.apply { restore(TutorialProgressCodec.decode(TutorialProgressCodec.encode(saved))) }
        restored.start(chapter)
        assertEquals("expand", restored.step?.id)
        assertEquals(setOf("import"), saved.chapters[chapter.id]?.completed)
        assertEquals(setOf("play"), saved.chapters[chapter.id]?.skipped)
        restored.start(TutorialCatalog.chapters[1])
        assertEquals(0, restored.stepIndex)
        restored.start(chapter)
        assertEquals(2, restored.stepIndex)
    }

    @Test fun wrongEventsCannotCompleteStepsAndSkippingDoesNotPretendSuccess() {
        var saved = TutorialProgress()
        val tutorial = TutorialController { saved = it }.apply { restore(saved) }
        val chapter = TutorialCatalog.chapters.first()
        tutorial.start(chapter)
        tutorial.event(TutorialCompletion.ChatSent)
        assertEquals(0, tutorial.stepIndex)
        repeat(chapter.steps.size) { tutorial.advance(skipped = true) }
        assertNull(tutorial.activeChapterId)
        assertTrue(saved.chapters.getValue(chapter.id).completed.isEmpty())
        assertEquals(chapter.steps.size, saved.chapters.getValue(chapter.id).skipped.size)
        tutorial.start(chapter)
        assertEquals(0, tutorial.stepIndex)
        assertTrue(saved.chapters.getValue(chapter.id).skipped.isEmpty())
    }

    @Test fun invalidAndFutureProgressCannotEscapeKnownChapterBounds() {
        assertEquals(TutorialProgress(), TutorialProgressCodec.decode("broken"))
        val chapter = TutorialCatalog.chapters.first()
        val restored = TutorialProgressCodec.decode("""{"welcomeDismissed":true,"quickPage":999,"chapters":{"first-song":{"nextStep":999,"completed":["import","unknown"],"skipped":["import","play"]},"unknown":{"nextStep":7}}}""")
        assertTrue(restored.welcomeDismissed)
        assertEquals(TutorialCatalog.quick.lastIndex, restored.quickPage)
        assertEquals(chapter.steps.size, restored.chapters.getValue(chapter.id).nextStep)
        assertEquals(setOf("import"), restored.chapters.getValue(chapter.id).completed)
        assertEquals(setOf("play"), restored.chapters.getValue(chapter.id).skipped)
        assertFalse(restored.chapters.containsKey("unknown"))
    }

    @Test fun tutorialPreferenceUsesAtomicUiStoreAndPreservesPlaybackSettings() {
        val directory = Files.createTempDirectory("tutorial-progress-")
        try {
            val store = DesktopLibraryStore(directory)
            store.writeUiValue("vol.global", "0.35")
            val progress = TutorialProgress(true, 2, false, mapOf("first-song" to ChapterProgress(1, setOf("import"))))
            store.writeUiValue(TutorialProgressCodec.KEY, TutorialProgressCodec.encode(progress))
            val reopened = DesktopLibraryStore(directory)
            assertEquals(progress, TutorialProgressCodec.decode(reopened.readUiValue(TutorialProgressCodec.KEY)))
            assertEquals("0.35", reopened.readUiValue("vol.global"))
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test fun missingTrackRoutesToLibraryWithoutExecutingAnOperation() {
        val tutorial = TutorialController {}.apply { restore(TutorialProgress()) }
        val destinations = mutableListOf<LibraryDestination>()
        tutorial.onNavigate = { destinations += it }
        tutorial.start(TutorialCatalog.chapter("lyrics")!!)
        assertEquals(LibraryDestination.Library, destinations.last())
        assertEquals("display", tutorial.step?.id)
        assertEquals(0, tutorial.stepIndex)
    }
}
