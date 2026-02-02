package com.tuxy.airo.data.flightdata_rework

import android.content.Context
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
import com.tuxy.airo.data.database.Converters
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.String

@Entity(tableName = "flight_table")
data class FlightData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
    val callSign: String = "---",
    val airline: String = "---",
    val airlineIcao: String = "---",
    val airlineIata: String = "---",
    val from: String = "---",
    val to: String = "---",
    val fromCountryCode: String = "---",
    val toCountryCode: String = "---",
    val fromName: String = "---",
    val departDate: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
    val arriveDate: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
    val duration: Duration = Duration.ofSeconds(1), // Prevents current = NaN
    val toName: String = "---",
    var ticketData: String = "",
    val gate: String = "---",
    val toGate: String = "---",
val terminal: String = "---",
    val toTerminal: String = "---",
    val toBaggageClaim: String = "---",
    val checkInDesk: String = "---",
    val aircraftName: String = "---",
    val aircraftUri: String = "---",
    val author: String = "---",
    val authorUri: String = "---",
    val attribution: String = "---"
) {
    internal fun parseDateTime(time: String?): ZonedDateTime {
        if (time == null) {
            return ZonedDateTime.now(ZoneOffset.UTC) // If time isn't available
        } // TODO Maybe add a notice that the time couldn't be received?
        val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmXXXXX")
        return ZonedDateTime.parse(time, pattern)
    }

    fun from(response: List<FlightContract>): FlightData? {
        val flight = response.firstOrNull() ?: return null

        val departDate = parseDateTime(flight.departure.scheduledTime?.local)
        val arriveDate = parseDateTime(flight.arrival.scheduledTime?.local)


        return FlightData(
            lastUpdate = LocalDateTime.now(),
            callSign = flight.number,

            // Airline Info (Safe handling of null airline object)
            airline = flight.airline?.name ?: "N/A",
            airlineIcao = flight.airline?.icao ?: "N/A",
            airlineIata = flight.airline?.iata ?: "N/A",

            // Departure Info
            from = flight.departure.airport.iata ?: "N/A",
            fromName = flight.departure.airport.name,
            fromCountryCode = flight.departure.airport.countryCode ?: "---",
            departDate = departDate,

            // Arrival Info
            to = flight.arrival.airport.iata ?: "N/A",
            toName = flight.arrival.airport.name,
            toCountryCode = flight.arrival.airport.countryCode ?: "---",
            arriveDate = arriveDate,

            // Airport Details
            gate = flight.departure.gate ?: "—",
            terminal = flight.departure.terminal ?: "—",
            checkInDesk = flight.departure.checkInDesk ?: "—",
            toGate = flight.arrival.gate ?: "—",
            toTerminal = flight.arrival.terminal ?: "—",
            toBaggageClaim = flight.arrival.baggageBelt ?: "—",

            // Aircraft Info
            aircraftName = flight.aircraft?.model ?: "N/A",
            aircraftUri = flight.aircraft?.image?.url ?: "",
            author = flight.aircraft?.image?.author ?: "",
            authorUri = flight.aircraft?.image?.webUrl ?: "",
            attribution = flight.aircraft?.image?.htmlAttributions?.firstOrNull() ?: "",

            // Location / Map
            mapOriginLat = flight.departure.airport.location?.lat?.toDouble() ?: 0.0, // TODO implement new map system
            mapOriginLon = flight.departure.airport.location?.lon?.toDouble() ?: 0.0,
            mapDestinationLat = flight.arrival.airport.location?.lat?.toDouble() ?: 0.0,
            mapDestinationLon = flight.arrival.airport.location?.lon?.toDouble() ?: 0.0,

            ticketData = "", // Keeping your default
            duration = Duration.between(
                departDate,
                arriveDate
            )
        )
    }
}

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
    fun readSingle(id: String): FlightData?

    /**
     * Checks if a flight with the given departure date and call sign already exists in the database.
     *
     * @param departDate The departure date and time of the flight.
     * @param callSign The call sign of the flight.
     * @return The number of flights matching the criteria (0 or 1, as duplicates are ignored on insert).
     */
    @Query("SELECT COUNT() FROM flight_table WHERE departDate=:departDate AND callSign=:callSign")
    fun queryExisting(departDate: ZonedDateTime, callSign: String): Int

//    @Query("DELETE FROM flight_table") // ONLY FOR DEVELOPMENT
//    fun nukeTable()
}


@Database(entities = [FlightData::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FlightDataBase : RoomDatabase() {
    abstract fun flightDataDao(): FlightDataDao

    companion object {
        @Volatile
        private var Instance: FlightDataBase? = null

        fun getDatabase(context: Context): FlightDataBase { // Gets database, creates if doesn't exist
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, FlightDataBase::class.java, "flight_database")
                    .setJournalMode(JournalMode.TRUNCATE)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}