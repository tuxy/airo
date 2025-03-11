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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataBase
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.ui.theme.AeroTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        Log.d("Permissions", "NOTIFICATION PERMISSION GRANTED")

                    } else {
                        Log.d("Permissions", "NOTIFICATION PERMISSION DENIED")
                    }
                }

                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(
                        LocalContext.current,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) -> {
                        Log.d("Permissions", "NOTIFICATION PERMISSION REQUIRED")
                    }

                    else -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Scaffold {
                    navController = rememberNavController()
                    SetupNavGraph(navController, data)
                }
            }
        }
    }
}

fun setAlarm(context: Context, flightData: FlightData) {

    val depTime =
        flightData.departDate.atZone(ZoneId.systemDefault()).toEpochSecond()

    if (depTime > System.currentTimeMillis() / 1000) {
        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, Alarm::class.java)

        val time = flightData.departDate.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))

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
