package com.example.kursovaya

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

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
        val card: MaterialCardView = itemView.findViewById(R.id.itemCard)
        val pairText: TextView = itemView.findViewById(R.id.pairText)
        val rateText: TextView = itemView.findViewById(R.id.rateText)
        val updateText: TextView = itemView.findViewById(R.id.updateText)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        val sparkline: SparklineView = itemView.findViewById(R.id.sparkline)
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

        val ctx = holder.itemView.context
        val isEmpty = item.pair == "__empty__"
        holder.deleteButton.visibility = if (isEmpty) View.GONE else View.VISIBLE
        holder.sparkline.visibility = if (isEmpty) View.GONE else View.VISIBLE

        holder.deleteButton.setOnClickListener { onDeleteClick(item.pair) }

        val strokeColor = when (item.trend) {
            RateTrend.UP -> android.R.color.holo_green_light
            RateTrend.DOWN -> android.R.color.holo_red_light
            RateTrend.SAME -> android.R.color.darker_gray
            RateTrend.UNKNOWN -> android.R.color.darker_gray
        }
        holder.card.strokeColor = ContextCompat.getColor(ctx, strokeColor)

        holder.sparkline.setData(item.series, item.trend)
    }
}
