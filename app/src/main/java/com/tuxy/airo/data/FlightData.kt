package com.tuxy.airo.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "flight_table")
data class FlightData(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val from: String,
    val to: String,
    val ticketSeat: String,
    val ticketData: String,
    val ticketQr: String,
    val aircraftIcao: String,
    val aircraftName: String,
    val aircraftUri: String,
    val mapOrigin: String,
    val mapDestination: String,
    val progress: Int,
)

@Database(entities = [FlightData::class], version = 1, exportSchema = true)
abstract class FlightDataBase: RoomDatabase() {
    abstract fun flightDataDao(): FlightDataDao

    companion object{
        @Volatile
        private var INSTANCE: FlightDataBase? = null

        fun getDataBase(context: Context): FlightDataBase { // Gets database, creates if doesn't exist
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context = context.applicationContext,
                    FlightDataBase::class.java,
                    "flight_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

class FlightRepository(private val flightDataDao: FlightDataDao) {
    val readAllData: LiveData<List<FlightData>> = flightDataDao.readAll()

    suspend fun addFlight(flightData: FlightData) {
        flightDataDao.addFlight(flightData)
    }
}

class FlightViewModel(application: Application): AndroidViewModel(application) {
    private val readAllData: LiveData<List<FlightData>>
    private val repository: FlightRepository

    init {
        val flightDataDao = FlightDataBase.getDataBase(application).flightDataDao()

        repository = FlightRepository(flightDataDao)
        readAllData = repository.readAllData
    }

    fun addFlight(flightData: FlightData) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addFlight(flightData)
        }
    }
}