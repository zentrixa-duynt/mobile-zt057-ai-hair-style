package com.example.aihair.core.language

import com.example.aihair.R
import com.example.aihair.feature.language.LanguageItemUiModel

object LanguageProvider {
    val defaultLanguages = listOf(
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

    fun getLanguageName(tag: String?): String {
        val currentTag = tag?.takeIf { it.isNotEmpty() } ?: "en"
        return defaultLanguages.find { it.languageTag == currentTag }?.nativeName ?: "English"
    }
}