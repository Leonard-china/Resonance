package com.resonance.player.sync

import java.nio.file.Files
import java.nio.file.Path

object LanShareProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        val projectRoot = Path.of(System.getProperty("user.dir")).parent
        val root = projectRoot.resolve("build/lan-probe")
        Files.createDirectories(root)
        val payload = root.resolve("payload.resonance")
        val fixture = projectRoot.resolve("build/test-media/cross-device-sync.resonance")
        check(Files.exists(fixture)) { "Run desktopTest first to create the cross-device sync fixture" }
        Files.copy(fixture, payload, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        DesktopLanShare(payload, "resonance-test-013").use { share ->
            Files.writeString(root.resolve("link.txt"), share.link)
            Thread.sleep(90_000)
        }
    }
}
