package com.mimusica.app.ui

import androidx.compose.ui.graphics.Color
import com.mimusica.app.ui.components.SectionAccents
import com.mimusica.app.ui.theme.ChipBg
import com.mimusica.app.ui.theme.CoverBg
import com.mimusica.app.ui.theme.Danger
import com.mimusica.app.ui.theme.DeepBlue
import com.mimusica.app.ui.theme.Ink
import com.mimusica.app.ui.theme.InkSoft
import com.mimusica.app.ui.theme.Line
import com.mimusica.app.ui.theme.Navy
import com.mimusica.app.ui.theme.OnNavy
import com.mimusica.app.ui.theme.Page
import com.mimusica.app.ui.theme.Paper
import com.mimusica.app.ui.theme.ProgressFill
import com.mimusica.app.ui.theme.ProgressTrack
import com.mimusica.app.ui.theme.SectionLists
import com.mimusica.app.ui.theme.SectionSearch
import com.mimusica.app.ui.theme.SectionSettings
import com.mimusica.app.ui.theme.SectionSing
import com.mimusica.app.ui.theme.Success
import com.mimusica.app.ui.theme.SurfaceFill
import com.mimusica.app.ui.theme.Sun
import com.mimusica.app.ui.theme.SunEdge
import com.mimusica.app.ui.theme.SunSoft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every text/background pair in the palette must reach WCAG AAA (7:1); non-text fills reach 3:1. */
class PaletteContrastTest {
    private fun channel(c: Float): Double = if (c <= 0.03928f) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = luminance(a).let { la -> luminance(b).let { lb -> if (la >= lb) la to lb else lb to la } }
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertAaa(name: String, fg: Color, bg: Color) {
        val r = ratio(fg, bg)
        assertTrue("$name is only ${"%.2f".format(r)}:1", r >= 7.0)
    }

    private fun assertNonText(name: String, a: Color, b: Color) {
        val r = ratio(a, b)
        assertTrue("$name is only ${"%.2f".format(r)}:1", r >= 3.0)
    }

    @Test
    fun `palette tokens carry the agreed values`() {
        assertEquals(Color(0xFFFFFFFF), Page)
        assertEquals(Color(0xFFF2F5F9), SurfaceFill)
        assertEquals(Color(0xFF0F2742), Ink)
        assertEquals(Color(0xFF3D516B), InkSoft)
        assertEquals(Color(0xFFC5CFDB), Line)
        assertEquals(Color(0xFF123C69), Navy)
        assertEquals(Color(0xFFFFFFFF), OnNavy)
        assertEquals(Color(0xFFFFD166), Sun)
        assertEquals(Color(0xFFFFE7A8), SunSoft)
        assertEquals(Color(0xFF8A6100), SunEdge)
        assertEquals(Color(0xFF1B5E20), Success.text)
        assertEquals(Color(0xFF9B0000), Danger)
        assertEquals(Color(0xFF6A1B6B), SectionLists)
        assertEquals(Color(0xFF00585C), SectionSearch)
        assertEquals(Color(0xFF7A3E00), SectionSing)
        assertEquals(Color(0xFF374151), SectionSettings)
        assertEquals(Color(0xFFE3ECFA), ChipBg)
        assertEquals(Color(0xFFDCE6F2), CoverBg)
    }

    @Test
    fun `the window background alias equals the page so the splash has no flash`() {
        assertEquals(Page, Paper)
    }

    @Test
    fun `body and secondary text read on the page and on surfaces`() {
        assertAaa("Ink on Page", Ink, Page)
        assertAaa("Ink on Surface", Ink, SurfaceFill)
        assertAaa("InkSoft on Page", InkSoft, Page)
        assertAaa("InkSoft on Surface", InkSoft, SurfaceFill)
        assertAaa("Ink on ChipBg", Ink, ChipBg)
    }

    @Test
    fun `action buttons and the sun highlights carry readable text`() {
        assertAaa("OnNavy on Navy", OnNavy, Navy)
        assertAaa("Ink on Sun", Ink, Sun)
        assertAaa("Ink on SunSoft", Ink, SunSoft)
        assertEquals(Navy, DeepBlue)
    }

    @Test
    fun `status colours read as text on the page`() {
        assertAaa("Success on Page", Success.text, Page)
        assertAaa("Danger on Page", Danger, Page)
        assertAaa("OnNavy on Success", OnNavy, Success.container)
        assertAaa("OnNavy on Danger", OnNavy, Danger)
    }

    @Test
    fun `section colours are visible on the page as non text elements and carry white text`() {
        listOf(SectionLists, SectionSearch, SectionSing, SectionSettings).forEach {
            assertNonText("$it on Page", it, Page)
            assertAaa("White on $it", OnNavy, it)
        }
        SectionAccents.all.forEach { assertNonText("${it.color} on Page", it.color, Page) }
        assertEquals(Navy, SectionAccents.Songs.color)
        assertEquals(SectionLists, SectionAccents.Playlists.color)
        assertEquals(SectionSearch, SectionAccents.Search.color)
        assertEquals(SectionSing, SectionAccents.Sing.color)
        assertEquals(SectionSettings, SectionAccents.Settings.color)
    }

    @Test
    fun `lines and cover background stay quiet but visible`() {
        assertNonText("Sun edge on SunSoft", SunEdge, SunSoft)
        assertTrue("Line must differ from Page", Line != Page)
        assertTrue("CoverBg must differ from Page", CoverBg != Page)
    }

    @Test
    fun `progress bar track is visible on the page and surface and the fill on the track`() {
        assertEquals(Color(0xFF7B8794), ProgressTrack)
        assertEquals(Color(0xFF0A2F73), ProgressFill)
        assertNonText("Track on Page", ProgressTrack, Page)
        assertNonText("Track on Surface", ProgressTrack, SurfaceFill)
        assertNonText("Fill on Track", ProgressFill, ProgressTrack)
    }
}
