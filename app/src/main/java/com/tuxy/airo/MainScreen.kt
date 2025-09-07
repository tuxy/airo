package com.tuxy.airo

import android.annotation.SuppressLint
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.data.FlightDataBase
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    backup: RoomBackup
) {
    val data = FlightDataBase.getDatabase(LocalContext.current).flightDataDao()

    Scaffold {
        val navController = rememberNavController()
        SetupNavGraph(navController, data, backup)
    }
}