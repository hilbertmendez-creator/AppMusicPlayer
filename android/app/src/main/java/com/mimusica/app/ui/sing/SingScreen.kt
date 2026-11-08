package com.mimusica.app.ui.sing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.components.Artwork
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.EmptyMessage
import com.mimusica.app.ui.components.RowCard
import com.mimusica.app.ui.components.ScreenScaffold

/** Presentational "Cantar": songs that have lyrics; one big button each opens the karaoke. */
@Composable
fun SingScreen(songs: List<Song>, onSing: (Song) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ScreenScaffold(title = stringResource(R.string.sing_title), onBack = onBack, modifier = modifier) {
        if (songs.isEmpty()) EmptyMessage(stringResource(R.string.sing_empty))
        songs.forEach { song ->
            RowCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(url = song.thumbnailUrl, size = 72.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 3)
                        Text(song.artist, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                    }
                }
                BigButton(stringResource(R.string.sing), { onSing(song) }, icon = Icons.Filled.Lyrics)
            }
        }
    }
}
