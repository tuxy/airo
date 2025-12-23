package com.tuxy.airo

import android.annotation.SuppressLint
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.data.background.ProgressNotification
import com.tuxy.airo.data.flightdata.FlightDataBase
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    backup: RoomBackup
) {
    val data = FlightDataBase.getDatabase(LocalContext.current).flightDataDao()

    ProgressNotification.show(LocalContext.current, "Flight Number", "Landing in ...", 100, 75)

    Scaffold {
        val navController = rememberNavController()
        SetupNavGraph(navController, data, backup)
    }
}