package com.tuxy.airo.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuxy.airo.R
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.repository.FlightRepository
import com.tuxy.airo.data.repository.RefreshFlightResult
import com.tuxy.airo.data.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.time.toKotlinDuration

class DetailsViewModel(
    private val repository: FlightRepository,
    preferences: AppPreferences,
    private val context: Context,
) : ViewModel() {

    var flightData = mutableStateOf(FlightData())
    var openDialog = mutableStateOf(false)

    private val _progress = MutableStateFlow(0.0F)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshMessage = MutableStateFlow<String?>(null)
    val refreshMessage: StateFlow<String?> = _refreshMessage.asStateFlow()

    val timeFormat: StateFlow<String> = preferences.timeFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "hh:mm a")

    fun loadFlightById(id: String) {
        val numericId = id.toIntOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getFlight(numericId) ?: FlightData()
            withContext(Dispatchers.Main) {
                flightData.value = data
                _progress.value = calculateProgressFraction()
            }
        }
    }

    /** Called by the screen on a lifecycle-aware tick. */
    fun tickProgress() {
        _progress.value = calculateProgressFraction()
    }

    /**
     * Progress using the effective (revised when available) times and duration.
     * A delayed flight therefore does not jump to "Landed" while airborne.
     */
    fun calculateProgressFraction(): Float {
        val now = ZonedDateTime.now()
        val depart = flightData.value.effectiveDepartDate
        val duration = flightData.value.effectiveDuration.toMillis()

        if (now.isBefore(depart) || duration <= 0) return 0.0F

        val elapsed = Duration.between(depart, now).toMillis()
        return (elapsed.toFloat() / duration.toFloat()).coerceIn(0.0F, 1.0F)
    }

    fun getZoneDifference(): String {
        val departOffset = flightData.value.scheduledDepartDate.offset
        val arriveOffset = flightData.value.scheduledArriveDate.offset
        val differenceInSeconds = arriveOffset.totalSeconds - departOffset.totalSeconds

        if (differenceInSeconds == 0) return ""

        val ahead = if (differenceInSeconds > 0) "+" else "-"
        val difference = Duration.ofSeconds(differenceInSeconds.toLong()).toKotlinDuration()
        difference.toComponents { hours, minutes, _, _ ->
            return "$ahead${hours.time()}:${minutes.toLong().time()}"
        }
    }

    private fun Long.time(): String = absoluteValue.toString().padStart(2, '0')

    fun getProgress(): Float = _progress.value

    suspend fun deleteFlight() {
        repository.deleteFlight(flightData.value)
        openDialog.value = false
    }

    fun refreshData() {
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            when (val result = repository.refreshFlight(flightData.value.id)) {
                is RefreshFlightResult.Updated -> {
                    flightData.value = result.flight
                    _progress.value = calculateProgressFraction()
                    result.change?.let { _refreshMessage.value = it.message }
                }

                RefreshFlightResult.NoData -> {
                    _refreshMessage.value = context.getString(R.string.refresh_no_data)
                }

                is RefreshFlightResult.Failed -> {
                    _refreshMessage.value = result.message
                }
            }
            _isRefreshing.value = false
        }
    }

    fun clearRefreshMessage() {
        _refreshMessage.value = null
    }

    fun getDuration(now: ZonedDateTime = ZonedDateTime.now()): String {
        val depart = flightData.value.effectiveDepartDate
        if (!now.isBefore(depart)) return ""

        val duration = Duration.between(now, depart)
        // During the check-in window the UI already shows "Ends at"; suppress
        // the duplicate countdown for the last hour.
        if (duration <= Duration.ofHours(1)) return ""

        val days = floor(duration.toDays().toDouble()).toInt()
        val hours = floor(duration.toHours().toDouble()).toInt() % 24
        val minutes = duration.toMinutes().toInt() % 60

        return when {
            days >= 1 -> "${context.getString(R.string.ins)} ${days}d ${hours}h"
            duration.toHours() >= 1 ->
                "${context.getString(R.string.ins)} ${duration.toHours()}h ${minutes}m"
            else -> "${context.getString(R.string.ins)} ${minutes}m"
        }
    }

    fun getEndTime(now: ZonedDateTime = ZonedDateTime.now()): String {
        return if (now.isBefore(flightData.value.effectiveDepartDate)) {
            context.getString(R.string.ends_at)
        } else {
            ""
        }
    }

    /**
     * Prefers the API's real status. Falls back to times only when the API has
     * no status, which also fixes the old "Flying" shown before departure.
     */
    fun getStatus(now: ZonedDateTime = ZonedDateTime.now()): String {
        if (flightData.value.hasKnownStatus) {
            return flightData.value.status.humanize()
        }
        val depart = flightData.value.effectiveDepartDate
        val arrive = flightData.value.effectiveArriveDate
        return when {
            now.isBefore(depart) -> context.getString(R.string.check_in)
            !now.isBefore(arrive) -> context.getString(R.string.landed)
            _progress.value >= 0.9F -> context.getString(R.string.landing)
            else -> context.getString(R.string.flying)
        }
    }

    private fun String.humanize(): String =
        replace(Regex("([a-z])([A-Z])"), "$1 $2")

    class Factory(
        private val context: Context,
        private val repository: FlightRepository,
        private val preferences: AppPreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailsViewModel(
                repository = repository,
                preferences = preferences,
                context = context.applicationContext,
            ) as T
        }
    }
}
