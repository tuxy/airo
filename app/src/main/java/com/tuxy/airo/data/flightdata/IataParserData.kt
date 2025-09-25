package com.tuxy.airo.data.flightdata

import android.content.Context
import com.tuxy.airo.R
import java.time.LocalDate

data class IataParserData(
    val passengerName: String = "",
    val eTicketIndicator: Boolean = true,
    val bookingReference: String = "",
    val fromIata: String = "",
    val toIata: String = "",
    val carrier: String = "",
    val flightNumber: String = "",
    val date: LocalDate = LocalDate.now(),
    val flightClass: String = "",
    val seat: String = "",
) {
    /**
     * Parses the IATA barcode string and extracts relevant flight information.
     *
     * This function takes a raw barcode string, typically scanned from a boarding pass,
     * and attempts to parse it according to the IATA standard format.
     * It extracts information such as passenger name, e-ticket indicator, booking reference,
     * origin and destination IATA codes, carrier code, flight number, date, flight class, and seat number.
     *
     * If the parsing is successful, it returns an [IataParserData] object populated with the extracted data.
     * If any error occurs during parsing (e.g., the barcode string is malformed or does not adhere to the expected format),
     * it catches the exception and returns a default [IataParserData] object with empty or default values.
     *
     * @param barcode The raw barcode string to be parsed.
     * @param context The Android [android.content.Context] used to access resources, such as localized strings for flight classes.
     * @return An [IataParserData] object containing the parsed flight information.
     *         Returns a default [IataParserData] object if parsing fails.
     */
    fun parseData(barcode: String, context: Context): IataParserData {
        try {
            return IataParserData(
                passengerName = barcode.substring(2, 21),
                eTicketIndicator = getETicketIndication(barcode.substring(22, 23)),
                bookingReference = barcode.substring(23, 29),
                fromIata = barcode.substring(30, 33),
                toIata = barcode.substring(33, 36),
                carrier = barcode.substring(36, 38),
                flightNumber = barcode.substring(39, 43),
                date = getDay(barcode.substring(44, 47)),
                flightClass = getClass(barcode.substring(47, 48).toCharArray()[0], context),
                seat = barcode.substring(48, 52),
            )
        } catch (_: Exception) {
            return IataParserData()
        }
    }

    private fun getETicketIndication(indicator: String): Boolean {
        return indicator == "E" || indicator == "e" // If ETicket indicator shows the character E, then that means that the ticket supports electronic (i think)
    }

    private fun getDay(string: String): LocalDate {
        return LocalDate.ofYearDay(
            2025,
            string.toInt()
        ) // Year doesn't really matter, it's the day that does
    }

    fun getClass(char: Char, context: Context): String {
        val classes = arrayOf(
            arrayOf('A', 'F', 'P'), // First Class
            arrayOf('C', 'D', 'I', 'J', 'Z'), // Business class
            arrayOf('E', 'W'), // Premium Economy
            arrayOf('B', 'H', 'K', 'L', 'M', 'N', 'Q', 'S', 'T', 'V', 'X', 'Y'), // Economy
        )
        return when (char) {
            in classes[0] -> context.resources.getString(R.string.first_class)
            in classes[1] -> context.resources.getString(R.string.business_class)
            in classes[2] -> context.resources.getString(R.string.premium_class)
            in classes[3] -> context.resources.getString(R.string.economy_class)
            else -> context.resources.getString(R.string.other)
        }
    }
}