package com.mimusica.app.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.playlists.AddToListScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddToListScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val lists = listOf(Playlist("p1", "Boleros", listOf("a")), Playlist("p2", "Para bailar", emptyList()))

    private fun show(
        addedTo: String? = null,
        song: Song? = null,
        onChoose: (Playlist) -> Unit = {},
        onNew: () -> Unit = {},
    ) {
        compose.setContent {
            MiMusicaTheme {
                AddToListScreen(playlists = lists, addedTo = addedTo, song = song, onChoose = onChoose, onNew = onNew, onBack = {})
            }
        }
    }

    @Test
    fun listsEveryPlaylistAsABigRowAndChoosingOneReportsIt() {
        var chosen: Playlist? = null
        show(onChoose = { chosen = it })
        compose.onNodeWithText("Boleros").assertHeightIsAtLeast(64.dp)
        compose.onNodeWithText("Para bailar").performClick()
        assertEquals("p2", chosen?.id)
    }

    @Test
    fun offersANewList() {
        var asked = false
        show(onNew = { asked = true })
        compose.onNodeWithText("Nueva lista").assertHeightIsAtLeast(64.dp).performClick()
        assertTrue(asked)
    }

    @Test
    fun confirmsWhereTheSongWent() {
        show(addedTo = "Boleros")
        compose.onNodeWithText("Listo, se agregó a “Boleros”.").assertIsDisplayed()
    }

    @Test
    fun saysWhichSongIsBeingAdded() {
        show(
            song = Song(
                id = "s1", sourceId = "x", title = "Cielito Lindo", artist = "Mariachi Mexico Joven",
                durationSeconds = 180, thumbnailUrl = null, hasSyncedLyrics = false, hasPlainLyrics = false,
                lyricsOffsetMs = 0, audioPath = "/a.m4a",
            ),
        )
        compose.onNodeWithText("Cielito Lindo · Mariachi Mexico Joven").assertIsDisplayed()
    }
}
