package com.tuxy.airo.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity(tableName = "flight_table")
data class FlightData(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val from: String,
    val to: String,
    val fromName: String,
    val localDepartDate: String,
    val localDepartTime: String,
    val localArriveDate: String,
    val localArriveTime: String,
    val toName: String,
    val ticketSeat: String,
    val ticketData: String,
    val ticketQr: String,
    val ticketGate: String,
    val ticketTerminal: String,
    val aircraftIcao: String,
    val aircraftName: String,
    val aircraftUri: String,
    val mapOriginLat: Double,
    val mapOriginLong: Double,
    val mapDestinationLat: Double,
    val mapDestinationLong: Double,
    val progress: Int,
)

@Dao
interface FlightDataDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFlight(flightData: FlightData)

    @Delete
    suspend fun deleteFlight(flightData: FlightData)

    @Update
    suspend fun updateFlight(flightData: FlightData)

    @Query("SELECT * FROM flight_table ORDER BY id ASC")
    fun readAll(): LiveData<List<FlightData>>

    @Query("DELETE FROM flight_table") // ONLY FOR DEVELOPMENT
    fun nukeTable()
}


@Database(entities = [FlightData::class], version = 1, exportSchema = false)
abstract class FlightDataBase: RoomDatabase() {
    abstract fun flightDataDao(): FlightDataDao

    companion object{
        @Volatile
        private var Instance: FlightDataBase? = null

        fun getDatabase(context: Context): FlightDataBase { // Gets database, creates if doesn't exist
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, FlightDataBase::class.java, "flight_database")
                    .build().also { Instance = it }
            }
        }
    }
}
