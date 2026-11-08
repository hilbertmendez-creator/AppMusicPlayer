package com.mimusica.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mimusica.app.ui.theme.AmberBrown
import com.mimusica.app.ui.theme.DeepBlue
import com.mimusica.app.ui.theme.DeepTeal
import com.mimusica.app.ui.theme.Plum

/**
 * The colour and icon of a Home tile, carried over to the screen that tile opens so nobody has to wonder where
 * they are. An accent only: a small chip beside the title and a thin bar under the header.
 */
data class SectionAccent(val color: Color, val icon: ImageVector)

object SectionAccents {
    val Songs = SectionAccent(DeepBlue, Icons.Filled.LibraryMusic)
    val Playlists = SectionAccent(Plum, Icons.Filled.QueueMusic)
    val Search = SectionAccent(DeepTeal, Icons.Filled.Search)

    // Lyrics icon, not a microphone: the mic is reserved for "Hablar" (voice search).
    val Sing = SectionAccent(AmberBrown, Icons.Filled.Lyrics)

    val all = listOf(Songs, Playlists, Search, Sing)
}
