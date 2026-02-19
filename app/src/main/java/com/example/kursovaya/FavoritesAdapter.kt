package com.example.kursovaya

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class FavoritesAdapter(
    private val onDeleteClick: (pair: String) -> Unit
) : ListAdapter<FavoriteUi, FavoritesAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<FavoriteUi>() {
        override fun areItemsTheSame(oldItem: FavoriteUi, newItem: FavoriteUi): Boolean =
            oldItem.pair == newItem.pair

        override fun areContentsTheSame(oldItem: FavoriteUi, newItem: FavoriteUi): Boolean =
            oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pairText: TextView = itemView.findViewById(R.id.pairText)
        val rateText: TextView = itemView.findViewById(R.id.rateText)
        val updateText: TextView = itemView.findViewById(R.id.updateText)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.pairText.text = "${item.base} → ${item.target}"
        holder.rateText.text = item.rateText
        holder.updateText.text = item.updatedText

        // скрываем кнопку удаления на "пустом" элементе
        val isEmpty = item.pair == "__empty__"
        holder.deleteButton.visibility = if (isEmpty) View.GONE else View.VISIBLE

        holder.deleteButton.setOnClickListener {
            onDeleteClick(item.pair)
        }
    }
}
