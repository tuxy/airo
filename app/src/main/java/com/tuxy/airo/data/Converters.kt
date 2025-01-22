package com.tuxy.airo.data

import androidx.room.TypeConverter
import java.time.Duration
import java.time.LocalDateTime

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
    fun convertDurationToString(duration: Duration): Long {
        return duration.toMillis()
    }

    @TypeConverter
    fun convertStringToDuration(long: Long): Duration {
        return Duration.ofMillis(long)
    }
}
