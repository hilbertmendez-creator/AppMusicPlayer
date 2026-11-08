package com.mimusica.app.ui

import com.mimusica.app.ui.theme.MaxCombinedTextScale
import com.mimusica.app.ui.theme.cappedOsFontScale
import com.mimusica.app.ui.theme.effectiveTextScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextScaleCapTest {
    private val delta = 1e-4f

    @Test
    fun `the cap is 1_6`() {
        assertEquals(1.6f, MaxCombinedTextScale, delta)
    }

    @Test
    fun `app 1_2 times os 1_5 is capped to 1_6`() {
        assertEquals(1.6f, effectiveTextScale(appFactor = 1.2f, osFontScale = 1.5f), delta)
    }

    @Test
    fun `app 1_2 at os 1_0 keeps the in-app choice`() {
        assertEquals(1.2f, effectiveTextScale(appFactor = 1.2f, osFontScale = 1.0f), delta)
    }

    @Test
    fun `app 1_0 times os 2_0 is capped to 1_6`() {
        assertEquals(1.6f, effectiveTextScale(appFactor = 1.0f, osFontScale = 2.0f), delta)
    }

    @Test
    fun `in-app choice is preserved whenever the product stays under the cap`() {
        listOf(0.8f, 1.0f, 1.2f).forEach { app ->
            assertEquals(app, effectiveTextScale(app, osFontScale = 1.0f), delta)
        }
        assertEquals(1.2f * 1.1f, effectiveTextScale(1.2f, 1.1f), delta)
    }

    @Test
    fun `a small OS scale is never raised`() {
        assertEquals(0.85f, cappedOsFontScale(appFactor = 1.0f, osFontScale = 0.85f), delta)
        assertEquals(1.2f * 0.85f, effectiveTextScale(1.2f, 0.85f), delta)
    }

    @Test
    fun `the OS scale handed to the density never exceeds what the cap allows`() {
        listOf(0.8f, 1.0f, 1.2f).forEach { app ->
            listOf(0.85f, 1.0f, 1.15f, 1.3f, 1.5f, 1.82f, 2.0f).forEach { os ->
                val capped = cappedOsFontScale(app, os)
                assertTrue("capped must not exceed the OS scale", capped <= os + delta)
                assertTrue("app $app x os $os must end <= 1.6", app * capped <= MaxCombinedTextScale + delta)
            }
        }
    }

    @Test
    fun `the worst case 1_2 x 2_0 is brought down to 1_6`() {
        assertEquals(1.6f, effectiveTextScale(1.2f, 2.0f), delta)
    }

    @Test
    fun `a preview row is capped for its own factor, not for the one in use`() {
        // The "Muy grande" row is drawn while "Normal" is active and the phone's font size is at its largest.
        val previewScale = 1.2f * cappedOsFontScale(appFactor = 1.2f, osFontScale = 2.0f)
        assertEquals(MaxCombinedTextScale, previewScale, delta)
        // Capping it for the active factor (0.8) instead would have drawn it at 1.2 x 2.0 = 2.4.
        assertTrue(1.2f * cappedOsFontScale(appFactor = 0.8f, osFontScale = 2.0f) > MaxCombinedTextScale)
    }
}
