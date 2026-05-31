package com.example.sourceformapp

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // -----------------------------------------
    // CLOUD RUN BACKEND
    // -----------------------------------------

    private const val BASE_URL =
        "https://sourceform-backend-395254787380.asia-south1.run.app/"

    // -----------------------------------------
    // LOGGING
    // -----------------------------------------

    private val logging =
        HttpLoggingInterceptor().apply {

            level =
                HttpLoggingInterceptor.Level.BODY
        }

    // -----------------------------------------
    // CLIENT
    // -----------------------------------------

    private val client =
        OkHttpClient.Builder()

            .addInterceptor(logging)

            .connectTimeout(
                30,
                TimeUnit.SECONDS
            )

            .readTimeout(
                30,
                TimeUnit.SECONDS
            )

            .writeTimeout(
                30,
                TimeUnit.SECONDS
            )

            .retryOnConnectionFailure(
                true
            )

            .build()

    // -----------------------------------------
    // RETROFIT INSTANCE
    // -----------------------------------------

    val api: ApiService by lazy {

        Retrofit.Builder()

            .baseUrl(BASE_URL)

            .client(client)

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

            .create(ApiService::class.java)
    }
}