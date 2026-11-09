package com.mimusica.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mimusica.app.ui.theme.Navy
import com.mimusica.app.ui.theme.SectionLists
import com.mimusica.app.ui.theme.SectionSearch
import com.mimusica.app.ui.theme.SectionSettings
import com.mimusica.app.ui.theme.SectionSing

/**
 * The colour and icon of a Home tile, carried over to the screen that tile opens so nobody has to wonder where
 * they are. An accent only: a small chip beside the title and a thin bar under the header.
 */
data class SectionAccent(val color: Color, val icon: ImageVector)

object SectionAccents {
    val Home = SectionAccent(Navy, Icons.Filled.Home)
    val Songs = SectionAccent(Navy, Icons.Filled.LibraryMusic)
    val Playlists = SectionAccent(SectionLists, Icons.Filled.QueueMusic)
    val Search = SectionAccent(SectionSearch, Icons.Filled.Search)

    // Lyrics icon, not a microphone: the mic is reserved for "Hablar" (voice search).
    val Sing = SectionAccent(SectionSing, Icons.Filled.Lyrics)

    val Settings = SectionAccent(SectionSettings, Icons.Filled.Settings)

    val all = listOf(Home, Songs, Playlists, Search, Sing, Settings)
}
