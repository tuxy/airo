package com.tuxy.airo.screens.settings.prefs

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jamal.composeprefs3.ui.LocalPrefsDataStore
import kotlinx.coroutines.launch

var currentValue by mutableStateOf("")

/**
 * Composable function that displays a text field preference.
 *
 * @param key The key to use for storing the preference value in DataStore.
 * @param title The title to display for the preference.
 * @param modifier The modifier to apply to the text field.
 * @param enabled Whether the text field should be enabled or disabled.
 */
@SuppressLint("UnrememberedMutableState")
@Composable
fun TextFieldPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val selectionKey = stringPreferencesKey(key)
    val scope = rememberCoroutineScope()

    val datastore = LocalPrefsDataStore.current

    fun edit(current: String) = run {
        scope.launch {
            try {
                datastore.edit { preferences ->
                    preferences[selectionKey] = current
                }
            } catch (e: Exception) {
                Log.e(
                    "TextFieldPref",
                    "Could not write pref $key to database. ${e.printStackTrace()}"
                )
            }
        }
    }

    OutlinedTextField(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        enabled = enabled,
        value = currentValue,
        label = { Text(title) },
        onValueChange = {
            currentValue = it
            edit(it)
        },
        singleLine = true,
    )
}