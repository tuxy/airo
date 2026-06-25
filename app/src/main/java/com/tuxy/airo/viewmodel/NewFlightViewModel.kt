package com.tuxy.airo.viewmodel

import androidx.lifecycle.ViewModel
import com.tuxy.airo.data.AirlinesData

sealed class SearchSuggestion {
    data class Airline(val name: String, val code: String) : SearchSuggestion()
    data class Flight(val flightNumber: String, val display: String) : SearchSuggestion()
}

class NewFlightViewModel : ViewModel() {
    fun getSuggestions(query: String, selectedAirline: String?): List<SearchSuggestion> {
        if (query.isBlank()) return emptyList()

        return if (selectedAirline != null) {
            if (query.all { it.isDigit() } && query.isNotEmpty()) {
                listOf(SearchSuggestion.Flight("${selectedAirline}$query", "${selectedAirline}${query}"))
            } else {
                emptyList()
            }
        } else {
            val lowerQuery = query.lowercase()
            AirlinesData.airlines.filter { (code, name) ->
                name.lowercase().contains(lowerQuery) || code.lowercase().contains(lowerQuery)
            }.map { SearchSuggestion.Airline(it.value, it.key) }.take(10)
        }
    }
}