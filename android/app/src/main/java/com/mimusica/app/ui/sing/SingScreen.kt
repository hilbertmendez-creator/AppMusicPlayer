package com.mimusica.app.ui.sing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.components.Artwork
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.EmptyMessage
import com.mimusica.app.ui.components.RowCard
import com.mimusica.app.ui.components.ScreenScaffold
import com.mimusica.app.ui.components.SectionAccents
import com.mimusica.app.ui.components.TagChip

/** Presentational "Cantar": songs that have lyrics; one big button each opens the karaoke. */
@Composable
fun SingScreen(
    songs: List<Song>,
    onSing: (Song) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.sing_title),
        onBack = onBack,
        modifier = modifier,
        accent = SectionAccents.Sing,
    ) {
        if (songs.isEmpty()) {
            EmptyMessage(
                text = stringResource(R.string.sing_empty),
                actionText = stringResource(R.string.sing_empty_action),
                onAction = onSearch,
                actionIcon = Icons.Filled.Search,
            )
        }
        songs.forEach { song ->
            RowCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
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
                        // Words without timing: the karaoke cannot follow along, so say so before opening it.
                        if (song.hasPlainLyricsOnly) {
                            TagChip(stringResource(R.string.tag_full_lyrics), Modifier.padding(top = 4.dp))
                        }
                    }
                }
                BigButton(
                    stringResource(R.string.sing),
                    { onSing(song) },
                    icon = Icons.Filled.Lyrics,
                    description = stringResource(R.string.sing_song_desc, song.title),
                )
            }
        }
    }
}
