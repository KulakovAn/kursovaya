package com.example.kursovaya

data class FavoriteUi(
    val pair: String,            // "USD->RUB"
    val base: String,            // "USD"
    val target: String,          // "RUB"
    val rateText: String,        // "1 USD = 76.67 RUB" / "Ошибка" / "Загрузка..."
    val updatedText: String      // "Обновлено: ..." / ""
)
