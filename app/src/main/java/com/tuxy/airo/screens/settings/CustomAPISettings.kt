package com.tuxy.airo.screens.settings

import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    Column {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(15.dp),
            value = viewModel.currentEndpoint,
            label = { Text(stringResource(R.string.api_endpoint)) },
            onValueChange = {
                if (URLUtil.isValidUrl(it)) {
                    viewModel.currentEndpoint = it
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.invalid_api_url),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            singleLine = true,
        )
        Spacer(Modifier.padding(4.dp))
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
