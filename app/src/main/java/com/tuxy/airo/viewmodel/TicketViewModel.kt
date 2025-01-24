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
import com.tuxy.airo.data.singleIntoMut

@Suppress("UNCHECKED_CAST")
class TicketViewModel(
    flightDataDao: FlightDataDao,
    id: String,
    context: Context,
) : ViewModel() {
    var flightData = mutableStateOf(FlightData())
    val toast = Toast.makeText(context, context.resources.getString(R.string.allow_camera_toast), Toast.LENGTH_LONG)

    var hasCameraPermission by mutableStateOf(
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    )

    init {
        singleIntoMut(
            flightData,
            flightDataDao,
            id
        ) // On initialisation, pass db data into flightData
    }

    fun showCamera(barCodeLauncher: ManagedActivityResultLauncher<ScanOptions, ScanIntentResult>) {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(
            ScanOptions.ALL_CODE_TYPES
        )
        options.setPrompt("Scan")
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
