package com.tuxy.airo

import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.data.flightdata.FlightDataBase
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

@Composable
fun MainScreen(
    backup: RoomBackup,
    powerManager: PowerManager
) {
    val context = LocalContext.current
    // Open/obtain the database once and keep it across recompositions instead of
    // creating the DAO (and touching the DB) on every recomposition.
    val data = remember {
        FlightDataBase.getDatabase(context.applicationContext).flightDataDao()
    }

    val navController = rememberNavController()
    SetupNavGraph(navController, data, backup, powerManager)
}
