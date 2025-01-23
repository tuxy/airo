package com.tuxy.airo.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.R
import com.tuxy.airo.data.UserPreferences

@Suppress("UNCHECKED_CAST")
class DateViewModel(context: Context) : ViewModel() {
    private val dataStore = UserPreferences(context)
    val toasts = arrayOf(
        Toast.makeText(context, context.resources.getString(R.string.no_api), Toast.LENGTH_SHORT),
        Toast.makeText(context, context.resources.getString(R.string.invalid_api_network), Toast.LENGTH_SHORT),
        Toast.makeText(context, context.resources.getString(R.string.no_flight), Toast.LENGTH_SHORT)
    )
    var loading by mutableStateOf(false)
    var key by mutableStateOf("")

    @Composable
    fun GetKey() {
        key = dataStore.getApiKey.collectAsState(initial = "").value
    }

    // Factory
    class Factory(
        private val context: Context,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DateViewModel(context) as T
        }
    }

}
