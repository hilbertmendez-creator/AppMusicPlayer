package com.mimusica.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.mimusica.app.data.settings.TextScale
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

    private fun song(id: String = "1", title: String = "Besame Mucho", hasLyrics: Boolean = true) = Song(
        id = id, sourceId = "s$id", title = title, artist = "Luis Miguel",
        durationSeconds = 180, thumbnailUrl = null,
        hasSyncedLyrics = hasLyrics, hasPlainLyrics = false, lyricsOffsetMs = 0, audioPath = "/a.m4a",
    )

    /** 1080x2400 @ 420dpi (Pixel 7a) is about 411x914dp. */
    private fun show(
        songs: List<Song>,
        scale: TextScale = TextScale.NORMAL,
        onSearch: () -> Unit = {},
        onPlay: (Int) -> Unit = {},
        onPlayAll: (List<Song>) -> Unit = {},
    ) {
        compose.setContent {
            MiMusicaTheme(textScale = scale) {
                Box(Modifier.size(411.dp, 914.dp)) {
                    SongsScreen(
                        songs = songs, error = null, onPlay = onPlay, onPlayAll = onPlayAll, onDelete = {},
                        onSearch = onSearch, onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun deleteSitsOnTheSameRowAsPlayAndIsStillBigEnough() {
        show(listOf(song()))
        val play = compose.onNodeWithText("Reproducir").getUnclippedBoundsInRoot()
        val delete = compose.onNodeWithText("Borrar").getUnclippedBoundsInRoot()
        val deleteCenter = (delete.top + delete.bottom) / 2
        assertTrue("Borrar must share Reproducir's row", deleteCenter >= play.top && deleteCenter <= play.bottom)
        assertTrue("Borrar must sit to the side of Reproducir", delete.left >= play.right)
        compose.onNodeWithText("Borrar").assertHeightIsAtLeast(64.dp)
        compose.onNodeWithText("Reproducir").assertHeightIsAtLeast(72.dp)
    }

    @Test
    fun rowsStayCompact() {
        show(listOf(song(), song(id = "2", title = "Sabor a mí")))
        // Cover + title + one action row: the old card was ~280dp tall.
        val first = compose.onNodeWithText("Besame Mucho").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("Sabor a mí").getUnclippedBoundsInRoot()
        assertTrue("two songs must be closer than 230dp", second.top - first.top < 230.dp)
    }

    @Test
    fun playAndDeleteNeverOverlapAtTheLargestTextSize() {
        show(listOf(song()), scale = TextScale.EXTRA_LARGE)
        val play = compose.onNodeWithText("Reproducir").performScrollTo().getUnclippedBoundsInRoot()
        val delete = compose.onNodeWithText("Borrar").performScrollTo().getUnclippedBoundsInRoot()
        assertTrue(
            "Borrar must be beside or below Reproducir, never on top of it",
            delete.left >= play.right || delete.top >= play.bottom,
        )
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

    // --- Reproducir todo + count -------------------------------------------------------------------------

    @Test
    fun playAllAndCountAppearWithTwoOrMoreSongs() {
        show(listOf(song(), song(id = "2", title = "Sabor a mí"), song(id = "3", title = "Cielito Lindo")))
        compose.onNodeWithText("3 canciones").assertIsDisplayed()
        compose.onNodeWithText("Reproducir todo").assertIsDisplayed().assertHeightIsAtLeast(72.dp)
    }

    @Test
    fun playAllCallsBackWithEverySongInOrder() {
        val songs = listOf(song(), song(id = "2", title = "Sabor a mí"), song(id = "3", title = "Cielito Lindo"))
        var received: List<Song>? = null
        show(songs, onPlayAll = { received = it })
        compose.onNodeWithText("Reproducir todo").performClick()
        assertEquals(songs, received)
    }

    @Test
    fun playAllAndCountAreHiddenWithASingleSong() {
        show(listOf(song()))
        compose.onAllNodesWithText("Reproducir todo").assertCountEquals(0)
        compose.onAllNodesWithText("1 canción").assertCountEquals(0)
    }

    @Test
    fun playAllAndCountAreHiddenWhenEmpty() {
        show(emptyList())
        compose.onAllNodesWithText("Reproducir todo").assertCountEquals(0)
    }

    @Test
    fun playAllIsAboveTheFirstSong() {
        show(listOf(song(), song(id = "2", title = "Sabor a mí")))
        val all = compose.onNodeWithText("Reproducir todo").getUnclippedBoundsInRoot()
        val first = compose.onNodeWithText("Besame Mucho").getUnclippedBoundsInRoot()
        assertTrue(all.bottom <= first.top)
    }

    @Test
    fun songButtonsStillSpeakTheSong() {
        show(listOf(song()))
        compose.onNodeWithContentDescription("Reproducir Besame Mucho").assertIsDisplayed()
        compose.onNodeWithContentDescription("Borrar Besame Mucho").assertIsDisplayed()
    }
}
