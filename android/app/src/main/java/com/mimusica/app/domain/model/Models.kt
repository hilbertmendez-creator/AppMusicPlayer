package com.mimusica.app.domain.model

/** A song of the shared library. [audioPath] is non-null once the audio file is on this device. */
data class Song(
    val id: String,
    val sourceId: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int?,
    val thumbnailUrl: String?,
    val hasSyncedLyrics: Boolean,
    val hasPlainLyrics: Boolean,
    val lyricsOffsetMs: Int,
    val audioPath: String?,
) {
    val hasLyrics: Boolean get() = hasSyncedLyrics || hasPlainLyrics

    /** Words only, no timing: the karaoke cannot follow along, so the UI says so ("Letra completa"). */
    val hasPlainLyricsOnly: Boolean get() = hasPlainLyrics && !hasSyncedLyrics
    val isDownloaded: Boolean get() = audioPath != null
}

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<String>,
)

data class Lyrics(
    val syncedLrc: String?,
    val plain: String?,
    val offsetMs: Int,
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
)

/** Result of parsing an LRC document. [offsetMs] comes from the offset tag (0 when absent). */
data class ParsedLrc(
    val lines: List<LyricLine>,
    val offsetMs: Long,
)

enum class MatchType { TITLE, LYRICS }

data class SearchResult(
    val sourceId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationSeconds: Int?,
    val thumbnailUrl: String?,
    val match: MatchType,
    val inLibrary: Boolean,
)

enum class DownloadStatus { PENDING, DOWNLOADING, READY, FAILED }

data class DownloadJob(
    val id: String,
    val sourceId: String,
    val status: DownloadStatus,
    val songId: String?,
    val error: String?,
)

/** What a search result card shows for its single action button. */
sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data object Downloading : DownloadState

    /** The server finished; the phone is still saving the audio file. */
    data object Saving : DownloadState
    data object InLibrary : DownloadState
    data object Failed : DownloadState
}
