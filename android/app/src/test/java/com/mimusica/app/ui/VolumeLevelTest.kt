package com.mimusica.app.ui

import com.mimusica.app.domain.player.VOLUME_STEPS
import com.mimusica.app.domain.player.volumeLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeLevelTest {
    @Test
    fun `the scale has ten steps`() {
        assertEquals(10, VOLUME_STEPS)
    }

    @Test
    fun `silent is 0 and full is 10 whatever the device range`() {
        assertEquals(0, volumeLevel(current = 0, max = 15))
        assertEquals(10, volumeLevel(current = 15, max = 15))
        assertEquals(10, volumeLevel(current = 30, max = 30))
    }

    @Test
    fun `values in between round to the nearest step`() {
        assertEquals(6, volumeLevel(current = 9, max = 15))
        assertEquals(5, volumeLevel(current = 7, max = 15))
        assertEquals(1, volumeLevel(current = 1, max = 15))
    }

    @Test
    fun `an out of range reading is clamped`() {
        assertEquals(10, volumeLevel(current = 20, max = 15))
        assertEquals(0, volumeLevel(current = -3, max = 15))
    }

    @Test
    fun `a device that reports no range reads as silent`() {
        assertEquals(0, volumeLevel(current = 5, max = 0))
    }
}
