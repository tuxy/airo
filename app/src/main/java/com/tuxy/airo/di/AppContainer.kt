package com.tuxy.airo.di

import android.content.Context
import com.tuxy.airo.data.background.ReminderScheduler
import com.tuxy.airo.data.flightdata.FlightDataBase
import com.tuxy.airo.data.network.FlightApi
import com.tuxy.airo.data.repository.FlightRepository
import com.tuxy.airo.data.settings.AppPreferences
import com.tuxy.airo.data.settings.dataStore
import kotlinx.coroutines.flow.first
import okhttp3.Cache
import java.io.File

/**
 * Hand-written dependency container.
 *
 * A full DI framework (Hilt/Koin) is deliberately avoided for now: the graph is
 * small and keeping it explicit makes the app easy to build and reason about.
 * ViewModels receive their dependencies from here through factories.
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val preferences: AppPreferences = AppPreferences(appContext.dataStore)

    val database: FlightDataBase = FlightDataBase.getDatabase(appContext)

    private val dao = database.flightDataDao()

    private val httpClient = FlightApi.defaultClient()
        .newBuilder()
        .cache(Cache(File(appContext.cacheDir, "flight-http"), HTTP_CACHE_BYTES))
        .build()

    val api: FlightApi = FlightApi(
        configProvider = { preferences.apiConfig.first() },
        client = httpClient,
        moshi = FlightApi.defaultMoshi,
    )

    val reminderScheduler: ReminderScheduler = ReminderScheduler(appContext, preferences)

    val repository: FlightRepository = FlightRepository(
        database = database,
        dao = dao,
        api = api,
        preferences = preferences,
        reminders = reminderScheduler,
    )

    private companion object {
        const val HTTP_CACHE_BYTES = 10L * 1024 * 1024
    }
}
