package com.tuxy.airo.data

/**
 * Minimal but complete AeroDataBox payload used by the mapper and API tests.
 * It contains the live/status fields the old app fetched and discarded.
 */
const val FULL_FLIGHT_JSON: String = """
{
  "number": "UA1",
  "status": "EnRoute",
  "codeshareStatus": "IsOperator",
  "isCargo": false,
  "lastUpdatedUtc": "2026-01-01T12:00:00Z",
  "departure": {
    "airport": {
      "name": "Newark Liberty International",
      "shortName": "Newark",
      "iata": "EWR",
      "icao": "KEWR",
      "countryCode": "US",
      "timeZone": "America/New_York",
      "location": { "lat": 40.69, "lon": -74.17 }
    },
    "quality": ["Basic"],
    "scheduledTime": { "utc": "2026-01-01T12:00:00Z", "local": "2026-01-01 07:00-05:00" },
    "revisedTime": { "utc": "2026-01-01T12:30:00Z", "local": "2026-01-01 07:30-05:00" },
    "terminal": "A",
    "gate": "A1",
    "checkInDesk": "1-4"
  },
  "arrival": {
    "airport": {
      "name": "Los Angeles International",
      "shortName": "Los Angeles",
      "iata": "LAX",
      "icao": "KLAX",
      "countryCode": "US",
      "timeZone": "America/Los_Angeles",
      "location": { "lat": 33.94, "lon": -118.40 }
    },
    "quality": ["Basic"],
    "scheduledTime": { "utc": "2026-01-01T17:00:00Z", "local": "2026-01-01 09:00-08:00" },
    "revisedTime": { "utc": "2026-01-01T17:30:00Z", "local": "2026-01-01 09:30-08:00" },
    "terminal": "B",
    "gate": "B2",
    "baggageBelt": "3"
  },
  "location": {
    "lat": 40.0,
    "lon": -100.0,
    "altitude": { "meter": 10000, "km": 10, "mile": 6.2, "nm": 5.4, "feet": 32808 },
    "pressureAltitude": { "meter": 10300, "km": 10.3, "mile": 6.4, "nm": 5.6, "feet": 33792 },
    "pressure": { "hPa": 250, "inHg": 7.4, "mmHg": 187 },
    "groundSpeed": { "kt": 486, "kmPerHour": 900, "miPerHour": 559, "meterPerSecond": 250 },
    "trueTrack": { "deg": 90, "rad": 1.57 },
    "reportedAtUtc": "2026-01-01T12:05:00Z"
  },
  "greatCircleDistance": {
    "meter": 1000000, "km": 1000, "mile": 621, "nm": 540, "feet": 3280840
  },
  "aircraft": { "reg": "N12345", "model": "Boeing 737-800" }
}
"""

/** A stub entry followed by a complete one; `bestFlight()` must choose the latter. */
val FLIGHT_LIST_JSON: String = """
[
  {
    "number": "UA1",
    "status": "Unknown",
    "codeshareStatus": "Unknown",
    "isCargo": false,
    "lastUpdatedUtc": "2026-01-01T12:00:00Z",
    "departure": {
      "airport": { "name": "Unknown" },
      "quality": ["Basic"]
    },
    "arrival": {
      "airport": { "name": "Unknown" },
      "quality": ["Basic"]
    }
  },
  $FULL_FLIGHT_JSON
]
"""
