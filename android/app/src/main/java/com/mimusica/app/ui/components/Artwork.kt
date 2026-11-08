package com.mimusica.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.mimusica.app.R

/**
 * Cover image with a fixed [size]; a music-note placeholder when there is no URL or it fails to load.
 * Pass [decorative] = true inside a card that is already named by its title: TalkBack then skips the cover
 * instead of announcing "Portada de la canción" on every row.
 */
@Composable
fun Artwork(url: String?, size: Dp, modifier: Modifier = Modifier, decorative: Boolean = false) {
    ArtworkFill(url = url, modifier = modifier.size(size), decorative = decorative)
}

/** Cover image that fills whatever square its parent gives it (used where the size depends on free space). */
@Composable
fun ArtworkFill(url: String?, modifier: Modifier = Modifier, decorative: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    val description = if (decorative) null else stringResource(R.string.cover_art)
    val placeholder: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = description,
                modifier = Modifier.fillMaxSize(0.5f),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
    Box(modifier = modifier.clip(shape)) {
        if (url.isNullOrBlank()) {
            placeholder()
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { placeholder() },
                error = { placeholder() },
            )
        }
    }
}
