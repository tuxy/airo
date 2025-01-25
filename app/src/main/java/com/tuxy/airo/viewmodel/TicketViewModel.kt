package com.tuxy.airo.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil3.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.tuxy.airo.R
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.IataParserData
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
    var flightData = mutableStateOf(FlightData())
    var ticketData by mutableStateOf(IataParserData())
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

    private fun getData(context: Context) {
        ticketData = IataParserData().parseData(flightData.value.ticketData, context)
    }

    fun updateData(result: String, flightDataDao: FlightDataDao, context: Context) {
        flightData.value.ticketData = result
        ticketData = IataParserData().parseData(result, context)
        GlobalScope.launch(Dispatchers.IO) {
            flightDataDao.updateFlight(flightData.value)
        }
    }

    fun deleteData(flightDataDao: FlightDataDao, context: Context) {
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
        }
    }

    fun isDataPopulated(): Boolean {
        return flightData.value.ticketData != ""
    }

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

    fun getQrCode(): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(flightData.value.ticketData, BarcodeFormat.QR_CODE, 400, 400)

        val w = bitMatrix.width
        val h = bitMatrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                pixels[y * w + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        val bitmap =
            Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
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
