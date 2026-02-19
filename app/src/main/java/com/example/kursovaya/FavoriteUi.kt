package com.example.kursovaya

enum class RateTrend { UP, DOWN, SAME, UNKNOWN }

data class FavoriteUi(
    val pair: String,
    val base: String,
    val target: String,
    val rateText: String,
    val updatedText: String,
    val trend: RateTrend = RateTrend.UNKNOWN,
    val series: List<Double> = emptyList()
)
