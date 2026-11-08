package com.mimusica.app.ui

import com.mimusica.app.ui.search.isInstrumental
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentalTagTest {
    @Test
    fun `title containing instrumental is tagged whatever the case`() {
        assertTrue(isInstrumental("Besame Mucho (Instrumental)"))
        assertTrue(isInstrumental("INSTRUMENTAL - Cielito Lindo"))
        assertTrue(isInstrumental("version instrumental"))
    }

    @Test
    fun `other titles are not tagged`() {
        assertFalse(isInstrumental("Besame Mucho"))
        assertFalse(isInstrumental(""))
    }
}
