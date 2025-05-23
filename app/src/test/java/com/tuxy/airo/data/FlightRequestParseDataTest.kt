package com.tuxy.airo.data

import com.beust.klaxon.Klaxon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class FlightRequestParseDataTest {

    private val klaxon = Klaxon()

    // Helper to build a Root object from a JSON string representing a single RootElement in an array
    private fun buildRoot(jsonElement: String): Root {
        val list = klaxon.parseArray<RootElement>("[$jsonElement]")
        assertNotNull("Failed to parse JSON into list of RootElement", list)
        assertTrue("Parsed list should not be empty", list!!.isNotEmpty())
        return Root(list)
    }

    private fun getFullValidJson(): String {
        // This JSON should contain all fields that parseData extracts, both critical and non-critical.
        // It's based on the sample JSON in FlightRequest.kt but fleshed out.
        return """
        {
            "greatCircleDistance": { "meter": 6556365.47, "km": 6556.37, "mile": 4073.94, "nm": 3540.15, "feet": 21510385.4 },
            "departure": {
                "airport": {
                    "icao": "YBBN", "iata": "BNE", "name": "Brisbane International", "shortName": "Brisbane",
                    "municipalityName": "Brisbane City", "location": { "lat": -27.3842, "lon": 153.117 },
                    "countryCode": "AU", "timeZone": "Australia/Brisbane"
                },
                "scheduledTime": { "utc": "2025-01-16T13:40:00Z", "local": "2025-01-16T23:40:00+10:00" },
                "revisedTime": { "utc": "2025-01-16T13:45:00Z", "local": "2025-01-16T23:45:00+10:00" },
                "terminal": "I", "gate": "79A", "quality": ["Basic", "Live"]
            },
            "arrival": {
                "airport": {
                    "icao": "VVTS", "iata": "SGN", "name": "Tan Son Nhat International", "shortName": "Tan Son Nhat",
                    "municipalityName": "Ho Chi Minh City", "location": { "lat": 10.818799, "lon": 106.652 },
                    "countryCode": "VN", "timeZone": "Asia/Ho_Chi_Minh"
                },
                "scheduledTime": { "utc": "2025-01-16T22:05:00Z", "local": "2025-01-17T05:05:00+07:00" },
                "predictedTime": { "utc": "2025-01-16T21:35:00Z", "local": "2025-01-17T04:35:00+07:00" },
                "terminal": "2", "gate": "12B", "quality": ["Basic"]
            },
            "lastUpdatedUtc": "2025-01-14T14:45:00Z",
            "number": "VJ84",
            "status": "Expected",
            "codeshareStatus": "IsOperator",
            "isCargo": false,
            "aircraft": {
                "model": "Airbus A330-300",
                "image": {
                    "url": "https://example.com/a330.jpg", "webUrl": "https://example.com/imagepage",
                    "author": "John Doe", "title": "A330 Landing", "description": "VietJet A330",
                    "license": "CC BY-SA 2.0", "htmlAttributions": ["Photo by John Doe"]
                }
            },
            "airline": { "name": "VietJet Air", "iata": "VJ", "icao": "VJC" }
        }
        """.trimIndent()
    }

    @Test
    fun parseData_completeAndValidJson_mapsAllFieldsCorrectly() {
        val json = getFullValidJson()
        val root = buildRoot(json)
        val flightData = parseData(root)

        // Critical fields (existence checked by direct parsing, values by assertions)
        assertEquals("VJ84", flightData.callSign)
        assertEquals("BNE", flightData.from)
        assertEquals("SGN", flightData.to)
        assertEquals("Brisbane", flightData.fromName)
        assertEquals("Tan Son Nhat", flightData.toName)

        // Dates - parseDateTime is tested separately, here we check if correct strings were passed
        val expectedDepartDateTime = parseDateTime("2025-01-16T23:40:00+10:00")
        val expectedArriveDateTime = parseDateTime("2025-01-17T05:05:00+07:00")
        assertEquals(expectedDepartDateTime, flightData.departDate)
        assertEquals(expectedArriveDateTime, flightData.arriveDate)
        
        // Duration - calculated from UTC times
        val expectedDuration = Duration.between(
            parseDateTime("2025-01-16T13:40:00Z"), // Departure UTC
            parseDateTime("2025-01-16T22:05:00Z")  // Arrival UTC
        )
        assertEquals(expectedDuration, flightData.duration)

        // Map coordinates - values depend on MapProjectionUtils, check they are not default/zero
        // Assuming MapProjectionUtilsTest covers the actual calculation correctness.
        // Here we just verify they are populated.
        assertTrue(flightData.mapOriginX != 0.0 || flightData.mapOriginY != 0.0)
        assertTrue(flightData.mapDestinationX != 0.0 || flightData.mapDestinationY != 0.0)


        // Non-critical fields
        assertEquals("VietJet Air", flightData.airline)
        assertEquals("VJC", flightData.airlineIcao)
        assertEquals("VJ", flightData.airlineIata)
        assertEquals("79A", flightData.gate) // Departure gate
        assertEquals("I", flightData.terminal) // Departure terminal
        assertEquals("Airbus A330-300", flightData.aircraftName)
        assertEquals("https://example.com/a330.jpg", flightData.aircraftUri)
        assertEquals("John Doe", flightData.author)
        assertEquals("https://example.com/imagepage", flightData.authorUri)
        assertEquals("Photo by John Doe", flightData.attribution)
    }

    // --- Test for missing critical data ---

    // Helper to test for MissingCriticalDataException
    private fun testMissingCriticalField(jsonUpdater: (MutableMap<String, Any?>) -> Unit, fieldName: String) {
        val baseJsonMap = klaxon.parse<Map<String, Any?>>(getFullValidJson())
        assertNotNull("Base JSON for $fieldName could not be parsed", baseJsonMap)
        val mutableJsonMap = baseJsonMap!!.toMutableMap()
        jsonUpdater(mutableJsonMap)
        
        // Klaxon stringify map to json
        val modifiedJsonString = klaxon.toJsonString(mutableJsonMap)
        val root = buildRoot(modifiedJsonString)
        
        try {
            parseData(root)
            fail("Expected MissingCriticalDataException for missing $fieldName, but no exception was thrown.")
        } catch (e: MissingCriticalDataException) {
            // Expected
            assertTrue("Exception message should contain field name '$fieldName'", e.message?.contains(fieldName, ignoreCase = true) ?: false)
        } catch (e: Exception) {
            fail("Expected MissingCriticalDataException for $fieldName, but got ${e::class.simpleName}: ${e.message}")
        }
    }
    
    // Simplified helper for top-level fields
    private fun testMissingTopLevelCriticalField(fieldName: String) {
        testMissingCriticalField({ it.remove(fieldName) }, fieldName)
    }

    @Test fun parseData_missingNumber_throwsException() = testMissingTopLevelCriticalField("number") // "Call sign (number)"

    // Departure fields
    @Test fun parseData_missingDeparture_throwsException() = testMissingTopLevelCriticalField("departure") // "Departure information"
    @Test fun parseData_missingDepartureAirport_throwsException() {
        testMissingCriticalField({ (it["departure"] as MutableMap<String, Any?>).remove("airport") }, "Departure airport data")
    }
    @Test fun parseData_missingDepartureAirportIata_throwsException() {
        testMissingCriticalField({ ((it["departure"] as MutableMap<String, Any?>)["airport"] as MutableMap<String, Any?>).remove("iata") }, "Departure airport IATA")
    }
    @Test fun parseData_missingDepartureAirportShortName_throwsException() {
        testMissingCriticalField({ ((it["departure"] as MutableMap<String, Any?>)["airport"] as MutableMap<String, Any?>).remove("shortName") }, "Departure airport short name")
    }
    @Test fun parseData_missingDepartureScheduledTime_throwsException() {
        testMissingCriticalField({ (it["departure"] as MutableMap<String, Any?>).remove("scheduledTime") }, "Departure scheduled time data")
    }
    @Test fun parseData_missingDepartureScheduledTimeLocal_throwsException() {
        testMissingCriticalField({ ((it["departure"] as MutableMap<String, Any?>)["scheduledTime"] as MutableMap<String, Any?>).remove("local") }, "Departure scheduled time (local)")
    }
    @Test fun parseData_missingDepartureScheduledTimeUtc_throwsException() {
        testMissingCriticalField({ ((it["departure"] as MutableMap<String, Any?>)["scheduledTime"] as MutableMap<String, Any?>).remove("utc") }, "Departure scheduled time (UTC)")
    }
    @Test fun parseData_missingDepartureLocation_throwsException() {
        testMissingCriticalField({ ((it["departure"] as MutableMap<String, Any?>)["airport"] as MutableMap<String, Any?>).remove("location") }, "Departure airport location")
    }
    @Test fun parseData_missingDepartureLat_throwsException() {
        testMissingCriticalField({ (((it["departure"] as MutableMap<String, Any?>)["airport"] as MutableMap<String, Any?>)["location"] as MutableMap<String, Any?>).remove("lat") }, "Departure airport latitude")
    }
    @Test fun parseData_missingDepartureLon_throwsException() {
        testMissingCriticalField({ (((it["departure"] as MutableMap<String, Any?>)["airport"] as MutableMap<String, Any?>)["location"] as MutableMap<String, Any?>).remove("lon") }, "Departure airport longitude")
    }

    // Arrival fields (similar structure to departure)
    @Test fun parseData_missingArrival_throwsException() = testMissingTopLevelCriticalField("arrival") // "Arrival information"
    @Test fun parseData_missingArrivalAirport_throwsException() {
        testMissingCriticalField({ (it["arrival"] as MutableMap<String, Any?>).remove("airport") }, "Arrival airport data")
    }
    @Test fun parseData_missingArrivalAirportIata_throwsException() {
        testMissingCriticalField({ ((it["arrival"] as MutableMap<String, Any?>)["airport"] as MutableMap<String, Any?>).remove("iata") }, "Arrival airport IATA")
    }
     @Test fun parseData_missingArrivalAirportShortName_throwsException() {
        testMissingCriticalField({ ((it["arrival"] as MutableMap<String, Any?>)["airport"] as MutableMap<String, Any?>).remove("shortName") }, "Arrival airport short name")
    }
    @Test fun parseData_missingArrivalScheduledTime_throwsException() {
        testMissingCriticalField({ (it["arrival"] as MutableMap<String, Any?>).remove("scheduledTime") }, "Arrival scheduled time data")
    }
    @Test fun parseData_missingArrivalScheduledTimeLocal_throwsException() {
        testMissingCriticalField({ ((it["arrival"] as MutableMap<String, Any?>)["scheduledTime"] as MutableMap<String, Any?>).remove("local") }, "Arrival scheduled time (local)")
    }
    @Test fun parseData_missingArrivalScheduledTimeUtc_throwsException() {
        testMissingCriticalField({ ((it["arrival"] as MutableMap<String, Any?>)["scheduledTime"] as MutableMap<String, Any?>).remove("utc") }, "Arrival scheduled time (UTC)")
    }
    @Test fun parseData_missingArrivalLocation_throwsException() {
        testMissingCriticalField({ ((it["arrival"] as MutableMap<String, Any?>)["airport"] as MutableMap<String, Any?>).remove("location") }, "Arrival airport location")
    }
    @Test fun parseData_missingArrivalLat_throwsException() {
        testMissingCriticalField({ (((it["arrival"] as MutableMap<String, Any?>)["airport"] as MutableMap<String, Any?>)["location"] as MutableMap<String, Any?>).remove("lat") }, "Arrival airport latitude")
    }
    @Test fun parseData_missingArrivalLon_throwsException() {
        testMissingCriticalField({ (((it["arrival"] as MutableMap<String, Any?>)["airport"] as MutableMap<String, Any?>)["location"] as MutableMap<String, Any?>).remove("lon") }, "Arrival airport longitude")
    }
    
    // --- Test for missing non-critical data ---
    @Test
    fun parseData_missingNonCriticalFields_completesWithDefaults() {
        var jsonMap = klaxon.parse<MutableMap<String, Any?>>(getFullValidJson())!!
        
        // Remove some non-critical fields
        (jsonMap["aircraft"] as? MutableMap<String, Any?>)?.set("model", null) // Set model to null
        ((jsonMap["aircraft"] as? MutableMap<String, Any?>)?.get("image") as? MutableMap<String, Any?>)?.remove("author")
        (jsonMap["departure"] as? MutableMap<String, Any?>)?.remove("gate")
        (jsonMap["airline"] as? MutableMap<String, Any?>)?.set("icao", null) // Set icao to null

        val modifiedJsonString = klaxon.toJsonString(jsonMap)
        val root = buildRoot(modifiedJsonString)
        val flightData = parseData(root)

        assertEquals("N/A", flightData.aircraftName) // Default from ifNullOrEmptyLog
        assertEquals(null, flightData.author) // Null if not present in JSON and no specific default in FlightData (Image.author is String?)
        assertEquals("N/A", flightData.gate) // Default from ifNullOrEmptyLog
        assertEquals("N/A", flightData.airlineIcao) // Default from ifNullOrEmptyLog
        
        // Check a field that was present to ensure it's still there
        assertEquals("VietJet Air", flightData.airline)
    }

    @Test
    fun parseData_emptyJsonInArray_throwsMissingCriticalDataException() {
        // Test what happens if the JSON array contains an empty object or an object not matching RootElement
        // Root.fromJson itself might fail, or firstOrNull might yield something not useful
        // The current buildRoot would fail if it can't parse to List<RootElement>
        // If it parses to RootElement but that element is effectively empty:
        val root = buildRoot("{}") // An empty object
        try {
            parseData(root)
            fail("Expected MissingCriticalDataException for empty JSON object, but no exception was thrown.")
        } catch (e: MissingCriticalDataException) {
            // Expected, as "number" and other critical fields will be missing.
            // The first check is for "Call sign (number) is missing"
             assertTrue(e.message?.contains("Call sign (number) is missing") == true)
        }
    }

    @Test
    fun parseData_completelyEmptyJsonArray_throwsMissingCriticalDataException() {
        // This tests the firstOrNull() check in parseData
        val root = Root(emptyList()) // Empty root list
        try {
            parseData(root)
            fail("Expected MissingCriticalDataException for empty JSON array, but no exception was thrown.")
        } catch (e: MissingCriticalDataException) {
            assertEquals("JSON root is null or empty", e.message)
        }
    }

    // --- Tests for aircraft image field defaults ---

    private fun getBaseJsonForImageTests(): String {
        // A minimal but valid JSON structure, focusing on testing aircraft image defaults.
        // Critical fields are present. Aircraft/image fields will be manipulated by each test.
        return """
        {
            "number": "IMGTEST01",
            "departure": {
                "airport": { "iata": "DEP", "shortName": "Departure Airport", "location": { "lat": 1.0, "lon": 1.0 } },
                "scheduledTime": { "local": "2024-01-01T10:00:00+00:00", "utc": "2024-01-01T10:00:00Z" }
            },
            "arrival": {
                "airport": { "iata": "ARR", "shortName": "Arrival Airport", "location": { "lat": 2.0, "lon": 2.0 } },
                "scheduledTime": { "local": "2024-01-01T12:00:00+00:00", "utc": "2024-01-01T12:00:00Z" }
            }
            // aircraft and airline objects will be added/modified by specific tests
        }
        """.trimIndent()
    }

    private fun assertImageDefaults(flightData: FlightData, callSign: String = "IMGTEST01") {
        assertEquals(callSign, flightData.callSign) // Ensure rest of parsing is okay
        assertEquals("Aircraft URI should be empty string", "", flightData.aircraftUri)
        assertEquals("Author should be empty string", "", flightData.author)
        assertEquals("Author URI should be empty string", "", flightData.authorUri)
        assertEquals("N/A", flightData.aircraftName) // Default for model
    }

    @Test
    fun parseData_aircraftObjectNull_defaultsImageFieldsToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = null // Explicitly set aircraft to null
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertImageDefaults(flightData)
    }

    @Test
    fun parseData_aircraftObjectMissing_defaultsImageFieldsToEmptyString() {
        // Aircraft object completely missing from JSON (baseJsonForImageTests doesn't include it by default)
        val json = getBaseJsonForImageTests()
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertImageDefaults(flightData)
    }

    @Test
    fun parseData_imageObjectNullInAircraft_defaultsImageFieldsToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = mutableMapOf<String, Any?>("model" to "Some Model", "image" to null)
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertImageDefaults(flightData)
        assertEquals("Some Model", flightData.aircraftName)
    }

    @Test
    fun parseData_imageObjectMissingInAircraft_defaultsImageFieldsToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = mutableMapOf<String, Any?>("model" to "Some Model") // Image object missing
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertImageDefaults(flightData)
        assertEquals("Some Model", flightData.aircraftName)
    }

    @Test
    fun parseData_imageUrlNull_defaultsAircraftUriToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = mutableMapOf<String, Any?>(
            "image" to mutableMapOf<String, Any?>("url" to null, "author" to "Test Author", "webUrl" to "http://example.com")
        )
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertEquals("IMGTEST01", flightData.callSign)
        assertEquals("", flightData.aircraftUri)
        assertEquals("Test Author", flightData.author)
        assertEquals("http://example.com", flightData.authorUri)
    }
    
    @Test
    fun parseData_imageUrlMissing_defaultsAircraftUriToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = mutableMapOf<String, Any?>(
            "image" to mutableMapOf<String, Any?>("author" to "Test Author", "webUrl" to "http://example.com") // url missing
        )
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertEquals("IMGTEST01", flightData.callSign)
        assertEquals("", flightData.aircraftUri)
        assertEquals("Test Author", flightData.author)
        assertEquals("http://example.com", flightData.authorUri)
    }

    @Test
    fun parseData_imageAuthorNull_defaultsAuthorToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = mutableMapOf<String, Any?>(
            "image" to mutableMapOf<String, Any?>("url" to "http://example.com/img.png", "author" to null, "webUrl" to "http://example.com")
        )
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertEquals("IMGTEST01", flightData.callSign)
        assertEquals("http://example.com/img.png", flightData.aircraftUri)
        assertEquals("", flightData.author)
        assertEquals("http://example.com", flightData.authorUri)
    }

    @Test
    fun parseData_imageAuthorMissing_defaultsAuthorToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = mutableMapOf<String, Any?>(
            "image" to mutableMapOf<String, Any?>("url" to "http://example.com/img.png", "webUrl" to "http://example.com") // author missing
        )
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertEquals("IMGTEST01", flightData.callSign)
        assertEquals("http://example.com/img.png", flightData.aircraftUri)
        assertEquals("", flightData.author)
        assertEquals("http://example.com", flightData.authorUri)
    }

    @Test
    fun parseData_imageWebUrlNull_defaultsAuthorUriToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = mutableMapOf<String, Any?>(
            "image" to mutableMapOf<String, Any?>("url" to "http://example.com/img.png", "author" to "Test Author", "webUrl" to null)
        )
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertEquals("IMGTEST01", flightData.callSign)
        assertEquals("http://example.com/img.png", flightData.aircraftUri)
        assertEquals("Test Author", flightData.author)
        assertEquals("", flightData.authorUri)
    }

    @Test
    fun parseData_imageWebUrlMissing_defaultsAuthorUriToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = mutableMapOf<String, Any?>(
            "image" to mutableMapOf<String, Any?>("url" to "http://example.com/img.png", "author" to "Test Author") // webUrl missing
        )
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertEquals("IMGTEST01", flightData.callSign)
        assertEquals("http://example.com/img.png", flightData.aircraftUri)
        assertEquals("Test Author", flightData.author)
        assertEquals("", flightData.authorUri)
    }

    @Test
    fun parseData_allImageSpecificFieldsNull_defaultsAllImageFieldsToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = mutableMapOf<String, Any?>(
            "image" to mutableMapOf<String, Any?>("url" to null, "author" to null, "webUrl" to null)
        )
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertImageDefaults(flightData)
    }

    @Test
    fun parseData_allImageSpecificFieldsMissing_defaultsAllImageFieldsToEmptyString() {
        val baseMap = klaxon.parse<MutableMap<String, Any?>>(getBaseJsonForImageTests())!!
        baseMap["aircraft"] = mutableMapOf<String, Any?>(
            "image" to emptyMap<String, Any?>() // All url, author, webUrl missing
        )
        val json = klaxon.toJsonString(baseMap)
        val root = buildRoot(json)
        val flightData = parseData(root)
        assertImageDefaults(flightData)
    }
}
