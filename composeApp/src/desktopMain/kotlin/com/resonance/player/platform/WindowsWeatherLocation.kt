package com.resonance.player.platform

import com.resonance.player.model.WeatherLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Reads the Windows location provider only when system access is already available.
 * An unavailable provider, denied access or absent sensor falls back to the user's city.
 */
internal object WindowsWeatherLocation {
    suspend fun current(): WeatherLocation? = withContext(Dispatchers.IO) {
        if (!System.getProperty("os.name").startsWith("Windows")) return@withContext null
        runCatching {
            val script = """
                Add-Type -AssemblyName System.Device
                ${'$'}watcher = New-Object System.Device.Location.GeoCoordinateWatcher
                try {
                    [void]${'$'}watcher.TryStart(${'$'}true, [TimeSpan]::FromSeconds(4))
                    ${'$'}point = ${'$'}watcher.Position.Location
                    if (-not ${'$'}point.IsUnknown) {
                        @{lat=${'$'}point.Latitude;lon=${'$'}point.Longitude} | ConvertTo-Json -Compress
                    }
                } finally { ${'$'}watcher.Dispose() }
            """.trimIndent()
            val process = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", script)
                .redirectError(ProcessBuilder.Redirect.DISCARD).start()
            if (!process.waitFor(7, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return@runCatching null
            }
            val raw = process.inputStream.bufferedReader().use { it.readText() }.trim()
            if (raw.isBlank()) return@runCatching null
            val point = JSONObject(raw)
            WeatherLocation(point.getDouble("lat"), point.getDouble("lon"), "当前位置").takeIf { it.valid }
        }.getOrNull()
    }
}
