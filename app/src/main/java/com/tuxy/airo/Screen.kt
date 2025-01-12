package com.tuxy.airo

sealed class Screen(val route: String) {
    data object MainFlightsScreen: Screen("flights_screen")
    data object SettingsScreen: Screen("settings_screen")
    data object FlightDetailsScreen: Screen("flights_details_screen")
    data object NewFlightScreen: Screen("new_flight_screen")
    data object DatePickerScreen: Screen("date_picker_screen")
}
