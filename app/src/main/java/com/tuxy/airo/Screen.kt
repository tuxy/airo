package com.tuxy.airo

sealed class Screen(val route: String) {
    data object MainFlightsScreen : Screen("flights_screen")
    data object SettingsScreen : Screen("settings_screen")
    data object FlightDetailsScreen : Screen("flights_details_screen")
    data object NewFlightScreen : Screen("new_flight_screen")
    data object DatePickerScreen : Screen("date_picker_screen")
    data object AircraftInformationScreen : Screen("aircraft_information_screen")
    data object TicketInformationScreen : Screen("ticket_information_screen")

    // Settings menus
    data object ApiSettingsScreen : Screen("api_settings_screen")
    data object NotificationsSettingsScreen : Screen("notifications_settings_screen")
    data object LocaleSettingsScreen : Screen("locale_settings_screen")
    data object BackupSettingsScreen : Screen("backup_settings_screen")
    data object AboutSettingsScreen : Screen("about_settings_screen")
}
