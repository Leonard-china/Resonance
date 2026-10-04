package com.resonance.player.weather

import com.resonance.player.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

/** Shared Android/JVM client. No precise coordinates or API credentials are persisted. */
object WeatherClient {
    private fun json(url: String): JSONObject {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Resonance/$APP_VERSION")
            check(connection.responseCode == 200) { "天气服务暂不可用" }
            return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun cities(query: String): List<WeatherLocation> = withContext(Dispatchers.IO) {
        val q = query.trim().take(100)
        if (q.length < 2) return@withContext emptyList()
        val array = json("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(q, "UTF-8")}&count=8&language=zh&format=json")
            .optJSONArray("results") ?: return@withContext emptyList()
        (0 until array.length()).mapNotNull { index ->
            val city = array.getJSONObject(index)
            WeatherLocation(city.optDouble("latitude"), city.optDouble("longitude"),
                listOf(city.optString("name"), city.optString("admin1"), city.optString("country"))
                    .filter(String::isNotBlank).distinct().joinToString(" · "))
                .takeIf { it.valid }
        }
    }

    suspend fun current(location: WeatherLocation, previous: WeatherPalette): WeatherSnapshot = withContext(Dispatchers.IO) {
        require(location.valid)
        // Rounding limits the precision sent to the weather service to approximately city scale.
        val latitude = kotlin.math.round(location.latitude * 10) / 10
        val longitude = kotlin.math.round(location.longitude * 10) / 10
        val key = "$latitude,$longitude"
        val current = json("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=weather_code&timezone=auto")
            .getJSONObject("current")
        val code = current.getInt("weather_code")
        WeatherSnapshot(code, weatherPalette(code, previous), System.currentTimeMillis(), location.label, key)
    }
}

object AppearanceStorage {
    fun preferences(raw: String?): AppearancePreferences = runCatching {
        val json = JSONObject(raw ?: "{}")
        val city = json.optJSONObject("city")?.let {
            WeatherLocation(it.optDouble("lat"), it.optDouble("lon"), it.optString("label"))
                .takeIf { city -> city.valid && city.label.isNotBlank() }
        }
        AppearancePreferences(
            AccentMode.entries.firstOrNull { it.name == json.optString("mode") } ?: AccentMode.Sage,
            city, json.optBoolean("reduceMotion", false),
            WeatherPalette.entries.firstOrNull { it.name == json.optString("retainedPalette") } ?: WeatherPalette.Sage,
        )
    }.getOrDefault(AppearancePreferences())

    fun encode(preferences: AppearancePreferences): String = JSONObject().apply {
        put("mode", preferences.accentMode.name)
        put("reduceMotion", preferences.reduceMotion)
        put("retainedPalette", preferences.retainedPalette.name)
        preferences.fallbackCity?.let { city ->
            put("city", JSONObject().put("lat", kotlin.math.round(city.latitude * 10) / 10)
                .put("lon", kotlin.math.round(city.longitude * 10) / 10).put("label", city.label))
        }
    }.toString()

    fun snapshot(raw: String?): WeatherSnapshot? = runCatching {
        val json = JSONObject(raw ?: return null)
        WeatherSnapshot(json.getInt("code"), WeatherPalette.valueOf(json.getString("palette")),
            json.getLong("updated"), json.getString("label"), json.getString("key"))
    }.getOrNull()

    fun encode(snapshot: WeatherSnapshot): String = JSONObject()
        .put("code", snapshot.code).put("palette", snapshot.palette.name)
        .put("updated", snapshot.updatedAtMillis).put("label", snapshot.locationLabel)
        .put("key", snapshot.locationKey).toString()
}
