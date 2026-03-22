package com.example.smartmove.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.content.Context
import com.example.smartmove.data.SessionManager
import okhttp3.Interceptor
object RetrofitClient {

    //כל בקשה הולכת לשרת
    private const val BASE_URL = "https://smartmove-server.onrender.com/"
    private var sessionManager: SessionManager? = null

    fun init(context: Context) {
        sessionManager = SessionManager(context)
    }
    //מדפיס בקשות ותשובות
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()

        val token = sessionManager?.getToken()

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }
    //לקוח שאחראי על החיבור לאינטרנט
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

//פה נוצר האובייקט של Retrofit שמדבר עם ApiService
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}