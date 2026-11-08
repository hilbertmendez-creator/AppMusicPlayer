package com.mimusica.app.ui

import com.mimusica.app.ui.karaoke.AUTO_SCROLL_PAUSE_MS
import com.mimusica.app.ui.karaoke.plainScrollOffset
import com.mimusica.app.ui.karaoke.playbackFraction
import com.mimusica.app.ui.karaoke.shouldAutoScroll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlainScrollTest {
    @Test
    fun `fraction is position over duration`() {
        assertEquals(0.25f, playbackFraction(50_000, 200_000), 0.0001f)
    }

    @Test
    fun `fraction is zero while the duration is unknown`() {
        assertEquals(0f, playbackFraction(5_000, 0), 0f)
    }

    @Test
    fun `fraction never leaves the 0 to 1 range`() {
        assertEquals(1f, playbackFraction(250_000, 200_000), 0f)
        assertEquals(0f, playbackFraction(-5, 200_000), 0f)
    }

    @Test
    fun `offset maps the fraction onto the scrollable distance`() {
        assertEquals(0, plainScrollOffset(0f, 1_000))
        assertEquals(500, plainScrollOffset(0.5f, 1_000))
        assertEquals(1_000, plainScrollOffset(1f, 1_000))
    }

    @Test
    fun `offset is clamped and safe without anything to scroll`() {
        assertEquals(1_000, plainScrollOffset(2f, 1_000))
        assertEquals(0, plainScrollOffset(-1f, 1_000))
        assertEquals(0, plainScrollOffset(0.5f, 0))
    }

    @Test
    fun `auto scroll waits after a manual touch and then resumes`() {
        assertFalse(shouldAutoScroll(nowMs = 10_000, lastTouchMs = 10_000 - AUTO_SCROLL_PAUSE_MS + 1))
        assertTrue(shouldAutoScroll(nowMs = 10_000, lastTouchMs = 10_000 - AUTO_SCROLL_PAUSE_MS))
    }

    @Test
    fun `auto scroll runs when the user never touched the lyrics`() {
        assertTrue(shouldAutoScroll(nowMs = 1_000, lastTouchMs = null))
    }
}
