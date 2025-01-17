package com.tuxy.airo.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FlightDataDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFlight(flight: FlightData)

    @Query("SELECT * FROM flight_table ORDER BY id ASC")
    fun readAll(): LiveData<List<FlightData>>
}