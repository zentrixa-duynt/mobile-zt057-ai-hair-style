package com.example.aihair.feature.makeup_result

import com.example.aihair.core.ui.base.BaseAdapter
import com.example.aihair.databinding.ItemMakeupRecommendationBinding

fun createMakeupRecommendationAdapter(): BaseAdapter<String, ItemMakeupRecommendationBinding> {
    return BaseAdapter(
        bindingInflater = ItemMakeupRecommendationBinding::inflate,
        areItemsTheSame = { old, new -> old == new },
        bind = { binding, item, _ ->
            binding.txtDesc.text = "• $item"
        }
    )
}
