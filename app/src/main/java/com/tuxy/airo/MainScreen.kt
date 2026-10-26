package com.tuxy.airo

import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.di.AppContainer
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

@Composable
fun MainScreen(
    backup: RoomBackup,
    powerManager: PowerManager,
    container: AppContainer,
) {
    val navController = rememberNavController()
    SetupNavGraph(
        navController = navController,
        container = container,
        backup = backup,
        powerManager = powerManager,
    )
}
