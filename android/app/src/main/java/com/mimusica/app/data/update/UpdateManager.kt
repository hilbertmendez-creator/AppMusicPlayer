package com.mimusica.app.data.update

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.mimusica.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Looks for a newer release on GitHub once at start and, on request, downloads it and hands it to the system
 * installer. Uses its own HTTP client: the app's main one rewrites every URL to the user's music server and adds
 * its token, which must never go to GitHub.
 *
 * Nothing here ever throws to the UI: no internet or a bad answer simply means "no update to offer".
 */
class UpdateManager(
    private val app: Application,
    private val scope: CoroutineScope,
    private val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    // Debug builds have another applicationId, so a release APK would install next to them, not over them.
    private val enabled: Boolean = !BuildConfig.DEBUG,
) {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // The redirect itself carries the answer (its Location header), so it must not be followed.
    private val noRedirectClient: OkHttpClient by lazy {
        client.newBuilder().followRedirects(false).followSslRedirects(false).build()
    }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.None)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _check = MutableStateFlow<UpdateCheck>(if (enabled) UpdateCheck.Idle else UpdateCheck.NotChecked)

    /** Outcome of the check asked for in Ajustes; [UpdateCheck.NotChecked] for builds that never look for updates. */
    val check: StateFlow<UpdateCheck> = _check.asStateFlow()

    /** "Buscar actualización": looks now and reports every outcome. A second tap while checking does nothing. */
    fun checkNow() {
        if (!enabled || _check.value is UpdateCheck.Checking) return
        _check.value = UpdateCheck.Checking
        scope.launch {
            val found = withContext(Dispatchers.IO) { fetchLatest() }
            val result = UpdateParser.checkResult(found, currentVersionCode)
            _check.value = result
            if (result is UpdateCheck.Available && _state.value is UpdateState.None) {
                _state.value = UpdateState.Available(result.update)
            }
        }
    }

    fun checkOnStart() {
        if (!enabled) return
        scope.launch {
            val found = withContext(Dispatchers.IO) { fetchLatest() }
            val newer = UpdateParser.newerThan(found, currentVersionCode)
            if (newer != null && _state.value is UpdateState.None) _state.value = UpdateState.Available(newer)
        }
    }

    /** Tap on "Actualizar": ask for the install permission if needed, else download and open the installer. */
    fun startUpdate() {
        val update = when (val s = _state.value) {
            is UpdateState.Available -> s.update
            is UpdateState.Failed -> s.update
            else -> return
        }
        if (!app.packageManager.canRequestPackageInstalls()) {
            // First time only: Android needs "allow installs from this app". The tile stays, tap again afterwards.
            app.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        _state.value = UpdateState.Downloading(update)
        scope.launch {
            val apk = withContext(Dispatchers.IO) { download(update) }
            if (apk == null) {
                _state.value = UpdateState.Failed(update)
            } else {
                _state.value = UpdateState.Available(update)
                install(apk)
            }
        }
    }

    /** The API first; if it refuses (rate limit, no answer) the public "latest" page, which has no such limit. */
    private fun fetchLatest(): AppUpdate? = fetchFromApi() ?: fetchFromLatestPage()

    private fun fetchFromApi(): AppUpdate? = try {
        val request = Request.Builder()
            .url(UpdateParser.LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) UpdateParser.parse(response.body?.string().orEmpty()) else null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun fetchFromLatestPage(): AppUpdate? = try {
        val request = Request.Builder().url(UpdateParser.LATEST_PAGE_URL).build()
        noRedirectClient.newCall(request).execute().use { response ->
            UpdateParser.parseLatestRedirect(response.header("Location"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun download(update: AppUpdate): File? = try {
        val dir = File(app.cacheDir, UPDATES_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, APK_FILE)
        val partial = File(dir, "$APK_FILE.part")
        client.newCall(Request.Builder().url(update.apkUrl).build()).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) return@use null
            body.byteStream().use { input -> partial.outputStream().use { input.copyTo(it) } }
            if (partial.renameTo(target)) target else null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        null
    }

    private fun install(apk: File) {
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", apk)
        app.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private companion object {
        const val UPDATES_DIR = "updates"
        const val APK_FILE = "MiMusica.apk"
    }
}
