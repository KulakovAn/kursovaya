package com.example.kursovaya

import android.content.Context

object RateHistoryStore {
    private const val PREFS = "rate_history_prefs"
    private const val MAX_POINTS = 20

    private fun key(pair: String) = "series_$pair"

    fun getSeries(context: Context, pair: String): List<Double> {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = sp.getString(key(pair), "") ?: ""
        if (raw.isBlank()) return emptyList()

        return raw.split(",")
            .mapNotNull { it.toDoubleOrNull() }
            .takeLast(MAX_POINTS)
    }

    fun append(context: Context, pair: String, rate: Double) {
        val current = getSeries(context, pair).toMutableList()

        // если последнее значение такое же — не дублируем
        val last = current.lastOrNull()
        if (last != null && last == rate) return

        current.add(rate)
        val trimmed = current.takeLast(MAX_POINTS)

        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putString(key(pair), trimmed.joinToString(",")).apply()
    }

    fun getLast(context: Context, pair: String): Double? =
        getSeries(context, pair).lastOrNull()
}
