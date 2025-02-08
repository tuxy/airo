package com.tuxy.airo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferences(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
        val API_KEY = stringPreferencesKey("API_KEY")
        val ENDPOINT = stringPreferencesKey("ENDPOINT")
        val API_SERVER = stringPreferencesKey("API_SERVER")
    }

    val getApiKey: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[API_KEY] ?: ""
        }

    val getEndpoint: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ENDPOINT] ?: ""
        }

    val getApiServer: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[API_SERVER] ?: ""
        }

    fun getValueWithKey(key: String, value: String): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: ""
        }
    }

    suspend fun saveValueToKey(key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }
}
