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

        recycler.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fall_down)

        swipe.setOnRefreshListener { refresh(showLoader = false) }

        refresh(showLoader = true)
    }

    override fun onResume() {
        super.onResume()
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
                        updatedText = "Добавь пары на вкладке «Курсы» (⭐)"
                    )
                )
            ) { runFirstAnimationOnce() }

            swipe.isRefreshing = false
            return
        }

        val initial = pairs.mapNotNull { parsePair(it) }.map {
            it.copy(rateText = "Загрузка...", updatedText = "", trend = RateTrend.UNKNOWN, series = emptyList())
        }

        adapter.submitList(initial) { runFirstAnimationOnce() }

        if (showLoader) swipe.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) { loadRatesForPairs(initial) }
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
            trend = RateTrend.UNKNOWN,
            series = emptyList()
        )
    }

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
                item.copy(rateText = "Нет данных", updatedText = "", trend = RateTrend.UNKNOWN, series = emptyList())
            } else if (resp.result != "success") {
                item.copy(rateText = "Ошибка API", updatedText = "", trend = RateTrend.UNKNOWN, series = emptyList())
            } else {
                val rate = resp.rates[item.target]
                val niceTime = formatUtc(resp.lastUpdateUtc)

                if (rate == null) {
                    item.copy(
                        rateText = "Нет ${item.target}",
                        updatedText = if (niceTime.isBlank()) "" else "Обновлено: $niceTime",
                        trend = RateTrend.UNKNOWN,
                        series = RateHistoryStore.getSeries(ctx, item.pair)
                    )
                } else {
                    // сохраняем точку в историю
                    RateHistoryStore.append(ctx, item.pair, rate)
                    val series = RateHistoryStore.getSeries(ctx, item.pair)

                    // тренд по последним двум точкам
                    val trend = if (series.size >= 2) {
                        val a = series[series.size - 2]
                        val b = series[series.size - 1]
                        when {
                            b > a -> RateTrend.UP
                            b < a -> RateTrend.DOWN
                            else -> RateTrend.SAME
                        }
                    } else RateTrend.UNKNOWN

                    item.copy(
                        rateText = "1 ${item.base} = ${"%.4f".format(rate)} ${item.target}",
                        updatedText = if (niceTime.isBlank()) "" else "Обновлено: $niceTime",
                        trend = trend,
                        series = series
                    )
                }
            }
        }
    }

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
