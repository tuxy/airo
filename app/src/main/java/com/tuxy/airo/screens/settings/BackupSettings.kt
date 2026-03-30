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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import com.tuxy.airo.R
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.viewmodel.settings.BackupViewModel
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun BackupSettingsView(
    paneNavigator: ThreePaneScaffoldNavigator<String>? = null,
    backup: RoomBackup
) {
    val context = LocalContext.current
    val viewModelFactory = BackupViewModel.Factory(context, backup)
    val viewModel: BackupViewModel = viewModel(factory = viewModelFactory)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar(stringResource(R.string.backup_and_restore), paneNavigator) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            ListItem(
                modifier = Modifier.clickable(
                    onClick = { viewModel.roomBackup(context) }
                ),
                headlineContent = { Text(stringResource(R.string.backup_now)) },
            )
            ListItem(
                modifier = Modifier.clickable(
                    onClick = { viewModel.roomRestore(context) }
                ),
                headlineContent = { Text(stringResource(R.string.restore_now)) },
            )
            Text(
                stringResource(R.string.restore_warning),
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}