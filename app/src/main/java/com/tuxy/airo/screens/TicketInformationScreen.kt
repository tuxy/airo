package com.tuxy.airo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.tuxy.airo.composables.LargeTopSmallBottom
import com.tuxy.airo.composables.SmallAppBar

@Composable
fun TicketInformationView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column( modifier = Modifier.padding(innerPadding) ) {
            SmallAppBar("UAL45", navController)
            MainTicketView()
        }
    }
}

@Composable
fun MainTicketView() {
    Column(
        modifier = Modifier,
    ) {
//        RouteBar() // TODO add flight
        Box(
            modifier = Modifier
                .padding(horizontal = 96.dp)
                .padding(vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
            ) {
                AsyncImage(
                    model = "",
                    contentDescription = "Ticket QR Code",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f/1f)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LargeTopSmallBottom("Terminal", "2")
            LargeTopSmallBottom("Gate", "5")
            LargeTopSmallBottom("Seat", "32A")
        }
    }
}

@Composable
@Preview(showBackground = true)
fun TicketInformationPreview() {
    TicketInformationView(rememberNavController())
}
