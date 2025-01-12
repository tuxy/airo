package com.tuxy.airo

sealed class Screen(val route: String) {
    object MainFlightsScreen: Screen("flights_screen")
    object SettingsScreen: Screen("settings_screen")
    object FlightDetailsScreen: Screen("flights_details_screen")
    object NewFlightScreen: Screen("new_flight_screen")
    object DatePickerScreen: Screen("date_picker_screen")
}
