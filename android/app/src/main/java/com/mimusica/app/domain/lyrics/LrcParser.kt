package com.mimusica.app.domain.lyrics

import com.mimusica.app.domain.model.LyricLine
import com.mimusica.app.domain.model.ParsedLrc

/**
 * Pure LRC parser. Supports `[mm:ss.xx]`, `[mm:ss.xxx]`, `[mm:ss]`, several timestamps per line,
 * the `[offset:+/-ms]` tag, ignores other metadata tags and garbage, and returns lines sorted by time.
 */
object LrcParser {
    private val timestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val offsetTag = Regex("""^\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)
    private val wordTag = Regex("""<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>""")

    fun parse(text: String): ParsedLrc {
        var offset = 0L
        val lines = mutableListOf<LyricLine>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val offsetMatch = offsetTag.find(line)
            if (offsetMatch != null) {
                offset = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
                continue
            }
            val stamps = mutableListOf<Long>()
            var cursor = 0
            while (true) {
                val m = timestamp.matchAt(line, cursor) ?: break
                stamps += toMillis(m)
                cursor = m.range.last + 1
            }
            if (stamps.isEmpty()) continue
            val content = wordTag.replace(line.substring(cursor), "").trim()
            stamps.forEach { lines += LyricLine(it, content) }
        }
        return ParsedLrc(lines.sortedBy { it.timeMs }, offset)
    }

    private fun toMillis(m: MatchResult): Long {
        val minutes = m.groupValues[1].toLong()
        val seconds = m.groupValues[2].toLong()
        val fraction = m.groupValues[3]
        val millis = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100
            2 -> fraction.toLong() * 10
            else -> fraction.toLong()
        }
        return minutes * 60_000 + seconds * 1_000 + millis
    }
}
