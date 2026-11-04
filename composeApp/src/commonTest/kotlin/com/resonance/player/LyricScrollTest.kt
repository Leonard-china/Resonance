package com.resonance.player

import com.resonance.player.ui.lyricCenterOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class LyricScrollTest {
    @Test fun usesTheViewportCoordinateSystemIncludingTopPadding() {
        assertEquals(-40, lyricCenterOffset(80, -300, 300))
        assertEquals(260, lyricCenterOffset(80, 0, 600))
        assertEquals(110, lyricCenterOffset(80, -150, 450))
    }

    @Test fun measuredBilingualAndWrappedRowsShareTheSameCenter() {
        for (size in listOf(48, 80, 144, 236)) {
            assertEquals(0, lyricCenterOffset(size, -300, 300) + size / 2)
        }
    }

    @Test fun oversizedLinesKeepTheirBeginningReadable() {
        assertEquals(-200, lyricCenterOffset(900, -200, 200))
        assertEquals(0, lyricCenterOffset(300, 0, 100))
    }
}
