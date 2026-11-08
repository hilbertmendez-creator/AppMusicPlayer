package com.mimusica.app.domain.error

/** The device cannot reach the server (offline, DNS, timeout, refused...). */
class NoInternetException(cause: Throwable? = null) : Exception("No internet connection", cause)

/** The server answered with an HTTP error. */
class ApiException(val code: Int, message: String? = null) : Exception(message ?: "HTTP $code")
