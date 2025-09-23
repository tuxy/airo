package com.tuxy.airo.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.R
import com.tuxy.airo.data.database.PreferencesInterface
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Suppress("UNCHECKED_CAST", "StaticFieldLeak") // Determined to (probably) not be an issue
class DateViewModel(
    private val context: Context
) : ViewModel() {
    // Initialise interface to preferences
    val preferencesInterface = PreferencesInterface(context)
    var loading by mutableStateOf(false)

    fun toast(idx: Int): Toast {
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
            ),
            Toast.makeText(
                context,
                context.resources.getString(R.string.no_flight),
                Toast.LENGTH_SHORT
            )
        )
        return toasts[idx]
    }

    fun getDateAsString(time: Long): ZonedDateTime? {
        return LocalDateTime.ofEpochSecond(time / 1000L, 0, ZoneOffset.UTC)
            .atZone(ZoneOffset.systemDefault())
    }

    fun formatFlightNumber(string: String): String {
        // Using " " or "-" as a space in between the carrier and flight number will be split either way
        val splitString = string.split("[- ]".toRegex())
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

