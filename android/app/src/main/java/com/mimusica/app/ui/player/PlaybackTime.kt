package com.mimusica.app.ui.player

import java.util.Locale

private fun clock(totalSecondsRaw: Long): String {
    val totalSeconds = totalSecondsRaw.coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** Elapsed time: "1:05", "12:00" or "1:01:01". Truncates, so it counts naturally. Negative reads as zero. */
fun formatClock(ms: Long): String = clock(ms.coerceAtLeast(0L) / 1000L)

/**
 * Total length, rounded to the nearest second so it matches the server's whole-second metadata
 * (242_600 ms -> "4:03"). The one formatter for every song duration shown anywhere.
 */
fun formatDuration(ms: Long): String = clock((ms.coerceAtLeast(0L) + 500L) / 1000L)

/** "1:05 / 3:40": elapsed over total, with elapsed never past the total. */
fun formatPlaybackProgress(positionMs: Long, durationMs: Long, separator: String = " / "): String {
    val total = durationMs.coerceAtLeast(0L)
    val elapsed = positionMs.coerceIn(0L, total)
    return formatClock(elapsed) + separator + formatDuration(total)
}
