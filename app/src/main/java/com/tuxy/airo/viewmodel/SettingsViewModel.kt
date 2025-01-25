package com.tuxy.airo.viewmodel

import android.content.Context
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
    var currentKey by mutableStateOf("")

    @OptIn(DelicateCoroutinesApi::class)
    fun saveKey(value: String) {
        GlobalScope.launch {
            dataStore.saveValueToKey("API_KEY", value)
        }
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


