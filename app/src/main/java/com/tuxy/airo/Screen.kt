package com.tuxy.airo

sealed class Screen(val route: String) {
    object MyFlightsScreen: Screen("flights_screen")
    object SettingsScreen: Screen("settings_screen")
    object FlightDetailsScreen: Screen("flights_details_screen")
}
