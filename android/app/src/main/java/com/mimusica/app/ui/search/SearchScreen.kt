package com.mimusica.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mimusica.app.R
import com.mimusica.app.domain.model.DownloadState
import com.mimusica.app.domain.model.MatchType
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.components.Artwork
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.BigButtonHeight
import com.mimusica.app.ui.components.OutlineBigButton
import com.mimusica.app.ui.components.EmptyMessage
import com.mimusica.app.ui.components.ErrorBanner
import com.mimusica.app.ui.components.InlineError
import com.mimusica.app.ui.components.errorText
import com.mimusica.app.ui.player.formatDuration
import com.mimusica.app.ui.components.MessageBanner
import com.mimusica.app.ui.components.MinTouch
import com.mimusica.app.ui.components.RowCard
import com.mimusica.app.ui.components.ScreenScaffold
import com.mimusica.app.ui.components.StatusPill

/** Presentational "Buscar": one big field, a very large microphone button and at most 5 result cards. */
@Composable
fun SearchScreen(
    state: SearchUiState,
    micMessage: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onMic: () -> Unit,
    onDownload: (String) -> Unit,
    onPlayNow: (Song) -> Unit,
    onAddToList: (Song) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // Close the keyboard on submit: on a real phone it otherwise covers the results.
    val submit = {
        keyboard?.hide()
        focusManager.clearFocus()
        onSearch()
    }
    ScreenScaffold(title = stringResource(R.string.search_title), onBack = onBack, modifier = modifier) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { text ->
                // Enter (hardware or IME) arrives as a newline: submit instead of inserting it.
                onQueryChange(sanitizeQuery(text))
                if (text.any { it == '\n' || it == '\r' }) submit()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp),
            textStyle = MaterialTheme.typography.titleMedium,
            placeholder = { Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyLarge) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            minLines = 2,
        )
        BigButton(
            text = stringResource(R.string.search_mic),
            onClick = onMic,
            icon = Icons.Filled.Mic,
            minHeight = 104.dp,
            description = stringResource(R.string.search_mic_desc),
        )
        // Secondary to "Hablar": outlined, never green (green means success only).
        OutlineBigButton(
            text = stringResource(R.string.search_button),
            onClick = submit,
            icon = Icons.Filled.Search,
            minHeight = BigButtonHeight,
            color = MaterialTheme.colorScheme.primary,
        )
        if (micMessage) MessageBanner(stringResource(R.string.mic_unavailable))
        state.error?.let { ErrorBanner(it) }
        when {
            state.isSearching -> SearchingIndicator()
            state.hasSearched && state.items.isEmpty() && state.error == null ->
                EmptyMessage(stringResource(R.string.search_no_results))
        }
        state.items.forEach { item ->
            ResultCard(
                item = item,
                onDownload = { onDownload(item.result.sourceId) },
                onPlayNow = onPlayNow,
                onAddToList = onAddToList,
            )
        }
    }
}

@Composable
private fun SearchingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(16.dp))
        Text(stringResource(R.string.search_searching), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ResultCard(
    item: SearchItem,
    onDownload: () -> Unit,
    onPlayNow: (Song) -> Unit,
    onAddToList: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    RowCard(modifier) {
        // One spoken unit for the song's identity (cover is decorative); the actions below stay separate.
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(url = item.result.thumbnailUrl, size = 80.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.result.title, style = MaterialTheme.typography.titleMedium, maxLines = 3)
                Text(item.result.artist, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                item.result.durationSeconds?.let {
                    Text(formatDuration(it * 1000L), style = MaterialTheme.typography.bodyLarge)
                }
                if (item.result.match == MatchType.LYRICS) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            stringResource(R.string.search_tag_lyrics),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
        DownloadAction(item, onDownload, onPlayNow, onAddToList)
    }
}

/** Spoken politely whenever its text changes (downloading, saving, ready, failed). */
private val LiveStatus = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }

@Composable
private fun DownloadAction(
    item: SearchItem,
    onDownload: () -> Unit,
    onPlayNow: (Song) -> Unit,
    onAddToList: (Song) -> Unit,
) {
    when (item.state) {
        DownloadState.NotDownloaded ->
            BigButton(stringResource(R.string.download), onDownload, icon = Icons.Filled.Download)
        DownloadState.Failed -> {
            InlineError(item.failure?.let { errorText(it) } ?: stringResource(R.string.download_failed))
            BigButton(
                stringResource(R.string.download_retry),
                onDownload,
                icon = Icons.Filled.Download,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
        }
        DownloadState.Downloading -> ProgressStatus(stringResource(R.string.downloading))
        DownloadState.Saving -> ProgressStatus(stringResource(R.string.download_saving))
        DownloadState.InLibrary -> {
            StatusPill(stringResource(R.string.in_library), Icons.Filled.CheckCircle, modifier = LiveStatus)
            item.song?.let { song ->
                BigButton(
                    stringResource(R.string.play_now),
                    { onPlayNow(song) },
                    icon = Icons.Filled.PlayArrow,
                    minHeight = MinTouch,
                )
                OutlineBigButton(
                    stringResource(R.string.add_to_list),
                    { onAddToList(song) },
                    icon = Icons.Filled.PlaylistAdd,
                    minHeight = MinTouch,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ProgressStatus(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .then(LiveStatus),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
