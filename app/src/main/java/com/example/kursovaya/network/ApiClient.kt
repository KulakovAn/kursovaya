package com.example.kursovaya.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.exchangerate.host/")
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: CurrencyApi = retrofit.create(CurrencyApi::class.java)
}
