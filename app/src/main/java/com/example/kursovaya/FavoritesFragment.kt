package com.example.kursovaya

import android.os.Bundle
import android.util.Log
import android.view.View
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe = view.findViewById(R.id.swipeRefresh)
        recycler = view.findViewById(R.id.favoritesRecycler)

        adapter = FavoritesAdapter { pair ->
            FavoritesStore.remove(requireContext(), pair)
            Toast.makeText(requireContext(), "Удалено: $pair", Toast.LENGTH_SHORT).show()
            refresh(false)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        swipe.setOnRefreshListener { refresh(false) }

        refresh(true)
    }

    override fun onResume() {
        super.onResume()
        refresh(false)
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
            )
            swipe.isRefreshing = false
            return
        }

        val initial = pairs.mapNotNull { parsePair(it) }.map {
            it.copy(rateText = "Загрузка...", updatedText = "")
        }

        adapter.submitList(initial)

        if (showLoader) swipe.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) {
                    loadRatesForPairs(initial)
                }
                adapter.submitList(updated)
            } catch (e: Exception) {
                Log.e(tagLog, "Ошибка обновления: ${e.message}", e)
                Toast.makeText(requireContext(), "Ошибка обновления: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                swipe.isRefreshing = false
            }
        }
    }

    private fun parsePair(pair: String): FavoriteUi? {
        val parts = pair.split("->")
        if (parts.size != 2) return null

        val base = parts[0].trim().uppercase()
        val target = parts[1].trim().uppercase()

        if (!base.matches(Regex("^[A-Z]{3}$")) ||
            !target.matches(Regex("^[A-Z]{3}$"))
        ) return null

        return FavoriteUi(
            pair = "$base->$target",
            base = base,
            target = target,
            rateText = "Загрузка...",
            updatedText = ""
        )
    }

    private suspend fun loadRatesForPairs(items: List<FavoriteUi>): List<FavoriteUi> {
        val bases = items.map { it.base }.distinct()

        val responses = kotlinx.coroutines.coroutineScope {
            bases.map { base ->
                async { base to ApiClient.api.latest(base) }
            }.awaitAll().toMap()
        }

        return items.map { item ->
            val resp = responses[item.base]

            if (resp == null) {
                item.copy(rateText = "Нет данных", updatedText = "")
            } else if (resp.result != "success") {
                item.copy(rateText = "Ошибка API", updatedText = "")
            } else {
                val rate = resp.rates[item.target]
                val niceTime = formatUtc(resp.lastUpdateUtc)

                if (rate == null) {
                    item.copy(
                        rateText = "Нет ${item.target}",
                        updatedText = "Обновлено: $niceTime"
                    )
                } else {
                    item.copy(
                        rateText = "1 ${item.base} = ${"%.4f".format(rate)} ${item.target}",
                        updatedText = "Обновлено: $niceTime"
                    )
                }
            }
        }
    }

    /**
     * Работает на minSdk 24 (без java.time)
     * Вход: "Tue, 18 Feb 2025 00:02:31 +0000"
     * Выход: "19.02.2026 12:00"
     */
    private fun formatUtc(input: String?): String {
        if (input.isNullOrBlank()) return ""

        return try {
            val parser = SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss Z",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val date: Date = parser.parse(input) ?: return input

            val formatter = SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.getDefault()
            ).apply {
                timeZone = TimeZone.getDefault()
            }

            formatter.format(date)
        } catch (_: Exception) {
            input
        }
    }
}
