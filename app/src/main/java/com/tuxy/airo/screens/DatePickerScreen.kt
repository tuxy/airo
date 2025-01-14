package com.tuxy.airo.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.tuxy.airo.composables.SmallAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerView(navController: NavController) {
    Scaffold(
        topBar = { SmallAppBar("", navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navController.navigateUp()
                    navController.navigateUp()
                },
                icon = { Icon(Icons.Filled.Check, "Add flight") },
                text = { Text(text = "Add Flight") },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            DatePicker(
                state = DatePickerState(locale = CalendarLocale.UK)
            )
        }
    }
}
