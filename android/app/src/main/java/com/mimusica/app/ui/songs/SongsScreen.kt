package com.mimusica.app.ui.songs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.components.Artwork
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.ConfirmDeleteDialog
import com.mimusica.app.ui.components.DangerOutlineButton
import com.mimusica.app.ui.components.EmptyMessage
import com.mimusica.app.ui.components.ErrorBanner
import com.mimusica.app.ui.components.MessageBanner
import com.mimusica.app.ui.components.MinTouch
import com.mimusica.app.ui.components.PrimaryWithSecondary
import com.mimusica.app.ui.components.RowCard
import com.mimusica.app.ui.components.ScreenScaffold
import com.mimusica.app.ui.components.SectionAccents
import com.mimusica.app.ui.state.UiError

/**
 * Presentational "Mis canciones". With two or more songs the top shows "N canciones" and a big
 * "Reproducir todo". Each card is compact: cover, title and artist, then one big "Reproducir" with a smaller
 * "Borrar" beside it (stacked below when the text is too large), the latter behind a confirmation.
 * ("Cantar" lives on Home and on the player, not on every card.)
 */
@Composable
fun SongsScreen(
    songs: List<Song>,
    error: UiError?,
    onPlay: (index: Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onDelete: (Song) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    deletedTitle: String? = null,
) {
    var pendingDelete by remember { mutableStateOf<Song?>(null) }
    ScreenScaffold(
        title = stringResource(R.string.songs_title),
        onBack = onBack,
        modifier = modifier,
        accent = SectionAccents.Songs,
    ) {
        error?.let { ErrorBanner(it) }
        // Polite live region inside MessageBanner: TalkBack says it once the delete went through.
        deletedTitle?.let { MessageBanner(stringResource(R.string.song_deleted, it), isError = false) }
        if (songs.isEmpty()) {
            EmptyMessage(
                text = stringResource(R.string.songs_empty),
                actionText = stringResource(R.string.songs_empty_action),
                onAction = onSearch,
                actionIcon = Icons.Filled.Search,
            )
        }
        if (songs.size >= 2) {
            Text(
                pluralStringResource(R.plurals.song_count, songs.size, songs.size),
                style = MaterialTheme.typography.titleMedium,
            )
            BigButton(stringResource(R.string.play_all), { onPlayAll(songs) }, icon = Icons.Filled.PlayArrow)
        }
        songs.forEachIndexed { index, song ->
            SongCard(
                song = song,
                onPlay = { onPlay(index) },
                onDelete = { pendingDelete = song },
            )
        }
    }
    pendingDelete?.let { song ->
        ConfirmDeleteDialog(
            message = stringResource(R.string.delete_song_confirm, song.title),
            note = stringResource(R.string.delete_song_note),
            onConfirm = {
                pendingDelete = null
                onDelete(song)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun SongCard(song: Song, onPlay: () -> Unit, onDelete: () -> Unit) {
    RowCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouch)
                // One spoken unit for the song's identity; "Reproducir" below is the single way to play it.
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(url = song.thumbnailUrl, size = 72.dp, decorative = true)
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
        PrimaryWithSecondary(
            primary = {
                BigButton(
                    stringResource(R.string.play),
                    onPlay,
                    icon = Icons.Filled.PlayArrow,
                    description = stringResource(R.string.song_play_desc, song.title),
                )
            },
            secondary = {
                DangerOutlineButton(
                    stringResource(R.string.delete),
                    onDelete,
                    description = stringResource(R.string.delete_song_desc, song.title),
                )
            },
        )
    }
}
