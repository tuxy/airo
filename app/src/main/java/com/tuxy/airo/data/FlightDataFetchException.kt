package com.tuxy.airo.data

/**
 * Custom exception to wrap [FlightDataError] types when using [kotlin.Result] for [getData].
 * This allows structured error types to be propagated through the standard Result failure channel.
 *
 * @property errorType The specific [FlightDataError] that occurred.
 */
class FlightDataFetchException(val errorType: FlightDataError) :
    Throwable("Flight data fetch failed: ${errorType::class.simpleName}")
