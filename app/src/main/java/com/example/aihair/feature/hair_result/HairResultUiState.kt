package com.example.aihair.feature.hair_result

sealed class HairResultUiState {
    object Loading : HairResultUiState()
    data class Success(val imageUrl: String?) : HairResultUiState()
    data class Error(val message: String) : HairResultUiState()
}
