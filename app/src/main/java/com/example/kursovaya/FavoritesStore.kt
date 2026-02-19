package com.example.kursovaya

import android.content.Context

object FavoritesStore {
    private const val PREFS = "favorites_prefs"
    private const val KEY = "pairs"

    fun getAll(context: Context): List<String> {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (sp.getStringSet(KEY, emptySet()) ?: emptySet())
            .toList()
            .sorted()
    }

    fun add(context: Context, pair: String) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = (sp.getStringSet(KEY, emptySet()) ?: emptySet()).toMutableSet()
        current.add(pair)
        sp.edit().putStringSet(KEY, current).apply()
    }

    fun remove(context: Context, pair: String) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = (sp.getStringSet(KEY, emptySet()) ?: emptySet()).toMutableSet()
        current.remove(pair)
        sp.edit().putStringSet(KEY, current).apply()
    }
}
