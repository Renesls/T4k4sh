package com.t4kash.app.ui.service

import com.t4kash.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val baseUrl: String
        get() {
            val configuredUrl = BuildConfig.API_BASE_URL.trim()
            return if (configuredUrl.endsWith("/")) configuredUrl else "$configuredUrl/"
        }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val marketplaceApiService: MarketplaceApiService by lazy {
        retrofit.create(MarketplaceApiService::class.java)
    }
}
