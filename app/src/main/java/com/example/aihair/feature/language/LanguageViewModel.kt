package com.example.aihair.feature.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aihair.R
import com.example.aihair.core.data.local.datastore.AppPreferences
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
    private val defaultLanguages = listOf(
        LanguageItemUiModel(R.string.language_english, "English", "gb", "en"),
        LanguageItemUiModel(R.string.language_vietnamese, "Tiếng Việt", "vn", "vi"),
        LanguageItemUiModel(R.string.language_spanish, "Español", "es", "es"),
        LanguageItemUiModel(R.string.language_french, "Français", "fr", "fr"),
        LanguageItemUiModel(R.string.language_german, "Deutsch", "de", "de"),
        LanguageItemUiModel(R.string.language_japanese, "日本語", "jp", "ja"),
        LanguageItemUiModel(R.string.language_korean, "한국어", "kr", "ko"),
        LanguageItemUiModel(R.string.language_russian, "Русский", "ru", "ru"),
        LanguageItemUiModel(R.string.language_hindi, "हिन्दी", "in", "hi"),
        LanguageItemUiModel(R.string.language_portuguese, "Português", "pt", "pt"),
        LanguageItemUiModel(R.string.language_turkish, "Türkçe", "tr", "tr"),
        LanguageItemUiModel(R.string.language_croatian, "Hrvatski", "hr", "hr"),
        LanguageItemUiModel(R.string.language_hungarian, "Magyar", "hu", "hu"),
        LanguageItemUiModel(R.string.language_indonesian, "Bahasa Indonesia", "id", "id"),
        LanguageItemUiModel(R.string.language_italian, "Italiano", "it", "it"),
        LanguageItemUiModel(R.string.language_nepali, "नेपाली", "np", "ne"),
        LanguageItemUiModel(R.string.language_thai, "ไทย", "th", "th"),
        LanguageItemUiModel(R.string.language_ukrainian, "Українська", "ua", "uk"),
        LanguageItemUiModel(R.string.language_chinese, "中文", "cn", "zh"),
        LanguageItemUiModel(R.string.language_arabic, "العربية", "sa", "ar"),
        LanguageItemUiModel(R.string.language_urdu, "اردو", "pk", "ur"),
        LanguageItemUiModel(R.string.language_bengali, "বাংলা", "bd", "bn"),
        LanguageItemUiModel(R.string.language_filipino, "Filipino", "ph", "tl"),
        LanguageItemUiModel(R.string.language_afrikaans, "Afrikaans", "za", "af"),
        LanguageItemUiModel(R.string.language_dutch, "Nederlands", "nl", "nl"),
    )

    private val _uiState = MutableStateFlow(LanguageUiState(items = defaultLanguages))
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
        val updatedItems = defaultLanguages.mapIndexed { index, item ->
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

