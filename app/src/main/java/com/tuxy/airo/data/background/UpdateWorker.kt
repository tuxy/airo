package com.tuxy.airo.data.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tuxy.airo.AlarmController
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataBase
import com.tuxy.airo.data.flightdata.getData
import com.tuxy.airo.screens.ApiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UpdateWorker(
    workerParameters: WorkerParameters,
    context: Context,
): CoroutineWorker(context, workerParameters) {
    private val alarmController = AlarmController(context)
    private val preferencesInterface = PreferencesInterface(context)
    private val flightDataDao = FlightDataBase.getDatabase(context).flightDataDao()
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    // private val preferencesInterface = PreferencesInterface(context) // For future use

    override suspend fun doWork(): Result {

        val apiSettings = ApiSettings(
            choice = preferencesInterface.getValueFlowString("selected_api").first(),
            adbEndpoint = preferencesInterface.getValueFlowString("endpoint_adb").first(),
            adbKey = preferencesInterface.getValueFlowString("endpoint_adb_key").first(),
            server = preferencesInterface.getValueFlowString("endpoint_airoapi").first()
        )

        refreshFlightDataList(apiSettings)
        Log.d("UpdateWorker", "Updating work")
        return Result.success()
    }

    private fun refreshFlightDataList(apiSettings: ApiSettings) {
        val flightDataList = flightDataDao.readAll()
        for (i in flightDataList) { // Will this exceed the worker time limit?
            if (i.departDate < LocalDateTime.now()) { // Ignore flight if depart date has already passed
                break
            }

            // Checks whether flight is within a week (refreshed every 2 days). If not, ignore to save api calls
            if (!(
                i.departDate > LocalDateTime.now()
                &&
                i.departDate < (LocalDateTime.now().plusDays(7))
            )) {
                break
            }

            scope.launch { // Get flight data and replace old data on success. TODO: Failure will be ignored
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
                    alarmController.setAlarmOnChange(i, flightData)
                }
            }
        }
    }
}