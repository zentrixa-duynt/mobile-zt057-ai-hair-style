package com.example.aihair.feature.language

import com.bumptech.glide.Glide
import com.example.aihair.core.language.AppLanguageManager
import com.example.aihair.core.ui.base.BaseAdapter
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.ItemLanguageBinding

fun createLanguageAdapter(
    onLanguageClick: (String) -> Unit,
    currentLanguageTagProvider: () -> String
) = BaseAdapter<LanguageItemUiModel, ItemLanguageBinding>(
    bindingInflater = ItemLanguageBinding::inflate,
    areItemsTheSame = { old, new -> old.languageTag == new.languageTag },
    bind = { itemBinding, item, _ ->
        itemBinding.root.isSelected = item.isSelected
        itemBinding.root.isActivated = item.isSelected
        itemBinding.root.refreshDrawableState()

        Glide.with(itemBinding.ivFlag.context)
            .load("https://flagcdn.com/w80/${item.countryCode}.png")
            .into(itemBinding.ivFlag)

        val context = itemBinding.root.context
        val tag = currentLanguageTagProvider()
        val localizedContext = AppLanguageManager.wrapContext(context, tag)
        val appLanguageName = localizedContext.getString(item.nameResId)
        
        itemBinding.root.layoutDirection = localizedContext.resources.configuration.layoutDirection
        
        itemBinding.tvLanguageEnglish.text = appLanguageName

        itemBinding.rbLanguage.isClickable = false
        itemBinding.rbLanguage.isFocusable = false
        itemBinding.rbLanguage.isChecked = item.isSelected
        
        itemBinding.root.setDebouncedClickListener {
            onLanguageClick(item.languageTag)
        }
    }
)
