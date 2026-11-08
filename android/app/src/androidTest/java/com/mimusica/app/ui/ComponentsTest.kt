package com.mimusica.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.mimusica.app.ui.components.ScreenScaffold
import com.mimusica.app.ui.components.SectionAccent
import com.mimusica.app.ui.components.SectionAccents
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.mimusica.app.ui.components.ConfirmDeleteDialog
import com.mimusica.app.ui.components.StatusPill
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun showDialog(
        note: String? = "Podrás volver a descargarla desde Buscar.",
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        compose.setContent {
            MiMusicaTheme {
                ConfirmDeleteDialog(
                    message = "¿Quieres borrar “Besame Mucho”?",
                    note = note,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    // --- Delete confirmation: the safe choice is the primary one --------------------------------------------

    @Test
    fun safeChoiceIsPrimaryAndComesFirst() {
        showDialog()
        val keep = compose.onNodeWithText("No, conservarla").assertIsDisplayed().assertHeightIsAtLeast(72.dp)
        val delete = compose.onNodeWithText("Sí, borrar").assertIsDisplayed().assertHeightIsAtLeast(64.dp)
        assertTrue(
            "the safe button must come before the destructive one",
            keep.getUnclippedBoundsInRoot().top < delete.getUnclippedBoundsInRoot().top,
        )
    }

    @Test
    fun oldBareNoIsGone() {
        showDialog()
        compose.onAllNodesWithText("No").assertCountEquals(0)
    }

    @Test
    fun bodyReassuresThatTheSongCanComeBack() {
        showDialog()
        compose.onNodeWithText("¿Quieres borrar “Besame Mucho”?").assertIsDisplayed()
        compose.onNodeWithText("Podrás volver a descargarla desde Buscar.").assertIsDisplayed()
    }

    @Test
    fun noReassuranceLineWhenNoneIsGiven() {
        showDialog(note = null)
        compose.onAllNodesWithText("Podrás volver a descargarla desde Buscar.").assertCountEquals(0)
    }

    @Test
    fun keepingDismissesAndDeletingConfirms() {
        var confirmed = 0
        var dismissed = 0
        showDialog(onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        compose.onNodeWithText("No, conservarla").performClick()
        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
        compose.onNodeWithText("Sí, borrar").performClick()
        assertEquals(1, confirmed)
    }

    // --- Status pill reads as status, not as a button -------------------------------------------------------

    @Test
    fun statusPillIsNotClickableAndKeepsItsLiveRegion() {
        compose.setContent {
            MiMusicaTheme {
                StatusPill(
                    "¡Lista! Ya está en tus canciones",
                    Icons.Filled.CheckCircle,
                    modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        compose.onNodeWithText("¡Lista! Ya está en tus canciones").assertIsDisplayed().assertHasNoClickAction()
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
        compose.onNodeWithText("¡Lista! Ya está en tus canciones").assert(
            SemanticsMatcher("polite live region") {
                it.config.getOrNull(SemanticsProperties.LiveRegion) == LiveRegionMode.Polite
            },
        )
    }

    @Test
    fun statusPillHasNoButtonSizedSlab() {
        compose.setContent {
            MiMusicaTheme { StatusPill("¡Lista! Ya está en tus canciones", Icons.Filled.CheckCircle) }
        }
        // It is a line of status, clearly shorter than the 72dp primary buttons it used to imitate.
        val bounds = compose.onNodeWithText("¡Lista! Ya está en tus canciones").getUnclippedBoundsInRoot()
        assertTrue("status must be shorter than a button, was ${bounds.height}", bounds.height < 72.dp)
    }

    // --- Screen header: Volver and the title share a row -----------------------------------------------------

    private fun showScaffold(title: String, fontScale: Float = 1f, accent: SectionAccent? = null) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                MiMusicaTheme {
                    // A fixed phone width keeps the side-by-side / stacked decision independent of the test device.
                    Box(Modifier.width(360.dp).height(640.dp)) {
                        ScreenScaffold(title = title, onBack = {}, accent = accent) { Text("contenido") }
                    }
                }
            }
        }
    }

    @Test
    fun volverAndAShortTitleShareOneRowAndContentStartsHigh() {
        showScaffold("Buscar")
        val back = compose.onNodeWithText("Volver").assertHeightIsAtLeast(64.dp).getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("Buscar").getUnclippedBoundsInRoot()
        assertTrue("title must sit right of Volver", title.left >= back.right)
        assertTrue("title must share Volver's row", title.top < back.bottom)
        // Measured from Volver, not from the screen top: on a real phone the status bar inset sits above both.
        // One row = 64dp button + 16dp gap = 80dp; a stacked header would add another 12dp + the title (~54dp).
        val content = compose.onNodeWithText("contenido").getUnclippedBoundsInRoot()
        val offset = content.top - back.top
        assertTrue("content starts ${offset} below Volver's top", offset < 100.dp)
    }

    @Test
    fun titleDropsBelowVolverWhenTheTextIsTooBigForOneRow() {
        // "Configuración" (a real screen title) cannot fit beside Volver at the capped 1.6x on a 360dp phone.
        // Shorter words like "Mis canciones" legitimately stay beside Volver and wrap between words.
        showScaffold("Configuración inicial", fontScale = 2f)
        val back = compose.onNodeWithText("Volver").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("Configuración inicial").getUnclippedBoundsInRoot()
        assertTrue("title must be below Volver (back=$back, title=$title)", title.top >= back.bottom)
        assertTrue("title must not be clipped by the screen edge", title.right <= 360.dp)
    }

    // --- Section accent: chip + thin bar, only when a screen opts in -----------------------------------------

    @Test
    fun accentAddsAChipAndABarWithoutMovingTheTitleOffScreen() {
        showScaffold("Buscar", accent = SectionAccents.Search)
        val title = compose.onNodeWithText("Buscar").getUnclippedBoundsInRoot()
        assertTrue("title must stay inside the screen, right edge ${title.right}", title.right <= 360.dp)
        compose.onNodeWithText("contenido").assertIsDisplayed()
    }
}
