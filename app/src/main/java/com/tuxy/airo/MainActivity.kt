package com.tuxy.airo

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.data.FlightDataBase
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.ui.theme.AeroTheme

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    lateinit var data: FlightDataDao

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        data = FlightDataBase.getDatabase(this).flightDataDao()
//        GlobalScope.launch {
//            data.nukeTable() // Deletes all table values when opening, ONLY FOR DEVELOPMENT
//        }

        enableEdgeToEdge()
        setContent {
            AeroTheme {
                Scaffold {
                    navController = rememberNavController()
                    SetupNavGraph(navController, data)
                }
            }
        }
    }
}
