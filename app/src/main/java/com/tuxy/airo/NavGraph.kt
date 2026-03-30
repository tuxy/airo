package com.tuxy.airo

import android.os.PowerManager
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tuxy.airo.data.flightdata_rework.FlightDataDao
import com.tuxy.airo.motion.materialSharedAxisXIn
import com.tuxy.airo.motion.materialSharedAxisXOut
import com.tuxy.airo.screens.DatePickerView
import com.tuxy.airo.screens.FoldableFlightScreen
import com.tuxy.airo.screens.NewFlightView
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

private const val INITIAL_OFFSET_FACTOR = 0.10f

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SetupNavGraph(
    // Transitions taken from Read You's repository
    navController: NavHostController,
    flightDataDao: FlightDataDao,
    backup: RoomBackup,
    powerManager: PowerManager
) {
    NavHost(
        navController = navController,
        startDestination = Screen.MainFlightsScreen.route,
        enterTransition = { materialSharedAxisXIn(initialOffsetX = { (it * INITIAL_OFFSET_FACTOR).toInt() }) },
        exitTransition = { materialSharedAxisXOut(targetOffsetX = { -(it * INITIAL_OFFSET_FACTOR).toInt() }) },
        popEnterTransition = { materialSharedAxisXIn(initialOffsetX = { -(it * INITIAL_OFFSET_FACTOR).toInt() }) },
        popExitTransition = { materialSharedAxisXOut(targetOffsetX = { (it * INITIAL_OFFSET_FACTOR).toInt() }) },
    ) {
        composable(route = Screen.MainFlightsScreen.route) {
            FoldableFlightScreen(
                navController = navController,
                flightDataDao = flightDataDao,
                backup = backup,
                powerManager = powerManager,
            )
        }
        composable(route = Screen.SettingsScreen.route) {
            FoldableFlightScreen(
                navController = navController,
                flightDataDao = flightDataDao,
                backup = backup,
                powerManager = powerManager,
            )
        }

        // Passing flight id into FlightDetails
        composable(route = "${Screen.FlightDetailsScreen.route}/{id}") {
            FoldableFlightScreen(
                navController = navController,
                flightDataDao = flightDataDao,
                backup = backup,
                powerManager = powerManager,
            )
        }

        composable(route = Screen.NewFlightScreen.route) { NewFlightView(navController) }

        // DatePickerView with flight_number passed into
        composable(route = "${Screen.DatePickerScreen.route}/{flight_number}") { backStackEntry ->
            DatePickerView(
                navController,
                backStackEntry.arguments?.getString("flight_number").toString(),
                flightDataDao,
            )
        }
    }
}
