package com.tuxy.airo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.data.FlightDataBase
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.ui.theme.AeroTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    lateinit var data: FlightDataDao

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        data = FlightDataBase.getDatabase(this).flightDataDao()

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
