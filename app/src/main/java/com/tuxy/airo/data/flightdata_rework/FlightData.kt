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
import org.openapitools.client.models.FlightContract
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
    val scheduledDepartDate: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
    val scheduledArriveDate: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
    val revisedDepartDate: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
    val revisedArriveDate: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
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
    val mapOriginLat: Double = 1.0,
    val mapOriginLon: Double = 1.0,
    val mapDestinationLat: Double = 1.0,
    val mapDestinationLon: Double = 1.0,
    val attribution: String = "---"
) {
    internal fun parseDateTime(time: String?): ZonedDateTime {
        if (time == null) {
            return ZonedDateTime.now(ZoneOffset.UTC) // If time isn't available
        } // TODO Maybe add a notice that the time couldn't be received?
        val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmXXXXX")
        return ZonedDateTime.parse(time, pattern)
    }

    fun from(response: FlightContract): FlightData {

        val scheduledDepartDate = parseDateTime(response.departure.scheduledTime?.local)
        val scheduledArriveDate = parseDateTime(response.arrival.scheduledTime?.local)

        val revisedDepartDate = parseDateTime(response.departure.revisedTime?.local)
        val revisedArriveDate = parseDateTime(response.arrival.revisedTime?.local)

        return FlightData(
            lastUpdate = LocalDateTime.now(),
            callSign = response.number,

            // Airline Info (Safe handling of null airline object)
            airline = response.airline?.name ?: "N/A",
            airlineIcao = response.airline?.icao ?: "N/A",
            airlineIata = response.airline?.iata ?: "N/A",

            // Departure Info
            from = response.departure.airport.iata ?: "N/A",
            fromName = response.departure.airport.name,
            fromCountryCode = response.departure.airport.countryCode ?: "---",
            scheduledDepartDate = scheduledDepartDate,
            revisedDepartDate = revisedDepartDate,

            // Arrival Info
            to = response.arrival.airport.iata ?: "N/A",
            toName = response.arrival.airport.name,
            toCountryCode = response.arrival.airport.countryCode ?: "---",
            scheduledArriveDate = scheduledArriveDate,
            revisedArriveDate = revisedArriveDate,

            // Airport Details
            gate = response.departure.gate ?: "—",
            terminal = response.departure.terminal ?: "—",
            checkInDesk = response.departure.checkInDesk ?: "—",
            toGate = response.arrival.gate ?: "—",
            toTerminal = response.arrival.terminal ?: "—",
            toBaggageClaim = response.arrival.baggageBelt ?: "—",

            // Aircraft Info
            aircraftName = response.aircraft?.model ?: "N/A",
            aircraftUri = response.aircraft?.image?.url ?: "",
            author = response.aircraft?.image?.author ?: "",
            authorUri = response.aircraft?.image?.webUrl ?: "",
            attribution = response.aircraft?.image?.htmlAttributions?.firstOrNull() ?: "",

            // Location / Map
            mapOriginLat = response.departure.airport.location?.lat?.toDouble() ?: 0.0, // TODO implement new map system
            mapOriginLon = response.departure.airport.location?.lon?.toDouble() ?: 0.0,
            mapDestinationLat = response.arrival.airport.location?.lat?.toDouble() ?: 0.0,
            mapDestinationLon = response.arrival.airport.location?.lon?.toDouble() ?: 0.0,

            ticketData = "", // Keeping your default
            duration = Duration.between(
                scheduledDepartDate,
                scheduledArriveDate
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