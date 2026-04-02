package com.tuxy.airo.viewmodel.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightDataDao

@Suppress("UNCHECKED_CAST")
class NotificationViewModel(
    context: Context,
    val flightDataDao: FlightDataDao
) : ViewModel() {
    val preferencesInterface = PreferencesInterface(context)

    fun check(option: Boolean) {

    }

    class Factory(
        private val context: Context,
        private val flightDataDao: FlightDataDao
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationViewModel(context, flightDataDao) as T
        }
    }
}