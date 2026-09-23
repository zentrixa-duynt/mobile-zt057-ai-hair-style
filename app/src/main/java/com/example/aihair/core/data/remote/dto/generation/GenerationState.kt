package com.example.aihair.core.data.remote.dto.generation

sealed class GenerationState {
    object Uploading : GenerationState()
    object Submitting : GenerationState()
    object Processing : GenerationState()
    data class Success(val imageUrl: String) : GenerationState()
    data class Error(val error: GenerationError) : GenerationState()

    sealed class GenerationError {
        data class StorageError(val message: String) : GenerationError()
        data class SubmitError(val code: Int, val message: String) : GenerationError()
        data class PollingError(val code: Int, val message: String) : GenerationError()
        data class TaskFailed(val message: String) : GenerationError()
        data class NetworkError(val message: String) : GenerationError()
        data class TimeoutError(val message: String) : GenerationError()
    }
}