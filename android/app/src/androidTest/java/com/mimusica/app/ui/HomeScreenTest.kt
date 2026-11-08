package com.mimusica.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.test.assertCountEquals
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.ui.home.HomeScreen
import com.mimusica.app.ui.home.HomeSummary
import com.mimusica.app.ui.home.MiniPlayerUi
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        mini: MiniPlayerUi? = null,
        summary: HomeSummary? = null,
        onSearch: () -> Unit = {},
    ) {
        compose.setContent {
            MiMusicaTheme {
                HomeScreen(
                    miniPlayer = mini,
                    summary = summary,
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
        // The spoken name starts with the visible "Aa" (label in name).
        compose.onNodeWithContentDescription("Aa, Tamaño de letra")
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

    // --- Step 2: live subtitles ---------------------------------------------------------------------------

    @Test
    fun subtitlesShowLiveCounts() {
        show(summary = HomeSummary(songs = 5, playlists = 2, singable = 3))
        compose.onNodeWithText("5 canciones").assertIsDisplayed()
        compose.onNodeWithText("2 listas").assertIsDisplayed()
        compose.onNodeWithText("Di o escribe una canción").assertIsDisplayed()
        compose.onNodeWithText("3 con letra").assertIsDisplayed()
    }

    @Test
    fun subtitlesUseTheSingularForOne() {
        show(summary = HomeSummary(songs = 1, playlists = 1, singable = 1))
        compose.onNodeWithText("1 canción").assertIsDisplayed()
        compose.onNodeWithText("1 lista").assertIsDisplayed()
    }

    @Test
    fun subtitlesWelcomeAnEmptyLibrary() {
        show(summary = HomeSummary(songs = 0, playlists = 0, singable = 0))
        compose.onNodeWithText("Todavía no tienes canciones").assertIsDisplayed()
        compose.onNodeWithText("Ninguna lista todavía").assertIsDisplayed()
        compose.onNodeWithText("Todavía ninguna con letra").assertIsDisplayed()
    }

    @Test
    fun tileIsOneSpokenUnitWithItsSubtitle() {
        show(summary = HomeSummary(songs = 5, playlists = 2, singable = 3))
        compose.onNodeWithText("Mis canciones").assertIsDisplayed()
        compose.onNodeWithText("5 canciones").assertIsDisplayed()
        // Title and subtitle live in one merged, clickable node.
        compose.onNode(
            hasText("Mis canciones") and hasText("5 canciones"),
        ).assertExists()
    }

    // --- Step 2: stable tiles -----------------------------------------------------------------------------

    /** 1080x2400 @ 420dpi (Pixel 7a) is about 411x914dp. */
    @Test
    fun tilesKeepTheirHeightWhenTheMiniPlayerAppears() {
        var mini by mutableStateOf<MiniPlayerUi?>(null)
        compose.setContent {
            MiMusicaTheme {
                Box(Modifier.size(411.dp, 914.dp)) {
                    HomeScreen(
                        miniPlayer = mini,
                        summary = HomeSummary(songs = 5, playlists = 2, singable = 3),
                        onSongs = {}, onPlaylists = {}, onSearch = {}, onSing = {},
                        onSettings = {}, onMiniPlayer = {}, onMiniToggle = {},
                    )
                }
            }
        }
        val labels = listOf("Mis canciones", "Mis listas", "Buscar", "Cantar")
        val before = labels.map { compose.onNodeWithText(it).getUnclippedBoundsInRoot() }

        mini = MiniPlayerUi("Volver", "Carlos Gardel", isPlaying = true)
        compose.waitForIdle()
        val after = labels.map { compose.onNodeWithText(it).getUnclippedBoundsInRoot() }

        before.zip(after).forEach { (b, a) ->
            assertEquals("tile height must not change", b.height.value, a.height.value, 0.5f)
            assertEquals("tile must not move", b.top.value, a.top.value, 0.5f)
        }
    }

    @Test
    fun tilesDoNotClipAtTheLargestTextSize() {
        compose.setContent {
            MiMusicaTheme(textScale = TextScale.EXTRA_LARGE) {
                Box(Modifier.size(360.dp, 640.dp)) {
                    HomeScreen(
                        miniPlayer = MiniPlayerUi("Volver", "Carlos Gardel", isPlaying = true),
                        summary = HomeSummary(songs = 5, playlists = 2, singable = 3),
                        onSongs = {}, onPlaylists = {}, onSearch = {}, onSing = {},
                        onSettings = {}, onMiniPlayer = {}, onMiniToggle = {},
                    )
                }
            }
        }
        // The column scrolls, so every tile can be reached and each one is at least 64dp tall.
        listOf("Mis canciones", "Mis listas", "Buscar", "Cantar").forEach {
            compose.onNodeWithText(it).performScrollTo().assertHeightIsAtLeast(64.dp)
        }
    }

    // --- Step 3: mini-player accessibility ---------------------------------------------------------------

    @Test
    fun miniPlayerCardOffersAnOpenLabelAndKeepsPauseSeparate() {
        show(mini = MiniPlayerUi("Volver", "Carlos Gardel", isPlaying = true))
        compose.onNode(
            SemanticsMatcher("click label is 'Abrir el reproductor'") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "Abrir el reproductor"
            },
        ).assertExists()
        compose.onNodeWithContentDescription("Pausar").assertIsDisplayed()
    }

    @Test
    fun miniPlayerCoverIsDecorative() {
        show(mini = MiniPlayerUi("Volver", "Carlos Gardel", isPlaying = true))
        compose.onAllNodesWithContentDescription("Portada de la canción", useUnmergedTree = true).assertCountEquals(0)
    }
}
