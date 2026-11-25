package com.example.pedalpalooza.data

import com.example.pedalpalooza.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ShiftApiService {
    @GET("api/search.php")
    suspend fun searchEvents(
        @Query("q") query: String = "%",
        @Query("l") limit: Int = 50,
        @Query("o") offset: Int = 0
    ): ShiftResponse

    companion object {
        private const val BASE_URL = "https://www.shift2bikes.org/"
        private const val NETWORK_TIMEOUT_SECONDS = 15L

        internal val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        val instance: ShiftApiService by lazy { create() }

        fun create(): ShiftApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(logger)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(ShiftApiService::class.java)
        }
    }
}
