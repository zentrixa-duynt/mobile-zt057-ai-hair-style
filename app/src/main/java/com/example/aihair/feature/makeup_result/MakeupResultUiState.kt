package com.example.aihair.feature.makeup_result

import com.example.aihair.core.data.remote.dto.analysis.MakeupAnalysisData

sealed class MakeupResultUiState {
    object Idle : MakeupResultUiState()
    object Loading : MakeupResultUiState()
    data class Success(val data: MakeupAnalysisData) : MakeupResultUiState()
    data class Error(val message: String) : MakeupResultUiState()
}
