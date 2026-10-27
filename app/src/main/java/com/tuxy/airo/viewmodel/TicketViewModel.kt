package com.tuxy.airo.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.tuxy.airo.R
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.IataParserData
import com.tuxy.airo.data.repository.FlightRepository
import com.tuxy.airo.data.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TicketViewModel(
    private val repository: FlightRepository,
    preferences: AppPreferences,
    private val id: String,
    private val context: Context,
) : ViewModel() {

    var flightData = mutableStateOf(FlightData())
    var ticketData by mutableStateOf(IataParserData())
    var ticketString by mutableStateOf("")

    var hasCameraPermission by mutableStateOf(
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    )

    var openDialog = mutableStateOf(false)

    val timeFormat: StateFlow<String> = preferences.timeFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "hh:mm a")

    init {
        load()
    }

    private fun load() {
        val numericId = id.toIntOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getFlight(numericId) ?: FlightData()
            withContext(Dispatchers.Main) {
                flightData.value = data
                ticketString = data.ticketData
                parseTicketData()
            }
        }
    }

    private fun parseTicketData() {
        ticketData = IataParserData().parseData(flightData.value.ticketData, context)
    }

    fun updateData() {
        val updated = flightData.value.copy(ticketData = ticketString)
        flightData.value = updated
        parseTicketData()
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTicket(updated)
        }
    }

    fun deleteData() {
        ticketString = ""
        val updated = flightData.value.copy(ticketData = "")
        flightData.value = updated
        parseTicketData()
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTicket(updated)
        }
    }

    fun isDataPopulated(): Boolean = ticketString.isNotEmpty()

    fun showCamera(
        barCodeLauncher: ManagedActivityResultLauncher<ScanOptions, ScanIntentResult>,
    ) {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt(context.resources.getString(R.string.scan_promt))
        options.setCameraId(0)
        options.setBeepEnabled(false)
        options.setOrientationLocked(false)
        barCodeLauncher.launch(options)
    }

    class Factory(
        private val repository: FlightRepository,
        private val preferences: AppPreferences,
        private val id: String,
        private val context: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TicketViewModel(
                repository = repository,
                preferences = preferences,
                id = id,
                context = context.applicationContext,
            ) as T
        }
    }
}
