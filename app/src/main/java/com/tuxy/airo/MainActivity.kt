package com.tuxy.airo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.ALARM_SERVICE
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataBase
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.PreferencesInterface
import com.tuxy.airo.ui.theme.AeroTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    lateinit var data: FlightDataDao

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data = FlightDataBase.getDatabase(this).flightDataDao()

        enableEdgeToEdge()
        setContent {
            AeroTheme {
                // Attempts to set up notification permissions
                if (Build.VERSION.SDK_INT >= 33) {
                    if (ContextCompat.checkSelfPermission(
                            LocalContext.current,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101
                        )
                    } // If the user denies notifications, we ignore forever
                }

                MainScreen()
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun setAlarm(context: Context, flightData: FlightData) {
    val preferencesInterface = PreferencesInterface(context)
    var timeFormatWait = ""

    GlobalScope.launch {
        val timeFormat = preferencesInterface.getValueTimeFormat("24_time")
        timeFormatWait = timeFormat
    }

    val depTime =
        flightData.departDate
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(flightData.departTimeZone).toEpochSecond()

    if (depTime > System.currentTimeMillis() / 1000) {
        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, Alarm::class.java)

        val time = flightData.departDate.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(timeFormatWait))

        val flight = context.getString(R.string.flight_alert_title)
        val content =
            "${context.getString(R.string.get_ready)} ${flightData.callSign} ${context.getString(R.string.to)} ${flightData.toName} ${
                context.getString(R.string.at)
            } $time"

        intent.putExtra("flight", flight)
        intent.putExtra("content", content)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            flightData.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            (depTime - 21600) * 1000,
            pendingIntent
        )
    }
}

fun cancelAlarm(context: Context, flightData: FlightData) {
    val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, Alarm::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        flightData.id,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}
