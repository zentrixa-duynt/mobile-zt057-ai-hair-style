package com.example.aihair.feature.photo_editor

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.aihair.core.ui.base.BaseAdapter
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.ItemEditorThumbnailBinding
import com.example.aihair.feature.hair_ai.HairStyleItem

fun createPhotoEditorAdapter(
    onStyleClick: (String) -> Unit
): BaseAdapter<HairStyleItem, ItemEditorThumbnailBinding> {
    var lastAnimatedPosition = -1
    return BaseAdapter(
        bindingInflater = ItemEditorThumbnailBinding::inflate,
        areItemsTheSame = { old, new -> old.id == new.id },
        bind = { binding, item, position ->
            if (item.nameResId != null) {
                binding.txtThumbnailName.setText(item.nameResId)
            } else {
                binding.txtThumbnailName.text = item.name
            }
            binding.txtThumbnailName.isSelected = true
            
            Glide.with(binding.imgThumbnail.context).clear(binding.imgThumbnail)
            Glide.with(binding.imgThumbnail.context)
                .load(item.imageResId)
                .into(binding.imgThumbnail)

            binding.viewStroke.isVisible = item.isSelected

            binding.root.setDebouncedClickListener {
                onStyleClick(item.id)
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
