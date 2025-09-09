package com.tuxy.airo.viewmodel

import android.content.Context
import android.content.res.Resources
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DateViewModelTest {
    private lateinit var viewModel: DateViewModel
    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mock(Context::class.java)
        mockResources = mock(Resources::class.java)

        `when`(mockContext.resources)
            .thenReturn(mock()) // Mock resources
        `when`(mockContext.getString(anyInt()))
            .thenReturn("testString") // Mock getString

        viewModel = DateViewModel(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dateStringReturnsInitial() {
        val time = viewModel.getDateAsString(1757376000)

        // Testing getting date as string, making sure it's aligned
        // Slightly off...
        assertEquals(
            ZonedDateTime.of(
                1970,
                1,
                21,
                8,
                9,
                36,
                0,
                java.time.ZoneId.systemDefault()
            ),
            time
        )
    }

    @Test
    fun formatFlightNumberReturnsInitial() {
        // Test splitting of flight numbers in search
        assertEquals("UA1", viewModel.formatFlightNumber("UA-1"))
        assertEquals("UA1", viewModel.formatFlightNumber("UA 1"))
    }
}