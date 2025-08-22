package com.tuxy.airo.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.tuxy.airo.R
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.IataParserData
import com.tuxy.airo.data.PreferencesInterface
import com.tuxy.airo.data.singleIntoMut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@Suppress("UNCHECKED_CAST", "OPT_IN_USAGE")
class TicketViewModel(
    flightDataDao: FlightDataDao,
    id: String,
    context: Context,
) : ViewModel() {
    val preferencesInterface = PreferencesInterface(context)

    var flightData = mutableStateOf(FlightData())
    var ticketData by mutableStateOf(IataParserData())
    var ticketString by mutableStateOf(flightData.value.ticketData) // This is needed to monitor for changes
    val toast = Toast.makeText(
        context,
        context.resources.getString(R.string.allow_camera_toast),
        Toast.LENGTH_LONG
    )!!

    var hasCameraPermission by mutableStateOf(
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    )

    var openDialog = mutableStateOf(false)

    private fun getData(context: Context) {
        ticketData = IataParserData().parseData(flightData.value.ticketData, context)
    }

    fun updateData(flightDataDao: FlightDataDao, context: Context) {
        flightData.value.ticketData = ticketString
        getData(context)
        GlobalScope.launch(Dispatchers.IO) {
            flightDataDao.updateFlight(flightData.value)
        }
    }

    fun deleteData(flightDataDao: FlightDataDao, context: Context) {
        ticketString = ""
        flightData.value.ticketData = ""
        getData(context)
        GlobalScope.launch(Dispatchers.IO) {
            flightDataDao.updateFlight(flightData.value)
        }
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            val job = singleIntoMut(
                flightData,
                flightDataDao,
                id
            ) // On initialisation, pass db data into flightData
            job.join()
            getData(context)
            ticketString = flightData.value.ticketData
        }
    }

    fun isDataPopulated(): Boolean {
        return ticketString != ""
    }

    // Show the camera
    fun showCamera(
        barCodeLauncher: ManagedActivityResultLauncher<ScanOptions, ScanIntentResult>,
        context: Context
    ) {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(
            ScanOptions.ALL_CODE_TYPES
        )
        options.setPrompt(context.resources.getString(R.string.scan_promt))
        options.setCameraId(0)
        options.setBeepEnabled(false)
        options.setOrientationLocked(false)

        barCodeLauncher.launch(options)
    }

    // Factory
    class Factory(
        private val flightDataDao: FlightDataDao,
        private val id: String,
        private val context: Context
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TicketViewModel(flightDataDao, id, context) as T
        }
    }
}
