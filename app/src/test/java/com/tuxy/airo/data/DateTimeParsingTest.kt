package com.tuxy.airo.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

class DateTimeParsingTest {

    @Test
    fun parseDateTime_utcTimestamp_correctlyConvertsToLocal() {
        val utcTimestamp = "2023-01-01T12:00:00Z"
        // Expected instant: 2023-01-01 at 12:00:00 UTC
        val expectedInstant = OffsetDateTime.parse(utcTimestamp).toInstant()
        val expectedLocalDateTime = LocalDateTime.ofInstant(expectedInstant, ZoneId.systemDefault())

        val resultLocalDateTime = parseDateTime(utcTimestamp)
        assertEquals(expectedLocalDateTime, resultLocalDateTime)
    }

    @Test
    fun parseDateTime_positiveOffsetTimestamp_correctlyConvertsToLocal() {
        val offsetTimestamp = "2023-01-01T15:00:00+03:00"
        // Expected instant: 2023-01-01 at 12:00:00 UTC
        val expectedInstant = OffsetDateTime.parse(offsetTimestamp).toInstant()
        val expectedLocalDateTime = LocalDateTime.ofInstant(expectedInstant, ZoneId.systemDefault())

        val resultLocalDateTime = parseDateTime(offsetTimestamp)
        assertEquals(expectedLocalDateTime, resultLocalDateTime)
    }

    @Test
    fun parseDateTime_negativeOffsetTimestamp_correctlyConvertsToLocal() {
        val offsetTimestamp = "2023-01-01T09:00:00-03:00"
        // Expected instant: 2023-01-01 at 12:00:00 UTC
        val expectedInstant = OffsetDateTime.parse(offsetTimestamp).toInstant()
        val expectedLocalDateTime = LocalDateTime.ofInstant(expectedInstant, ZoneId.systemDefault())

        val resultLocalDateTime = parseDateTime(offsetTimestamp)
        assertEquals(expectedLocalDateTime, resultLocalDateTime)
    }
    
    @Test
    fun parseDateTime_timestampWithMinutesInOffset_correctlyConvertsToLocal() {
        val offsetTimestamp = "2023-01-01T12:30:00+05:30" // India Standard Time example
        val expectedInstant = OffsetDateTime.parse(offsetTimestamp).toInstant()
        val expectedLocalDateTime = LocalDateTime.ofInstant(expectedInstant, ZoneId.systemDefault())

        val resultLocalDateTime = parseDateTime(offsetTimestamp)
        assertEquals(expectedLocalDateTime, resultLocalDateTime)
    }


    @Test(expected = DateTimeParseException::class)
    fun parseDateTime_malformedString_throwsDateTimeParseException() {
        parseDateTime("This is not a date")
    }

    @Test(expected = DateTimeParseException::class)
    fun parseDateTime_incompleteDateString_throwsDateTimeParseException() {
        parseDateTime("2023-01-01T12:00") // Missing offset/Z
    }

    @Test(expected = DateTimeParseException::class)
    fun parseDateTime_invalidOffsetFormat_throwsDateTimeParseException() {
        parseDateTime("2023-01-01T12:00:00+0300") // Should be +03:00
    }
}
