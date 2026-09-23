package com.example.aihair.feature.main.history.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.aihair.feature.main.history.HistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "history_prefs")

@Singleton
class HistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val HISTORY_KEY = stringPreferencesKey("history_list")
    private val gson = Gson()

    val historyFlow: Flow<List<HistoryItem>> = context.dataStore.data.map { prefs ->
        val json = prefs[HISTORY_KEY]
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            try {
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun addHistoryItem(item: HistoryItem) {
        context.dataStore.edit { prefs ->
            val json = prefs[HISTORY_KEY]
            val list: MutableList<HistoryItem> = if (json.isNullOrEmpty()) {
                mutableListOf()
            } else {
                val type = object : TypeToken<MutableList<HistoryItem>>() {}.type
                try {
                    gson.fromJson(json, type)
                } catch (e: Exception) {
                    mutableListOf()
                }
            }
            list.add(0, item) // Thêm vào đầu danh sách
            prefs[HISTORY_KEY] = gson.toJson(list)
        }
    }

    suspend fun removeHistoryItem(id: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[HISTORY_KEY]
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<MutableList<HistoryItem>>() {}.type
                try {
                    val list: MutableList<HistoryItem> = gson.fromJson(json, type)
                    list.removeAll { it.id == id }
                    prefs[HISTORY_KEY] = gson.toJson(list)
                } catch (e: Exception) {}
            }
        }
    }
}
