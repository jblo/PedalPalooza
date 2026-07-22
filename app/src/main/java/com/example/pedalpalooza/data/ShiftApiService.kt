package com.example.pedalpalooza.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface ShiftApiService {
    @GET("api/search.php")
    suspend fun searchEvents(
        @Query("q") query: String = "%",
        @Query("l") limit: Int = 50,
        @Query("o") offset: Int = 0
    ): ShiftResponse

    companion object {
        private const val BASE_URL = "https://www.shift2bikes.org/"

        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        fun create(): ShiftApiService {
            val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

            val client = OkHttpClient.Builder()
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
