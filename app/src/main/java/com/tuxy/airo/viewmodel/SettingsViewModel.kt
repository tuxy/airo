package com.tuxy.airo.viewmodel

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.data.UserPreferences
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
class SettingsViewModel(context: Context) : ViewModel() {
    private val dataStore = UserPreferences(context)

    // These 2 will be used if the user opts to directly use aerodatabox's api
    var currentApiKey by mutableStateOf("")
    var currentEndpoint by mutableStateOf("")

    // This is used if the user opts for AiroApi's servers
    var currentApiServer by mutableStateOf("")

    @OptIn(DelicateCoroutinesApi::class)
    fun saveKey(key: String, value: String) {
        GlobalScope.launch {
            dataStore.saveValueToKey(key, value)
        }
    }

    @Composable
    fun GetEndpoint() {
        currentEndpoint = dataStore.getEndpoint.collectAsState(initial = "").value
    }

    @Composable
    fun GetApiServer() {
        currentApiServer = dataStore.getApiServer.collectAsState(initial = "").value
    }

    // Factory
    class Factory(
        private val context: Context,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(context) as T
        }
    }
}


