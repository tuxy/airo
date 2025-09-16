package com.tuxy.airo.data.background

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tuxy.airo.AlarmController
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.data.flightdata.getData
import com.tuxy.airo.screens.ApiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class UpdateWorker(
    workerParameters: WorkerParameters,
    private val context: Context,
    private val flightDataDao: FlightDataDao,
    private val apiSettings: ApiSettings,
    private val scope: CoroutineScope
): Worker(context, workerParameters) {
    private val alarmController = AlarmController(context)
    // private val preferencesInterface = PreferencesInterface(context)

    override fun doWork(): Result {
        refreshFlightDataList()

        return Result.success()
    }

    private fun refreshFlightDataList() {
        val flightDataList = flightDataDao.readAll()
        for (i in flightDataList) { // Will this exceed the worker time limit?
            scope.launch {
                val result = getData(
                    flightNumber = i.callSign,
                    flightDataDao = flightDataDao,
                    date = i.departDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    settings = apiSettings,
                    context = applicationContext,
                    update = true
                )

                if (result.isSuccess) { flightData: FlightData ->
                    alarmController.cancelAlarm(i)
                    scope.launch {
                        flightDataDao.deleteFlight(i)
                        flightDataDao.addFlight(flightData)
                    }
                    alarmController.setAlarm(flightData)
                }
            }
        }
    }
}