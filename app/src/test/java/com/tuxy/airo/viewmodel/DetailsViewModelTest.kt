package com.tuxy.airo.viewmodel

import android.content.Context
import android.content.res.Resources
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.PreferencesInterface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import kotlin.math.roundToInt

@ExperimentalCoroutinesApi
class DetailsViewModelTest {
    // Initialise viewmodel, context & resources for testing
    private lateinit var viewModel: DetailsViewModel
    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources
    private lateinit var mockPreferencesInterface: PreferencesInterface

    private val testDispatcher = StandardTestDispatcher()
    val flightDataDao: FlightDataDao = mock(FlightDataDao::class.java)

    @OptIn(DelicateCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mock(Context::class.java)
        mockResources = mock(Resources::class.java)
        mockPreferencesInterface = mock(PreferencesInterface::class.java)

        `when`(mockContext.resources)
            .thenReturn(mock()) // Mock resources
        `when`(mockContext.getString(anyInt()))
            .thenReturn("testString") // Mock getString

        mockPreferencesInterface.stub { // Mock suspend function
            onBlocking { getValueTimeFormat("24_time") }.doReturn("HH:mm")
        }

        viewModel = DetailsViewModel(
            context = mockContext,
            flightDataDao = flightDataDao,
            id = "testId",
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getProgressReturnsInitial() {
        assertEquals(0, viewModel.getProgress().roundToInt())
    }

    @Test
    fun getEndTimeReturnsInitial() {
        // Test getStatus
        assertEquals("testString", viewModel.getStatus(mockContext))

        // Test getEndTime
        assertEquals("testString ", viewModel.getEndTime(mockContext))
    }

    @Test
    fun getDurationReturnsInitial() {
        assertEquals("", viewModel.getDuration(mockContext))
    }

    @Test
    fun calculateScaleReturnsInitial() {
        // Test avr()
        assertEquals(2.0F, viewModel.avr(1.0, 3.0).toFloat())

        // Test calculateScale(), Number calculated manually
        assertEquals(
            0.0282842712474619F,
            viewModel.calculateScale(2.0, 2.0, 1.0, 1.0).toFloat()
        )
    }
}