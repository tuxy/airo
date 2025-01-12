package com.tuxy.airo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.ui.theme.AeroTheme

@Composable
fun MainFlightView(navController: NavController) {
    AeroTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { MainTopBar(navController = navController) },
            floatingActionButton = { AddFlightButton(navController) }
        ) { innerPadding ->
            Column (
                modifier = Modifier
                    .padding(innerPadding),
            ) {
                FlightTabs()
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    repeat(10) {
                        FlightCard(navController)
                    }
                }
            }
        }
    }
}

@Composable
fun FlightCard(navController: NavController) { // TODO Add parameters to create multiple seperate instances of flights
    ElevatedCard(
        onClick = {
            navController.navigate(route = Screen.FlightDetailsScreen.route)
        },
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp)
    ) {
        Column {
            FlightDepartureAndArrival()
            TicketInformationCard()
            CheckInButtonGroup()
            LinearProgressIndicator(
                progress = { 0F },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun CheckInButtonGroup() {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        OutlinedButton(
            modifier = Modifier.padding(horizontal = 12.dp),
            onClick = {}
        ) {
            Text("Show Ticket")
        }
        FilledTonalButton(onClick = {}) {
            Text("Check-in")
        }
    }
}

@Composable
fun TicketInformationCard() { // TODO How to get ticket information from ticket?
    Row (
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Terminal",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Text("2")
        }
        Column {
            Text(
                "Gate",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Text("5")
        }
        Column {
            Text(
                "Seat",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Text("32A")
        }
        Badge(
            modifier = Modifier.size(16.dp),
            containerColor = Color.Gray
        )
    }
}

@Composable
fun FlightDepartureAndArrival() { // TODO Implement API
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "San Francisco",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Text("KSFO")
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "To"
        )
        Column( horizontalAlignment = Alignment.End ) {
            Text(
                "Atlanta",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Text("KATL")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(modifier: Modifier = Modifier, navController: NavController) {
    LargeTopAppBar(
        title = { Text("DEBUG MODE") },
        colors = TopAppBarDefaults.topAppBarColors(),
        actions = {
            IconButton(onClick = {
                navController.navigate(route = Screen.SettingsScreen.route)
            }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightTabs() {
    PrimaryTabRow(
        selectedTabIndex = 0,
    ) {
        Tab(
            selected = true,
            onClick = {},
            text = { Text("Upcoming") }
        )
        Tab(
            selected = false,
            onClick = {},
            text = { Text("Past") }
        )
    }
}

@Composable
fun AddFlightButton(navController: NavController) {
    ExtendedFloatingActionButton(
        onClick = {
            navController.navigate(Screen.NewFlightScreen.route)
        },
        icon = { Icon(Icons.Filled.Add, "Add flight") },
        text = { Text(text = "Add Flight") },
    )
}


@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    Column {
        MainTopBar(navController = rememberNavController())
        FlightTabs()
    }
}

@Preview(showBackground = true)
@Composable
fun FlightCardPreview() {
    AeroTheme {
        FlightCard(navController = rememberNavController())
    }
}
