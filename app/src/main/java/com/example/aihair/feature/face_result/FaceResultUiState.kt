package com.example.aihair.feature.face_result

import com.example.aihair.core.data.remote.dto.analysis.FaceAnalysisData
import com.example.aihair.feature.hair_ai.HairStyleItem

sealed class FaceResultUiState {
    object Idle : FaceResultUiState()
    object Loading : FaceResultUiState()
    data class Success(
        val data: FaceAnalysisData, 
        val recommendedHairstyles: List<HairStyleItem>
    ) : FaceResultUiState()
    data class Error(val message: String) : FaceResultUiState()
}
