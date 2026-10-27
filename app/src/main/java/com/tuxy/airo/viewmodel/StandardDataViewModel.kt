package com.tuxy.airo.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuxy.airo.R
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.repository.FlightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StandardDataViewModel(
    private val repository: FlightRepository,
    id: String,
) : ViewModel() {

    var flightData = mutableStateOf(FlightData())

    init {
        val numericId = id.toIntOrNull()
        viewModelScope.launch(Dispatchers.IO) {
            val data = numericId?.let { repository.getFlight(it) } ?: FlightData()
            withContext(Dispatchers.Main) {
                flightData.value = data
            }
        }
    }

    fun openWebpage(context: Context, url: String) {
        try {
            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .build()
            intent.launchUrl(context, url.toUri())
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.resources.getString(R.string.not_avail),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    class Factory(
        private val repository: FlightRepository,
        private val id: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StandardDataViewModel(repository, id) as T
        }
    }
}
