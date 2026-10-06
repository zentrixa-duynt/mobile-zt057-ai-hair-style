package com.example.aihair.feature.main.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.ItemHistoryBinding
import androidx.core.view.isVisible
import dev.zentrixa.common.admob.ZTNativeAdView

sealed class HistoryAdapterItem {
    data class Item(val history: HistoryItem) : HistoryAdapterItem()
    object Ad : HistoryAdapterItem()
}

class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryAdapterItem, RecyclerView.ViewHolder>(HistoryDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoryAdapterItem.Item -> TYPE_ITEM
            is HistoryAdapterItem.Ad -> TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_AD -> {
                val context = parent.context
                val margin8dp = (8 * context.resources.displayMetrics.density).toInt()

                val cardView = androidx.cardview.widget.CardView(context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(margin8dp, margin8dp, margin8dp, margin8dp)
                    }
                    radius = 16 * context.resources.displayMetrics.density
                    cardElevation = 0f
                    setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                }

                val adView = ZTNativeAdView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setConfig("native_home", "p_native_history")
                }
                
                adView.viewTreeObserver.addOnGlobalLayoutListener {
                    if (cardView.isVisible != adView.isVisible) {
                        cardView.isVisible = adView.isVisible
                    }
                }

                cardView.addView(adView)
                AdViewHolder(cardView)
            }
            else -> {
                val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ItemViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is ItemViewHolder && item is HistoryAdapterItem.Item) {
            holder.bind(item.history, onItemClick)
        }
    }

    class ItemViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryItem, onItemClick: (HistoryItem) -> Unit) {
            item.resultImageUri?.let { uriStr ->
                Glide.with(binding.imgHistory.context)
                    .load(uriStr.toUri())
                    .into(binding.imgHistory)
            }
            binding.root.setDebouncedClickListener {
                onItemClick(item)
            }
        }
    }

    class AdViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view)

    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryAdapterItem>() {
        override fun areItemsTheSame(oldItem: HistoryAdapterItem, newItem: HistoryAdapterItem): Boolean {
            return when {
                oldItem is HistoryAdapterItem.Item && newItem is HistoryAdapterItem.Item -> oldItem.history.id == newItem.history.id
                oldItem is HistoryAdapterItem.Ad && newItem is HistoryAdapterItem.Ad -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HistoryAdapterItem, newItem: HistoryAdapterItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        const val TYPE_ITEM = 0
        const val TYPE_AD = 1
    }
}
