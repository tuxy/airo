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
fun ApiServerView(
    navController: NavController,
    viewModel: SettingsViewModel
) {
    Column {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(15.dp),
            value = viewModel.currentApiServer,
            label = { Text(stringResource(R.string.airo_api_server)) },
            onValueChange = { viewModel.currentApiServer = it },
            singleLine = true,
        )
    }
}
