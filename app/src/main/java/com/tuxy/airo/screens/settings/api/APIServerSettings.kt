package com.tuxy.airo.screens.settings.api

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import com.jamal.composeprefs3.ui.PrefsScreen
import com.tuxy.airo.newDataStore

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ApiServerView() {
    Column {
        PrefsScreen(LocalContext.current.newDataStore) {
            prefsItem {

            }
        }
    }
}
