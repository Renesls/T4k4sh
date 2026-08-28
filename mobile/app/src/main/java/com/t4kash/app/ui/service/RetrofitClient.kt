package com.t4kash.app.ui.service

import com.t4kash.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.t4kash.app.ui.session.UserSession
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val baseUrl: String
        get() {
            val configuredUrl = BuildConfig.API_BASE_URL.trim()
            return if (configuredUrl.endsWith("/")) configuredUrl else "$configuredUrl/"
        }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val sessionToken = UserSession.accessToken
            val authenticatedRequest = sessionToken
                ?.takeIf { it.isNotBlank() }
                ?.let { token ->
                    request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                }
                ?: request
            chain.proceed(authenticatedRequest).also { response ->
                if (sessionToken != null && response.code == 401) {
                    UserSession.clear()
                }
            }
        }
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

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val communicationApiService: CommunicationApiService by lazy {
        retrofit.create(CommunicationApiService::class.java)
    }

    val networkApiService: NetworkApiService by lazy {
        retrofit.create(NetworkApiService::class.java)
    }

    val identityVerificationApiService: IdentityVerificationApiService by lazy {
        retrofit.create(IdentityVerificationApiService::class.java)
    }
}
