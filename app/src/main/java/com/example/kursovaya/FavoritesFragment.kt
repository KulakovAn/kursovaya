package com.example.kursovaya

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFavorites(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadFavorites(it) }
    }

    private fun loadFavorites(root: View) {
        val container = root.findViewById<LinearLayout>(R.id.favoritesContainer)
        container.removeAllViews()

        val favorites = FavoritesStore.getAll(requireContext())

        if (favorites.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "Нет избранных валют"
                textSize = 18f
            }
            container.addView(emptyText)
            return
        }

        favorites.forEach { pair ->
            val item = TextView(requireContext()).apply {
                text = pair
                textSize = 20f
                setPadding(0, 20, 0, 20)
                setOnClickListener {
                    FavoritesStore.remove(requireContext(), pair)
                    Toast.makeText(requireContext(), "Удалено: $pair", Toast.LENGTH_SHORT).show()
                    loadFavorites(root)
                }
            }
            container.addView(item)
        }
    }
}
