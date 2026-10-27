package com.tuxy.airo.viewmodel

import com.tuxy.airo.data.network.formatFlightNumber
import com.tuxy.airo.data.repository.AddFlightResult
import com.tuxy.airo.data.repository.FlightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DateViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FlightRepository
    private lateinit var viewModel: DateViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mock()
        viewModel = DateViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `date picker millis preserve the selected calendar day`() {
        // 2025-09-09T00:00:00Z, as returned by Material3's date picker.
        val date = viewModel.getDateAsString(1757376000000L)

        assertEquals(LocalDate.of(2025, 9, 9), date.toLocalDate())
    }

    @Test
    fun `formatFlightNumber removes separators`() {
        assertEquals("UA1", formatFlightNumber("UA-1"))
        assertEquals("UA1", formatFlightNumber("UA 1"))
        assertEquals("ua1", formatFlightNumber(" ua1 "))
    }

    @Test
    fun `successful add emits success`() = runTest {
        whenever(repository.addFlight(any(), any())).thenReturn(AddFlightResult.Added(1))

        viewModel.addFlight("UA1", 1757376000000L)
        advanceUntilIdle()

        assertEquals(AddFlightState.Success, viewModel.state.value)
    }

    @Test
    fun `duplicate add is reported, not treated as success`() = runTest {
        whenever(repository.addFlight(any(), any())).thenReturn(AddFlightResult.Duplicate)

        viewModel.addFlight("UA1", 1757376000000L)
        advanceUntilIdle()

        assertEquals(AddFlightState.Duplicate, viewModel.state.value)
    }

    @Test
    fun `failure keeps the message for the UI`() = runTest {
        whenever(repository.addFlight(any(), any()))
            .thenReturn(AddFlightResult.Failed("The API key was rejected"))

        viewModel.addFlight("UA1", 1757376000000L)
        advanceUntilIdle()

        assertEquals(
            AddFlightState.Error("The API key was rejected"),
            viewModel.state.value,
        )
    }

    @Test
    fun `missing date is rejected before any request`() = runTest {
        viewModel.addFlight("UA1", null)
        advanceUntilIdle()

        assertEquals(AddFlightState.Error("Pick a date first."), viewModel.state.value)
    }
}
