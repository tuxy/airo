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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.R
import com.tuxy.airo.composables.LargeAppBar

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackupSettingsView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar(stringResource(R.string.backup_and_restore), navController) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            ListItem(
                modifier = Modifier.clickable(
                    onClick = {}
                ),
                headlineContent = { Text(stringResource(R.string.backup_now)) },
            )
            ListItem(
                modifier = Modifier.clickable(
                    onClick = {}
                ),
                headlineContent = { Text(stringResource(R.string.restore_now)) },
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun BackupSettingsViewPreview() {
    BackupSettingsView(rememberNavController())
}