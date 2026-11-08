package com.mimusica.app.ui.karaoke

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimusica.app.R
import com.mimusica.app.domain.model.LyricLine
import com.mimusica.app.ui.components.BackButton
import com.mimusica.app.ui.components.BigIconButton
import com.mimusica.app.ui.theme.KaraokeBackground
import com.mimusica.app.ui.theme.KaraokeCurrent
import com.mimusica.app.ui.theme.KaraokeDim

/** Presentational karaoke: previous line dim above, current line very large and highlighted, next line below. */
@Composable
fun KaraokeScreen(
    state: KaraokeUiState,
    textScale: Float,
    onToggle: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KaraokeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackButton(onBack, dark = true)
        state.song?.let {
            Text(
                "${it.title} · ${it.artist}",
                style = MaterialTheme.typography.titleSmall,
                color = KaraokeDim,
                maxLines = 2,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.mode) {
                KaraokeMode.LOADING -> LyricText(stringResource(R.string.karaoke_loading), 28.sp * textScale, KaraokeDim)
                KaraokeMode.NONE -> LyricText(stringResource(R.string.karaoke_no_lyrics), 30.sp * textScale, Color.White)
                KaraokeMode.PLAIN -> PlainLyrics(state.plain.orEmpty(), textScale)
                KaraokeMode.SYNCED -> SyncedLines(state.lines, state.currentIndex, textScale)
            }
        }
        if (state.mode != KaraokeMode.NONE && state.mode != KaraokeMode.LOADING) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BigIconButton(
                    icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    description = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                    onClick = onToggle,
                    size = 96.dp,
                    containerColor = KaraokeCurrent,
                    contentColor = Color.Black,
                )
            }
        }
    }
}

@Composable
private fun SyncedLines(lines: List<LyricLine>, currentIndex: Int, textScale: Float) {
    val previous = lines.getOrNull(currentIndex - 1)?.text.orEmpty()
    val current = if (currentIndex >= 0) lines[currentIndex].text else ""
    val next = lines.getOrNull(currentIndex + 1)?.text.orEmpty()
    LyricText(previous, 26.sp * textScale, KaraokeDim)
    LyricText(
        current.ifBlank { "♪" },
        (CURRENT_LINE_SP * textScale).sp,
        KaraokeCurrent,
        weight = FontWeight.Bold,
    )
    LyricText(next, 28.sp * textScale, Color.White.copy(alpha = 0.75f))
}

@Composable
private fun PlainLyrics(text: String, textScale: Float) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(fontSize = 28.sp * textScale, lineHeight = 40.sp * textScale),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LyricText(text: String, size: TextUnit, color: Color, weight: FontWeight = FontWeight.Normal) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        style = TextStyle(fontSize = size, lineHeight = size * 1.25f, fontWeight = weight),
        color = color,
        textAlign = TextAlign.Center,
    )
}

/** Current line size in sp; the spec asks for at least 34sp. */
const val CURRENT_LINE_SP = 38f
