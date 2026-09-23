package com.example.aihair.feature.language

sealed interface LanguageEvent {
    object LanguageSaved : LanguageEvent
    data class ShowError(val message: String) : LanguageEvent
}
