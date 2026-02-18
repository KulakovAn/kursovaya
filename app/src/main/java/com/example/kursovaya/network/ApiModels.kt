package com.example.kursovaya.network

data class ConvertResponse(
    val info: Info?
) {
    data class Info(
        val rate: Double?
    )
}
