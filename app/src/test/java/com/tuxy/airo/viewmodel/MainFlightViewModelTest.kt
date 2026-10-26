package com.tuxy.airo.viewmodel

import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.repository.FlightRepository
import com.tuxy.airo.data.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class MainFlightViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainFlightViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val repository: FlightRepository = mock()
        val preferences: AppPreferences = mock()
        whenever(preferences.timeFormat).thenReturn(flowOf("HH:mm"))
        whenever(preferences.recentFlights).thenReturn(flowOf(emptyList()))
        viewModel = MainFlightViewModel(repository, preferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun flight(id: Int, departure: String) = FlightData(
        id = id,
        scheduledDepartDate = ZonedDateTime.parse(departure),
        scheduledArriveDate = ZonedDateTime.parse(departure).plusHours(2),
    )

    @Test
    fun `groups flights within 24 hours and starts a new group after that`() {
        val first = flight(1, "2026-01-01T10:00:00Z")
        val second = flight(2, "2026-01-02T09:00:00Z") // 23 h later
        val third = flight(3, "2026-01-04T12:00:00Z") // > 24 h later

        val groups = viewModel.groupFlightsByProximity(listOf(first, second, third))

        assertEquals(2, groups.size)
        assertEquals(listOf(1, 2), groups[0].map { it.id })
        assertEquals(listOf(3), groups[1].map { it.id })
    }

    @Test
    fun `does not drop flights that share an arrival second`() {
        val a = FlightData(
            id = 1,
            scheduledDepartDate = ZonedDateTime.parse("2026-01-01T10:00:00Z"),
            scheduledArriveDate = ZonedDateTime.parse("2026-01-01T12:00:00Z"),
        )
        val b = FlightData(
            id = 2,
            scheduledDepartDate = ZonedDateTime.parse("2026-01-01T11:00:00Z"),
            scheduledArriveDate = ZonedDateTime.parse("2026-01-01T12:00:00Z"),
        )

        val groups = viewModel.groupFlightsByProximity(listOf(a, b))

        assertEquals(listOf(1, 2), groups.single().map { it.id })
    }
}
