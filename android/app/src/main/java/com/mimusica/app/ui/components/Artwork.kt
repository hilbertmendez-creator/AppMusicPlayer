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

/** Cover image with a fixed [size]; a music-note placeholder when there is no URL or it fails to load. */
@Composable
fun Artwork(url: String?, size: Dp, modifier: Modifier = Modifier) {
    ArtworkFill(url = url, modifier = modifier.size(size))
}

/** Cover image that fills whatever square its parent gives it (used where the size depends on free space). */
@Composable
fun ArtworkFill(url: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val placeholder: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = stringResource(R.string.cover_art),
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
                contentDescription = stringResource(R.string.cover_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { placeholder() },
                error = { placeholder() },
            )
        }
    }
}
