package com.mimusica.app.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.ui.settings.PinScreen
import com.mimusica.app.ui.settings.TextSizeScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TextSizeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun textSizeIsReachableWithoutAnyPin() {
        var chosen: TextScale? = null
        compose.setContent {
            MiMusicaTheme {
                TextSizeScreen(
                    current = TextScale.NORMAL,
                    onSelect = { chosen = it },
                    onCaregiver = {},
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("Tamaño de letra").assertIsDisplayed()
        compose.onNodeWithText("Muy grande").assertHeightIsAtLeast(64.dp).performClick()
        assertEquals(TextScale.EXTRA_LARGE, chosen)
    }

    @Test
    fun currentSizeIsMarkedAsSelectedAndShowsAPreview() {
        compose.setContent {
            MiMusicaTheme {
                TextSizeScreen(current = TextScale.LARGE, onSelect = {}, onCaregiver = {}, onBack = {})
            }
        }
        compose.onNodeWithText("Grande").assertIsSelected()
        compose.onNodeWithText("Normal").assertIsDisplayed()
    }

    @Test
    fun caregiverEntryIsAQuietLinkWithANoteAtTheBottom() {
        var clicked = false
        compose.setContent {
            MiMusicaTheme {
                TextSizeScreen(current = TextScale.NORMAL, onSelect = {}, onCaregiver = { clicked = true }, onBack = {})
            }
        }
        compose.onNodeWithText("Solo para quien configuró la app").assertExists()
        val link = compose.onNodeWithText("Para quien ayuda").assertHeightIsAtLeast(64.dp)
        val lastSize = compose.onNodeWithText("Muy grande").getUnclippedBoundsInRoot()
        assertTrue("the link must come after the text sizes", link.getUnclippedBoundsInRoot().top >= lastSize.bottom)
        link.performClick()
        assertTrue(clicked)
    }

    @Test
    fun pinGateExplainsItselfAndShowsAPlainError() {
        compose.setContent {
            MiMusicaTheme {
                PinScreen(wrongPin = true, onSubmit = {}, onPinEdited = {}, onBack = {})
            }
        }
        compose.onNodeWithText("Esta parte es para quien te ayuda con la app.").assertIsDisplayed()
        compose.onNodeWithText("¿No sabes el PIN? Pregúntale a quien configuró la app.").assertIsDisplayed()
        compose.onNodeWithText("Ese PIN no es correcto. Prueba otra vez.").assertIsDisplayed()
    }
}
