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
import androidx.room.Update
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Entity(tableName = "flight_table")
data class FlightData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val from: String = "",
    val to: String = "",
    val fromName: String = "",
    val localDepartDate: String = "",
    val localDepartTime: String = "",
    val localArriveDate: String = "",
    val localArriveTime: String = "",
    val toName: String = "",
    val ticketSeat: String = "",
    val ticketData: String = "",
    val ticketQr: String = "",
    val ticketGate: String = "",
    val ticketTerminal: String = "",
    val aircraftIcao: String = "",
    val aircraftName: String = "",
    val aircraftUri: String = "",
    val mapOriginLat: Double = 0.0,
    val mapOriginLong: Double = 0.0,
    val mapDestinationLat: Double = 0.0,
    val mapDestinationLong: Double = 0.0,
    val progress: Int = 0,
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

@OptIn(DelicateCoroutinesApi::class)
fun dataIntoMut(flightList: MutableState<List<FlightData>>, flightDataDao: FlightDataDao) {
    GlobalScope.launch {
        flightList.value = flightDataDao.readAll()
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun singleIntoMut(flightData: MutableState<FlightData>, flightDataDao: FlightDataDao, id: String) {
    GlobalScope.launch {
        flightData.value = flightDataDao.readSingle(id)
    }
}
