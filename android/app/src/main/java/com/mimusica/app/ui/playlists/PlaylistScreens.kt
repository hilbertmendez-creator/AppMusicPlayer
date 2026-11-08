package com.mimusica.app.ui.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.ConfirmDeleteDialog
import com.mimusica.app.ui.components.DemotedDangerAction
import com.mimusica.app.ui.components.EmptyMessage
import com.mimusica.app.ui.components.ErrorBanner
import com.mimusica.app.ui.components.ListRowButton
import com.mimusica.app.ui.components.MessageBanner
import com.mimusica.app.ui.components.MinTouch
import com.mimusica.app.ui.components.OutlineBigButton
import com.mimusica.app.ui.components.RowCard
import com.mimusica.app.ui.components.ScreenScaffold
import com.mimusica.app.ui.state.UiError

/** Presentational "Mis listas": the name on its own line, the song count as a quieter second line. */
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    error: UiError?,
    onOpen: (Playlist) -> Unit,
    onNew: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(title = stringResource(R.string.playlists_title), onBack = onBack, modifier = modifier) {
        error?.let { ErrorBanner(it) }
        BigButton(stringResource(R.string.new_playlist), onNew, icon = Icons.Filled.Add)
        if (playlists.isEmpty()) EmptyMessage(stringResource(R.string.playlists_empty))
        playlists.forEach { playlist ->
            ListRowButton(
                title = playlist.name,
                subtitle = pluralStringResource(R.plurals.song_count, playlist.songIds.size, playlist.songIds.size),
                onClick = { onOpen(playlist) },
            )
        }
    }
}

/**
 * Presentational picker for "Agregar a una lista": one big row per list plus "Nueva lista".
 * [addedTo] is the name of the list the song just went to; it shows a green confirmation (success only).
 */
@Composable
fun AddToListScreen(
    playlists: List<Playlist>,
    addedTo: String?,
    onChoose: (Playlist) -> Unit,
    onNew: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    song: Song? = null,
) {
    ScreenScaffold(title = stringResource(R.string.add_to_list_title), onBack = onBack, modifier = modifier) {
        song?.let {
            Text(
                stringResource(R.string.add_to_list_song, it.title, it.artist),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
            )
        }
        addedTo?.let { MessageBanner(stringResource(R.string.added_to_list, it), isError = false) }
        if (playlists.isEmpty()) EmptyMessage(stringResource(R.string.add_to_list_empty))
        playlists.forEach { playlist ->
            ListRowButton(
                title = playlist.name,
                subtitle = pluralStringResource(R.plurals.song_count, playlist.songIds.size, playlist.songIds.size),
                onClick = { onChoose(playlist) },
            )
        }
        BigButton(stringResource(R.string.new_playlist), onNew, icon = Icons.Filled.Add)
    }
}

/** Dialog to name a new list by keyboard or voice. "Crear" is the dominant action; "Hablar" is secondary. */
@Composable
fun NewPlaylistDialog(
    name: String,
    micUnavailable: Boolean,
    onNameChange: (String) -> Unit,
    onMic: () -> Unit,
    onCreate: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.new_playlist), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                    textStyle = MaterialTheme.typography.titleMedium,
                    placeholder = { Text(stringResource(R.string.playlist_name_hint)) },
                    singleLine = true,
                )
                OutlineBigButton(
                    text = stringResource(R.string.search_mic),
                    onClick = onMic,
                    icon = Icons.Filled.Mic,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (micUnavailable) MessageBanner(stringResource(R.string.mic_unavailable))
            }
        },
        confirmButton = { BigButton(stringResource(R.string.create), onCreate, minHeight = MinTouch) },
        dismissButton = { OutlineBigButton(stringResource(R.string.cancel), onCancel) },
    )
}

/**
 * Presentational list detail. [songs] are already resolved in playlist order.
 * One clear primary ("Reproducir todo"); each song has "Reproducir" with "Quitar" demoted and apart;
 * "Borrar lista" sits alone at the bottom.
 */
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    songs: List<Song>,
    error: UiError?,
    onPlayAll: () -> Unit,
    onPlaySong: (index: Int) -> Unit,
    onAddSongs: () -> Unit,
    onRemoveSong: (Song) -> Unit,
    onDeletePlaylist: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingRemove by remember { mutableStateOf<Song?>(null) }
    var confirmDeleteList by remember { mutableStateOf(false) }
    ScreenScaffold(title = playlist.name, onBack = onBack, modifier = modifier) {
        error?.let { ErrorBanner(it) }
        if (songs.isNotEmpty()) {
            BigButton(stringResource(R.string.play_all), onPlayAll, icon = Icons.Filled.PlayArrow)
        } else {
            EmptyMessage(stringResource(R.string.playlist_empty_songs))
        }
        songs.forEachIndexed { index, song ->
            RowCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinTouch),
                ) {
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
                OutlineBigButton(
                    stringResource(R.string.play),
                    { onPlaySong(index) },
                    icon = Icons.Filled.PlayArrow,
                    minHeight = 72.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                DemotedDangerAction(stringResource(R.string.remove), { pendingRemove = song })
            }
        }
        OutlineBigButton(
            stringResource(R.string.add_songs),
            onAddSongs,
            icon = Icons.Filled.Add,
            minHeight = 72.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        DemotedDangerAction(
            stringResource(R.string.delete_playlist),
            { confirmDeleteList = true },
            icon = Icons.Filled.Delete,
        )
    }
    pendingRemove?.let { song ->
        ConfirmDeleteDialog(
            message = stringResource(R.string.remove_song_confirm, song.title),
            onConfirm = {
                pendingRemove = null
                onRemoveSong(song)
            },
            onDismiss = { pendingRemove = null },
        )
    }
    if (confirmDeleteList) {
        ConfirmDeleteDialog(
            message = stringResource(R.string.delete_playlist_confirm, playlist.name),
            onConfirm = {
                confirmDeleteList = false
                onDeletePlaylist()
            },
            onDismiss = { confirmDeleteList = false },
        )
    }
}

/** Presentational picker: large checkbox rows for the songs not yet in the list. */
@Composable
fun AddSongsScreen(
    candidates: List<Song>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(title = stringResource(R.string.add_songs_title), onBack = onBack, modifier = modifier) {
        if (candidates.isEmpty()) {
            EmptyMessage(
                text = stringResource(R.string.add_none),
                actionText = stringResource(R.string.add_none_action),
                onAction = onSearch,
                actionIcon = Icons.Filled.Search,
            )
        }
        candidates.forEach { song ->
            val checked = song.id in selected
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle(song.id) }),
                shape = RoundedCornerShape(16.dp),
                color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 80.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        modifier = Modifier.size(48.dp),
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (selected.isNotEmpty()) {
            BigButton(stringResource(R.string.add_selected, selected.size), onConfirm, icon = Icons.Filled.Add)
        }
    }
}
