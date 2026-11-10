package com.mimusica.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.data.update.UpdateState
import com.mimusica.app.ui.components.MinTouch
import com.mimusica.app.ui.components.ScreenScaffold
import com.mimusica.app.ui.components.SectionAccents
import com.mimusica.app.ui.theme.InkSoft
import com.mimusica.app.ui.theme.MiMusicaTheme
import com.mimusica.app.ui.theme.Navy
import com.mimusica.app.ui.theme.OnNavy
import com.mimusica.app.ui.theme.Card

/** Minimum heights of the hero card and of the two list rows. */
val HeroMinHeight = 96.dp
val HomeRowMinHeight = 76.dp

/**
 * Presentational "Inicio": the section header, the update card when there is one, the hero "Seguir cantando"
 * (one button that opens the karaoke, or Buscar when no song has lyrics yet) and two full-width rows,
 * "Mis canciones" and "Mis listas". The mini player and the tab bar live in the navigation shell, not here.
 * A null [summary] means the library is still loading: the rows show no subtitle and the hero does nothing yet.
 */
@Composable
fun HomeScreen(
    onContinue: (songId: String) -> Unit,
    onSearch: () -> Unit,
    onSongs: () -> Unit,
    onPlaylists: () -> Unit,
    modifier: Modifier = Modifier,
    summary: HomeSummary? = null,
    updateState: UpdateState = UpdateState.None,
    onUpdate: () -> Unit = {},
) {
    // Null summary = still loading: no subtitle rather than a wrong "Todavía no tienes canciones".
    val songsSubtitle = summary?.let {
        if (it.songs == 0) stringResource(R.string.home_songs_none)
        else pluralStringResource(R.plurals.song_count, it.songs, it.songs)
    }
    val playlistsSubtitle = summary?.let {
        if (it.playlists == 0) stringResource(R.string.home_playlists_none)
        else pluralStringResource(R.plurals.home_playlists_count, it.playlists, it.playlists)
    }
    ScreenScaffold(
        title = stringResource(R.string.app_name),
        onBack = null,
        modifier = modifier,
        accent = SectionAccents.Home,
    ) {
        if (updateState !is UpdateState.None) {
            UpdateBanner(updateState, onClick = onUpdate)
        }
        val song = summary?.continueSong
        HeroCard(
            loaded = summary != null,
            songTitle = song?.title,
            onClick = { if (song != null) onContinue(song.id) else onSearch() },
        )
        HomeRow(R.string.home_songs, songsSubtitle, Icons.Filled.LibraryMusic, onSongs)
        HomeRow(R.string.home_playlists, playlistsSubtitle, Icons.Filled.QueueMusic, onPlaylists)
    }
}

/**
 * Big, plain "new version" card above the hero: says what is happening in two short lines and is one tap to
 * act. While the file downloads it is not clickable (a second tap would start a second download).
 */
@Composable
private fun UpdateBanner(state: UpdateState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val busy = state is UpdateState.Downloading
    val title = stringResource(
        when (state) {
            is UpdateState.Downloading -> R.string.update_downloading
            is UpdateState.Failed -> R.string.update_failed
            else -> R.string.update_available
        },
    )
    val hint = stringResource(if (busy) R.string.update_wait else R.string.update_tap)
    Surface(
        onClick = onClick,
        enabled = !busy,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouch)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/**
 * "Seguir cantando": the one big Navy card. Left, the small line and the song title (one line, ellipsis); right, a
 * translucent box with the lyrics icon. The whole card is one button. With no singable song it says so and opens
 * Buscar instead. While the library is still loading it is shown but does nothing.
 */
@Composable
private fun HeroCard(loaded: Boolean, songTitle: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.continue_singing)
    val empty = stringResource(R.string.continue_singing_none)
    val spoken = when {
        !loaded -> label
        songTitle != null -> stringResource(R.string.continue_singing_desc, songTitle)
        else -> empty
    }
    Surface(
        onClick = onClick,
        enabled = loaded,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeroMinHeight)
            .semantics { contentDescription = spoken },
        shape = RoundedCornerShape(22.dp),
        color = Navy,
        contentColor = OnNavy,
        // A disabled Surface would dim its own colours; the card keeps them while loading.
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (loaded && songTitle == null) {
                    Text(
                        empty,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = OnNavy.copy(alpha = 0.85f))
                    if (songTitle != null) {
                        Text(
                            songTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(OnNavy.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lyrics, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }
    }
}

/** Full-width row: icon in a Navy circle, title with its live count, and a chevron that says "opens a screen". */
@Composable
private fun HomeRow(
    labelRes: Int,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HomeRowMinHeight)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(18.dp),
        color = Card,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Navy, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = OnNavy, modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                )
                if (subtitle != null) {
                    // No line cap: at the largest text sizes the subtitle wraps and the row grows (it has a
                    // minimum height, not a fixed one) instead of cutting a count short.
                    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = InkSoft, textAlign = TextAlign.Start)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = InkSoft, modifier = Modifier.size(32.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomeScreenPreview() {
    MiMusicaTheme {
        HomeScreen(
            summary = HomeSummary(songs = 12, playlists = 2, singable = 8),
            onContinue = {}, onSearch = {}, onSongs = {}, onPlaylists = {},
        )
    }
}
