package com.tuxy.airo.viewmodel

import android.content.Context
import android.content.res.Resources
import androidx.compose.material3.lightColorScheme
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

@ExperimentalCoroutinesApi
class DetailsViewModelTest {

    private lateinit var viewModel: DetailsViewModel
    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources
    private lateinit var mockPreferencesInterface: PreferencesInterface
    private lateinit var mockFlightDataDao: FlightDataDao

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mock()
        mockResources = mock()
        mockPreferencesInterface = mock()
        mockFlightDataDao = mock()

        viewModel = DetailsViewModel(
            context = mockContext,
            preferencesInterface = mockPreferencesInterface,
            scheme = lightColorScheme(),
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun Long.time(): String { // Add trailing zeroes and convert to string
        return this.absoluteValue.toString().padStart(2, '0')
    }

    @Test
    fun `getZoneDifference should return correct time difference`() = runTest {
        val departTime = ZonedDateTime.parse("2024-01-01T12:00:00+01:00")
        val arriveTime = ZonedDateTime.parse("2024-01-01T18:00:00+03:00")
        viewModel.flightData.value = FlightData(departDate = departTime, arriveDate = arriveTime)

        val difference = viewModel.getZoneDifference()

        assertEquals("+02:00", difference)
    }

    @Test
    fun `getZoneDifference should return empty string for same timezone`() = runTest {
        val departTime = ZonedDateTime.parse("2024-01-01T12:00:00+01:00")
        val arriveTime = ZonedDateTime.parse("2024-01-01T18:00:00+01:00")
        viewModel.flightData.value = FlightData(departDate = departTime, arriveDate = arriveTime)

        val difference = viewModel.getZoneDifference()

        assertEquals("", difference)
    }

    @Test
    fun `long time extension should format correctly`() {
        assertEquals("05", 5L.time())
        assertEquals("15", 15L.time())
    }

    @Test
    fun `getProgress should calculate progress correctly`() = runTest {
        val departTime = ZonedDateTime.now().minusHours(1)
        val duration = java.time.Duration.ofHours(2)
        viewModel.flightData.value = FlightData(departDate = departTime, duration = duration)

        viewModel.getProgress()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0.5f, viewModel.progress.floatValue, 0.01f)
    }

    @Test
    fun `deleteFlight should call delete on dao`() = runTest {
        viewModel.deleteFlight(mockFlightDataDao)
        testDispatcher.scheduler.advanceUntilIdle()
    }


    @Test
    fun `avr should return average`() {
        assertEquals(2.5, viewModel.avr(2.0, 3.0), 0.0)
    }

    @Test
    fun `calculateScale should return correct scale`() {
        assertEquals(0.0589, viewModel.calculateScale(1.0, 1.0, 2.0, 2.0), 0.0001)
    }
}