package com.mimusica.app.data

/** Picks the file extension that matches the real format of a downloaded audio file. */
object AudioFormat {
    const val DEFAULT_EXTENSION = "m4a"

    private val extensionByMediaType = mapOf(
        "audio/mp4" to "m4a",
        "audio/webm" to "webm",
        "audio/ogg" to "ogg",
        "audio/mpeg" to "mp3",
    )

    /** Extension for the server's `Content-Type`; unknown or missing types fall back to m4a. */
    fun extensionFor(mediaType: String?): String {
        val type = mediaType?.substringBefore(';')?.trim()?.lowercase()
        return extensionByMediaType[type] ?: DEFAULT_EXTENSION
    }
}
