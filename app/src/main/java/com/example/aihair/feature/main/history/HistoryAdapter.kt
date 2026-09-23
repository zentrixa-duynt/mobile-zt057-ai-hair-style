package com.example.aihair.feature.main.history

import android.net.Uri
import com.bumptech.glide.Glide
import com.example.aihair.core.ui.base.BaseAdapter
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.ItemHistoryBinding
import androidx.core.net.toUri

fun createHistoryAdapter(
    onItemClick: (HistoryItem) -> Unit
): BaseAdapter<HistoryItem, ItemHistoryBinding> {
    return BaseAdapter(
        bindingInflater = ItemHistoryBinding::inflate,
        areItemsTheSame = { old, new -> old.id == new.id },
        bind = { binding, item, _ ->
            item.resultImageUri?.let { uriStr ->
                Glide.with(binding.imgHistory.context)
                    .load(uriStr.toUri())
                    .into(binding.imgHistory)
            }
            binding.root.setDebouncedClickListener {
                onItemClick(item)
            }
        }
    )
}
