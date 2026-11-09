package com.mimusica.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A newer build published on GitHub Releases by the "Release Android" workflow. */
data class AppUpdate(val versionCode: Int, val apkUrl: String)

/** What Home shows: nothing, an offer, a download in progress, or a failed download that can be retried. */
sealed interface UpdateState {
    data object None : UpdateState
    data class Available(val update: AppUpdate) : UpdateState
    data class Downloading(val update: AppUpdate) : UpdateState
    data class Failed(val update: AppUpdate) : UpdateState
}

/**
 * Result of the check the person asks for in Ajustes ("Buscar actualización"). [NotChecked] is a build that does
 * not look for updates (debug): it must never be shown as "up to date".
 */
sealed interface UpdateCheck {
    data object Idle : UpdateCheck
    data object Checking : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Available(val update: AppUpdate) : UpdateCheck
    data object Failed : UpdateCheck
    data object NotChecked : UpdateCheck
}

@Serializable
private data class ReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val assets: List<AssetDto> = emptyList(),
)

@Serializable
private data class AssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val url: String = "",
)

/** Pure parsing and comparison for GitHub's `releases/latest` answer; no Android or network involved. */
object UpdateParser {
    const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/hilbertmendez-creator/AppMusicPlayer/releases/latest"

    /**
     * Same answer without GitHub's API: this page redirects to `.../releases/tag/build-N`. The API allows only
     * 60 requests per hour per IP address, and phone networks share addresses, so it often refuses.
     */
    const val LATEST_PAGE_URL = "https://github.com/hilbertmendez-creator/AppMusicPlayer/releases/latest"

    /** The APK is only trusted if it comes from this repository's releases (it is also signature-checked by Android). */
    const val ALLOWED_DOWNLOAD_PREFIX =
        "https://github.com/hilbertmendez-creator/AppMusicPlayer/releases/download/"

    private const val APK_NAME = "MiMusica.apk"
    private val tagPattern = Regex("""^build-(\d+)$""")
    private val json = Json { ignoreUnknownKeys = true }

    /** Null when the answer is not one of our releases (wrong tag, no APK, foreign URL, broken JSON). */
    fun parse(body: String): AppUpdate? {
        val release = try {
            json.decodeFromString<ReleaseDto>(body)
        } catch (_: kotlinx.serialization.SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        val code = tagPattern.matchEntire(release.tagName)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val apk = release.assets.firstOrNull { it.name == APK_NAME } ?: return null
        if (!apk.url.startsWith(ALLOWED_DOWNLOAD_PREFIX)) return null
        return AppUpdate(code, apk.url)
    }

    /** From the redirect of [LATEST_PAGE_URL] (its `Location` header). Null if it is not one of our tags. */
    fun parseLatestRedirect(location: String?): AppUpdate? {
        val tag = location?.substringAfterLast("/releases/tag/", missingDelimiterValue = "")?.substringBefore('?')
        val code = tagPattern.matchEntire(tag.orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: return null
        return AppUpdate(code, "${ALLOWED_DOWNLOAD_PREFIX}$tag/$APK_NAME")
    }

    /** [update] only if it is strictly newer than the installed build; Android refuses same-or-older installs. */
    fun newerThan(update: AppUpdate?, currentVersionCode: Int): AppUpdate? =
        update?.takeIf { it.versionCode > currentVersionCode }

    /** Null [latest] means the lookup itself failed (no internet, bad answer): never "up to date". */
    fun checkResult(latest: AppUpdate?, currentVersionCode: Int): UpdateCheck = when {
        latest == null -> UpdateCheck.Failed
        latest.versionCode > currentVersionCode -> UpdateCheck.Available(latest)
        else -> UpdateCheck.UpToDate
    }
}
