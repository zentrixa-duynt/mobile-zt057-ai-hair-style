package com.example.aihair.core.data.remote.dto.analysis

sealed class AnalysisState {
    object Uploading : AnalysisState()
    object Submitting : AnalysisState()
    object Processing : AnalysisState()
    data class FaceSuccess(val data: FaceAnalysisData) : AnalysisState()
    data class MakeupSuccess(val data: MakeupAnalysisData) : AnalysisState()
    data class Error(val error: AnalysisError) : AnalysisState()

    sealed class AnalysisError {
        data class NetworkError(val message: String) : AnalysisError()
        data class StorageError(val message: String) : AnalysisError()
        data class SubmitError(val code: Int, val message: String) : AnalysisError()
        data class PollingError(val code: Int, val message: String) : AnalysisError()
        data class TimeoutError(val message: String) : AnalysisError()
        data class TaskFailed(val message: String) : AnalysisError()
        data class ParseError(val message: String) : AnalysisError()
    }
}