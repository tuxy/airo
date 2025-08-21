package com.tuxy.airo.screens.settings.prefs

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jamal.composeprefs3.ui.LocalPrefsDataStore
import kotlinx.coroutines.launch

/**
 * A composable function that displays a single-choice segmented button row for selecting a preference.
 * The selected value is stored in DataStore.
 *
 * @param key The key to use for storing the preference in DataStore.
 * @param modifier The modifier to apply to the composable.
 * @param enabled Whether the segmented buttons are enabled.
 * @param entries A map of entries to display in the segmented button row. The keys are the values to store in DataStore, and the values are the labels to display.
 */
@Composable
fun SingleSegmentedListPref(
    key: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    entries: Map<String, String> = mapOf(),
) {
    val entryList = entries.toList()
    val selectionKey = stringPreferencesKey(key)
    val scope = rememberCoroutineScope()

    val datastore = LocalPrefsDataStore.current
    val prefs by remember { datastore.data }.collectAsState(initial = null)

    var selected = ""
    prefs?.get(selectionKey)?.also { selected = it } // starting value if it exists in datastore

    fun edit(current: Pair<String, String>) = run {
        scope.launch {
            try {
                datastore.edit { preferences ->
                    preferences[selectionKey] = current.first
                }
            } catch (e: Exception) {
                Log.e(
                    "SingleSegmentedListPref",
                    "Could not write pref $key to database. ${e.printStackTrace()}"
                )
            }
        }
    }

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        entryList.forEach { entry ->
            SegmentedButton(
                selected = entry.first == selected,
                enabled = enabled,
                label = { Text(entry.second) },
                onClick = {
                    edit(current = entry)
                    selected = entry.first
                },
                shape = SegmentedButtonDefaults.itemShape(
                    index = entry.first.toInt(),
                    count = entries.size
                ),
            )
        }
    }
}