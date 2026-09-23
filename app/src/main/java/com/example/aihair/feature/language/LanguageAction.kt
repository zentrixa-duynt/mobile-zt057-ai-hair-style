package com.example.aihair.feature.language

sealed interface LanguageAction {
    data class SelectLanguage(val languageTag: String) : LanguageAction
    data object ConfirmSelection : LanguageAction
}
