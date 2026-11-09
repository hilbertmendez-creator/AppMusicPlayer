package com.mimusica.app.data

import com.mimusica.app.data.update.AppUpdate
import com.mimusica.app.data.update.UpdateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateParserTest {
    private val goodUrl = UpdateParser.ALLOWED_DOWNLOAD_PREFIX + "build-120/MiMusica.apk"

    private fun release(tag: String, assetName: String = "MiMusica.apk", url: String = goodUrl) = """
        {"tag_name":"$tag","name":"x","draft":false,
         "assets":[{"name":"$assetName","size":1,"browser_download_url":"$url"}]}
    """.trimIndent()

    @Test
    fun `reads build number and apk url from a release`() {
        assertEquals(AppUpdate(120, goodUrl), UpdateParser.parse(release("build-120")))
    }

    @Test
    fun `ignores releases that are not ours`() {
        assertNull(UpdateParser.parse(release("v1.2.3")))
        assertNull(UpdateParser.parse(release("build-abc")))
        assertNull(UpdateParser.parse(release("build-120", assetName = "notes.txt")))
    }

    @Test
    fun `refuses an apk hosted outside this repository`() {
        assertNull(UpdateParser.parse(release("build-120", url = "https://evil.example/MiMusica.apk")))
    }

    @Test
    fun `broken json is no update instead of a crash`() {
        assertNull(UpdateParser.parse("not json"))
        assertNull(UpdateParser.parse(""))
        assertNull(UpdateParser.parse("""{"message":"Not Found"}"""))
    }

    @Test
    fun `only a strictly higher build counts as newer`() {
        val update = AppUpdate(120, goodUrl)
        assertEquals(update, UpdateParser.newerThan(update, currentVersionCode = 119))
        assertNull(UpdateParser.newerThan(update, currentVersionCode = 120))
        assertNull(UpdateParser.newerThan(update, currentVersionCode = 121))
        assertNull(UpdateParser.newerThan(null, currentVersionCode = 1))
    }

    @Test
    fun `reads the build from the latest page redirect`() {
        val location = "https://github.com/hilbertmendez-creator/AppMusicPlayer/releases/tag/build-120"
        assertEquals(AppUpdate(120, goodUrl), UpdateParser.parseLatestRedirect(location))
    }

    @Test
    fun `ignores a redirect that is not one of our tags`() {
        assertNull(UpdateParser.parseLatestRedirect(null))
        assertNull(UpdateParser.parseLatestRedirect("https://github.com/login"))
        assertNull(UpdateParser.parseLatestRedirect("https://github.com/x/y/releases/tag/v1.0"))
    }
}
