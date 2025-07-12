package com.tuxy.airo

import android.annotation.SuppressLint
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.data.FlightDataBase

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    val data = FlightDataBase.getDatabase(LocalContext.current).flightDataDao()

    Scaffold {
        val navController = rememberNavController()
        SetupNavGraph(navController, data)
    }
}