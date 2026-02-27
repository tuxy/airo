package com.tuxy.airo.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.R
import com.tuxy.airo.data.flightdata_rework.FlightData
import com.tuxy.airo.data.flightdata_rework.FlightDataDao
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * A ViewModel that provides standard data for a flight.
 *
 * @property flightData The flight data for the current flight.
 * @param flightDataDao The DAO for accessing flight data.
 * @param id The ID of the flight.
 */
@OptIn(DelicateCoroutinesApi::class)
@Suppress("UNCHECKED_CAST")
class StandardDataViewModel(flightDataDao: FlightDataDao, id: String) : ViewModel() {
    var flightData = mutableStateOf(FlightData())

    init {
        GlobalScope.launch(Dispatchers.IO) {
            val job = GlobalScope.launch(Dispatchers.IO) { // TODO is this really the best way?
                flightData.value = flightDataDao.readSingle(id) ?: FlightData()
            }
            job.join()
        }
    }

    /**
     * Opens a webpage in a custom tab.
     * @param context The application context.
     * @param url The URL of the webpage to open.
     */
    fun openWebpage(context: Context, url: String) {
        try {
            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true) //
                .setUrlBarHidingEnabled(true)
                .build()
            intent.launchUrl(context, url.toUri())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.resources.getString(R.string.not_avail),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Factory for creating [StandardDataViewModel] instances.
     */
    class Factory(
        private val flightDataDao: FlightDataDao,
        private val id: String,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StandardDataViewModel(flightDataDao, id) as T
        }
    }
}
