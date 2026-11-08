package com.mimusica.app.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.songs.SongsScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SongsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun song(hasLyrics: Boolean = true) = Song(
        id = "1", sourceId = "s1", title = "Besame Mucho", artist = "Luis Miguel",
        durationSeconds = 180, thumbnailUrl = null,
        hasSyncedLyrics = hasLyrics, hasPlainLyrics = false, lyricsOffsetMs = 0, audioPath = "/a.m4a",
    )

    private fun show(songs: List<Song>, onSearch: () -> Unit = {}, onPlay: (Int) -> Unit = {}) {
        compose.setContent {
            MiMusicaTheme {
                SongsScreen(
                    songs = songs, error = null, onPlay = onPlay, onSing = {}, onDelete = {},
                    onSearch = onSearch, onBack = {},
                )
            }
        }
    }

    @Test
    fun playComesBeforeSing() {
        show(listOf(song()))
        val play = compose.onNodeWithText("Reproducir").getUnclippedBoundsInRoot()
        val sing = compose.onNodeWithText("Cantar").getUnclippedBoundsInRoot()
        assertTrue("Reproducir must sit above Cantar", play.top < sing.top)
    }

    @Test
    fun deleteIsSeparatedFromOtherActionsAndStillBigEnough() {
        show(listOf(song()))
        val sing = compose.onNodeWithText("Cantar").getUnclippedBoundsInRoot()
        val delete = compose.onNodeWithText("Borrar").getUnclippedBoundsInRoot()
        assertTrue("Borrar must be >= 24dp below Cantar", delete.top - sing.bottom >= 24.dp)
        compose.onNodeWithText("Borrar").assertHeightIsAtLeast(64.dp)
    }

    @Test
    fun singIsHiddenWithoutLyrics() {
        show(listOf(song(hasLyrics = false)))
        compose.onNodeWithText("Reproducir").assertIsDisplayed()
        compose.onNodeWithText("Cantar").assertDoesNotExist()
    }

    @Test
    fun playButtonStartsThatSong() {
        var played = -1
        show(listOf(song()), onPlay = { played = it })
        compose.onNodeWithText("Reproducir").performClick()
        assertEquals(0, played)
    }

    @Test
    fun emptyLibraryOffersToSearch() {
        var searched = false
        show(emptyList(), onSearch = { searched = true })
        compose.onNodeWithText("Buscar canciones").assertIsDisplayed().performClick()
        assertTrue(searched)
    }
}
