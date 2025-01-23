package com.tuxy.airo.data

import android.content.Context
import com.tuxy.airo.R
import java.time.LocalDate

data class IataParserData(
    val passengerName: String,
    val eTicketIndicator: Boolean,
    val bookingReference: String,
    val fromIata: String,
    val toIata: String,
    val carrier: String,
    val flightNumber: String,
    val date: LocalDate,
    val flightClass: String,
    val seat: String,
) {
    fun parseData(barcode: String, context: Context): IataParserData {
        return IataParserData(
            passengerName = barcode.substring(2, 21),
            eTicketIndicator = getETicketIndication(barcode.substring(22, 22)),
            bookingReference = barcode.substring(23, 29),
            fromIata = barcode.substring(30, 32),
            toIata = barcode.substring(33, 35),
            carrier = barcode.substring(36, 38),
            flightNumber = barcode.substring(39, 43),
            date = getDay(barcode.substring(44, 46)),
            flightClass = getClass(barcode.substring(47, 47).toCharArray()[0], context),
            seat = barcode.substring(48, 51),
        )
    }

    private fun getETicketIndication(indicator: String): Boolean {
        return indicator == "E" || indicator == "e" // If ETicket indicator shows the character E, then that means that the ticket supports electronic (i think)
    }

    private fun getDay(string: String): LocalDate {
        return LocalDate.ofYearDay(2025, string.toInt()) // Year doesn't really matter, it's the day that does
    }

    private fun getClass(char: Char, context: Context): String {
        val classes = arrayOf(
            arrayOf('A', 'F', 'P'), // First Class
            arrayOf('C', 'D', 'I', 'J', 'Z'), // Business class
            arrayOf('E', 'W'), // Premium Economy
            arrayOf('B', 'H', 'K', 'L', 'M', 'N', 'Q', 'S', 'T', 'V', 'X', 'Y'), // Economy
        )
        return when(char) {
            in classes[0] -> context.resources.getString(R.string.first_class)
            in classes[1] -> context.resources.getString(R.string.business_class)
            in classes[2] -> context.resources.getString(R.string.premium_class)
            in classes[3] -> context.resources.getString(R.string.economy_class)
            else -> context.resources.getString(R.string.other)
        }
    }
}
