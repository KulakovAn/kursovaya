package com.example.kursovaya.network

import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApi {

    @GET("convert")
    suspend fun convert(
        @Query("from") from: String,
        @Query("to") to: String = "RUB",
        @Query("amount") amount: Double = 1.0
    ): ConvertResponse
}
