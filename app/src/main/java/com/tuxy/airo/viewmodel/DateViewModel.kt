package com.tuxy.airo.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.R
import com.tuxy.airo.data.PreferencesInterface
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Suppress("UNCHECKED_CAST")
class DateViewModel(context: Context) : ViewModel() {
    // Initialise interface to preferences
    val preferencesInterface = PreferencesInterface(context)

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
        ),
        Toast.makeText(
            context,
            context.resources.getString(R.string.error_unknown),
            Toast.LENGTH_SHORT
        ),
        Toast.makeText(
            context,
            context.resources.getString(R.string.update_error),
            Toast.LENGTH_SHORT
        )
    )
    var loading by mutableStateOf(false)

    fun maybe(time: Long?): Long {
        return time ?: 0
    }

    fun getDateAsString(time: Long): ZonedDateTime? {
        return LocalDateTime.ofEpochSecond(time / 1000L, 0, ZoneOffset.UTC)
            .atZone(ZoneOffset.systemDefault())
        // return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
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

