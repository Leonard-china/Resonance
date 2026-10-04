package com.resonance.player

import com.resonance.player.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WeatherPaletteTest {
    @Test fun clearWeatherUsesSage() {
        listOf(0, 1, 2).forEach { assertEquals(WeatherPalette.Sage, weatherPalette(it, WeatherPalette.IceBlue)) }
    }
    @Test fun rainSnowAndStormsUseBlue() {
        listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 97, 99)
            .forEach { assertEquals(WeatherPalette.IceBlue, weatherPalette(it, WeatherPalette.Sage)) }
    }
    @Test fun ambiguousWeatherPreservesLastPalette() {
        listOf(3, 45, 48, -1, 999).forEach { code ->
            WeatherPalette.entries.forEach { assertEquals(it, weatherPalette(code, it)) }
        }
    }
    @Test fun invalidLocationsCannotBeUsed() {
        assertFalse(WeatherLocation(Double.NaN, 0.0, "").valid)
        assertFalse(WeatherLocation(91.0, 0.0, "").valid)
        assertFalse(WeatherLocation(0.0, 181.0, "").valid)
    }
}
