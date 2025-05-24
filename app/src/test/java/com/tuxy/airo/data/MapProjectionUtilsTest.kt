package com.tuxy.airo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MapProjectionUtilsTest {

    private val delta = 1e-5 // For floating point comparisons

    // --- Tests for doProjection ---

    @Test
    fun doProjection_validCoordinates_returnsCorrectPair() {
        // Example coordinates (e.g., Greenwich Observatory)
        val lat = 51.4778
        val lon = -0.0014

        MapProjectionUtils.doProjection(lat, lon)
        // Note: The exact projection calculation is complex and might differ slightly based on formula.
        // For this test, we are primarily checking if it runs and produces plausible values.
        // A more robust test would require known input-output pairs from the exact projection algorithm used.
        // Using placeholder values for now, assuming the internal formula is what we're testing.
        // If specific values are expected, they need to be derived from the same projection constants/formula.
        // Let's use a known input that gives somewhat rounder numbers if possible, or use a wider delta.
        // For now, assert it runs and gives two numbers.
        // A more concrete example: Paris, approx (2.3522 E, 48.8566 N)
        // X = 6378137.0 * (2.3522 * PI/180) approx 261848
        // Y = 3189068.5 * ln((1.0 + sin(48.8566*PI/180)) / (1.0 - sin(48.8566*PI/180))) approx 6245550
        val parisLat = 48.8566
        val parisLon = 2.3522
        val expectedParisX = 261848.0
        val expectedParisY = 6245550.0 // Values are approximate for this example

        val (x, y) = MapProjectionUtils.doProjection(parisLat, parisLon)
        assertEquals(expectedParisX, x, 2000.0) // Using a large delta due to approximation
        assertEquals(expectedParisY, y, 2000.0) // Using a large delta due to approximation
    }

    @Test(expected = MissingCriticalDataException::class)
    fun doProjection_invalidLatitudeTooHigh_throwsMissingCriticalDataException() {
        MapProjectionUtils.doProjection(90.1, 0.0)
    }

    @Test(expected = MissingCriticalDataException::class)
    fun doProjection_invalidLatitudeTooLow_throwsMissingCriticalDataException() {
        MapProjectionUtils.doProjection(-90.1, 0.0)
    }

    @Test(expected = MissingCriticalDataException::class)
    fun doProjection_invalidLongitudeTooHigh_throwsMissingCriticalDataException() {
        MapProjectionUtils.doProjection(0.0, 180.1)
    }

    @Test(expected = MissingCriticalDataException::class)
    fun doProjection_invalidLongitudeTooLow_throwsMissingCriticalDataException() {
        MapProjectionUtils.doProjection(0.0, -180.1)
    }

    // --- Tests for normalize ---

    @Test
    fun normalize_middleValue_returnsHalf() {
        val t = 50.0
        val min = 0.0
        val max = 100.0
        val expected = 0.5
        assertEquals(expected, MapProjectionUtils.normalize(t, min, max), delta)
    }

    @Test
    fun normalize_minValue_returnsZero() {
        val t = 0.0
        val min = 0.0
        val max = 100.0
        val expected = 0.0
        assertEquals(expected, MapProjectionUtils.normalize(t, min, max), delta)
    }

    @Test
    fun normalize_maxValue_returnsOne() {
        val t = 100.0
        val min = 0.0
        val max = 100.0
        val expected = 1.0
        assertEquals(expected, MapProjectionUtils.normalize(t, min, max), delta)
    }

    @Test
    fun normalize_valueOutsideMax_returnsGreaterThanOne() {
        val t = 150.0
        val min = 0.0
        val max = 100.0
        val expected = 1.5
        assertEquals(expected, MapProjectionUtils.normalize(t, min, max), delta)
    }

    @Test
    fun normalize_valueOutsideMin_returnsLessThanZero() {
        val t = -50.0
        val min = 0.0
        val max = 100.0
        val expected = -0.5
        assertEquals(expected, MapProjectionUtils.normalize(t, min, max), delta)
    }

    @Test
    fun normalize_withNegativeRangeAndValue() {
        val t = -150.0
        val min = -200.0
        val max = -100.0
        // Formula: (-150 - (-200)) / (-100 - (-200)) = (-150 + 200) / (-100 + 200) = 50 / 100 = 0.5
        val expected = 0.5
        assertEquals(expected, MapProjectionUtils.normalize(t, min, max), delta)
    }

    @Test
    fun normalize_minMaxSame_returnsNaNOrInfinity() {
        // Behavior when min == max can be problematic (division by zero).
        // Standard behavior for (x-m)/(m-m) is NaN if x==m, or +/- Infinity if x!=m.
        // Let's test what the current implementation does.
        val t = 50.0
        val min = 50.0
        val max = 50.0
        val result = MapProjectionUtils.normalize(t, min, max)
        assertEquals(true, result.isNaN()) // Expect NaN for (50-50)/(50-50) -> 0/0 = NaN

        val t2 = 60.0
        val result2 = MapProjectionUtils.normalize(t2, min, max)
        assertEquals(true, result2.isInfinite() && result2 > 0) // Expect Infinity for (60-50)/(50-50) -> 10/0 = Infinity
    }
}
