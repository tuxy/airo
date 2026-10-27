package com.tuxy.airo.data.network

import com.tuxy.airo.data.FLIGHT_LIST_JSON
import com.tuxy.airo.data.settings.ApiConfig
import com.tuxy.airo.data.settings.ApiProvider
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FlightApiTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun api(
        baseUrl: String = server.url("/").toString(),
        key: String? = null,
    ) = FlightApi(
        configProvider = { ApiConfig(ApiProvider.DIRECT, baseUrl, key) },
        client = OkHttpClient(),
    )

    @Test
    fun `successful response parses the list and picks the complete entry`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(FLIGHT_LIST_JSON))

        val result = api().getFlight("UA1", LocalDate.of(2026, 1, 1))

        assertTrue(result is ApiResult.Success)
        val flights = (result as ApiResult.Success).data
        assertEquals(2, flights.size)
        assertEquals("EWR", flights.bestFlight()?.departure?.airport?.iata)
    }

    @Test
    fun `401 surfaces the server message instead of a generic network error`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"message":"The API key is invalid"}""")
        )

        val result = api(key = "bad").getFlight("UA1", LocalDate.of(2026, 1, 1))

        assertTrue(result is ApiResult.HttpError)
        result as ApiResult.HttpError
        assertEquals(401, result.code)
        assertEquals("The API key is invalid", result.message)
    }

    @Test
    fun `204 means no flight rather than a failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = api().getFlight("UA1", LocalDate.of(2026, 1, 1))

        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.isEmpty())
    }

    @Test
    fun `malformed body is a parse error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>not json</html>"))

        val result = api().getFlight("UA1", LocalDate.of(2026, 1, 1))

        assertTrue(result is ApiResult.ParseError)
    }

    @Test
    fun `missing endpoint is a configuration error`() = runTest {
        val result = api(baseUrl = "").getFlight("UA1", LocalDate.of(2026, 1, 1))
        assertTrue(result is ApiResult.ConfigError)
    }

    @Test
    fun `url segments are appended without losing the base path`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        api(baseUrl = server.url("/api/").toString())
            .getFlight("UA-1", LocalDate.of(2026, 1, 1))

        val request = server.takeRequest()
        assertEquals("/api/flights/Number/UA1/2026-01-01", request.requestUrl?.encodedPath)
        assertEquals("Departure", request.requestUrl?.queryParameter("dateLocalRole"))
    }
}
