package com.tuxy.airo.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ireward.htmlcompose.HtmlText
import com.tuxy.airo.R
import com.tuxy.airo.viewmodel.SettingsViewModel

@Composable
fun CustomApiView(
    navController: NavController,
    viewModel: SettingsViewModel,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        HtmlText(
            stringResource(R.string.adb_text),
            fontSize = 10.sp,
            style = TextStyle(color = Color.Gray)
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            value = viewModel.currentEndpoint,
            label = { Text(stringResource(R.string.api_endpoint)) },
            onValueChange = { viewModel.currentEndpoint = it },
            singleLine = true,
        )
        Spacer(Modifier.padding(4.dp))
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            value = viewModel.currentApiKey,
            label = { Text(stringResource(R.string.api_key)) },
            onValueChange = { viewModel.currentApiKey = it },
            singleLine = true,
        )
    }
}
