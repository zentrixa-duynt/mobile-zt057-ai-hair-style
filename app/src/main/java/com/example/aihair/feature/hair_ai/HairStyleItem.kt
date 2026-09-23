package com.example.aihair.feature.hair_ai

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class HairStyleItem(
    val id: String,
    val name: String = "",
    @DrawableRes val imageResId: Int,
    @StringRes val nameResId: Int? = null,
    val isSelected: Boolean = false
)
