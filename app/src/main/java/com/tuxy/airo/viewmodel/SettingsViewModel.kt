package com.tuxy.airo.viewmodel

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.newDataStore
import kotlinx.coroutines.flow.map

@Suppress("UNCHECKED_CAST")
class SettingsViewModel() : ViewModel() {
    /**
     * Retrieves a string value from DataStore based on the provided key.
     *
     * This Composable function asynchronously fetches a string value associated with the given `key`
     * from the application's DataStore. It uses `collectAsState` to observe the data flow
     * and recompose when the value changes. If the key is not found or the value is null,
     * it returns an empty string.
     *
     * @param context The Android [Context] used to access the DataStore.
     * @param key The key (String) for which to retrieve the value.
     * @return The String value associated with the key, or an empty string if not found.
     */
    @Composable
    fun getValue(context: Context, key: String): String {
        val fetch = context.newDataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: ""
        }
        return fetch.collectAsState(initial = "").value
    }

    /**
     * Retrieves a value from SharedPreferences as an Int.
     *
     * @param context The context to use for accessing SharedPreferences.
     * @param key The key of the preference to retrieve.
     * @return The value of the preference as an Int, or 0 if the preference does not exist or cannot be parsed as an Int.
     */
    @Composable
    fun getValueAsInt(context: Context, key: String): Int {
        val string = getValue(context, key)
        return if (string == "") {
            0
        } else {
            string.toInt()
        }
    }

    // Factory
    class Factory(
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel() as T
        }
    }
}


