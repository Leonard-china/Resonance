package com.resonance.player.platform

import com.resonance.player.audio.FfmpegExecutable
import com.resonance.player.features.DesktopCredentialVault
import com.resonance.player.features.LinuxCredentialVault
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import org.junit.Assume.assumeTrue
import kotlin.test.*

class LinuxDesktopSupportTest {
    private fun <T> temporary(block: (Path) -> T): T {
        val root = Files.createTempDirectory("resonance-linux-test-")
        return try { block(root) } finally { root.toFile().deleteRecursively() }
    }

    @Test fun dataPathsRespectXdgOverridesAndWindowsLayout() = temporary { home ->
        val xdg = home.resolve("xdg")
        assertEquals(xdg.resolve("resonance"), DesktopPaths.dataDirectory("Linux", home,
            mapOf("XDG_DATA_HOME" to xdg.toString()), null))
        assertEquals(home.resolve(".local/share/resonance"), DesktopPaths.dataDirectory("Linux", home,
            mapOf("XDG_DATA_HOME" to "relative/path"), null))
        assertEquals(home.resolve("isolated"), DesktopPaths.dataDirectory("Linux", home, emptyMap(), home.resolve("isolated").toString()))
        assertEquals(home.resolve("Local/Resonance"), DesktopPaths.dataDirectory("Windows 11", home,
            mapOf("LOCALAPPDATA" to home.resolve("Local").toString()), null))
    }

    @Test fun existingLinuxLibraryIsKeptUntilXdgDirectoryExists() = temporary { home ->
        val legacy = Files.createDirectory(home.resolve("Resonance"))
        assertEquals(legacy, DesktopPaths.dataDirectory("Linux", home, emptyMap(), null))
        val current = Files.createDirectories(home.resolve(".local/share/resonance"))
        assertEquals(current, DesktopPaths.dataDirectory("Linux", home, emptyMap(), null))
        assertTrue(Files.exists(legacy))
    }

    @Test fun ffmpegUsesHostNameAndPackagedOrExplicitExecutable() = temporary { root ->
        val resources = Files.createDirectories(root.resolve("resources with spaces"))
        val binary = Files.write(resources.resolve("ffmpeg"), byteArrayOf(1)).toFile()
        assertTrue(binary.setExecutable(true))
        assertEquals(binary.toString(), FfmpegExecutable.resolve("Linux", emptyList(), resources.toString(), null))
        val explicit = Files.copy(binary.toPath(), root.resolve("custom ffmpeg"))
        assertEquals(explicit.toString(), FfmpegExecutable.resolve("Linux", listOf(explicit.toString()), resources.toString(), null))
        assertEquals("ffmpeg", FfmpegExecutable.resolve("Linux", listOf("", "missing"), null, null))
        assertEquals("ffmpeg.exe", FfmpegExecutable.resolve("Windows", emptyList(), null, null))
        val launcher = root.resolve("bin/Resonance")
        val packaged = Files.createDirectories(root.resolve("lib/app/resources")).resolve("ffmpeg")
        Files.copy(binary.toPath(), packaged)
        assertEquals(packaged.toString(), FfmpegExecutable.resolve("Linux", emptyList(), null, launcher.toString()))
    }

    @Test fun linuxCredentialsSurviveRestartAndUsePrivatePermissions() = temporary { root ->
        assumeTrue(System.getProperty("os.name").startsWith("Linux"))
        val directory = root.resolve("credentials")
        val vault = LinuxCredentialVault(directory)
        val secret = "synthetic-test-token-非真实凭据"
        val first = vault.encrypt(secret)
        val second = vault.encrypt(secret)
        assertNotEquals(first, second, "Each encryption must have a fresh nonce")
        assertFalse(first.contains(secret))
        assertEquals(secret, LinuxCredentialVault(directory).decrypt(first))
        assertEquals(secret, vault.decrypt(second))
        assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(directory))
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(directory.resolve("key.bin")))
        assertEquals(32, Files.size(directory.resolve("key.bin")).toInt())
    }

    @Test fun linuxCredentialsRejectTamperingWrongDirectoryAndMissingKey(): Unit = temporary { root ->
        assumeTrue(System.getProperty("os.name").startsWith("Linux"))
        val directory = root.resolve("one")
        val vault = LinuxCredentialVault(directory)
        val sealed = vault.encrypt("synthetic")
        val changed = sealed.dropLast(4) + "AAAA"
        assertFails { vault.decrypt(changed) }
        val other = LinuxCredentialVault(root.resolve("two"))
        other.encrypt("other")
        assertFails { other.decrypt(sealed) }
        Files.delete(directory.resolve("key.bin"))
        assertFails { vault.decrypt(sealed) }
        assertFalse(Files.exists(directory.resolve("key.bin")), "Decryption must not replace a missing key")
        val desktop = DesktopCredentialVault(root)
        assertEquals("", desktop.encrypt(""))
        assertEquals("legacy-value", desktop.decrypt("legacy-value"))
        assertFailsWith<IllegalStateException> { desktop.decrypt("dpapi:synthetic") }
    }

    @Test fun linuxCredentialsRejectSymlinkedKeyAndDirectory(): Unit = temporary { root ->
        assumeTrue(System.getProperty("os.name").startsWith("Linux"))
        val original = root.resolve("original")
        val sealed = LinuxCredentialVault(original).encrypt("synthetic")
        val linked = Files.createSymbolicLink(root.resolve("linked"), original)
        assertFails { LinuxCredentialVault(linked).decrypt(sealed) }
        val key = original.resolve("key.bin")
        val moved = Files.move(key, root.resolve("moved-key"))
        Files.createSymbolicLink(key, moved)
        assertFails { LinuxCredentialVault(original).decrypt(sealed) }
    }

    @Test fun linuxNetworkCostOnlyAcceptsConnectedUnmeteredInterfaces() {
        fun output(cost: String) = "GENERAL.STATE:100 (connected)\nGENERAL.METERED:$cost\n"
        assertTrue(AutomaticDownloadNetwork.linuxUnmetered(output("no (guessed)")))
        assertFalse(AutomaticDownloadNetwork.linuxUnmetered(output("yes")))
        assertFalse(AutomaticDownloadNetwork.linuxUnmetered(output("unknown")))
        assertFalse(AutomaticDownloadNetwork.linuxUnmetered(output("no") + output("yes (guessed)")))
        assertTrue(AutomaticDownloadNetwork.linuxUnmetered(output("no") + "GENERAL.STATE:30 (disconnected)\nGENERAL.METERED:unknown"))
        assertFalse(AutomaticDownloadNetwork.linuxUnmetered("GENERAL.STATE:100 (connected)"))
        assertFalse(AutomaticDownloadNetwork.linuxUnmetered(""))
    }
}
