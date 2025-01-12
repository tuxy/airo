package com.tuxy.airo

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

const val SCREEN_TRANSITION_MILLIS = 350

@Composable
fun SetupNavGraph( // TODO Currently, while the animations "Match" android's choice of animation for material 3, it feels very unnatural
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.MainFlightsScreen.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(SCREEN_TRANSITION_MILLIS))
        },
        exitTransition = {
            fadeOut(tween(SCREEN_TRANSITION_MILLIS))
        },
        popEnterTransition = {
            fadeIn(tween(SCREEN_TRANSITION_MILLIS))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(SCREEN_TRANSITION_MILLIS))
        }
    ) {
        composable( route = Screen.MainFlightsScreen.route ) { MainFlightView(navController) }
        composable( route = Screen.SettingsScreen.route ) { SettingsView(navController) }
        composable( route = Screen.FlightDetailsScreen.route ) { FlightDetailsView(navController) }

        composable( route = Screen.NewFlightScreen.route ) { NewFlightView(navController) }
        composable( route = Screen.DatePickerScreen.route ) { DatePickerView() }
    }
}
