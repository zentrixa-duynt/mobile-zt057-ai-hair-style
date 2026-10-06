package com.example.aihair.feature.hair_ai

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.aihair.core.ui.base.BaseAdapter
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.ItemStyleBinding

fun createHairAIAdapter(
    onStyleClick: (HairStyleItem) -> Unit
): BaseAdapter<HairStyleItem, ItemStyleBinding> {
    var lastAnimatedPosition = -1
    return BaseAdapter(
        bindingInflater = ItemStyleBinding::inflate,
        areItemsTheSame = { old, new -> old.id == new.id },
        bind = { binding, item, position ->
            Glide.with(binding.imgStyle.context)
                .load(item.imageResId)
                .into(binding.imgStyle)
            if (item.nameResId != null) {
                binding.tvStyleName.setText(item.nameResId)
            } else {
                binding.tvStyleName.text = item.name
            }
            binding.viewSelectionBorder.isSelected = item.isSelected
            
            binding.root.setDebouncedClickListener {
                onStyleClick(item)
            }
            
            if (position > lastAnimatedPosition) {
                val anim = android.view.animation.AnimationUtils.loadAnimation(binding.root.context, com.example.aihair.R.anim.item_scale_up)
                anim.startOffset = (position * 50L).coerceAtMost(500L)
                binding.root.startAnimation(anim)
                lastAnimatedPosition = position
            }
        }
    )
}
