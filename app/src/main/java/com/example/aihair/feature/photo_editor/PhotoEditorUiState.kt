package com.example.aihair.feature.photo_editor

import android.net.Uri

sealed interface PhotoEditorUiState {
    object Loading : PhotoEditorUiState
    
    data class Success(
        val selectedPhotoUri: Uri? = null,
        val isVerticalPhoto: Boolean = true,
        val currentSelectedStyleId: String? = null,
        val currentSelectedStyleResId: Int? = null,
        val selectedColorId: String? = null,
        val customColorHex: String? = null
    ) : PhotoEditorUiState
    
    data class Error(val message: String) : PhotoEditorUiState
}
