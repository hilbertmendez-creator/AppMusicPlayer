package com.mimusica.app.ui.settings

import com.mimusica.app.data.update.UpdateCheck
import com.mimusica.app.data.update.UpdateState

/** "Versión 0.1.0": [template] is the `about_version` string resource (one `%1$s`), kept pure for testing. */
fun versionLabel(template: String, versionName: String): String =
    template.format(versionName.trim()).trimEnd()

/** What the "Acerca de" card says about updates; one line at a time. */
enum class AboutUpdateUi { Idle, Checking, UpToDate, Available, Downloading, DownloadFailed, CheckFailed, NotChecked }

/**
 * An update already found (at start, or by this check) or being downloaded wins over the check result, so the
 * card never says "up to date" while Home is offering a new version.
 */
fun aboutUpdateUi(check: UpdateCheck, state: UpdateState): AboutUpdateUi = when {
    state is UpdateState.Downloading -> AboutUpdateUi.Downloading
    state is UpdateState.Failed -> AboutUpdateUi.DownloadFailed
    state is UpdateState.Available -> AboutUpdateUi.Available
    else -> when (check) {
        UpdateCheck.Idle -> AboutUpdateUi.Idle
        UpdateCheck.Checking -> AboutUpdateUi.Checking
        UpdateCheck.UpToDate -> AboutUpdateUi.UpToDate
        is UpdateCheck.Available -> AboutUpdateUi.Available
        UpdateCheck.Failed -> AboutUpdateUi.CheckFailed
        UpdateCheck.NotChecked -> AboutUpdateUi.NotChecked
    }
}
