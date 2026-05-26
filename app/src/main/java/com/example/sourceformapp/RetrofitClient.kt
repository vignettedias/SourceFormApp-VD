package com.example.sourceformapp
import com.example.sourceformapp.BuildConfig
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
object RetrofitClient {

    // -----------------------------------------
    // ENVIRONMENT CONFIG
    // -----------------------------------------

    private const val DEV_URL =
        "http://10.0.2.2:5000/"

    private const val PROD_URL =
        "https://your-production-domain.com/"

    // -----------------------------------------
    // ACTIVE BASE URL
    // -----------------------------------------

    private var BASE_URL =

        if (BuildConfig.DEBUG) {

            DEV_URL

        } else {

            PROD_URL
        }

    // -----------------------------------------
    // SET DYNAMIC URL
    // -----------------------------------------

    fun setBaseUrl(
        url: String
    ) {

        BASE_URL =

            if (
                url.endsWith("/")
            ) {

                url

            } else {

                "$url/"
            }

        Log.d(
            "RetrofitClient",
            "BASE URL UPDATED: $BASE_URL"
        )
    }

    // -----------------------------------------
    // GET API CLIENT
    // -----------------------------------------

    fun getClient(): ApiService {

        // -------------------------------------
        // LOGGING
        // -------------------------------------

        val logging =
            HttpLoggingInterceptor().apply {

                level =

                    if (BuildConfig.DEBUG) {

                        HttpLoggingInterceptor
                            .Level.BODY

                    } else {

                        HttpLoggingInterceptor
                            .Level.NONE
                    }
            }

        // -------------------------------------
        // OKHTTP CLIENT
        // -------------------------------------

        val client =
            OkHttpClient.Builder()

                // Logging

                .addInterceptor(logging)

                // Timeouts

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

                // Retry failed connections

                .retryOnConnectionFailure(
                    true
                )

                .build()

        // -------------------------------------
        // RETROFIT
        // -------------------------------------

        return Retrofit.Builder()

            .baseUrl(BASE_URL)

            .client(client)

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

            .create(ApiService::class.java)
    }
}
