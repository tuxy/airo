package com.tuxy.airo.viewmodel

import android.content.Context
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.repository.FlightRepository
import com.tuxy.airo.data.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FlightRepository
    private lateinit var preferences: AppPreferences
    private lateinit var viewModel: DetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mock()
        preferences = mock()
        whenever(preferences.timeFormat).thenReturn(flowOf("HH:mm"))
        viewModel = DetailsViewModel(repository, preferences, mock<Context>())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `zone difference is computed from the two offsets`() {
        viewModel.flightData.value = FlightData(
            scheduledDepartDate = ZonedDateTime.parse("2024-01-01T12:00:00+01:00"),
            scheduledArriveDate = ZonedDateTime.parse("2024-01-01T18:00:00+03:00"),
        )

        assertEquals("+02:00", viewModel.getZoneDifference())
    }

    @Test
    fun `zone difference is empty for the same offset`() {
        viewModel.flightData.value = FlightData(
            scheduledDepartDate = ZonedDateTime.parse("2024-01-01T12:00:00+01:00"),
            scheduledArriveDate = ZonedDateTime.parse("2024-01-01T18:00:00+01:00"),
        )

        assertEquals("", viewModel.getZoneDifference())
    }

    @Test
    fun `progress uses revised times and effective duration`() {
        val now = ZonedDateTime.now()
        viewModel.flightData.value = FlightData(
            revisedDepartDate = now.minusHours(1),
            revisedArriveDate = now.plusHours(1),
        )

        val progress = viewModel.calculateProgressFraction()

        assertEquals(0.5f, progress, 0.05f)
    }

    @Test
    fun `progress is zero before departure`() {
        val now = ZonedDateTime.now()
        viewModel.flightData.value = FlightData(
            revisedDepartDate = now.plusHours(1),
            revisedArriveDate = now.plusHours(3),
        )

        assertEquals(0.0f, viewModel.calculateProgressFraction(), 0.0f)
    }

    @Test
    fun `delete delegates to the repository`() = runTest {
        viewModel.flightData.value = FlightData(id = 7)

        viewModel.deleteFlight()

        verify(repository).deleteFlight(any())
    }

    @Test
    fun `effective duration falls back to scheduled times`() {
        viewModel.flightData.value = FlightData(
            scheduledDepartDate = ZonedDateTime.parse("2026-01-01T12:00:00Z"),
            scheduledArriveDate = ZonedDateTime.parse("2026-01-01T15:00:00Z"),
        )

        assertEquals(Duration.ofHours(3), viewModel.flightData.value.effectiveDuration)
    }
}
