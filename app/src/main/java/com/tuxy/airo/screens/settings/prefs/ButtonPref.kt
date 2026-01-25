package com.tuxy.airo.screens.settings.prefs

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.jamal.composeprefs3.ui.LocalPrefsDataStore
import kotlinx.coroutines.launch

@Composable
fun ButtonPref(
    key: String,
    modifier: Modifier = Modifier,
    defaultChecked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    title: String,
) {
    val selectionKey = booleanPreferencesKey(key)
    val scope = rememberCoroutineScope()

    val datastore = LocalPrefsDataStore.current

    val prefs by remember { datastore.data }.collectAsState(initial = null)

    var checked: Boolean = defaultChecked
    prefs?.get(selectionKey)?.also { checked = it } // starting value if it exists in datastore

    fun edit(current: Boolean) = run {
        scope.launch {
            try {
                datastore.edit { preferences ->
                    preferences[selectionKey] = current
                }
                checked = current
                onCheckedChange?.invoke(current)
            } catch (e: Exception) {
                Log.e("SwitchPref", "Could not write pref $key to database. ${e.printStackTrace()}")
            }
        }
    }

    TextButton(
        modifier = modifier,
        onClick = {
            edit(!checked)
        }
    ) {
        Text(title)
    }
}