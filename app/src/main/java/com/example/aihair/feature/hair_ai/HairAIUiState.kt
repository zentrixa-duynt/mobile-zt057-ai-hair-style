package com.example.aihair.feature.hair_ai

import android.net.Uri

sealed interface HairAIUiState {
    object Loading : HairAIUiState
    
    data class Success(
        val isColorMode: Boolean = false,
        val isFemaleTabSelected: Boolean = true,
        val styles: List<HairStyleItem> = emptyList(),
        val selectedStyleId: String? = null,
        val selectedPhotoUri: Uri? = null,
        val isPhotoVertical: Boolean = false
    ) : HairAIUiState
    
    data class Error(val message: String) : HairAIUiState
}
