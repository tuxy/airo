package com.tuxy.airo.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.tuxy.airo.composables.LargeAppBar

@Composable
fun BackupSettingsView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar("Backup Settings", navController) },
    ) { innerPadding ->
        Text(
            modifier = Modifier.padding(innerPadding),
            text = "WIP"
        )
    }
}