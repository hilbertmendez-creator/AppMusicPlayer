package com.mimusica.app.ui.songs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.components.Artwork
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.ConfirmDeleteDialog
import com.mimusica.app.ui.components.DemotedDangerAction
import com.mimusica.app.ui.components.EmptyMessage
import com.mimusica.app.ui.components.ErrorBanner
import com.mimusica.app.ui.components.MinTouch
import com.mimusica.app.ui.components.OutlineBigButton
import com.mimusica.app.ui.components.RowCard
import com.mimusica.app.ui.components.ScreenScaffold
import com.mimusica.app.ui.state.UiError

/**
 * Presentational "Mis canciones". Each card: a big "Reproducir" first, "Cantar" (when there are lyrics) second,
 * and "Borrar" demoted and apart, behind a confirmation. Tapping the card itself also plays.
 */
@Composable
fun SongsScreen(
    songs: List<Song>,
    error: UiError?,
    onPlay: (index: Int) -> Unit,
    onSing: (Song) -> Unit,
    onDelete: (Song) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<Song?>(null) }
    ScreenScaffold(title = stringResource(R.string.songs_title), onBack = onBack, modifier = modifier) {
        error?.let { ErrorBanner(it) }
        if (songs.isEmpty()) {
            EmptyMessage(
                text = stringResource(R.string.songs_empty),
                actionText = stringResource(R.string.songs_empty_action),
                onAction = onSearch,
                actionIcon = Icons.Filled.Search,
            )
        }
        songs.forEachIndexed { index, song ->
            SongCard(
                song = song,
                onPlay = { onPlay(index) },
                onSing = { onSing(song) },
                onDelete = { pendingDelete = song },
            )
        }
    }
    pendingDelete?.let { song ->
        ConfirmDeleteDialog(
            message = stringResource(R.string.delete_song_confirm, song.title),
            onConfirm = {
                pendingDelete = null
                onDelete(song)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun SongCard(song: Song, onPlay: () -> Unit, onSing: () -> Unit, onDelete: () -> Unit) {
    RowCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouch)
                .clickable(
                    onClickLabel = stringResource(R.string.song_play_desc, song.title),
                    onClick = onPlay,
                ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(url = song.thumbnailUrl, size = 72.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        BigButton(stringResource(R.string.play), onPlay, icon = Icons.Filled.PlayArrow)
        if (song.hasLyrics) {
            OutlineBigButton(
                text = stringResource(R.string.sing),
                onClick = onSing,
                icon = Icons.Filled.Lyrics,
                minHeight = 72.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DemotedDangerAction(stringResource(R.string.delete), onDelete, icon = Icons.Filled.Delete)
    }
}
