package com.example.aihair.feature.makeup_result

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aihair.core.data.local.datastore.AppPreferences
import com.example.aihair.core.data.remote.dto.analysis.AnalysisState
import com.example.aihair.core.data.repository.AnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MakeupResultViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<MakeupResultUiState>(MakeupResultUiState.Idle)
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<MakeupResultEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: MakeupResultAction) {
        when (action) {
            is MakeupResultAction.StartAnalysis -> {
                startAnalysis(action.imageUri)
            }
            is MakeupResultAction.BackClicked -> {
                viewModelScope.launch {
                    _event.emit(MakeupResultEvent.NavigateBack)
                }
            }
        }
    }

    private fun startAnalysis(imageUri: String) {
        viewModelScope.launch {
            val launchState = appPreferences.launchState.first()
            val languageCode = launchState.selectedLanguage ?: "en"
            val targetLanguage = when (languageCode) {
                "vi" -> "Vietnamese"
                "es" -> "Spanish"
                "fr" -> "French"
                "de" -> "German"
                "ja" -> "Japanese"
                "ko" -> "Korean"
                "ru" -> "Russian"
                "hi" -> "Hindi"
                "pt" -> "Portuguese"
                "tr" -> "Turkish"
                "hr" -> "Croatian"
                "hu" -> "Hungarian"
                "id" -> "Indonesian"
                "it" -> "Italian"
                "ne" -> "Nepali"
                "th" -> "Thai"
                "uk" -> "Ukrainian"
                "zh" -> "Chinese"
                "ar" -> "Arabic"
                "ur" -> "Urdu"
                "bn" -> "Bengali"
                "tl" -> "Filipino"
                "af" -> "Afrikaans"
                "nl" -> "Dutch"
                else -> "English"
            }
            analysisRepository.startMakeupAnalysis(imageUri, targetLanguage).collect { analysisState ->
                when (analysisState) {
                    is AnalysisState.Uploading,
                    is AnalysisState.Submitting,
                    is AnalysisState.Processing -> {
                        _state.update { MakeupResultUiState.Loading }
                    }
                    is AnalysisState.MakeupSuccess -> {
                        _state.update { MakeupResultUiState.Success(analysisState.data) }
                    }
                    is AnalysisState.Error -> {
                        val message = when (val err = analysisState.error) {
                            is AnalysisState.AnalysisError.NetworkError -> err.message
                            is AnalysisState.AnalysisError.StorageError -> err.message
                            is AnalysisState.AnalysisError.SubmitError -> err.message
                            is AnalysisState.AnalysisError.PollingError -> err.message
                            is AnalysisState.AnalysisError.TimeoutError -> err.message
                            is AnalysisState.AnalysisError.TaskFailed -> err.message
                            is AnalysisState.AnalysisError.ParseError -> err.message
                        }
                        _state.update { MakeupResultUiState.Error(message) }
                        _event.emit(MakeupResultEvent.ShowToast(message))
                        _event.emit(MakeupResultEvent.NavigateBack)
                    }
                    else -> {}
                }
            }
        }
    }
}
