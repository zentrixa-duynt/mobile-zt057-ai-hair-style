package com.example.aihair.core.data.local.datastore

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_hair_prefs")

data class LaunchState(
    val selectedLanguage: String?,
    val isLanguageSelected: Boolean,
    val isOnboardCompleted: Boolean,
    val selectedGender: String?
) {
    val effectiveLanguage: String?
        get() = if (isLanguageSelected && !isOnboardCompleted) "" else selectedLanguage
}

data class UsageState(
    val lastUsageDate: String,
    val usageCount: Int,
    val unlockedItems: Set<String>
)

class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {


    companion object {
        private val IS_LANGUAGE_SELECTED = booleanPreferencesKey("is_language_selected")
        private val IS_ONBOARD_COMPLETED = booleanPreferencesKey("is_onboard_completed")
        private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        private val SELECTED_GENDER = stringPreferencesKey("selected_gender")
        private val DAILY_USAGE_DATE = stringPreferencesKey("daily_usage_date")
        private val DAILY_USAGE_COUNT = intPreferencesKey("daily_usage_count")
        private val UNLOCKED_ITEMS = stringSetPreferencesKey("unlocked_items")
    }

    val launchState: Flow<LaunchState> = context.dataStore.data.map { preferences ->
        LaunchState(
            selectedLanguage = preferences[SELECTED_LANGUAGE],
            isLanguageSelected = preferences[IS_LANGUAGE_SELECTED] ?: false,
            isOnboardCompleted = preferences[IS_ONBOARD_COMPLETED] ?: false,
            selectedGender = preferences[SELECTED_GENDER]
        )
    }

    suspend fun setLanguageSelected(isSelected: Boolean, languageCode: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[IS_LANGUAGE_SELECTED] = isSelected
            if (languageCode != null) {
                preferences[SELECTED_LANGUAGE] = languageCode
            }
        }
    }

    suspend fun setOnboardCompleted(isCompleted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARD_COMPLETED] = isCompleted
        }
    }

    suspend fun setSelectedGender(gender: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_GENDER] = gender
        }
    }

    val usageState: Flow<UsageState> = context.dataStore.data.map { preferences ->
        UsageState(
            lastUsageDate = preferences[DAILY_USAGE_DATE] ?: "",
            usageCount = preferences[DAILY_USAGE_COUNT] ?: 0,
            unlockedItems = preferences[UNLOCKED_ITEMS] ?: emptySet()
        )
    }

    suspend fun incrementUsageCount(currentDate: String) {
        context.dataStore.edit { preferences ->
            val lastDate = preferences[DAILY_USAGE_DATE] ?: ""
            if (lastDate != currentDate) {
                preferences[DAILY_USAGE_DATE] = currentDate
                preferences[DAILY_USAGE_COUNT] = 1
            } else {
                val currentCount = preferences[DAILY_USAGE_COUNT] ?: 0
                preferences[DAILY_USAGE_COUNT] = currentCount + 1
            }
        }
    }

    suspend fun unlockItem(itemId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[UNLOCKED_ITEMS] ?: emptySet()
            preferences[UNLOCKED_ITEMS] = current + itemId
        }
    }
}
