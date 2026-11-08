package com.mimusica.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFormatTest {
    @Test
    fun `maps the media types the server can send to their real extension`() {
        assertEquals("m4a", AudioFormat.extensionFor("audio/mp4"))
        assertEquals("webm", AudioFormat.extensionFor("audio/webm"))
        assertEquals("ogg", AudioFormat.extensionFor("audio/ogg"))
        assertEquals("mp3", AudioFormat.extensionFor("audio/mpeg"))
    }

    @Test
    fun `ignores parameters and letter case`() {
        assertEquals("webm", AudioFormat.extensionFor("Audio/WebM; codecs=opus"))
    }

    @Test
    fun `falls back to m4a when the type is missing or unknown`() {
        assertEquals("m4a", AudioFormat.extensionFor(null))
        assertEquals("m4a", AudioFormat.extensionFor(""))
        assertEquals("m4a", AudioFormat.extensionFor("application/octet-stream"))
    }
}
