package com.mimusica.app.domain

import com.mimusica.app.fakes.song
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongLyricsKindTest {
    @Test
    fun `plain only means plain lyrics without synced ones`() {
        assertTrue(song("a", plain = true).hasPlainLyricsOnly)
        assertFalse(song("b", synced = true).hasPlainLyricsOnly)
        assertFalse(song("c", synced = true, plain = true).hasPlainLyricsOnly)
        assertFalse(song("d").hasPlainLyricsOnly)
    }
}
