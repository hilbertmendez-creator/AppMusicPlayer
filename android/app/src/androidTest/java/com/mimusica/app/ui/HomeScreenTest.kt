package com.mimusica.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.home.HomeScreen
import com.mimusica.app.ui.home.HomeSummary
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun song(id: String, title: String, synced: Boolean) = Song(
        id = id, sourceId = "s$id", title = title, artist = "Gardel", durationSeconds = 200, thumbnailUrl = null,
        hasSyncedLyrics = synced, hasPlainLyrics = false, lyricsOffsetMs = 0, audioPath = "/a.m4a",
    )

    private val hero = song("1", title = "Volver a empezar", synced = true)

    private fun show(
        summary: HomeSummary? = null,
        onContinue: (String) -> Unit = {},
        onSearch: () -> Unit = {},
        onSongs: () -> Unit = {},
        onPlaylists: () -> Unit = {},
        scale: TextScale = TextScale.NORMAL,
    ) {
        compose.setContent {
            MiMusicaTheme(textScale = scale) {
                Box(Modifier.size(411.dp, 914.dp)) {
                    HomeScreen(
                        summary = summary,
                        onContinue = onContinue, onSearch = onSearch, onSongs = onSongs, onPlaylists = onPlaylists,
                    )
                }
            }
        }
    }

    private fun loaded(continueSong: Song? = hero) =
        HomeSummary(songs = 5, playlists = 2, singable = if (continueSong != null) 3 else 0, continueSong = continueSong)

    @Test
    fun showsTheAppNameAndTheTwoRowsAndNoOldTiles() {
        show(loaded())
        compose.onNodeWithText("Mi Música").assertIsDisplayed()
        compose.onNodeWithText("Mis canciones").assertIsDisplayed()
        compose.onNodeWithText("Mis listas").assertIsDisplayed()
        // Buscar and Cantar are tabs now, and the "Aa" pill moved into Ajustes.
        compose.onAllNodesWithText("Buscar").assertCountEquals(0)
        compose.onAllNodesWithText("Cantar").assertCountEquals(0)
        compose.onAllNodesWithText("Aa").assertCountEquals(0)
    }

    @Test
    fun heroNamesTheSongAndOpensItsKaraoke() {
        var opened: String? = null
        show(loaded(), onContinue = { opened = it })
        compose.onNodeWithText("Seguir cantando").assertIsDisplayed()
        compose.onNodeWithText("Volver a empezar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Seguir cantando: Volver a empezar").performClick()
        assertEquals("1", opened)
    }

    @Test
    fun heroWithoutSingableSongsSaysSoAndOpensSearch() {
        var searched = 0
        var opened = 0
        show(loaded(continueSong = null), onContinue = { opened++ }, onSearch = { searched++ })
        compose.onNodeWithText("Todavía no hay canciones con letra").assertIsDisplayed().performClick()
        assertEquals(1, searched)
        assertEquals(0, opened)
    }

    @Test
    fun heroDoesNothingWhileTheLibraryIsStillLoading() {
        var searched = 0
        var opened = 0
        show(summary = null, onContinue = { opened++ }, onSearch = { searched++ })
        compose.onNodeWithText("Seguir cantando").assertIsDisplayed().performClick()
        assertEquals(0, searched + opened)
        compose.onAllNodesWithText("Todavía no hay canciones con letra").assertCountEquals(0)
    }

    @Test
    fun heroIsAtLeast96dpAndRowsAtLeast76dp() {
        show(loaded())
        compose.onNodeWithContentDescription("Seguir cantando: Volver a empezar").assertHeightIsAtLeast(96.dp)
        compose.onNode(hasText("Mis canciones")).assertHeightIsAtLeast(76.dp)
        compose.onNode(hasText("Mis listas")).assertHeightIsAtLeast(76.dp)
    }

    @Test
    fun rowsShowLiveCountsAndOpenTheirScreens() {
        var songs = 0
        var lists = 0
        show(loaded(), onSongs = { songs++ }, onPlaylists = { lists++ })
        compose.onNodeWithText("5 canciones").assertIsDisplayed()
        compose.onNodeWithText("2 listas").assertIsDisplayed()
        compose.onNodeWithText("Mis canciones").performClick()
        compose.onNodeWithText("Mis listas").performClick()
        assertEquals(1, songs)
        assertEquals(1, lists)
    }

    @Test
    fun rowsUseTheSingularForOne() {
        show(HomeSummary(songs = 1, playlists = 1, singable = 1, continueSong = hero))
        compose.onNodeWithText("1 canción").assertIsDisplayed()
        compose.onNodeWithText("1 lista").assertIsDisplayed()
    }

    @Test
    fun rowsWelcomeAnEmptyLibrary() {
        show(HomeSummary(songs = 0, playlists = 0, singable = 0))
        compose.onNodeWithText("Todavía no tienes canciones").assertIsDisplayed()
        compose.onNodeWithText("Ninguna lista todavía").assertIsDisplayed()
    }

    @Test
    fun rowIsOneSpokenUnitWithItsCount() {
        show(loaded())
        // Title and count live in one merged, clickable node.
        compose.onNode(hasText("Mis canciones") and hasText("5 canciones")).assertExists()
    }

    @Test
    fun noMiniPlayerIsRenderedByHome() {
        show(loaded())
        compose.onAllNodesWithContentDescription("Pausar").assertCountEquals(0)
        compose.onAllNodesWithText("Sonando").assertCountEquals(0)
    }

    @Test
    fun aLongSongTitleStaysOnOneLineAtTheLargestTextSize() {
        val long = song("2", title = "Himno de la alegría y de la esperanza para todos los días", synced = true)
        show(loaded(long), scale = TextScale.EXTRA_LARGE)
        val node = compose.onNodeWithText(long.title, useUnmergedTree = true).performScrollTo()
        node.assertIsDisplayed()
        val results = mutableListOf<TextLayoutResult>()
        // fetchSemanticsNode synchronises with the UI thread, so it must not run inside runOnUiThread.
        val config = node.fetchSemanticsNode().config
        compose.runOnUiThread { config[SemanticsActions.GetTextLayoutResult].action!!(results) }
        assertEquals("the title must stay on one line", 1, results.first().lineCount)
        assertTrue("the title is cut with an ellipsis", results.first().hasVisualOverflow)
    }

    @Test
    fun heroAndRowsReachableAtTheLargestTextSizeOnASmallPhone() {
        compose.setContent {
            MiMusicaTheme(textScale = TextScale.EXTRA_LARGE) {
                Box(Modifier.size(360.dp, 640.dp)) {
                    HomeScreen(
                        summary = loaded(),
                        onContinue = {}, onSearch = {}, onSongs = {}, onPlaylists = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Seguir cantando").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Mis canciones").performScrollTo().assertHeightIsAtLeast(76.dp)
        compose.onNodeWithText("Mis listas").performScrollTo().assertHeightIsAtLeast(76.dp)
    }
}
