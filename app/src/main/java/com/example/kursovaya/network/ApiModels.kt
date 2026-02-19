package com.example.kursovaya.network

import com.google.gson.annotations.SerializedName

data class ExchangeResponse(
    val result: String, // "success" / "error"
    @SerializedName("base_code") val baseCode: String,
    @SerializedName("time_last_update_utc") val lastUpdateUtc: String,
    val rates: Map<String, Double>,
    @SerializedName("error-type") val errorType: String? = null
)
