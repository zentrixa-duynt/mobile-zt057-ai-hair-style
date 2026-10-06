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
            binding.root.clearAnimation()
            
            if (item.nameResId != null) {
                binding.txtThumbnailName.setText(item.nameResId)
            } else {
                binding.txtThumbnailName.text = item.name
            }
            binding.txtThumbnailName.isSelected = item.isSelected
            
            Glide.with(binding.imgThumbnail.context)
                .load(item.imageResId)
                .placeholder(android.R.color.transparent)
                .dontAnimate()
                .into(binding.imgThumbnail)

            binding.viewStroke.isVisible = item.isSelected
            val rewardConfig = dev.zentrixa.common.admob.ZTRewardedAdUtils.getRewardAdsConfig("reward_function_tool")
            val isAdEnabled = rewardConfig?.enabled != false && rewardConfig?.placements?.get("p_unlock_tool") != false
            val isTurnOffAllAds = dev.zentrixa.common.utils.ZTUtils.isTurnOffAllAds
            binding.imgLock.isVisible = item.isLocked && isAdEnabled && !isTurnOffAllAds

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
