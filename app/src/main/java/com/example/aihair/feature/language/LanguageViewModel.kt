package com.example.aihair.feature.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aihair.core.data.local.datastore.AppPreferences
import com.example.aihair.core.language.LanguageProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(LanguageUiState(items = LanguageProvider.defaultLanguages))
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<LanguageEvent>()
    val event: SharedFlow<LanguageEvent> = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val launchState = appPreferences.launchState.first()
                val isSelected = launchState.isLanguageSelected
                val selectedLanguage = launchState.selectedLanguage
                if (isSelected) {
                    val langToSelect = selectedLanguage ?: "en"
                    updateStateWithLanguage(langToSelect, showBackButton = true)
                } else {
                    updateStateWithLanguage("", showBackButton = false)
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
            }
        }
    }

    private fun updateStateWithLanguage(languageTag: String, showBackButton: Boolean? = null) {
        val updatedItems = LanguageProvider.defaultLanguages.mapIndexed { index, item ->
            item.copy(
                isSelected = item.languageTag == languageTag && languageTag.isNotEmpty(),
                uiLanguageTag = languageTag
            )
        }
        _uiState.value = _uiState.value.copy(
            selectedLanguageTag = languageTag,
            items = updatedItems,
            showBackButton = showBackButton ?: _uiState.value.showBackButton
        )
    }

    fun onAction(action: LanguageAction) {
        when (action) {
            is LanguageAction.SelectLanguage -> {
                if (_uiState.value.selectedLanguageTag == action.languageTag) return
                updateStateWithLanguage(action.languageTag)
                
                viewModelScope.launch {
                    try {
                        appPreferences.setLanguageSelected(true, action.languageTag)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
                com.example.aihair.core.language.AppLanguageManager.setPendingLanguage(action.languageTag)
                com.example.aihair.core.language.AppLanguageManager.applyLanguage(action.languageTag)
            }
            is LanguageAction.ConfirmSelection -> onConfirmSelection()
        }
    }

    private fun onConfirmSelection() {
        viewModelScope.launch {
            val selectedLanguageTag = _uiState.value.selectedLanguageTag
            if (selectedLanguageTag.isEmpty()) return@launch
            try {
                appPreferences.setLanguageSelected(true, selectedLanguageTag)
                _event.emit(LanguageEvent.LanguageSaved)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _event.emit(LanguageEvent.ShowError(e.message.toString()))
            }
        }
    }

}

