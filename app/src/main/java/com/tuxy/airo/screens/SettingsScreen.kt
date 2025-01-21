package com.tuxy.airo.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.data.UserPreferences
import kotlinx.coroutines.launch

@Composable
fun SettingsView( // TODO Implement notification permissions
    navController: NavController
) {
    val dataStore = UserPreferences(LocalContext.current)
    val scope = rememberCoroutineScope()

    val retrievedKey = dataStore.getApiKey.collectAsState(initial = "")

    val key = remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar("Settings", navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        dataStore.saveApiKey(key.value)
                    }
                    Log.d("ApiAccess", retrievedKey.value)
                    navController.navigateUp()
                },
                icon = { Icon(Icons.Filled.Check, "Apply settings") },
                text = { Text(text = "Apply settings") },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(15.dp),
                value = key.value,
                label = { Text("API Key") },
                onValueChange = { key.value = it },
                singleLine = true,
            )
        }
    }
}

@Composable
fun Setting(name: String) {
    val value = remember { mutableStateOf(true) }

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontSize = 16.sp)
        Switch(
            onCheckedChange = { value.value = it },
            checked = value.value
        )
    }
}

@Composable
@Preview(showBackground = true)
fun SettingsPreview() {
    SettingsView(rememberNavController())
}
