package com.example.aihair.feature.face_result

import com.example.aihair.core.ui.base.BaseAdapter
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.ItemHairstyleSuitBinding
import com.example.aihair.feature.hair_ai.HairStyleItem

fun createHairstyleSuitAdapter(
    onItemClick: (HairStyleItem) -> Unit
): BaseAdapter<HairStyleItem, ItemHairstyleSuitBinding> {
    return BaseAdapter(
        bindingInflater = ItemHairstyleSuitBinding::inflate,
        areItemsTheSame = { old, new -> old.id == new.id },
        bind = { binding, item, _ ->
            binding.imgHairstyle.setImageResource(item.imageResId)
            
            if (item.nameResId != null) {
                binding.txtHairstyleName.setText(item.nameResId)
            } else {
                binding.txtHairstyleName.text = item.name
            }

            val rewardConfig = dev.zentrixa.common.admob.ZTRewardedAdUtils.getRewardAdsConfig("reward_function_AI")
            val isAdEnabled = rewardConfig?.enabled != false && rewardConfig?.placements?.get("p_reward_apply") != false
            binding.imgAdIconApply.visibility = if (isAdEnabled && !dev.zentrixa.common.utils.ZTUtils.isTurnOffAllAds) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            binding.btnApply.setDebouncedClickListener {
                onItemClick(item)
            }
        }
    )
}
