package com.resonance.player.audio

import com.resonance.player.model.Track
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class DesktopPcmPlayerSessionTest {
    private val track = Track("fixture-a", "Fixture", "", "", "0:10", 0, sourceUri = "synthetic")
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(5)
        assertTrue(condition(), "Playback did not reach the expected state")
    }
    private class Output : PcmOutput {
        var zeroWrites = false
        var pauseOnFirstWrite: (() -> Unit)? = null
        val writes = AtomicInteger()
        val frames = java.util.concurrent.atomic.AtomicLong()
        @Volatile var started = false
        var closeGate: CountDownLatch? = null
        val closed = CountDownLatch(1)
        override val available get() = 4096
        override val framePosition get() = frames.get()
        override fun start() { started = true }
        override fun stop() { started = false }
        override fun write(bytes: ByteArray, offset: Int, length: Int): Int {
            writes.incrementAndGet()
            if (zeroWrites) return 0
            assertTrue(length in 4..2048 && length % 4 == 0)
            val count = if (writes.get() == 1 && pauseOnFirstWrite != null) { pauseOnFirstWrite!!.invoke(); 4 } else length
            frames.addAndGet((count / 4).toLong())
            return count
        }
        override fun close() { closeGate?.await(2, TimeUnit.SECONDS); closed.countDown() }
    }
    private class Backend(val output: Output = Output(), val bytes: ByteArray = ByteArray(16384)) : DesktopAudioBackend {
        var readGate: CountDownLatch? = null
        val opened = CountDownLatch(1)
        val cancelled = AtomicInteger()
        override fun decode(track: Track, offset: Double, speed: Double) = object : DecodedPcm {
            override val input = object : InputStream() {
                private val source = ByteArrayInputStream(bytes)
                override fun read(): Int = source.read()
                override fun read(b: ByteArray, off: Int, len: Int): Int { readGate?.await(2, TimeUnit.SECONDS); return source.read(b, off, len) }
            }
            override fun cancel() { cancelled.incrementAndGet(); readGate?.countDown() }
            override fun verifyCompleted() {}
            override fun close() { cancel() }
        }
        override fun output(): PcmOutput { opened.countDown(); return output }
    }
    @Test fun bufferingEndsOnlyWhenPcmArrivesRatherThanWhenDeviceOpens() {
        val backend = Backend().apply { readGate = CountDownLatch(1) }
        val events = CopyOnWriteArrayList<DesktopPlaybackEvent>()
        val player = DesktopPcmPlayer(backend, 150, events::add)
        try {
            player.play(track)
            assertTrue(backend.opened.await(2, TimeUnit.SECONDS))
            assertTrue(player.snapshot().isBuffering)
            assertFalse(player.isPrepared(track.id))
            assertFalse(events.any { it.kind == DesktopPlaybackEvent.Kind.Playing })
            backend.readGate!!.countDown()
            await { events.any { it.kind == DesktopPlaybackEvent.Kind.Ended } }
            assertTrue(events.any { it.kind == DesktopPlaybackEvent.Kind.Playing && it.snapshot.isPlaying })
        } finally { player.close(); backend.readGate!!.countDown() }
    }
    @Test fun outputThatMakesNoProgressBacksOffAndReportsOneBoundedFailure() {
        val backend = Backend().apply { output.zeroWrites = true }
        val events = CopyOnWriteArrayList<DesktopPlaybackEvent>()
        val player = DesktopPcmPlayer(backend, 100, events::add)
        try {
            player.play(track)
            await { events.any { it.kind == DesktopPlaybackEvent.Kind.Error } }
            assertTrue(backend.output.writes.get() <= 30, "A stalled device must not busy-spin")
            assertEquals(1, events.count { it.kind == DesktopPlaybackEvent.Kind.Error })
            assertFalse(events.any { it.kind == DesktopPlaybackEvent.Kind.Ended })
        } finally { player.close() }
    }
    @Test fun nativeCleanupDoesNotBlockStopOrTheNextPlayCommand() {
        val backend = Backend().apply { output.zeroWrites = true; output.closeGate = CountDownLatch(1) }
        val events = CopyOnWriteArrayList<DesktopPlaybackEvent>()
        val player = DesktopPcmPlayer(backend, 100, events::add)
        try {
            player.play(track)
            assertTrue(backend.opened.await(2, TimeUnit.SECONDS))
            val start = System.nanoTime()
            player.close()
            player.play(track.copy(id = "fixture-b"), false)
            assertTrue(System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(200))
            assertEquals("fixture-b", player.snapshot().currentTrack?.id)
        } finally { player.close(); backend.output.closeGate!!.countDown() }
    }
    @Test fun replacementRejectsAnOldDecoderWhichFinishesAfterTheNewRequest() {
        val backend = Backend().apply { readGate = CountDownLatch(1) }
        val events = CopyOnWriteArrayList<DesktopPlaybackEvent>()
        val player = DesktopPcmPlayer(backend, 100, events::add)
        try {
            player.play(track)
            assertTrue(backend.opened.await(2, TimeUnit.SECONDS))
            val old = player.currentRequestId
            player.play(track.copy(id = "fixture-b"))
            val current = player.currentRequestId
            await { events.any { it.requestId == current && it.kind == DesktopPlaybackEvent.Kind.Ended } }
            assertNotEquals(old, current)
            assertFalse(events.any { it.requestId == old && it.kind in listOf(DesktopPlaybackEvent.Kind.Error, DesktopPlaybackEvent.Kind.Ended) })
        } finally { player.close(); backend.readGate!!.countDown() }
    }
    @Test fun pausingDuringAPartialWriteStopsRetriesUntilResume() {
        val backend = Backend()
        val events = CopyOnWriteArrayList<DesktopPlaybackEvent>()
        val player = DesktopPcmPlayer(backend, 100, events::add)
        backend.output.pauseOnFirstWrite = { player.setPlaying(false) }
        try {
            player.play(track)
            await { backend.output.writes.get() > 0 && !player.snapshot().isPlaying }
            Thread.sleep(150)
            assertEquals(1, backend.output.writes.get(), "Paused partial writes must not spin on a stopped line")
            assertFalse(events.any { it.kind == DesktopPlaybackEvent.Kind.Error })
            player.setPlaying(true)
            await { events.any { it.kind == DesktopPlaybackEvent.Kind.Ended } }
        } finally { player.close() }
    }
    @Test fun preparedPausedPlaybackWaitsForResumeAndIsNotAStalledDevice() {
        val backend = Backend()
        val events = CopyOnWriteArrayList<DesktopPlaybackEvent>()
        val player = DesktopPcmPlayer(backend, 100, events::add)
        try {
            player.play(track, false)
            await { player.isPrepared(track.id) }
            Thread.sleep(150)
            assertEquals(0, backend.output.writes.get())
            assertFalse(events.any { it.kind == DesktopPlaybackEvent.Kind.Error })
            player.setPlaying(true)
            await { events.any { it.kind == DesktopPlaybackEvent.Kind.Ended } }
        } finally { player.close() }
    }
}
