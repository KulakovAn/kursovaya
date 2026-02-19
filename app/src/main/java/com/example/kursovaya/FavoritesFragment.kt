package com.example.kursovaya

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.kursovaya.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private val tagLog = "KURSOVAYA_FAV"

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FavoritesAdapter

    // Чтобы анимация списка была только один раз и не дергала при обновлениях
    private var firstAnimationDone = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe = view.findViewById(R.id.swipeRefresh)
        recycler = view.findViewById(R.id.favoritesRecycler)

        adapter = FavoritesAdapter { pair ->
            FavoritesStore.remove(requireContext(), pair)
            Toast.makeText(requireContext(), "Удалено: $pair", Toast.LENGTH_SHORT).show()
            refresh(showLoader = false)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // Layout-анимация (важно: запускать будем только один раз)
        recycler.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fall_down)

        swipe.setOnRefreshListener { refresh(showLoader = false) }

        refresh(showLoader = true)
    }

    override fun onResume() {
        super.onResume()
        // Обновим, если на первом табе добавили новое избранное
        refresh(showLoader = false)
    }

    private fun refresh(showLoader: Boolean) {
        val pairs = FavoritesStore.getAll(requireContext())

        if (pairs.isEmpty()) {
            adapter.submitList(
                listOf(
                    FavoriteUi(
                        pair = "__empty__",
                        base = "—",
                        target = "—",
                        rateText = "Нет избранных валют",
                        updatedText = "Добавь пары на вкладке «Курсы» (⭐)",
                        trend = RateTrend.UNKNOWN
                    )
                )
            ) {
                runFirstAnimationOnce()
            }

            swipe.isRefreshing = false
            return
        }

        // Показать “Загрузка...” без повторной анимации каждый раз
        val initial = pairs.mapNotNull { parsePair(it) }.map {
            it.copy(rateText = "Загрузка...", updatedText = "", trend = RateTrend.UNKNOWN)
        }

        adapter.submitList(initial) {
            runFirstAnimationOnce()
        }

        if (showLoader) swipe.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) { loadRatesForPairs(initial) }
                // ВАЖНО: тут НЕ запускаем layoutAnimation, чтобы не было дергания
                adapter.submitList(updated)
            } catch (e: Exception) {
                Log.e(tagLog, "Ошибка обновления: ${e.message}", e)
                Toast.makeText(requireContext(), "Ошибка обновления: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                swipe.isRefreshing = false
            }
        }
    }

    private fun runFirstAnimationOnce() {
        if (!firstAnimationDone) {
            recycler.scheduleLayoutAnimation()
            firstAnimationDone = true
        }
    }

    private fun parsePair(pair: String): FavoriteUi? {
        val parts = pair.split("->")
        if (parts.size != 2) return null

        val base = parts[0].trim().uppercase()
        val target = parts[1].trim().uppercase()

        if (!base.matches(Regex("^[A-Z]{3}$")) || !target.matches(Regex("^[A-Z]{3}$"))) return null

        return FavoriteUi(
            pair = "$base->$target",
            base = base,
            target = target,
            rateText = "Загрузка...",
            updatedText = "",
            trend = RateTrend.UNKNOWN
        )
    }

    /**
     * Оптимизация: 1 запрос на каждую базовую валюту
     */
    private suspend fun loadRatesForPairs(items: List<FavoriteUi>): List<FavoriteUi> {
        val ctx = requireContext()
        val bases = items.map { it.base }.distinct()

        val responses = kotlinx.coroutines.coroutineScope {
            bases.map { base ->
                async { base to ApiClient.api.latest(base) }
            }.awaitAll().toMap()
        }

        return items.map { item ->
            val resp = responses[item.base]

            if (resp == null) {
                item.copy(rateText = "Нет данных", updatedText = "", trend = RateTrend.UNKNOWN)
            } else if (resp.result != "success") {
                item.copy(rateText = "Ошибка API", updatedText = "", trend = RateTrend.UNKNOWN)
            } else {
                val rate = resp.rates[item.target]
                val niceTime = formatUtc(resp.lastUpdateUtc)

                if (rate == null) {
                    item.copy(
                        rateText = "Нет ${item.target}",
                        updatedText = if (niceTime.isBlank()) "" else "Обновлено: $niceTime",
                        trend = RateTrend.UNKNOWN
                    )
                } else {
                    val old = RateHistoryStore.get(ctx, item.pair)
                    val trend = when {
                        old == null -> RateTrend.UNKNOWN
                        rate > old -> RateTrend.UP
                        rate < old -> RateTrend.DOWN
                        else -> RateTrend.SAME
                    }

                    RateHistoryStore.put(ctx, item.pair, rate)

                    item.copy(
                        rateText = "1 ${item.base} = ${"%.4f".format(rate)} ${item.target}",
                        updatedText = if (niceTime.isBlank()) "" else "Обновлено: $niceTime",
                        trend = trend
                    )
                }
            }
        }
    }

    /**
     * minSdk 24: форматируем RFC1123 в локальное время телефона
     * вход: "Tue, 18 Feb 2025 00:02:31 +0000"
     */
    private fun formatUtc(input: String?): String {
        if (input.isNullOrBlank()) return ""

        return try {
            val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date: Date = parser.parse(input) ?: return input

            val out = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            out.format(date)
        } catch (_: Exception) {
            input
        }
    }
}
