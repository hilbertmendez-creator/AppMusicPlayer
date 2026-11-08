package com.mimusica.app.ui

import androidx.compose.ui.graphics.Color
import com.mimusica.app.ui.theme.AmberBrown
import com.mimusica.app.ui.theme.Card
import com.mimusica.app.ui.theme.DeepBlue
import com.mimusica.app.ui.theme.DeepGreen
import com.mimusica.app.ui.theme.DeepRed
import com.mimusica.app.ui.theme.DeepTeal
import com.mimusica.app.ui.theme.Ink
import com.mimusica.app.ui.theme.OnSolid
import com.mimusica.app.ui.theme.Outline
import com.mimusica.app.ui.theme.Paper
import com.mimusica.app.ui.theme.Plum
import com.mimusica.app.ui.theme.ProgressFill
import com.mimusica.app.ui.theme.ProgressTrack
import com.mimusica.app.ui.theme.SoftBlue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every text/background pair in the warm palette must reach WCAG AAA (7:1). */
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

    @Test
    fun `background is the cool pearl grey and cards are white`() {
        assertEquals(Color(0xFFF1F3F5), Paper)
        assertEquals(Color.White, Card)
    }

    @Test
    fun `outlines and secondary text read on paper and cards`() {
        assertAaa("Outline on Paper", Outline, Paper)
        assertAaa("Outline on Card", Outline, Card)
    }

    @Test
    fun `body text reads on paper cards and tints`() {
        assertAaa("Ink on Paper", Ink, Paper)
        assertAaa("Ink on Card", Ink, Card)
        assertAaa("Ink on SoftBlue", Ink, SoftBlue)
    }

    @Test
    fun `each home tile colour carries white text`() {
        assertAaa("White on DeepBlue", OnSolid, DeepBlue)
        assertAaa("White on Plum", OnSolid, Plum)
        assertAaa("White on DeepTeal", OnSolid, DeepTeal)
        assertAaa("White on AmberBrown", OnSolid, AmberBrown)
    }

    @Test
    fun `status colours read as text and as fills`() {
        assertAaa("Success text on Card", DeepGreen, Card)
        assertAaa("White on DeepGreen", OnSolid, DeepGreen)
        assertAaa("Error text on Card", DeepRed, Card)
        assertAaa("Error text on Paper", DeepRed, Paper)
        assertAaa("White on DeepRed", OnSolid, DeepRed)
    }

    @Test
    fun `blue accent text reads on paper card and tint`() {
        assertAaa("Blue on Paper", DeepBlue, Paper)
        assertAaa("Blue on Card", DeepBlue, Card)
        assertAaa("Blue on SoftBlue", DeepBlue, SoftBlue)
    }

    private fun assertNonText(name: String, a: Color, b: Color) {
        val r = ratio(a, b)
        assertTrue("$name is only ${"%.2f".format(r)}:1", r >= 3.0)
    }

    @Test
    fun `progress bar track is visible on paper and card and the fill on the track`() {
        assertNonText("Track on Paper", ProgressTrack, Paper)
        assertNonText("Track on Card", ProgressTrack, Card)
        assertNonText("Fill on Track", ProgressFill, ProgressTrack)
    }
}
