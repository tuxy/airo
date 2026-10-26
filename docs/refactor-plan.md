# Airo refactor — phases 0–2

Branch: `refactor/phase-0-2`
Status: landed (phase 0 + core of phases 1–2)
Verification: `testDebugUnitTest` (26 tests), `assembleDebug`, `assembleRelease` (R8 + resource shrinking).
No device/emulator was available, so runtime behaviour is build-verified, not UI-tested.

## Goal

Make the existing features truthful and observable, replace the ad-hoc
plumbing with a small clean architecture, and land the cheapest, highest-impact
performance and feature work.

## Landed

### Correctness and shipping

- [x] Typed `AppPreferences`; `update_interval` is read from the same Float key
      the slider writes (default 48 h, clamped 1–168 h).
- [x] `ApiConfig` + `FlightApi`; add-flight, details refresh and the background
      worker all use the configured server and key.
- [x] Network hardening: `use {}`, error bodies parsed, typed `ApiResult`,
      singleton Moshi, OkHttp cache, bounded retry/backoff, `HttpUrl` building,
      `withFlightPlan` no longer requested.
- [x] One `FlightRepository` with per-flight error isolation, 4-way bounded
      parallelism, transactional updates, duplicate/clash handling and change
      detection.
- [x] WorkManager only: unique periodic refresh, unique one-time refresh on
      network/startup, reminder rescheduling on boot. Foreground service and
      AlarmManager refresh removed (Android 12+ FGS crash gone).
- [x] Reminders keyed by database id, exact alarm with WorkManager fallback,
      scheduled on add/refresh, cancelled on delete.
- [x] Add-flight returns a result; the screen pops only on success and reports
      duplicates/server/network errors via a Snackbar.
- [x] Room v4 migration (live/status columns), `exportSchema=true`, committed
      schema, WAL (TRUNCATE removed), no destructive fallback.
- [x] Release: R8 + resource shrinking, release ABI filters (arm64/armv7),
      release signing from `RELEASE_*` env vars with local debug fallback,
      `versionCode`/`versionName` from Gradle properties.
- [x] Removed unused dependencies (Klaxon, MapCompose, CameraX, unused
      Navigation artifacts, test monitor as implementation).
- [x] No cleartext traffic; `allowBackup=false`; unused MapLibre location/Wi-Fi
      permissions removed; `FLAG_SECURE` on the ticket screen; WorkManager
      on-demand initializer only.

### Plumbing

- [x] Hand-written `AppContainer` (no Context/DAO in ViewModels; repository and
      preferences injected through factories).
- [x] `FlightMapper` extracted from the Room entity.
- [x] `FlightRepository` is the single data entry point.
- [x] Typed preferences replace `PreferencesInterface`; Compose widgets keep
      writing the same keys.
- [x] Unit tests: mapper/date parsing, API success/401/204/malformed/config,
      add-flight state, progress/zone/delete, grouping, best-flight selection.
- [x] CI: tests + lint in the build workflow, release version wiring,
      concurrency/permissions/timeouts, airline PR permissions.

### UI/performance/features

- [x] `LazyColumn` flight list; one `timeFormat` collector for the list and
      remembered formatters; stable card parameters.
- [x] Map lifecycle: `MapView.onDestroy()` on release, style-loaded signal
      instead of a 200 ms delay, annotations redrawn when flight data changes,
      live aircraft marker.
- [x] Details: no `ColorScheme`/settings collectors in composition, typed
      `timeFormat`, lifecycle-aware 60 s tick that actually refreshes the
      status card.
- [x] Live status + position fields mapped from `status`/`location`; status
      card prefers the API status and falls back to times.
- [x] Smart change alerts for status/gate/terminal/departure/arrival changes.
- [x] `collectAsStateWithLifecycle` on the screens that outlive composition.

## Deferred (next branch)

- Full domain model split (`FlightEntity` / `Flight` / DTO) and immutable
  `UiState` per screen.
- Hilt/Koin, Retrofit/kotlinx.serialization migration.
- Paging 3, baseline profile/macrobenchmark module.
- Feature roadmap: FIDS board, calendar/.ics, trip grouping, stats, widget,
  offline airport DB, accessibility/localization pass.
- Release keystore must be supplied by the repository owner; CI falls back to
  the debug key when the `RELEASE_*` secrets are absent.

## Architecture

```
MainApplication
 └─ AppContainer
     ├─ AppPreferences (DataStore, typed)
     ├─ FlightDatabase / FlightDataDao (Room, WAL, migration 3→4)
     ├─ FlightApi (OkHttp + Moshi, typed ApiResult)
     ├─ FlightRepository (DAO + API + mapper + reminders)
     └─ ReminderScheduler (AlarmManager / WorkManager)

UI (Compose) → ViewModel (StateFlow) → repository / use case → WorkManager
```

## Verification notes

The sandbox cannot write `~/.gradle` or `~/.local/share/kotlin`, so builds use
a workspace-local Gradle home, the host dependency cache, and a workspace
Android user home:

```
GRADLE_USER_HOME=$PWD/.gradle-home \
GRADLE_RO_DEP_CACHE=$HOME/.gradle/caches \
ANDROID_USER_HOME=$PWD/.android-home \
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

The Kotlin compiler daemon falls back to in-process compilation in this
sandbox (read-only `~/.local/share/kotlin`); CI is unaffected.
