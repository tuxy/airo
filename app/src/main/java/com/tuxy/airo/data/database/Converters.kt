package com.tuxy.airo.data.database

import androidx.room.TypeConverter
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class Converters {
    @TypeConverter
    fun convertDateToString(date: LocalDateTime): String {
        return date.toString()
    }

    @TypeConverter
    fun convertStringToDate(date: String): LocalDateTime {
        return LocalDateTime.parse(date)
    }

    @TypeConverter
    fun convertZonedDateToString(date: ZonedDateTime): String {
        return date.toString()
    }

    @TypeConverter
    fun convertZonedStringToDate(date: String): ZonedDateTime {
        return ZonedDateTime.parse(date)
    }

    @TypeConverter
    fun convertDurationToString(duration: Duration): Long {
        return duration.toMillis()
    }

    @TypeConverter
    fun convertStringToDuration(long: Long): Duration {
        return Duration.ofMillis(long)
    }

    @TypeConverter
    fun convertZoneToString(zoneId: ZoneId): String {
        return zoneId.toString()
    }

    @TypeConverter
    fun convertStringToZone(string: String): ZoneId {
        return ZoneId.of(string)
    }
}
