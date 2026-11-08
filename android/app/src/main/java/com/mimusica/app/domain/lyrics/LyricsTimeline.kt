package com.mimusica.app.domain.lyrics

import com.mimusica.app.domain.model.LyricLine

/**
 * Finds the active lyric line for a playback position.
 * A positive offset makes lyrics appear earlier (LRC convention): effective time = position + offset.
 */
class LyricsTimeline(private val lines: List<LyricLine>) {

    /** Index of the last line whose time is <= the effective position, or -1 before the first line. */
    fun currentIndex(positionMs: Long, offsetMs: Long): Int {
        val effective = positionMs + offsetMs
        var low = 0
        var high = lines.size - 1
        var found = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= effective) {
                found = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return found
    }
}
