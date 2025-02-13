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
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("UNCHECKED_CAST")
class DateViewModel(context: Context) : ViewModel() {
    private val dataStore = UserPreferences(context)
    val toasts = arrayOf( // Toasts on error
        Toast.makeText(
            context,
            context.resources.getString(R.string.no_api),
            Toast.LENGTH_SHORT
        ),
        Toast.makeText(
            context,
            context.resources.getString(R.string.invalid_api_network),
            Toast.LENGTH_SHORT
        ),
        Toast.makeText(
            context,
            context.resources.getString(R.string.no_flight),
            Toast.LENGTH_SHORT
        ),
        Toast.makeText(
            context,
            context.resources.getString(R.string.flight_exists),
            Toast.LENGTH_SHORT
        )
    )
    var loading by mutableStateOf(false)

    @Composable // Definitely not a composable, but it works to get API Key
    fun getValue(key: String): String {
        return dataStore.getValueWithKey(key).collectAsState(initial = "").value
    }


    fun maybe(time: Long?): Long {
        return time ?: 0
    }

    fun getDateAsString(time: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
        return dateFormat.format(time)
    }

    fun formatFlightNumber(string: String): String {
        // Using " " or "-" as a space in between the carrier and flight number will be split either way
        val splitString = string.split("[- ]")
        if (splitString.size == 2) {
            return "${splitString[0]}${splitString[1]}"
        }
        return string
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

