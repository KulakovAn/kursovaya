package com.example.kursovaya.network

import retrofit2.http.GET
import retrofit2.http.Path

interface CurrencyApi {
    // пример: https://open.er-api.com/v6/latest/USD
    @GET("v6/latest/{base}")
    suspend fun latest(@Path("base") base: String): ExchangeResponse
}
