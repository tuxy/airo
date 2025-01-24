package com.tuxy.airo

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.motion.materialSharedAxisXIn
import com.tuxy.airo.motion.materialSharedAxisXOut
import com.tuxy.airo.screens.AircraftInformationView
import com.tuxy.airo.screens.DatePickerView
import com.tuxy.airo.screens.FlightDetailsView
import com.tuxy.airo.screens.MainFlightView
import com.tuxy.airo.screens.NewFlightView
import com.tuxy.airo.screens.SettingsView
import com.tuxy.airo.screens.TicketInformationView

private const val INITIAL_OFFSET_FACTOR = 0.10f

@Composable
fun SetupNavGraph( // Transitions taken from Read You's repository
    navController: NavHostController,
    flightDataDao: FlightDataDao
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
            MainFlightView(
                navController,
                flightDataDao
            )
        }
        composable(route = Screen.SettingsScreen.route) { SettingsView(navController) }

        // Passing flight id into FlightDetails
        composable(route = "${Screen.FlightDetailsScreen.route}/{id}") { backStackEntry ->
            FlightDetailsView(
                navController,
                backStackEntry.arguments?.getString("id").toString(),
                flightDataDao
            )
        }

        composable(route = Screen.NewFlightScreen.route) { NewFlightView(navController) }

        // DatePickerView with flight_number passed into
        composable(route = "${Screen.DatePickerScreen.route}/{flight_number}") { backStackEntry ->
            DatePickerView(
                navController,
                backStackEntry.arguments?.getString("flight_number").toString(),
                flightDataDao
            )
        }

        // Aircraft Information
        composable(route = "${Screen.AircraftInformationScreen.route}/{id}") { backStackEntry ->
            AircraftInformationView(
                navController,
                backStackEntry.arguments?.getString("id").toString(),
                flightDataDao
            )
        }

        // Ticket information
        composable(route = "${Screen.TicketInformationScreen.route}/{id}") { backStackEntry ->
            TicketInformationView(
                navController,
                backStackEntry.arguments?.getString("id").toString(),
                flightDataDao,
            )
        }
    }
}
