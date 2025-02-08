package com.tuxy.airo.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.viewmodel.SettingsViewModel

@Composable
fun CustomApiView(
    navController: NavController,
    viewModel: SettingsViewModel,
) {
    Column {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(15.dp),
            value = viewModel.currentEndpoint,
            label = { Text(stringResource(R.string.api_endpoint)) },
            onValueChange = { viewModel.currentEndpoint = it },
            singleLine = true,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(15.dp),
            value = viewModel.currentApiKey,
            label = { Text(stringResource(R.string.api_key)) },
            onValueChange = { viewModel.currentApiKey = it },
            singleLine = true,
        )
    }
}
