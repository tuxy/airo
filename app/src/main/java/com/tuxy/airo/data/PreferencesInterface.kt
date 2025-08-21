package com.tuxy.airo.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tuxy.airo.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesInterface(private val context: Context) {
    /**
     * Retrieves a string value from DataStore based on the provided key.
     *
     * This Composable function asynchronously fetches a string value associated with the given `key`
     * from the application's DataStore. It uses `collectAsState` to observe the data flow
     * and recompose when the value changes. If the key is not found or the value is null,
     * it returns an empty string.
     *
     * @param key The key (String) for which to retrieve the value.
     * @return The String value associated with the key, or an empty string if not found.
     */
    private fun getValueFlow(key: String): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: ""
        }
    }

    @Composable
    fun getValue(key: String): String {
        return getValueFlow(key).collectAsState(initial = "").value
    }

    /**
     * Retrieves a value from SharedPreferences as an Int.
     *
     * @param key The key of the preference to retrieve.
     * @return The value of the preference as an Int, or 0 if the preference does not exist or cannot be parsed as an Int.
     */
    @Composable
    fun getValueAsInt(key: String): Int {
        val string = getValue(key)
        return if (string == "") {
            0
        } else {
            string.toInt()
        }
    }
}