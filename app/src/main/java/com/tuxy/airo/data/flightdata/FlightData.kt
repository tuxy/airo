package com.tuxy.airo.data.flightdata

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tuxy.airo.data.database.Converters
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Persisted flight.
 *
 * This is still the Room entity and is used directly by the UI (a separate
 * domain model is a later refactor), but it no longer knows how to map itself
 * from the API contract — see `FlightMapper`. New fields are nullable where the
 * API can legitimately omit them.
 */
@Entity(
    tableName = "flight_table",
    indices = [Index(value = ["callSign", "scheduledDepartDate"], unique = true)]
)
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
    val revisedDepartDate: ZonedDateTime? = null,
    val revisedArriveDate: ZonedDateTime? = null,
    val duration: Duration = Duration.ofSeconds(1),
    val toName: String = "---",
    val ticketData: String = "",
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
    val attribution: String = "---",

    // --- Live status / position (schema v4) -------------------------------
    @ColumnInfo(defaultValue = "''")
    val status: String = "Unknown",
    val lastUpdatedUtc: String? = null,
    val departureRunwayTime: ZonedDateTime? = null,
    val arrivalRunwayTime: ZonedDateTime? = null,
    val departurePredictedTime: ZonedDateTime? = null,
    val arrivalPredictedTime: ZonedDateTime? = null,
    val liveLat: Double? = null,
    val liveLon: Double? = null,
    val liveAltitudeM: Double? = null,
    val liveGroundSpeedKmh: Double? = null,
    val liveTrackDeg: Double? = null,
    val liveReportedAtUtc: String? = null,
    val greatCircleDistanceKm: Double? = null,
    val aircraftReg: String? = null,
    val fromTimeZoneId: String? = null,
    val toTimeZoneId: String? = null,
    @ColumnInfo(defaultValue = "0")
    val hasLocation: Boolean = false,
) {
    /** Estimated/actual departure when the API revised it, otherwise scheduled. */
    val effectiveDepartDate: ZonedDateTime
        get() = revisedDepartDate ?: scheduledDepartDate

    /** Estimated/actual arrival when the API revised it, otherwise scheduled. */
    val effectiveArriveDate: ZonedDateTime
        get() = revisedArriveDate ?: scheduledArriveDate

    /** Duration between the effective times, falling back to the stored value. */
    val effectiveDuration: Duration
        get() = Duration.between(effectiveDepartDate, effectiveArriveDate)
            .takeUnless { it.isZero || it.isNegative }
            ?: duration

    val hasLivePosition: Boolean
        get() = liveLat != null && liveLon != null

    val hasKnownStatus: Boolean
        get() = status.isNotBlank() && !status.equals("Unknown", ignoreCase = true)
}

@Dao
interface FlightDataDao {
    /** Returns the new row id, or -1 when the unique index rejected the insert. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFlight(flightData: FlightData): Long

    @Delete
    suspend fun deleteFlight(flightData: FlightData)

    @Update
    suspend fun updateFlight(flightData: FlightData)

    @Update
    suspend fun updateFlights(flights: List<FlightData>)

    @Query("SELECT * FROM flight_table ORDER BY id ASC")
    fun readAll(): Flow<List<FlightData>>

    @Query("SELECT * FROM flight_table WHERE id = :id")
    suspend fun readSingle(id: Int): FlightData?

    @Query("SELECT * FROM flight_table WHERE id = :id")
    fun observeSingle(id: Int): Flow<FlightData?>

    @Query(
        "SELECT * FROM flight_table WHERE callSign = :callSign " +
            "AND scheduledDepartDate = :scheduledDepartDate LIMIT 1"
    )
    suspend fun findByKey(
        callSign: String,
        scheduledDepartDate: ZonedDateTime,
    ): FlightData?

    @Query(
        "SELECT COUNT() FROM flight_table WHERE scheduledDepartDate = :scheduledDepartDate " +
            "AND callSign = :callSign"
    )
    suspend fun countExisting(
        scheduledDepartDate: ZonedDateTime,
        callSign: String,
    ): Int
}

@Database(entities = [FlightData::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class FlightDataBase : RoomDatabase() {
    abstract fun flightDataDao(): FlightDataDao

    companion object {
        @Volatile
        private var Instance: FlightDataBase? = null

        /**
         * Adds the live-status columns introduced in schema v4. Every new column
         * is nullable or has a database default, so this is a plain ALTER and
         * existing rows — including ticket barcodes — are preserved.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE flight_table ADD COLUMN status TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL("ALTER TABLE flight_table ADD COLUMN lastUpdatedUtc TEXT")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN departureRunwayTime TEXT")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN arrivalRunwayTime TEXT")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN departurePredictedTime TEXT")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN arrivalPredictedTime TEXT")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN liveLat REAL")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN liveLon REAL")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN liveAltitudeM REAL")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN liveGroundSpeedKmh REAL")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN liveTrackDeg REAL")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN liveReportedAtUtc TEXT")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN greatCircleDistanceKm REAL")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN aircraftReg TEXT")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN fromTimeZoneId TEXT")
                db.execSQL("ALTER TABLE flight_table ADD COLUMN toTimeZoneId TEXT")
                db.execSQL(
                    "ALTER TABLE flight_table ADD COLUMN hasLocation INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context): FlightDataBase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, FlightDataBase::class.java, "flight_database")
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
