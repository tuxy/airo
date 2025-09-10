package com.tuxy.airo.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.data.FlightData

// Used in Airport, Aircraft, Ticket and Flight information screens
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallAppBar(text: String, navController: NavController) {
    TopAppBar(
        title = { Text(text) },
        navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}

// Used in main flight screen and settings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeAppBar(text: String, navController: NavController) {
    LargeTopAppBar(
        title = { Text(text) },
        navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
        }
    )
}

// Used in Flight, Ticket and main screen
@Composable
fun RouteBar(flightData: FlightData) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoldDepartureAndDestinationText(
            icao = flightData.from,
            countryCode = flightData.fromCountryCode,
            fullName = flightData.fromName,
            time = "",
            alignment = Alignment.Start
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.to)
        )
        BoldDepartureAndDestinationText(
            icao = flightData.to,
            countryCode = flightData.toCountryCode,
            fullName = flightData.toName,
            time = "",
            alignment = Alignment.End
        )
    }
}

// Used in main flight and ticket screen
@Composable
fun LargeTopSmallBottom(top: String, bottom: String) {
    Column {
        Text(
            top,
            fontSize = 12.sp,
            lineHeight = 2.sp
        )
        Text(
            bottom,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DepartureAndDestinationText(icao: String, fullName: String) {
    Column {
        Text(
            fullName,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
        Text(icao)
    }
}

@Composable
fun BoldDepartureAndDestinationText(
    icao: String,
    countryCode: String,
    fullName: String,
    time: String,
    alignment: Alignment.Horizontal,
) {
    Column(
        modifier = Modifier.defaultMinSize(minWidth = 64.dp),
        horizontalAlignment = alignment
    ) {
        if (alignment == Alignment.Start) {
            Text(
                "${countryCodeToEmoji(countryCode)}  ${fullName.wrap()}",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1,
            )
        } else {
            Text(
                "${fullName.wrap()}  ${countryCodeToEmoji(countryCode)}",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        if (alignment == Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    icao,
                    fontWeight = FontWeight.W500,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    time,
                    fontWeight = FontWeight.W400,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    time,
                    fontWeight = FontWeight.W400,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    icao,
                    fontWeight = FontWeight.W500,
                    fontSize = 24.sp,
                )
            }
        }
    }
}

fun String.wrap(): String {
    if (this.length > 15) {
        return "${this.substring(0, 14)}..."
    } else {
        return this
    }
}

@Composable
@Preview(showBackground = true)
fun TextPreview() {
    BoldDepartureAndDestinationText(
        icao = "FCO",
        countryCode = "IT",
        fullName = "Leonardo Da Vinci-Fiumicino",
        time = "5:00",
        alignment = Alignment.Start
    )
}

fun countryCodeToEmoji(countryCode: String): String {
    if (countryCode == "") {
        return countryCode
    }
    val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}
