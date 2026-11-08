package com.mimusica.app.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.sing.SingScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(songs: List<Song>, onSearch: () -> Unit = {}) {
        compose.setContent {
            MiMusicaTheme { SingScreen(songs = songs, onSing = {}, onSearch = onSearch, onBack = {}) }
        }
    }

    @Test
    fun emptyStateExplainsWhyAndOffersSearch() {
        var searches = 0
        show(emptyList(), onSearch = { searches++ })
        compose.onNodeWithText(
            "Todavía no hay canciones con letra. Cuando descargas una canción, buscamos su letra.",
        ).assertIsDisplayed()
        compose.onNodeWithText("Buscar canciones").assertIsDisplayed().assertHeightIsAtLeast(64.dp)
        compose.onNodeWithText("Buscar canciones").performClick()
        assertEquals(1, searches)
    }

    @Test
    fun noSearchActionWhenThereAreSongsToSing() {
        val song = Song(
            id = "1", sourceId = "s1", title = "Besame Mucho", artist = "Luis Miguel",
            durationSeconds = 180, thumbnailUrl = null,
            hasSyncedLyrics = true, hasPlainLyrics = false, lyricsOffsetMs = 0, audioPath = "/a.m4a",
        )
        show(listOf(song))
        // "Cantar" is both the screen title and the row button, so assert on the song instead.
        compose.onNodeWithText("Besame Mucho", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Buscar canciones").assertDoesNotExist()
    }

    private fun songWith(synced: Boolean, plain: Boolean) = Song(
        id = if (synced) "s" else "p", sourceId = "src", title = if (synced) "Con tiempos" else "Solo letra",
        artist = "Alguien", durationSeconds = 180, thumbnailUrl = null,
        hasSyncedLyrics = synced, hasPlainLyrics = plain, lyricsOffsetMs = 0, audioPath = "/a.m4a",
    )

    @Test
    fun plainOnlySongsAreTaggedLetraCompleta() {
        show(listOf(songWith(synced = false, plain = true)))
        compose.onNodeWithText("Letra completa", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun syncedSongsHaveNoTag() {
        show(listOf(songWith(synced = true, plain = false)))
        compose.onNodeWithText("Letra completa", useUnmergedTree = true).assertDoesNotExist()
    }
}
