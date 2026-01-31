package com.tuxy.airo

import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.motion.materialSharedAxisXIn
import com.tuxy.airo.motion.materialSharedAxisXOut
import com.tuxy.airo.screens.DatePickerView
import com.tuxy.airo.screens.FoldableFlightScreen
import com.tuxy.airo.screens.NewFlightView
import com.tuxy.airo.screens.SettingsView
import com.tuxy.airo.screens.settings.ApiSettingsView
import com.tuxy.airo.screens.settings.BackupSettingsView
import com.tuxy.airo.screens.settings.LocaleSettingsView
import com.tuxy.airo.screens.settings.NotificationsSettingsView
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

private const val INITIAL_OFFSET_FACTOR = 0.10f

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
                navController,
                flightDataDao,
            )
        }
        composable(route = Screen.SettingsScreen.route) { SettingsView(navController, powerManager) }

        // Passing flight id into FlightDetails
        composable(route = "${Screen.FlightDetailsScreen.route}/{id}") {
            FoldableFlightScreen(
                navController,
                flightDataDao,
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

//        // Aircraft Information
//        composable(route = "${Screen.AircraftInformationScreen.route}/{id}") { backStackEntry ->
//            AircraftInformationView(
//                navController,
//                backStackEntry.arguments?.getString("id").toString(),
//                flightDataDao
//            )
//        }
//
//        // Ticket information
//        composable(route = "${Screen.TicketInformationScreen.route}/{id}") { backStackEntry ->
//            TicketInformationView(
//                navController,
//                backStackEntry.arguments?.getString("id").toString(),
//                flightDataDao,
//            )
//        }

        // Settings Sub-Screens
        composable(route = Screen.ApiSettingsScreen.route) {
            ApiSettingsView(navController = navController)
        }
        composable(route = Screen.NotificationsSettingsScreen.route) {
            NotificationsSettingsView(navController, flightDataDao)
        }
        composable(route = Screen.LocaleSettingsScreen.route) {
            LocaleSettingsView(navController = navController)
        }
        composable(route = Screen.BackupSettingsScreen.route) {
            BackupSettingsView(
                navController,
                backup
            )
        }
    }
}
