package com.mimusica.app.data

import com.mimusica.app.data.remote.BaseUrlInterceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class BaseUrlInterceptorTest {
    private val placeholder = "http://localhost/api/v1/search?q=a%20b&mode=auto".toHttpUrl()

    @Test
    fun `rewrites scheme host and port keeping path and query`() {
        val out = BaseUrlInterceptor.rewrite(placeholder, "http://192.168.1.5:8000".toHttpUrl())
        assertEquals("http://192.168.1.5:8000/api/v1/search?q=a%20b&mode=auto", out.toString())
    }

    @Test
    fun `keeps a path prefix configured by the user`() {
        val out = BaseUrlInterceptor.rewrite(placeholder, "https://mi.casa/musica".toHttpUrl())
        assertEquals("https://mi.casa/musica/api/v1/search?q=a%20b&mode=auto", out.toString())
    }

    @Test
    fun `base with trailing slash does not double the slash`() {
        val out = BaseUrlInterceptor.rewrite(placeholder, "https://mi.casa/".toHttpUrl())
        assertEquals("https://mi.casa/api/v1/search?q=a%20b&mode=auto", out.toString())
    }
}
