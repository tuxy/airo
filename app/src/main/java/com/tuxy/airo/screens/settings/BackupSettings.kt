package com.tuxy.airo.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.composables.LargeAppBar

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackupSettingsView(navController: NavController) {
    println("")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar("Backup Settings", navController) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            ListItem(
                modifier = Modifier.clickable(
                    onClick = {}
                ),
                headlineContent = { Text("Backup now") },
                supportingContent = { Text("Save current & past flights to file") }
            )
            ListItem(
                modifier = Modifier.clickable(
                    onClick = {}
                ),
                headlineContent = { Text("Restore from file") },
                supportingContent = { Text("Restore flight database from file") }
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun BackupSettingsViewPreview() {
    BackupSettingsView(rememberNavController())
}