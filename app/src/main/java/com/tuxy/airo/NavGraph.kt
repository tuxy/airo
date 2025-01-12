package com.tuxy.airo

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tuxy.airo.motion.materialSharedAxisXIn
import com.tuxy.airo.motion.materialSharedAxisXOut
import com.tuxy.airo.motion.materialSharedAxisYIn
import com.tuxy.airo.motion.materialSharedAxisYOut
import com.tuxy.airo.screens.DatePickerView
import com.tuxy.airo.screens.FlightDetailsView
import com.tuxy.airo.screens.MainFlightView
import com.tuxy.airo.screens.NewFlightView
import com.tuxy.airo.screens.SettingsView

private const val INITIAL_OFFSET_FACTOR = 0.10f

@Composable
fun SetupNavGraph( // Transitions taken from Read You's repository
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.MainFlightsScreen.route,
        enterTransition = { materialSharedAxisXIn(initialOffsetX = { (it * INITIAL_OFFSET_FACTOR).toInt() }) },
        exitTransition = { materialSharedAxisXOut(targetOffsetX = { -(it * INITIAL_OFFSET_FACTOR).toInt() }) },
        popEnterTransition = { materialSharedAxisXIn(initialOffsetX = { -(it * INITIAL_OFFSET_FACTOR).toInt() }) },
        popExitTransition = { materialSharedAxisXOut(targetOffsetX = { (it * INITIAL_OFFSET_FACTOR).toInt() }) },
    ) {
        composable( route = Screen.MainFlightsScreen.route ) { MainFlightView(navController) }
        composable( route = Screen.SettingsScreen.route ) { SettingsView(navController) }
        composable( route = Screen.FlightDetailsScreen.route ) { FlightDetailsView(navController) }

        composable(
            route = Screen.NewFlightScreen.route,
            enterTransition = { materialSharedAxisYIn(initialOffsetY = { it / 4 }) },
            exitTransition = { materialSharedAxisYOut(targetOffsetY = { it / 4 }) },
        ) { NewFlightView(navController) }
        composable(
            route = Screen.DatePickerScreen.route,
            enterTransition = { materialSharedAxisYIn(initialOffsetY = { it / 4 }) },
            exitTransition = { materialSharedAxisYOut(targetOffsetY = { it / 4 }) },
        ) { DatePickerView() }
    }
}
