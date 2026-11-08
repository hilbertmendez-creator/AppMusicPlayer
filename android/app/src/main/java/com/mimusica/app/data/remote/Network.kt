package com.mimusica.app.data.remote

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Runtime-mutable server address and token (changed from Ajustes without rebuilding Retrofit). */
class ConnectionConfig {
    @Volatile
    var baseUrl: HttpUrl? = null

    @Volatile
    var token: String = ""
}

/** Points the placeholder base URL of Retrofit at the configured server and adds the bearer token. */
class BaseUrlInterceptor(private val config: ConnectionConfig) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val base = config.baseUrl ?: throw IOException("Server is not configured")
        val request = chain.request()
        val rebuilt = request.newBuilder()
            .url(rewrite(request.url, base))
            .header("Authorization", "Bearer ${config.token}")
            .build()
        return chain.proceed(rebuilt)
    }

    companion object {
        /** Keeps path and query of [original], takes scheme/host/port (and a path prefix) from [base]. */
        fun rewrite(original: HttpUrl, base: HttpUrl): HttpUrl {
            val prefix = base.pathSegments.filter { it.isNotEmpty() }
            val builder = original.newBuilder()
                .scheme(base.scheme)
                .host(base.host)
                .port(base.port)
            if (prefix.isNotEmpty()) {
                builder.encodedPath("/" + prefix.joinToString("/") + original.encodedPath)
            }
            return builder.build()
        }
    }
}

interface MusicApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("mode") mode: String,
        @Query("limit") limit: Int,
    ): SearchResponseDto

    @POST("downloads")
    suspend fun requestDownload(@Body body: DownloadRequestDto): DownloadJobDto

    @GET("downloads/{id}")
    suspend fun getDownload(@Path("id") id: String): DownloadJobDto

    @GET("library")
    suspend fun getLibrary(): LibraryIdentityDto

    @GET("songs")
    suspend fun listSongs(): SongListDto

    @GET("songs/{id}")
    suspend fun getSong(@Path("id") id: String): SongDto

    @DELETE("songs/{id}")
    suspend fun deleteSong(@Path("id") id: String): retrofit2.Response<Unit>

    @Streaming
    @GET("songs/{id}/audio")
    suspend fun getAudio(@Path("id") id: String): ResponseBody

    @GET("songs/{id}/lyrics")
    suspend fun getLyrics(@Path("id") id: String): LyricsDto

    @GET("playlists")
    suspend fun listPlaylists(): PlaylistListDto

    @POST("playlists")
    suspend fun createPlaylist(@Body body: PlaylistCreateDto): PlaylistDto

    @PATCH("playlists/{id}")
    suspend fun updatePlaylist(@Path("id") id: String, @Body body: PlaylistUpdateDto): PlaylistDto

    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: String): retrofit2.Response<Unit>
}

object NetworkFactory {
    private const val PLACEHOLDER_BASE = "http://localhost/api/v1/"

    fun okHttp(config: ConnectionConfig): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(BaseUrlInterceptor(config))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun service(client: OkHttpClient): MusicApiService = Retrofit.Builder()
        .baseUrl(PLACEHOLDER_BASE)
        .client(client)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(MusicApiService::class.java)
}
