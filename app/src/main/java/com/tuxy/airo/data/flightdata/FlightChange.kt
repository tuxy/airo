package com.tuxy.airo.data.flightdata

/**
 * One flight's user-relevant changes found during a refresh.
 *
 * [changes] contains lowercase fragments such as "gate from A1 to B2" or
 * "departure time from 22:35 to 23:10"; [message] joins them into a readable
 * sentence so one flight always produces one notification.
 */
data class FlightChange(
    val flightId: Int,
    val callSign: String,
    val changes: List<String>,
) {
    val title: String
        get() = "$callSign has changed"

    val message: String
        get() = changes.toNaturalList()

    private fun List<String>.toNaturalList(): String = when (size) {
        0 -> ""
        1 -> first()
        2 -> "${this[0]}, and ${this[1]}"
        else -> dropLast(1).joinToString(", ") + ", and " + last()
    }
}
