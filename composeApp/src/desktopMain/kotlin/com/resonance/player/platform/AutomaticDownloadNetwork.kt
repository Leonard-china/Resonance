package com.resonance.player.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Query Windows' actual connection cost; an unknown result never authorizes metered downloads. */
internal object AutomaticDownloadNetwork {
    private var checkedAt = 0L
    private var unrestricted = false
    suspend fun allowed(unmeteredOnly: Boolean): Boolean = if (!unmeteredOnly) true else withContext(Dispatchers.IO) { checkCost() }
    @Synchronized private fun checkCost(): Boolean {
        val now = System.nanoTime() / 1_000_000
        if (checkedAt != 0L && now - checkedAt < 30_000) return unrestricted
        checkedAt = now
        unrestricted = false
        if (!System.getProperty("os.name").startsWith("Windows", true)) return false
        val command = "[void][Windows.Networking.Connectivity.NetworkInformation,Windows,ContentType=WindowsRuntime]; " +
            "\u0024connectionProfile=[Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile(); " +
            "if (\u0024null -ne \u0024connectionProfile) { \u0024connectionProfile.GetConnectionCost().NetworkCostType.ToString() }"
        val process = runCatching { ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", command).redirectErrorStream(true).start() }.getOrNull() ?: return false
        try {
            if (process.waitFor(3, TimeUnit.SECONDS)) unrestricted = process.exitValue() == 0 && process.inputStream.bufferedReader().readText().trim() == "Unrestricted"
        } finally { process.destroyForcibly(); process.inputStream.close() }
        return unrestricted
    }
}
