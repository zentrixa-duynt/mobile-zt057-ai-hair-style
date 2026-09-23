package com.example.aihair.feature.language

import androidx.annotation.StringRes

data class LanguageUiState(
    val selectedLanguageTag: String = "",
    val items: List<LanguageItemUiModel> = emptyList(),
    val showBackButton: Boolean = false
)

data class LanguageItemUiModel(
    @StringRes val nameResId: Int,
    val nativeName: String,
    val countryCode: String?,
    val languageTag: String,
    val isSelected: Boolean = false,
    val uiLanguageTag: String = ""
)
