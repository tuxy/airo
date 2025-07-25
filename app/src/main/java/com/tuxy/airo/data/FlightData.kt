package com.tuxy.airo.data

import android.content.Context
import androidx.compose.runtime.MutableState
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
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@Entity(tableName = "flight_table")
data class FlightData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
    val callSign: String = "",
    val airline: String = "",
    val airlineIcao: String = "",
    val airlineIata: String = "",
    val from: String = "",
    val to: String = "",
    val fromCountryCode: String = "",
    val toCountryCode: String = "",
    val fromName: String = "",
    val departDate: LocalDateTime = LocalDateTime.now(),
    val arriveDate: LocalDateTime = LocalDateTime.now(),
    val departTimeZone: ZoneId = ZoneOffset.UTC,
    val arriveTimeZone: ZoneId = ZoneOffset.UTC,
    val duration: Duration = Duration.ZERO,
    val toName: String = "",
    var ticketData: String = "",
    val gate: String = "",
    val toGate: String = "",
    val terminal: String = "",
    val toTerminal: String = "",
    val toBaggageClaim: String = "",
    val checkInDesk: String = "",
    val aircraftName: String = "",
    val aircraftUri: String = "",
    val author: String = "",
    val authorUri: String = "",
    val mapOriginX: Double = 0.0,
    val mapOriginY: Double = 0.0,
    val mapDestinationX: Double = 0.0,
    val mapDestinationY: Double = 0.0,
    val attribution: String = ""
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
    fun readAll(): List<FlightData>

    @Query("SELECT * FROM flight_table WHERE id=:id ")
    fun readSingle(id: String): FlightData

    @Query("SELECT COUNT() FROM flight_table WHERE departDate=:departDate AND callSign=:callSign")
    fun queryExisting(departDate: LocalDateTime, callSign: String): Int

    @Query("DELETE FROM flight_table") // ONLY FOR DEVELOPMENT
    fun nukeTable()
}


@Database(entities = [FlightData::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FlightDataBase : RoomDatabase() {
    abstract fun flightDataDao(): FlightDataDao

    companion object {
        @Volatile
        private var Instance: FlightDataBase? = null

        fun getDatabase(context: Context): FlightDataBase { // Gets database, creates if doesn't exist
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, FlightDataBase::class.java, "flight_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun singleIntoMut(
    flightData: MutableState<FlightData>,
    flightDataDao: FlightDataDao,
    id: String
): Job {
    val job = GlobalScope.launch {
        flightData.value = flightDataDao.readSingle(id)
    }
    return job
}
