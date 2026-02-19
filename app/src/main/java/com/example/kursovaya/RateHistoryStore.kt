package com.example.kursovaya

import android.content.Context

object RateHistoryStore {
    private const val PREFS = "rate_history_prefs"
    private const val KEY = "rate_history"

    // Храним как set строк вида "USD->RUB=76.67"
    private fun getMap(context: Context): MutableMap<String, Double> {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY, emptySet()) ?: emptySet()

        val map = mutableMapOf<String, Double>()
        for (s in set) {
            val parts = s.split("=")
            if (parts.size != 2) continue
            val pair = parts[0]
            val value = parts[1].toDoubleOrNull() ?: continue
            map[pair] = value
        }
        return map
    }

    fun get(context: Context, pair: String): Double? {
        return getMap(context)[pair]
    }

    fun put(context: Context, pair: String, rate: Double) {
        val map = getMap(context)
        map[pair] = rate

        val set = map.map { "${it.key}=${it.value}" }.toSet()
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putStringSet(KEY, set).apply()
    }
}
