package com.tuxy.airo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.SmallAppBarLegacy
import com.tuxy.airo.data.AirlinesData

private sealed class SearchSuggestion {
    data class Airline(val name: String, val code: String) : SearchSuggestion()
    data class Flight(val flightNumber: String, val display: String) : SearchSuggestion()
}

private fun getSuggestions(query: String, selectedAirline: String?, airlines: Map<String, String>): List<SearchSuggestion> {
    if (query.isBlank()) return emptyList()

    return if (selectedAirline != null) {
        if (query.all { it.isDigit() } && query.isNotEmpty()) {
            listOf(SearchSuggestion.Flight("${selectedAirline}$query", "${selectedAirline}${query}"))
        } else {
            emptyList()
        }
    } else {
        val lowerQuery = query.lowercase()
        airlines.filter { (code, name) ->
            name.lowercase().contains(lowerQuery) || code.lowercase().contains(lowerQuery)
        }.map { SearchSuggestion.Airline(it.value, it.key) }.take(10)
    }
}

@Composable
fun NewFlightView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            SmallAppBarLegacy("", navController)
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                FlightSearch(navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightSearch(navController: NavController) {
    val focusRequester = remember { FocusRequester() }
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedAirline by rememberSaveable { mutableStateOf<String?>(null) }
    val suggestions = remember(query, selectedAirline) { getSuggestions(query, selectedAirline, AirlinesData.airlines) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {
                        expanded = false
                        val flightNumber = if (selectedAirline != null) "${selectedAirline}${query}" else query
                        navController.navigate("${Screen.DatePickerScreen.route}/${flightNumber}")
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text(stringResource(R.string.flight_number)) },
                    leadingIcon = if (selectedAirline != null) {
                        {
                            InputChip(
                                selected = true,
                                onClick = { selectedAirline = null },
                                label = { Text(selectedAirline!!) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.clickable { selectedAirline = null }
                                    )
                                }
                            )
                        }
                    } else null,
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                suggestions.forEach { suggestion ->
                    when (suggestion) {
                        is SearchSuggestion.Airline -> {
                            ListItem(
                                headlineContent = { Text("${suggestion.name} (${suggestion.code})") },
                                supportingContent = { Text(suggestion.code) },
                                modifier = Modifier
                                    .focusable(false)
                                    .clickable {
                                        selectedAirline = suggestion.code
                                        query = ""
                                    }
                                    .fillMaxWidth()
                            )
                        }
                        is SearchSuggestion.Flight -> {
                            ListItem(
                                headlineContent = { Text(suggestion.display) },
                                supportingContent = { Text("Flight number") },
                                modifier = Modifier
                                    .focusable(false)
                                    .clickable {
                                        navController.navigate("${Screen.DatePickerScreen.route}/${suggestion.flightNumber}")
                                    }
                                    .fillMaxWidth()
                            )
                        }
                    }
                    HorizontalDivider()
                }
                if (suggestions.isEmpty() && query.isNotBlank() && selectedAirline == null) {
                    ListItem(
                        headlineContent = { Text("No suggestions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}
