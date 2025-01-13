package com.tuxy.airo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.tuxy.airo.composables.SmallAppBar

@Composable
fun AircraftInformationView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column( modifier = Modifier.padding(innerPadding) ) {
            SmallAppBar("Aircraft Information", navController)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
            ) {
                AsyncImage(
                    model = "",
                    contentDescription = "Aircraft",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1280f/847f)
                )
            }
            AircraftListView()
        }
    }
}

@Composable
fun AircraftListView() {
    Column {
        ListItem(
            headlineContent = { Text("Boeing 777-300ER") },
            supportingContent = { Text("United Airlines") }
        )
        ListItem(
            headlineContent = { Text("Seating information") },
            trailingContent = { Icon(Icons.Filled.Info, "Seating information") }
        )
        ListItem(
            headlineContent = { Text("Aircraft information") },
            trailingContent = { Icon(Icons.Filled.Info, "Aircraft information") }
        )
    }
}

@Composable
@Preview(showBackground = true)
fun AircraftInformationPreview() {
    AircraftInformationView(rememberNavController())
}
