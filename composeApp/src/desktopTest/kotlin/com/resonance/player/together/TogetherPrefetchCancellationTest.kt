package com.resonance.player.together

import com.resonance.player.model.*
import com.resonance.player.platform.TogetherStatus
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import kotlin.test.*

class TogetherPrefetchCancellationTest {
    @Test fun networkChangeOrExplicitDisableRetiresPartialPrefetchWithoutDroppingCurrentSong() = runBlocking {
        for (networkChange in listOf(true, false)) {
            val root = Files.createTempDirectory("together-prefetch-stop-")
            val allowed = AtomicBoolean(true)
            val readNext = AtomicInteger()
            lateinit var host: JvmListenTogetherService
            fun track(id: String) = Track(id, "原创 $id", "Resonance", "", "4:00", 1, sourceUri = "fixture-$id")
            host = JvmListenTogetherService(root.resolve("host"), { track ->
                if (track.id == "a") ByteArrayInputStream(ByteArray(4096) { 1 }) else object : InputStream() {
                    override fun read(): Int = error("Use bounded reads")
                    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                        val read = readNext.addAndGet(length)
                        if (read >= 65_536) { if (networkChange) allowed.set(false) else host.enablePrefetch(false) }
                        java.util.Arrays.fill(bytes, offset, offset + length, 2.toByte())
                        return length
                    }
                }
            }, prefetchNetworkAllowed = { allowed.get() })
            val guest = JvmListenTogetherService(root.resolve("guest"), { error("Guest never reads local songs") })
            try {
                host.enablePrefetch(true); host.publishQueue(listOf(track("a"), track("b")))
                host.publishPlayback(PlayerState(currentTrack = track("a")))
                host.host(0); guest.join(host.invitation("127.0.0.1"))
                withTimeout(8_000) { guest.remotePlayback.first { it?.track?.title == "原创 a" } }
                withTimeout(8_000) { guest.state.first { it.nextTrackTitle == "原创 b" && !it.prefetching } }
                assertTrue(readNext.get() in 65_536..98_304)
                assertEquals(TogetherStatus.Connected, guest.state.value.status)
                Files.list(root.resolve("guest")).use { paths -> assertFalse(paths.anyMatch { it.fileName.toString().endsWith(".part") }) }
                host.publishPlayback(PlayerState(currentTrack = track("a"), isPlaying = true))
                withTimeout(3_000) { guest.remotePlayback.first { it?.playing == true } }
            } finally { guest.close(); host.close() }
        }
    }
}
