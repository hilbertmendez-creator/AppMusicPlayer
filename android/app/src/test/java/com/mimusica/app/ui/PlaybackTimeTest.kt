package com.mimusica.app.ui

import com.mimusica.app.ui.player.formatClock
import com.mimusica.app.ui.player.formatDuration
import com.mimusica.app.ui.player.formatPlaybackProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTimeTest {
    @Test
    fun `clock shows minutes and zero padded seconds`() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:07", formatClock(7_400))
        assertEquals("1:05", formatClock(65_000))
        assertEquals("3:40", formatClock(220_000))
        assertEquals("12:00", formatClock(720_000))
    }

    @Test
    fun `clock switches to hours past sixty minutes`() {
        assertEquals("1:00:00", formatClock(3_600_000))
        assertEquals("1:01:01", formatClock(3_661_000))
    }

    @Test
    fun `clock never goes negative`() {
        assertEquals("0:00", formatClock(-5_000))
    }

    @Test
    fun `progress reads elapsed over total`() {
        assertEquals("1:05 / 3:40", formatPlaybackProgress(65_000, 220_000))
    }

    @Test
    fun `progress can use a spoken separator`() {
        assertEquals("1:05 de 3:40", formatPlaybackProgress(65_000, 220_000, separator = " de "))
    }

    @Test
    fun `elapsed is clamped to the total`() {
        assertEquals("3:40 / 3:40", formatPlaybackProgress(999_000, 220_000))
    }

    @Test
    fun `duration rounds to the nearest second so it matches server metadata`() {
        assertEquals("4:03", formatDuration(242_600))
        assertEquals("4:02", formatDuration(242_400))
        assertEquals("0:00", formatDuration(0))
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("0:00", formatDuration(-5_000))
        assertEquals("4:03", formatDuration(243_000))
    }

    @Test
    fun `elapsed keeps truncating while duration rounds`() {
        assertEquals("4:02", formatClock(242_600))
        assertEquals("4:02 / 4:03", formatPlaybackProgress(242_600, 242_600))
    }
}
