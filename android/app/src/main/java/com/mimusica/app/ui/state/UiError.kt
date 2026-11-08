package com.mimusica.app.ui.state

import com.mimusica.app.domain.error.ApiException
import com.mimusica.app.domain.error.NoInternetException
import kotlinx.coroutines.CancellationException

/** Failure categories a screen can show; each maps to a friendly Spanish message in the UI layer. */
enum class UiError { NO_INTERNET, UNAUTHORIZED, SERVER, UNKNOWN }

fun Throwable.toUiError(): UiError = when {
    this is NoInternetException -> UiError.NO_INTERNET
    this is ApiException && (code == 401 || code == 403) -> UiError.UNAUTHORIZED
    this is ApiException -> UiError.SERVER
    else -> UiError.UNKNOWN
}

/** Runs [block]; cancellation propagates, any other failure becomes a [UiError]. */
inline fun <T> runCatchingUi(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
