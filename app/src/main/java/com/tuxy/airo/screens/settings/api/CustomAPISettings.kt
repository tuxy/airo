package com.tuxy.airo.screens.settings.api

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jamal.composeprefs3.ui.PrefsScreen
import com.tuxy.airo.R
import com.tuxy.airo.dataStore
import com.tuxy.airo.screens.settings.prefs.TextFieldPref

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomApiView() {


    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        PrefsScreen(LocalContext.current.dataStore) {
            prefsItem {
                TextFieldPref(
                    title = stringResource(R.string.api_endpoint),
                    key = "endpoint_adb"
                )
                TextFieldPref(
                    title = stringResource(R.string.api_endpoint),
                    key = "endpoint_adb_key"
                )
            }
        }
    }
}