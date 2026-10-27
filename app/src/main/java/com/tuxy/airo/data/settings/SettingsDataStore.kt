package com.tuxy.airo.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore instance for user settings.
 *
 * Kept in the data layer (not in an Activity) so that it can be injected into
 * repositories, workers and ViewModels without pulling UI classes in.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
