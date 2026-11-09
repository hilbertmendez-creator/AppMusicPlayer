package com.mimusica.app.ui

import androidx.compose.ui.text.font.FontFamily
import com.mimusica.app.ui.theme.scaledTypography
import org.junit.Assert.assertEquals
import org.junit.Test

class FontTypographyTest {
    @Test
    fun `the chosen family is applied to every text style`() {
        val t = scaledTypography(1f, FontFamily.Serif)
        listOf(
            t.displayLarge, t.displayMedium, t.displaySmall, t.headlineLarge, t.headlineMedium, t.headlineSmall,
            t.titleLarge, t.titleMedium, t.titleSmall, t.bodyLarge, t.bodyMedium, t.bodySmall,
            t.labelLarge, t.labelMedium, t.labelSmall,
        ).forEach { assertEquals(FontFamily.Serif, it.fontFamily) }
    }

    @Test
    fun `the size scale still applies with a family`() {
        assertEquals(22f * 1.2f, scaledTypography(1.2f, FontFamily.SansSerif).bodyLarge.fontSize.value, 1e-3f)
    }
}
