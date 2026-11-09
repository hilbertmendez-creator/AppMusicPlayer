package com.mimusica.app.ui

import com.mimusica.app.data.update.AppUpdate
import com.mimusica.app.data.update.UpdateCheck
import com.mimusica.app.data.update.UpdateParser
import com.mimusica.app.data.update.UpdateState
import com.mimusica.app.ui.settings.AboutUpdateUi
import com.mimusica.app.ui.settings.aboutUpdateUi
import com.mimusica.app.ui.settings.versionLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class VersionAndUpdateStatusTest {
    private val update = AppUpdate(versionCode = 9, apkUrl = "https://x/MiMusica.apk")

    @Test
    fun `version label fills the template`() {
        assertEquals("Versión 0.1.0", versionLabel("Versión %1\$s", "0.1.0"))
    }

    @Test
    fun `version label trims the name and survives a blank one`() {
        assertEquals("Versión 1.2", versionLabel("Versión %1\$s", " 1.2 "))
        assertEquals("Versión", versionLabel("Versión %1\$s", "  "))
    }

    @Test
    fun `a failed fetch is a failure and never up to date`() {
        assertEquals(UpdateCheck.Failed, UpdateParser.checkResult(null, currentVersionCode = 5))
    }

    @Test
    fun `same or older release means up to date and newer means available`() {
        assertEquals(UpdateCheck.UpToDate, UpdateParser.checkResult(update, currentVersionCode = 9))
        assertEquals(UpdateCheck.UpToDate, UpdateParser.checkResult(update, currentVersionCode = 12))
        assertEquals(UpdateCheck.Available(update), UpdateParser.checkResult(update, currentVersionCode = 8))
    }

    @Test
    fun `about status follows the check result`() {
        assertEquals(AboutUpdateUi.Idle, aboutUpdateUi(UpdateCheck.Idle, UpdateState.None))
        assertEquals(AboutUpdateUi.Checking, aboutUpdateUi(UpdateCheck.Checking, UpdateState.None))
        assertEquals(AboutUpdateUi.UpToDate, aboutUpdateUi(UpdateCheck.UpToDate, UpdateState.None))
        assertEquals(AboutUpdateUi.CheckFailed, aboutUpdateUi(UpdateCheck.Failed, UpdateState.None))
        assertEquals(AboutUpdateUi.NotChecked, aboutUpdateUi(UpdateCheck.NotChecked, UpdateState.None))
        assertEquals(AboutUpdateUi.Available, aboutUpdateUi(UpdateCheck.Available(update), UpdateState.None))
    }

    @Test
    fun `an update found at start or being downloaded wins over the check result`() {
        assertEquals(AboutUpdateUi.Available, aboutUpdateUi(UpdateCheck.Idle, UpdateState.Available(update)))
        assertEquals(AboutUpdateUi.Available, aboutUpdateUi(UpdateCheck.Checking, UpdateState.Available(update)))
        assertEquals(AboutUpdateUi.Downloading, aboutUpdateUi(UpdateCheck.UpToDate, UpdateState.Downloading(update)))
        assertEquals(AboutUpdateUi.DownloadFailed, aboutUpdateUi(UpdateCheck.Idle, UpdateState.Failed(update)))
    }
}
