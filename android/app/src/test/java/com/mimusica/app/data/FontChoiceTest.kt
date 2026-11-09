package com.mimusica.app.data

import com.mimusica.app.data.settings.AppSettings
import com.mimusica.app.data.settings.FontChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FontChoiceTest {
    @Test
    fun `the five fonts come in the agreed order with their display names`() {
        assertEquals(
            listOf("Atkinson Hyperlegible", "Lexend", "Andika", "Source Sans 3", "Del sistema"),
            FontChoice.entries.map { it.displayName },
        )
        assertEquals(
            listOf(FontChoice.ATKINSON, FontChoice.LEXEND, FontChoice.ANDIKA, FontChoice.SOURCE_SANS, FontChoice.SYSTEM),
            FontChoice.entries.toList(),
        )
    }

    @Test
    fun `storage ids are stable and unique`() {
        assertEquals(
            listOf("atkinson", "lexend", "andika", "source_sans", "system"),
            FontChoice.entries.map { it.id },
        )
        assertEquals(FontChoice.entries.size, FontChoice.entries.map { it.id }.toSet().size)
    }

    @Test
    fun `parse round trips every id`() {
        FontChoice.entries.forEach { assertEquals(it, FontChoice.parse(it.id)) }
    }

    @Test
    fun `parse is lenient and falls back to Atkinson`() {
        assertEquals(FontChoice.LEXEND, FontChoice.parse("  LEXEND "))
        assertEquals(FontChoice.ATKINSON, FontChoice.parse(null))
        assertEquals(FontChoice.ATKINSON, FontChoice.parse(""))
        assertEquals(FontChoice.ATKINSON, FontChoice.parse("comic_sans"))
    }

    @Test
    fun `default settings use Atkinson`() {
        assertEquals(FontChoice.ATKINSON, AppSettings().fontChoice)
    }

    @Test
    fun `only the system font has no google family name`() {
        assertNull(FontChoice.SYSTEM.googleName)
        FontChoice.entries.filter { it != FontChoice.SYSTEM }.forEach { assertTrue(it.googleName!!.isNotBlank()) }
        assertEquals("Source Sans 3", FontChoice.SOURCE_SANS.googleName)
    }
}
