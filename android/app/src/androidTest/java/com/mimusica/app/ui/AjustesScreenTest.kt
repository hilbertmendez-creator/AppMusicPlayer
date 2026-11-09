package com.mimusica.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.mimusica.app.data.settings.FontChoice
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.ui.settings.AboutUpdateUi
import com.mimusica.app.ui.settings.AjustesScreen
import com.mimusica.app.ui.settings.PinScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AjustesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val polite = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)

    private fun show(
        font: FontChoice = FontChoice.ATKINSON,
        scale: TextScale = TextScale.NORMAL,
        update: AboutUpdateUi = AboutUpdateUi.Idle,
        onFont: (FontChoice) -> Unit = {},
        onScale: (TextScale) -> Unit = {},
        onCheck: () -> Unit = {},
        onInstall: () -> Unit = {},
        onCaregiver: () -> Unit = {},
    ) {
        compose.setContent {
            MiMusicaTheme(textScale = scale, fontChoice = font) {
                AjustesScreen(
                    fontChoice = font,
                    textScale = scale,
                    versionName = "0.1.0",
                    updateUi = update,
                    onSelectFont = onFont,
                    onSelectTextScale = onScale,
                    onCheckUpdate = onCheck,
                    onInstallUpdate = onInstall,
                    onCaregiver = onCaregiver,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun byDefaultOnlyTheSelectedFontIsListed() {
        show(font = FontChoice.LEXEND)
        compose.onNodeWithText("Ajustes").assertIsDisplayed()
        compose.onNodeWithText("Fuente de la app").assertIsDisplayed()
        compose.onNodeWithText("Lexend").assertIsDisplayed().assertIsSelected().assertHeightIsAtLeast(64.dp)
        compose.onNodeWithText("Atkinson Hyperlegible").assertDoesNotExist()
        compose.onNodeWithText("Andika").assertDoesNotExist()
        compose.onNodeWithText("Cambiar fuente").assertHeightIsAtLeast(64.dp)
    }

    @Test
    fun changeFontExpandsToFiveRowsAndTheButtonBecomesListo() {
        show()
        compose.onNodeWithText("Cambiar fuente").performClick()
        FontChoice.entries.forEach {
            compose.onNodeWithText(it.displayName).assertExists().assertHeightIsAtLeast(64.dp)
        }
        compose.onNodeWithText("Atkinson Hyperlegible").assertIsSelected()
        compose.onNodeWithText("Cambiar fuente").assertDoesNotExist()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Lexend").assertDoesNotExist()
        compose.onNodeWithText("Cambiar fuente").assertExists()
    }

    @Test
    fun choosingAFontCallsBack() {
        var chosen: FontChoice? = null
        show(onFont = { chosen = it })
        compose.onNodeWithText("Cambiar fuente").performClick()
        compose.onNodeWithText("Andika").performScrollTo().performClick()
        assertEquals(FontChoice.ANDIKA, chosen)
    }

    @Test
    fun threeSizeButtonsShowTheirLabelsAndMarkTheCurrentOne() {
        var chosen: TextScale? = null
        show(scale = TextScale.LARGE, onScale = { chosen = it })
        compose.onNodeWithText("Tamaño de letra").assertExists()
        listOf("Normal", "Grande", "Muy grande").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(64.dp)
        }
        compose.onNodeWithText("Grande").assertIsSelected()
        compose.onNodeWithText("Muy grande").performClick()
        assertEquals(TextScale.EXTRA_LARGE, chosen)
        // The "Aa" samples sit inside merged buttons, so they only exist in the unmerged tree.
        assertEquals(
            3,
            listOf("NORMAL", "LARGE", "EXTRA_LARGE").sumOf {
                compose.onAllNodesWithTag("size_sample_$it", useUnmergedTree = true).fetchSemanticsNodes().size
            },
        )
    }

    @Test
    fun aTextSizeChangeIsAnnouncedPolitely() {
        show(scale = TextScale.LARGE)
        compose.onNodeWithText("Tamaño elegido: Grande").performScrollTo().assertIsDisplayed().assert(polite)
    }

    @Test
    fun aboutShowsTheVersionAndTheUpdateButton() {
        var checks = 0
        show(onCheck = { checks++ })
        compose.onNodeWithText("Acerca de").performScrollTo().assertExists()
        compose.onNodeWithText("Mi Música").performScrollTo().assertExists()
        compose.onNodeWithText("Versión 0.1.0").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Buscar actualización").performScrollTo().assertHeightIsAtLeast(64.dp).performClick()
        assertEquals(1, checks)
    }

    @Test
    fun upToDateIsAPoliteGreenLineNotAButton() {
        show(update = AboutUpdateUi.UpToDate)
        compose.onNodeWithText("Ya tienes la última versión").performScrollTo().assertIsDisplayed().assert(polite)
    }

    @Test
    fun anAvailableUpdateOffersTheInstallButton() {
        var installs = 0
        show(update = AboutUpdateUi.Available, onInstall = { installs++ })
        compose.onNodeWithText("Hay una versión nueva").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Actualizar ahora").performScrollTo().assertHeightIsAtLeast(64.dp).performClick()
        assertEquals(1, installs)
    }

    @Test
    fun aFailedCheckAndTheDebugBuildSayTheirOwnThing() {
        show(update = AboutUpdateUi.CheckFailed)
        compose.onNodeWithText("No pudimos buscar. Revisa tu internet e inténtalo otra vez.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ya tienes la última versión").assertDoesNotExist()
    }

    @Test
    fun theDebugBuildNeverClaimsToBeUpToDate() {
        show(update = AboutUpdateUi.NotChecked)
        compose.onNodeWithText("Esta versión de pruebas no busca actualizaciones.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ya tienes la última versión").assertDoesNotExist()
    }

    @Test
    fun caregiverEntryIsAQuietLinkWithANoteAtTheBottom() {
        var clicked = false
        show(onCaregiver = { clicked = true })
        compose.onNodeWithText("Solo para quien configuró la app").performScrollTo().assertExists()
        val link = compose.onNodeWithText("Para quien ayuda").performScrollTo().assertHeightIsAtLeast(64.dp)
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
