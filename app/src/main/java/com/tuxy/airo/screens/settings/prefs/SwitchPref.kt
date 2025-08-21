package com.tuxy.airo.screens.settings.prefs

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.jamal.composeprefs3.ui.LocalPrefsDataStore
import kotlinx.coroutines.launch

/**
 * A Composable function that displays a switch preference item.
 * It allows the user to toggle a boolean value, which is stored in DataStore.
 *
 * @param key The key to use for storing the preference in DataStore.
 * @param title The title to display for the preference item.
 * @param modifier Optional [Modifier] to be applied to the preference item.
 * @param defaultChecked The default value of the switch if no value is found in DataStore.
 * @param onCheckedChange An optional callback that is invoked when the switch state changes.
 * @param enabled A boolean indicating whether the switch should be enabled or disabled.
 */
@Composable
fun SwitchPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    defaultChecked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
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

    ListItem(
        modifier = modifier
            .padding(horizontal = 8.dp),
        headlineContent = {
            Text(
                text = title,
                color = if (!enabled) Color.Gray else Color.Unspecified
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { edit(it) }
            )
        }
    )
}