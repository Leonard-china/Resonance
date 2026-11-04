package com.resonance.player.together

import com.resonance.player.model.PlayerState
import com.resonance.player.model.Track
import com.resonance.player.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.ByteArrayInputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class TogetherSessionTest {
    @Test fun thirtyJoinLeaveCyclesRetireOwnedFilesAndPreserveOtherCacheFiles() = runBlocking<Unit> {
        val root = Files.createTempDirectory("together-cycles-")
        val host = host(root.resolve("host")); val guestDirectory = root.resolve("guest")
        Files.createDirectories(guestDirectory)
        val sentinel = guestDirectory.resolve("unrelated-user-file")
        Files.writeString(sentinel, "preserve")
        val guest = guest(guestDirectory)
        try {
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.host(0)
            repeat(30) {
                guest.join(host.invitation("127.0.0.1"))
                val frame = withTimeout(5_000) { guest.remotePlayback.first { it?.track != null } }!!
                val file = java.nio.file.Paths.get(frame.track!!.sourceUri!!)
                assertTrue(Files.exists(file))
                guest.leave()
                withTimeout(5_000) { while (Files.exists(file)) delay(20) }
                withTimeout(5_000) { host.state.first { it.status == TogetherStatus.Listening } }
                assertEquals("preserve", Files.readString(sentinel))
            }
        } finally { guest.close(); host.close() }
    }
    @Test fun lyricsArriveBoundedWithoutExposingHostTrackIdsOrPaths() = runBlocking<Unit> {
        val root = Files.createTempDirectory("together-lyrics-")
        val host = host(root.resolve("host")); val guest = guest(root.resolve("guest"))
        try {
            val lyrics = com.resonance.player.model.Lyrics("a", List(1000) { com.resonance.player.model.LyricLine(it * 1000L, "原".repeat(200), "译".repeat(200)) }, true, false, "private-path", offsetMs = 300)
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.publishLyrics("a", lyrics)
            host.host(0); guest.join(host.invitation("127.0.0.1"))
            val received = withTimeout(8_000) { guest.remoteLyrics.first { !it?.trackId.isNullOrEmpty() } }!!
            assertTrue(received.trackId.startsWith("together:"))
            assertEquals(300, received.offsetMs)
            assertTrue(received.lines.size < 320)
            assertEquals(128, received.lines.first().text.length)
            assertEquals("朋友分享（歌词节选）", received.source)
            guest.leave(); assertNull(guest.remoteLyrics.value)
        } finally { guest.close(); host.close() }
    }
    @Test fun invitationInspectionAndQrAreLocalStrictAndRoundTrip() = runBlocking<Unit> {
        val host = host(Files.createTempDirectory("together-qr-"))
        try {
            host.host(0)
            val invitation = host.invitation("192.0.2.1")
            assertEquals("192.0.2.1", host.inspectInvitation(invitation)?.host)
            assertNull(host.inspectInvitation(invitation + "&extra=value"))
            assertNull(host.inspectInvitation("https://example.com"))
            val qr = assertNotNull(host.invitationQr(invitation))
            val scale = 5
            val width = qr.size * scale
            val pixels = IntArray(width * width) { index -> if (qr.bits[(index / width / scale) * qr.size + index % width / scale]) 0xff000000.toInt() else -1 }
            val bitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(com.google.zxing.RGBLuminanceSource(width, width, pixels)))
            assertEquals(invitation, com.google.zxing.qrcode.QRCodeReader().decode(bitmap).text)
            assertEquals(TogetherStatus.Listening, host.state.value.status)
        } finally { host.close() }
    }

    @Test fun evictedPreviousTrackIsRequestedAgainWithoutLosingCurrentAudio() = runBlocking<Unit> {
        val root = Files.createTempDirectory("together-cache-revisit-")
        val reads = AtomicInteger()
        val host = host(root.resolve("host"), reads)
        val guest = guest(root.resolve("guest"))
        try {
            host.publishQueue(listOf(track("a"), track("b"), track("c")))
            host.publishPlayback(PlayerState(currentTrack = track("a")))
            host.host(0); guest.join(host.invitation("127.0.0.1"))
            for (id in listOf("a", "b", "c", "a")) {
                host.publishPlayback(PlayerState(currentTrack = track(id)))
                val frame = withTimeout(8_000) { guest.remotePlayback.first { it?.track?.title == "原创歌曲 $id" } }!!
                assertTrue(Files.exists(java.nio.file.Paths.get(frame.track!!.sourceUri!!)))
            }
            assertEquals(4, reads.get())
        } finally { guest.close(); host.close() }
    }
    private fun track(id: String = "a") = Track(id, "原创歌曲 $id", "Resonance", "测试", "4:00", 7, sourceUri = "private-fixture-$id")
    private fun host(root: java.nio.file.Path, reads: AtomicInteger = AtomicInteger(), ttl: Long = 600_000, prefetchAllowed: (suspend () -> Boolean)? = null) =
        JvmListenTogetherService(root, { reads.incrementAndGet(); ByteArrayInputStream(ByteArray(4096) { 42 }) }, invitationTtlMs = ttl, prefetchNetworkAllowed = prefetchAllowed)
    private fun guest(root: java.nio.file.Path) = JvmListenTogetherService(root, { error("Guests never expose local files") })

    @Test fun hostApprovalPrecedesAnyAudioReadAndCanReject() = runBlocking<Unit> {
        val root = Files.createTempDirectory("together-approval-")
        val reads = AtomicInteger()
        val host = host(root.resolve("host"), reads)
        val guest = guest(root.resolve("guest"))
        try {
            host.requireJoinApproval(true)
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.host(0)
            val joining = async(Dispatchers.IO) { guest.join(host.invitation("127.0.0.1")) }
            withTimeout(5_000) { host.state.first { it.pendingJoin } }
            assertEquals(0, reads.get())
            host.approveJoin(false)
            joining.await()
            assertEquals(TogetherStatus.Error, guest.state.value.status)
            assertEquals(0, reads.get())
            withTimeout(5_000) { host.state.first { it.status == TogetherStatus.Listening } }
            val approved = async(Dispatchers.IO) { guest.join(host.invitation("127.0.0.1")) }
            withTimeout(5_000) { host.state.first { it.pendingJoin } }
            host.approveJoin(true)
            approved.await()
            withTimeout(5_000) { guest.remotePlayback.first { it?.track != null } }
            assertEquals(1, reads.get())
        } finally { guest.close(); host.close() }
    }

    @Test fun refreshedAndExpiredInvitationsCannotStartTransfers() = runBlocking<Unit> {
        val root = Files.createTempDirectory("together-expiry-")
        val reads = AtomicInteger()
        val host = host(root.resolve("host"), reads, 2_000)
        val guest = guest(root.resolve("guest"))
        try {
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.host(0)
            val retired = host.invitation("127.0.0.1")
            host.renewInvitation()
            guest.join(retired)
            assertEquals(TogetherStatus.Error, guest.state.value.status)
            assertEquals(0, reads.get())
            host.renewInvitation()
            val expired = host.invitation("127.0.0.1")
            delay(2_200)
            guest.join(expired)
            assertEquals(TogetherStatus.Error, guest.state.value.status)
            assertEquals(0, reads.get())
            host.renewInvitation()
            guest.join(host.invitation("127.0.0.1"))
            val frame = withTimeout(5_000) { guest.remotePlayback.first { it?.track != null } }
            host.renewInvitation()
            assertNotNull(frame?.track)
            assertNotNull(guest.remotePlayback.value?.track)
        } finally { guest.close(); host.close() }
    }

    @Test fun sharedQueueUsesOpaqueIdsAndMembersCanControlWithoutApproval() = runBlocking<Unit> {
        val root = Files.createTempDirectory("together-requests-")
        val reads = AtomicInteger()
        val host = host(root.resolve("host"), reads)
        val guest = guest(root.resolve("guest"))
        try {
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.publishQueue(listOf(track(), track("b")))
            host.host(0)
            guest.join(host.invitation("127.0.0.1"))
            val queue = withTimeout(5_000) { guest.state.first { it.sharedQueue.size == 2 }.sharedQueue }
            withTimeout(5_000) { guest.remotePlayback.first { it?.track != null } }
            assertTrue(queue.all { it.id.matches(Regex("[a-f0-9-]{36}")) && !it.id.contains("private-fixture") })
            val accepted = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(5_000) { host.approvedActions.first() } }
            guest.requestControl(TogetherControl.Select, queue[1].id)
            assertEquals("b", accepted.await().trackId)
            assertEquals(1, reads.get()) // Requesting a song does not read or transfer it by itself.
            assertTrue(host.state.value.requests.isEmpty())
        } finally { guest.close(); host.close() }
    }

    @Test fun prefetchedNextTrackIsVerifiedAndReusedWithoutAnotherRead() = runBlocking<Unit> {
        val root = Files.createTempDirectory("together-prefetch-")
        val reads = AtomicInteger()
        val host = host(root.resolve("host"), reads)
        val guest = guest(root.resolve("guest"))
        try {
            host.publishQueue(listOf(track(), track("b")))
            host.enablePrefetch(true)
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.host(0)
            guest.join(host.invitation("127.0.0.1"))
            withTimeout(5_000) { guest.remotePlayback.first { it?.track?.title == track().title } }
            withTimeout(5_000) { guest.state.first { it.nextTrackTitle == track("b").title && !it.prefetching } }
            assertEquals(2, reads.get())
            host.publishPlayback(PlayerState(currentTrack = track("b"), progress = .3f))
            val frame = withTimeout(3_000) { guest.remotePlayback.first { it?.track?.title == track("b").title } }!!
            assertContentEquals(ByteArray(4096) { 42 }, Files.readAllBytes(java.nio.file.Paths.get(frame.track!!.sourceUri!!)))
            delay(700)
            assertEquals(2, reads.get())
        } finally { guest.close(); host.close() }
    }

    @Test fun temporaryNetworkBreakRecoversWithSeparateCredential() = runBlocking<Unit> {
        val root = Files.createTempDirectory("together-recovery-")
        val host = host(root.resolve("host"))
        val guest = guest(root.resolve("guest"))
        val links = CopyOnWriteArrayList<Socket>()
        val proxy = ServerSocket(0)
        try {
            host.publishPlayback(PlayerState(currentTrack = track(), isPlaying = true, progress = .3f))
            host.host(0)
            val forward = launch(Dispatchers.IO) {
                try {
                    while (isActive) {
                        val left = proxy.accept()
                        val right = Socket("127.0.0.1", host.state.value.port)
                        links += left; links += right
                        launch { runCatching { left.getInputStream().copyTo(right.getOutputStream()) } }
                        launch { runCatching { right.getInputStream().copyTo(left.getOutputStream()) } }
                    }
                } catch (_: java.io.IOException) { }
            }
            guest.join(host.invitation("127.0.0.1", proxy.localPort))
            val first = withTimeout(7_000) { guest.remotePlayback.first { it?.track != null } }!!
            host.renewInvitation() // Recovery is independent of the now-revoked original invitation.
            val recovering = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(7_000) { guest.state.first { it.status == TogetherStatus.Reconnecting } } }
            links.toList().forEach { it.close() }
            recovering.await()
            withTimeout(12_000) { guest.state.first { it.status == TogetherStatus.Connected } }
            val second = withTimeout(12_000) { guest.remotePlayback.first { it?.track != null && it.sequence > first.sequence } }!!
            assertContentEquals(ByteArray(4096) { 42 }, Files.readAllBytes(java.nio.file.Paths.get(second.track!!.sourceUri!!)))
            guest.leave()
            delay(1_500)
            assertEquals(TogetherStatus.Idle, guest.state.value.status)
            proxy.close(); links.forEach { it.close() }; forward.cancelAndJoin()
        } finally { proxy.close(); links.forEach { runCatching { it.close() } }; guest.close(); host.close() }
    }
    @Test fun dynamicMeteredNetworkPreventsAndCancelsPrefetch() = runBlocking<Unit> {
        val root = Files.createTempDirectory("together-metered-")
        val reads = AtomicInteger()
        var allowPrefetch = false
        val host = host(root.resolve("host"), reads, prefetchAllowed = { allowPrefetch })
        val guest = guest(root.resolve("guest"))
        try {
            host.enablePrefetch(true)
            host.publishQueue(listOf(track("a"), track("b")))
            host.publishPlayback(PlayerState(currentTrack = track("a")))
            host.host(0); guest.join(host.invitation("127.0.0.1"))
            withTimeout(8_000) { guest.remotePlayback.first { it?.track?.title == "原创歌曲 a" } }
            delay(1_000)
            assertEquals(1, reads.get())
            allowPrefetch = true
            host.publishPlayback(PlayerState(currentTrack = track("a"), isPlaying = true))
            withTimeout(8_000) { while (reads.get() < 2) delay(50) }
            assertEquals(2, reads.get())
        } finally { guest.close(); host.close() }
    }
}
