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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

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
            refresh(showLoader = false)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        swipe.setOnRefreshListener { refresh(showLoader = false) }

        refresh(showLoader = true)
    }

    override fun onResume() {
        super.onResume()
        // если добавили что-то в избранное на первом табе — обновим список
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
            )
            swipe.isRefreshing = false
            return
        }

        // Показать “загрузка” сразу
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
                Log.e(tagLog, "Ошибка обновления избранного: ${e.message}", e)
                Toast.makeText(requireContext(), "Не удалось обновить курсы: ${e.message}", Toast.LENGTH_LONG).show()
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
        if (!base.matches(Regex("^[A-Z]{3}$")) || !target.matches(Regex("^[A-Z]{3}$"))) return null

        return FavoriteUi(
            pair = "$base->$target",
            base = base,
            target = target,
            rateText = "Загрузка...",
            updatedText = ""
        )
    }

    /**
     * Оптимизация:
     * Если избранных 10, но баз всего 2 (USD и EUR) — мы делаем 2 запроса, а не 10.
     */
    private suspend fun loadRatesForPairs(items: List<FavoriteUi>): List<FavoriteUi> {
        val bases = items.map { it.base }.distinct()

        // грузим ответы параллельно (по каждой базе один запрос)
        val responses = kotlinx.coroutines.coroutineScope {
            bases.map { base ->
                async {
                    base to ApiClient.api.latest(base)
                }
            }.awaitAll().toMap()
        }

        return items.map { item ->
            val resp = responses[item.base]
            if (resp == null) {
                item.copy(rateText = "Нет данных", updatedText = "")
            } else if (resp.result != "success") {
                item.copy(rateText = "Ошибка API: ${resp.errorType ?: "unknown"}", updatedText = "")
            } else {
                val rate = resp.rates[item.target]
                if (rate == null) {
                    item.copy(rateText = "Нет ${item.target} в ответе", updatedText = "Обновлено: ${resp.lastUpdateUtc}")
                } else {
                    item.copy(
                        rateText = "1 ${item.base} = ${"%.4f".format(rate)} ${item.target}",
                        updatedText = "Обновлено: ${resp.lastUpdateUtc}"
                    )
                }
            }
        }
    }
}
