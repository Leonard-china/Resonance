package com.resonance.player.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Query system connection cost; unknown cost never authorizes an unmetered-only download. */
internal object AutomaticDownloadNetwork {
    private var checkedAt = 0L
    private var unrestricted = false
    suspend fun allowed(unmeteredOnly: Boolean): Boolean = if (!unmeteredOnly) true else withContext(Dispatchers.IO) { checkCost() }
    @Synchronized private fun checkCost(): Boolean {
        val now = System.nanoTime() / 1_000_000
        if (checkedAt != 0L && now - checkedAt < 30_000) return unrestricted
        checkedAt = now
        unrestricted = false
        if (System.getProperty("os.name").startsWith("Linux", true)) {
            val output = query(listOf("nmcli", "-t", "-f", "GENERAL.STATE,GENERAL.METERED", "device", "show"))
            unrestricted = output != null && linuxUnmetered(output)
            return unrestricted
        }
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

    private fun query(command: List<String>): String? {
        val builder = ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD)
        builder.environment()["LC_ALL"] = "C"
        val process = runCatching { builder.start() }.getOrNull() ?: return null
        return try {
            if (process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0)
                process.inputStream.bufferedReader().use { it.readText() } else null
        } finally { process.destroyForcibly(); process.inputStream.close() }
    }

    internal fun linuxUnmetered(output: String): Boolean {
        var connected = false
        var anyConnected = false
        var costSeen = true
        for (line in output.lineSequence()) {
            when {
                line.startsWith("GENERAL.STATE:") -> {
                    if (connected && !costSeen) return false
                    connected = line.substringAfter(':').substringBefore(' ') == "100"
                    anyConnected = anyConnected || connected
                    costSeen = !connected
                }
                line.startsWith("GENERAL.METERED:") && connected -> {
                    if (line.substringAfter(':').substringBefore(' ').trim() != "no") return false
                    costSeen = true
                }
            }
        }
        return anyConnected && costSeen
    }
}
