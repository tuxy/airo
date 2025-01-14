package com.tuxy.airo.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.composables.SmallAppBar

@Composable
fun NewFlightView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { SmallAppBar("", navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            FlightSearch(navController)
        }
    }
}

@Composable
fun FlightSearch(navController: NavController) {

    val focusRequester = remember { FocusRequester() }
    var value by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        modifier = Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        value = value,
        label = { Text("Flight number") },
        onValueChange = { value = it },
        singleLine = true,
    )
}

@Composable
@Preview(showBackground = true)
fun NewFlightViewPreview() {
    NewFlightView(rememberNavController())
}
