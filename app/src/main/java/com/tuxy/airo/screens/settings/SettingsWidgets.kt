package com.tuxy.airo.screens.settings

import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.viewmodel.ApiSettingsViewModel

/**
 * A Composable function that displays an ExtendedFloatingActionButton (FAB)
 * for applying API settings.
 *
 * This FAB allows the user to save the selected API configuration.
 * It performs URL validation for custom API server and endpoint options.
 * If validation fails, a toast message is shown, and navigation is prevented.
 * On successful validation or for default options, the settings are saved
 * using the provided [viewModel], and the user is navigated back.
 *
 * @param selectedIndex The index of the currently selected API option.
 *                      Can be null if no specific option is selected yet.
 * @param viewModel The [ApiSettingsViewModel] used to access and save API settings.
 * @param toast A [Toast] instance used to display error messages.
 * @param navController The [NavController] used for navigating back after applying settings
 *                      or when validation fails.
 */
@Composable
fun ApplyFAB(
    selectedIndex: Int?,
    viewModel: ApiSettingsViewModel,
    toast: Toast,
    navController: NavController
) {
    ExtendedFloatingActionButton(
        modifier = Modifier.testTag("apply_settings"),
        onClick = {
            if (selectedIndex != null) {
                when (selectedIndex) { // Parse and check for URL
                    // No checks for 0 (default airoapi.tuxy.stream).
                    1 -> {
                        if (!URLUtil.isValidUrl(viewModel.currentApiServer)) {
                            navController.navigateUp()
                            toast.show()
                            return@ExtendedFloatingActionButton
                        }
                    }

                    2 -> { // Checks whether the URL is valid. Cannot check API key for now.
                        if (!URLUtil.isValidUrl(viewModel.currentEndpoint)) {
                            navController.navigateUp()
                            toast.show()
                            return@ExtendedFloatingActionButton
                        }
                    }
                }

                viewModel.saveKey("API_CHOICE", selectedIndex.toString())
            }

            viewModel.saveKey("API_KEY", viewModel.currentApiKey)
            viewModel.saveKey("ENDPOINT", viewModel.currentEndpoint)
            viewModel.saveKey("API_SERVER", viewModel.currentApiServer)

            navController.navigateUp()
        },
        icon = {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.apply_settings)
            )
        },
        text = { Text(stringResource(R.string.apply_settings)) },
    )
}


/**
 * A Composable function that displays a single setting item as a [ListItem].
 *
 * This item typically represents a sub-setting or a navigation point within the settings screen.
 * It displays a name, an icon, and a description. Clicking on the item will navigate
 * to the specified [location] using the provided [navController].
 *
 * @param name The name of the setting to be displayed as the headline.
 * @param icon The [ImageVector] to be displayed as the leading icon.
 * @param description A brief description of the setting, displayed as supporting content.
 *                    This is also used as the content description for the icon.
 * @param location The route or destination to navigate to when the item is clicked.
 * @param navController The [NavController] used for handling navigation.
 */
@Composable
fun SettingSub(
    name: String,
    icon: ImageVector,
    description: String,
    location: String,
    navController: NavController
) {
    ListItem(
        modifier = Modifier.clickable(onClick = {
            navController.navigate(location)
        }),
        headlineContent = { Text(name) },
        supportingContent = { Text(description) },
        leadingContent = { Icon(imageVector = icon, contentDescription = description) }
    )
}

/**
 * A Composable function that displays a setting with a name and a toggle switch.
 *
 * This component renders a row containing the setting's name as [Text]
 * and a [Switch] to control its on/off state. The state of the switch is
 * managed internally using `remember { mutableStateOf(true) }`.
 *
 * @param name The name of the setting to be displayed.
 *
 * Still currently a TODO.
 */
@Composable
fun Setting(name: String) {
    val value = remember { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
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