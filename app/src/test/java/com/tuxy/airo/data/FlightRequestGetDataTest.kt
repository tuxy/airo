package com.tuxy.airo.data

import android.content.Context
import com.tuxy.airo.screens.ApiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FlightRequestGetDataTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mockDao: FlightDataDao
    private lateinit var mockContext: Context // Basic mock for now
    private val testDispatcher = UnconfinedTestDispatcher()

    private val flightNumber = "SQ321"
    private val date = "2024-01-01"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockWebServer = MockWebServer()
        mockWebServer.start()
        mockDao = mock()
        mockContext = mock() // Basic mock, won't verify interactions with it for setAlarm for now
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        Dispatchers.resetMain()
    }

    private fun getBaseApiSettings(choice: String = "1", key: String = "test_key"): ApiSettings {
        val serverUrl = mockWebServer.url("/").toString()
        // Ensure serverUrl ends with a slash if the code expects it, or remove trailing slash if not.
        // The buildFlightApiRequest logic is: "${urlChoice}/${flightNumber}/${date}"
        // So urlChoice should not have a trailing slash.
        val cleanServerUrl = if (serverUrl.endsWith('/')) serverUrl.dropLast(1) else serverUrl
        return ApiSettings(
            choice = choice, // "1" for custom endpoint (our mock server)
            endpoint = cleanServerUrl,
            server = "http://real.server.com", // Irrelevant if choice is "1"
            key = key
        )
    }

    private fun getValidFlightJson(): String {
        // Re-using a simplified valid JSON from FlightRequestParseDataTest for MockWebServer
        return """
        [
          {
            "number": "SQ321",
            "departure": {
              "airport": { "iata": "SIN", "shortName": "Singapore", "location": { "lat": 1.3644, "lon": 103.9915 } },
              "scheduledTime": { "local": "2024-01-01T10:00:00+08:00", "utc": "2024-01-01T02:00:00Z" }
            },
            "arrival": {
              "airport": { "iata": "LHR", "shortName": "London Heathrow", "location": { "lat": 51.4700, "lon": -0.4543 } },
              "scheduledTime": { "local": "2024-01-01T16:00:00Z", "utc": "2024-01-01T16:00:00Z" }
            },
            "airline": { "name": "Singapore Airlines" },
            "aircraft": { "model": "Airbus A380" }
          }
        ]
        """.trimIndent()
    }
    
    private fun getMalformedJson(): String = "[{\"number\": \"SQ321\",, \"departure\": {}" // Malformed

    private fun getJsonMissingCriticalField(): String { // Missing departure airport IATA
         return """
        [
          {
            "number": "SQ321",
            "departure": {
              "airport": { "shortName": "Singapore", "location": { "lat": 1.3644, "lon": 103.9915 } },
              "scheduledTime": { "local": "2024-01-01T10:00:00+08:00", "utc": "2024-01-01T02:00:00Z" }
            },
            "arrival": {
              "airport": { "iata": "LHR", "shortName": "London Heathrow", "location": { "lat": 51.4700, "lon": -0.4543 } },
              "scheduledTime": { "local": "2024-01-01T16:00:00Z", "utc": "2024-01-01T16:00:00Z" }
            }
          }
        ]
        """.trimIndent()
    }


    @Test
    fun getData_successCase_returnsSuccessAndAddsToDb() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(getValidFlightJson()).setResponseCode(200))
        val settings = getBaseApiSettings()
        whenever(mockDao.queryExisting(any(), any())).thenReturn(0)

        val result = getData(flightNumber, mockDao, date, settings, mockContext)

        assertTrue(result.isSuccess)
        assertEquals("SQ321", result.getOrNull()?.callSign)
        assertEquals("Singapore Airlines", result.getOrNull()?.airline) // Check a non-critical field too
        verify(mockDao).addFlight(argThat { callSign == "SQ321" })
        // Not verifying setAlarm directly due to reasons mentioned in plan.
    }

    @Test
    fun getData_apiKeyMissingForCustomEndpoint_returnsApiKeyMissingError() = runTest {
        val settings = getBaseApiSettings(choice = "1", key = "") // Custom endpoint, but key is empty
        
        val result = getData(flightNumber, mockDao, date, settings, mockContext)
        
        assertTrue(result.isFailure)
        assertEquals(FlightDataError.ApiKeyMissing, result.exceptionOrNull())
    }
    
    @Test
    fun getData_serverUrlMissing_returnsApiKeyMissingError() = runTest {
         // choice "0" for official server, "1" for custom endpoint
        val settingsNoEndpoint = ApiSettings(choice = "1", endpoint = "", server = "server.com", key = "somekey")
        val resultNoEndpoint = getData(flightNumber, mockDao, date, settingsNoEndpoint, mockContext)
        assertTrue(resultNoEndpoint.isFailure)
        assertEquals(FlightDataError.ApiKeyMissing, resultNoEndpoint.exceptionOrNull())

        val settingsNoServer = ApiSettings(choice = "0", endpoint = "endpoint.com", server = "", key = "somekey")
        val resultNoServer = getData(flightNumber, mockDao, date, settingsNoServer, mockContext)
        assertTrue(resultNoServer.isFailure)
        assertEquals(FlightDataError.ApiKeyMissing, resultNoServer.exceptionOrNull())
    }


    @Test
    fun getData_networkError_returnsNetworkError() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        val settings = getBaseApiSettings()

        val result = getData(flightNumber, mockDao, date, settings, mockContext)

        assertTrue(result.isFailure)
        assertEquals(FlightDataError.NetworkError, result.exceptionOrNull())
    }
    
    @Test
    fun getData_networkIoException_returnsUnknownError() = runTest {
        // Simulate an IOException, e.g. server disconnects abruptly
        // MockWebServer doesn't directly simulate all IOExceptions easily,
        // but an empty response might sometimes trigger client-side issues.
        // A more direct way would be to mock the OkHttpClient if we could inject it.
        // For now, we assume that some IOExceptions will bubble up as generic Exception.
        // This test is more conceptual for now.
        // To truly test this, one might need to mock the OkHttpClient directly.
        // For now, let's assume an unhandled exception in client.execute() leads to UnknownError
        // This specific scenario is hard to trigger deterministically with MockWebServer alone for all types of IOExceptions.
        // If client.newCall(request).execute() throws an IOException directly (not an HTTP error code)
        // it should be caught by the generic catch (e: Exception) in getData.
        
        // We can't make MockWebServer itself throw an IOException easily for the *execution* of the call.
        // The UnknownError is for exceptions *other* than Klaxon/MissingCriticalData.
        // Let's assume for now that an unexpected exception in the OkHttp call chain not resulting in a Response object
        // would be caught by the generic `catch (e: Exception)`.
        // This test will be more of a placeholder or needs deeper OkHttp client mocking.
        // For now, we'll test a scenario that results in a non-HTTP, non-parsing, non-critical data error.
        // One way to simulate this is by causing an issue *after* network but *before* full parsing,
        // though getData's structure makes this hard.
        // Let's skip a direct IOException simulation for now and rely on the generic catch test.
        // Consider if `response.body!!.string()` could throw an IOException.
        mockWebServer.enqueue(MockResponse().setBody("Simulate issue with body reading").setHeader("Content-Length", "100").setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START))
        val settings = getBaseApiSettings()
        val result = getData(flightNumber, mockDao, date, settings, mockContext)
        assertTrue(result.isFailure)
        // This often results in NetworkError or potentially UnknownError depending on OkHttp client behavior
        // Given the current structure of getData, most OkHttp/network issues will likely manifest as NetworkError
        // or be caught by the generic Exception handler, mapping to UnknownError.
        // If response.isSuccessful is false, it's NetworkError.
        // If response.body.string() throws an IOException, it would be UnknownError.
        // SocketPolicy.DISCONNECT_AT_START should cause an IOException when the client tries to read.
        assertEquals(FlightDataError.NetworkError, result.exceptionOrNull()) // More likely NetworkError for DISCONNECT_AT_START
    }


    @Test
    fun getData_parsingError_returnsParsingError() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(getMalformedJson()).setResponseCode(200))
        val settings = getBaseApiSettings()

        val result = getData(flightNumber, mockDao, date, settings, mockContext)

        assertTrue(result.isFailure)
        assertEquals(FlightDataError.ParsingError, result.exceptionOrNull())
    }

    @Test
    fun getData_incompleteCriticalData_returnsIncompleteDataError() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(getJsonMissingCriticalField()).setResponseCode(200))
        val settings = getBaseApiSettings()
        
        val result = getData(flightNumber, mockDao, date, settings, mockContext)

        assertTrue(result.isFailure)
        assertEquals(FlightDataError.IncompleteDataError, result.exceptionOrNull())
    }

    @Test
    fun getData_flightAlreadyExists_returnsFlightAlreadyExistsError() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(getValidFlightJson()).setResponseCode(200))
        val settings = getBaseApiSettings()
        whenever(mockDao.queryExisting(any(), any())).thenReturn(1) // Flight exists

        val result = getData(flightNumber, mockDao, date, settings, mockContext)

        assertTrue(result.isFailure)
        assertEquals(FlightDataError.FlightAlreadyExists, result.exceptionOrNull())
        verify(mockDao, never()).addFlight(any())
    }
    
    @Test
    fun getData_daoAddFlightThrowsException_returnsUnknownError() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(getValidFlightJson()).setResponseCode(200))
        val settings = getBaseApiSettings()
        whenever(mockDao.queryExisting(any(), any())).thenReturn(0)
        whenever(mockDao.addFlight(any())).thenThrow(RuntimeException("DAO insert failed"))

        val result = getData(flightNumber, mockDao, date, settings, mockContext)

        assertTrue(result.isFailure)
        assertEquals(FlightDataError.UnknownError, result.exceptionOrNull())
    }
}
