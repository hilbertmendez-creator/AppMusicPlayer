package com.mimusica.app.domain

import com.mimusica.app.domain.lyrics.LyricsTimeline
import com.mimusica.app.domain.model.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTimelineTest {

    private val lines = listOf(
        LyricLine(1_000, "one"),
        LyricLine(5_000, "two"),
        LyricLine(9_000, "three"),
    )
    private val timeline = LyricsTimeline(lines)

    @Test
    fun `before first line returns minus one`() {
        assertEquals(-1, timeline.currentIndex(0, 0))
        assertEquals(-1, timeline.currentIndex(999, 0))
    }

    @Test
    fun `exact boundary selects the line that starts`() {
        assertEquals(0, timeline.currentIndex(1_000, 0))
        assertEquals(1, timeline.currentIndex(5_000, 0))
        assertEquals(2, timeline.currentIndex(9_000, 0))
    }

    @Test
    fun `one millisecond before boundary keeps previous line`() {
        assertEquals(0, timeline.currentIndex(4_999, 0))
        assertEquals(1, timeline.currentIndex(8_999, 0))
    }

    @Test
    fun `after last line stays on last`() {
        assertEquals(2, timeline.currentIndex(600_000, 0))
    }

    @Test
    fun `positive offset makes lyrics appear earlier`() {
        assertEquals(1, timeline.currentIndex(4_500, 500))
        assertEquals(0, timeline.currentIndex(4_499, 500))
    }

    @Test
    fun `negative offset makes lyrics appear later`() {
        assertEquals(0, timeline.currentIndex(5_200, -500))
        assertEquals(1, timeline.currentIndex(5_500, -500))
    }

    @Test
    fun `offset can bring first line into view at start`() {
        assertEquals(0, timeline.currentIndex(0, 1_000))
    }

    @Test
    fun `empty timeline always returns minus one`() {
        assertEquals(-1, LyricsTimeline(emptyList()).currentIndex(10_000, 0))
    }

    @Test
    fun `duplicate timestamps pick the last of equal times`() {
        val t = LyricsTimeline(listOf(LyricLine(1_000, "a"), LyricLine(1_000, "b"), LyricLine(2_000, "c")))
        assertEquals(1, t.currentIndex(1_500, 0))
    }
}
