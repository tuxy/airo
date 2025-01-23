package com.tuxy.airo.viewmodel

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.singleIntoMut

@Suppress("UNCHECKED_CAST")
class StandardDataViewModel(flightDataDao: FlightDataDao, id: String) : ViewModel() {
    var flightData = mutableStateOf(FlightData())

    init {
        singleIntoMut(
            flightData,
            flightDataDao,
            id
        ) // On initialisation, pass db data into flightData
    }

    fun openWebpage(context: Context, url: String) {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true) //
            .setUrlBarHidingEnabled(true)
            .build()
        intent.launchUrl(context, Uri.parse(url))
    }

    // Factory
    class Factory(
        private val flightDataDao: FlightDataDao,
        private val id: String,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StandardDataViewModel(flightDataDao, id) as T
        }
    }
}
