package com.tuxy.airo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

data class UserPreferences(
    val showEnabled: Boolean
)

class UserPreferencesRepository( // Access point
    private val dataStore: DataStore<Preferences>,
    context: Context
)
