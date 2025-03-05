package com.tuxy.airo

import android.annotation.SuppressLint
import android.app.Activity.ALARM_SERVICE
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataBase
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.ui.theme.AeroTheme
import java.time.ZoneOffset

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    lateinit var data: FlightDataDao


    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data = FlightDataBase.getDatabase(this).flightDataDao()

        enableEdgeToEdge()
        setContent {
            AeroTheme {
                Scaffold {
                    navController = rememberNavController()
                    SetupNavGraph(navController, data)
                }
            }
        }
    }
}

fun setAlarm(context: Context, flightData: FlightData) {
    val timeSec =
        (flightData.departDate.atOffset(ZoneOffset.UTC).toEpochSecond()) * 1000
//    val timeSec = System.currentTimeMillis() + 5000
    val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, Alarm::class.java)
    intent.putExtra("title", flightData.callSign)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        flightData.hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        timeSec,
        pendingIntent
    )
}

fun cancelAlarm(context: Context, flightData: FlightData) {
    val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, Alarm::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        flightData.hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}
