package com.mimusica.app.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mimusica.app.ui.home.HomeScreen
import com.mimusica.app.ui.home.MiniPlayerUi
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(mini: MiniPlayerUi? = null, onSearch: () -> Unit = {}) {
        compose.setContent {
            MiMusicaTheme {
                HomeScreen(
                    miniPlayer = mini,
                    onSongs = {}, onPlaylists = {}, onSearch = onSearch, onSing = {},
                    onSettings = {}, onMiniPlayer = {}, onMiniToggle = {},
                )
            }
        }
    }

    @Test
    fun showsTheFourMainButtons() {
        show()
        listOf("Mis canciones", "Mis listas", "Buscar", "Cantar").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun mainButtonsAreAtLeast64dpTall() {
        show()
        listOf("Mis canciones", "Mis listas", "Buscar", "Cantar").forEach {
            compose.onNodeWithText(it).assertHeightIsAtLeast(64.dp)
        }
    }

    @Test
    fun textSizeButtonShowsAaAndIsNamedForTalkBack() {
        show()
        compose.onNodeWithText("Aa").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tamaño de letra")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(64.dp)
            .assertWidthIsAtLeast(64.dp)
    }

    @Test
    fun miniPlayerShowsTitleArtistAndABigToggle() {
        show(mini = MiniPlayerUi("Volver", "Carlos Gardel", isPlaying = true))
        compose.onNodeWithText("Carlos Gardel").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pausar").assertHeightIsAtLeast(64.dp)
    }

    @Test
    fun showsTheAppNameAsAHeading() {
        show()
        compose.onNodeWithText("Mi Música").assertIsDisplayed()
    }

    @Test
    fun searchButtonInvokesCallback() {
        var clicked = false
        show(onSearch = { clicked = true })
        compose.onNodeWithText("Buscar").performClick()
        assertTrue(clicked)
    }

    @Test
    fun miniPlayerShowsOnlyWhileSomethingPlays() {
        show(mini = MiniPlayerUi("Volver", "Gardel", isPlaying = true))
        compose.onNodeWithText("Sonando ahora").assertIsDisplayed()
        compose.onNodeWithText("Volver").assertIsDisplayed()
    }
}
