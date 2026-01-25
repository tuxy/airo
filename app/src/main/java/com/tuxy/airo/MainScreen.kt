package com.tuxy.airo

import android.annotation.SuppressLint
import android.os.PowerManager
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.data.flightdata.FlightDataBase
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    backup: RoomBackup,
    powerManager: PowerManager
) {
    val data = FlightDataBase.getDatabase(LocalContext.current).flightDataDao()

    Scaffold {
        val navController = rememberNavController()
        SetupNavGraph(navController, data, backup, powerManager)
    }
}