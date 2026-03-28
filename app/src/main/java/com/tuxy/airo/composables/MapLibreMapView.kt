package com.tuxy.airo.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.UiSettings
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/* I'll be honest, this entire composable and class was vibe-coded. 
 * I do not have the skills to work with Android views, nor do I want to.
 * The map does work really well though...
 * */

/**
 * Holds a reference to a MapView and its associated MapLibreMap instance.
 * This allows stable access to map resources across Compose recompositions.
 */
private data class MapViewHolder(
    val mapView: MapView,
    var mapLibreMap: MapLibreMap? = null,
    var isStyleLoaded: Boolean = false
)

/**
 * A Compose wrapper around MapLibre Native Android SDK's MapView.
 *
 * This composable provides a stable MapView instance that persists across
 * Compose recompositions, avoiding expensive map recreation. It handles:
 * - Lifecycle synchronization between Compose and Android View system
 * - Gesture configuration for static (non-movable) flight route display
 * - Optional camera positioning
 *
 * @param modifier Compose modifier for the map container
 * @param styleUrl URL to the map style JSON (default: OpenFreeMap Liberty style)
 * @param cameraTarget Geographic center point for the camera
 * @param cameraZoom Zoom level (1 = world view, higher = more zoomed in)
 * @param scrollEnabled Whether panning gestures are enabled
 * @param zoomEnabled Whether pinch-to-zoom gestures are enabled
 * @param tiltEnabled Whether tilt gestures are enabled
 * @param rotateEnabled Whether rotation gestures are enabled
 * @param onMapReady Callback fired when the map and style are loaded
 */
@Composable
fun MapLibreMapView(
    modifier: Modifier = Modifier,
    styleUrl: String = "https://tiles.openfreemap.org/styles/liberty",
    cameraTarget: LatLng? = null,
    cameraZoom: Double = 1.0,
    scrollEnabled: Boolean = false,
    zoomEnabled: Boolean = false,
    tiltEnabled: Boolean = false,
    rotateEnabled: Boolean = false,
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapHolder by remember {
        mutableStateOf<MapViewHolder?>(null)
    }

    var pendingOnMapReady by remember { mutableStateOf<(MapLibreMap) -> Unit>({}) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapLibre.getInstance(ctx)
            val mapView = MapView(ctx)
            mapHolder = MapViewHolder(mapView = mapView)

            mapView.getMapAsync { map ->
                map.setStyle(styleUrl)
                mapHolder = mapHolder?.copy(
                    mapLibreMap = map,
                    isStyleLoaded = true
                )
                configureMapSettings(
                    map.uiSettings,
                    scrollEnabled = scrollEnabled,
                    zoomEnabled = zoomEnabled,
                    tiltEnabled = tiltEnabled,
                    rotateEnabled = rotateEnabled
                )
                pendingOnMapReady(map)
            }

            mapView
        }
    )

    LaunchedEffect(Unit) {
        pendingOnMapReady = onMapReady
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapHolder?.mapView?.onStart()
                Lifecycle.Event.ON_RESUME -> mapHolder?.mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapHolder?.mapView?.onPause()
                Lifecycle.Event.ON_STOP -> mapHolder?.mapView?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapHolder?.mapView?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(cameraTarget, cameraZoom, scrollEnabled, zoomEnabled, tiltEnabled, rotateEnabled) {
        mapHolder?.mapLibreMap?.let { map ->
            configureMapSettings(
                map.uiSettings,
                scrollEnabled = scrollEnabled,
                zoomEnabled = zoomEnabled,
                tiltEnabled = tiltEnabled,
                rotateEnabled = rotateEnabled
            )
            cameraTarget?.let {
                map.cameraPosition = CameraPosition.Builder()
                    .target(it)
                    .zoom(cameraZoom)
                    .build()
            }
        }
    }
}

/**
 * Configures the map's gesture handling settings.
 *
 * @param uiSettings The UiSettings instance to configure
 * @param scrollEnabled Enable/disable panning gestures
 * @param zoomEnabled Enable/disable pinch-to-zoom gestures
 * @param tiltEnabled Enable/disable tilt gestures
 * @param rotateEnabled Enable/disable rotation gestures
 */
private fun configureMapSettings(
    uiSettings: UiSettings,
    scrollEnabled: Boolean,
    zoomEnabled: Boolean,
    tiltEnabled: Boolean,
    rotateEnabled: Boolean
) {
    uiSettings.apply {
        isScrollGesturesEnabled = scrollEnabled
        isZoomGesturesEnabled = zoomEnabled
        isTiltGesturesEnabled = tiltEnabled
        isRotateGesturesEnabled = rotateEnabled
        isDoubleTapGesturesEnabled = zoomEnabled
        isQuickZoomGesturesEnabled = zoomEnabled
    }
}

/**
 * Adds a great-circle flight route polyline to the map.
 *
 * Draws a curved line representing the shortest path between two points
 * on Earth's surface (great-circle distance), using 50 interpolated points.
 *
 * @param originLat Latitude of departure airport
 * @param originLon Longitude of departure airport
 * @param destLat Latitude of arrival airport
 * @param destLon Longitude of arrival airport
 * @param color Color of the route line (default: #2563EB blue)
 * @param width Width of the route line in pixels (default: 5f)
 * @return The added Polyline, or null if map is not ready
 */
fun MapLibreMap.addFlightRoute(
    originLat: Double,
    originLon: Double,
    destLat: Double,
    destLon: Double,
    color: Int = "#2563EB".toColorInt(),
    width: Float = 5f
): org.maplibre.android.annotations.Polyline? {
    val points = greatCirclePoints(originLat, originLon, destLat, destLon)
    if (points.isEmpty()) return null

    val polylineOptions = PolylineOptions()
        .addAll(points)
        .color(color)
        .width(width)
    return addPolyline(polylineOptions)
}

/**
 * Adds a pin marker at an airport location with an info window.
 *
 * The marker displays the airport code when tapped, showing either
 * "Departure airport" or "Arrival airport" as the description.
 *
 * @param lat Latitude of the airport
 * @param lon Longitude of the airport
 * @param code IATA/ICAO airport code (e.g., "SGN", "MEL")
 * @param isOrigin true for departure airport, false for arrival
 * @return The added Marker, or null if map is not ready
 */
fun MapLibreMap.addAirportMarker(
    lat: Double,
    lon: Double,
    code: String,
    isOrigin: Boolean
): org.maplibre.android.annotations.Marker? {
    val markerOptions = MarkerOptions()
        .position(LatLng(lat, lon))
        .title(code)
        .snippet(if (isOrigin) "Departure airport" else "Arrival airport")
    return addMarker(markerOptions)
}

/**
 * Moves the camera to center on the flight route, with zoom calculated
 * based on the distance between airports.
 *
 * @param originLat Latitude of departure airport
 * @param originLon Longitude of departure airport
 * @param destLat Latitude of arrival airport
 * @param destLon Longitude of arrival airport
 * @param zoom Optional explicit zoom level. If null, calculates automatically
 *             based on great-circle distance between points
 */
fun MapLibreMap.centerOnRoute(
    originLat: Double,
    originLon: Double,
    destLat: Double,
    destLon: Double,
    zoom: Double? = null
) {
    val centerLat = (originLat + destLat) / 2
    val centerLon = (originLon + destLon) / 2
    val calculatedZoom = zoom ?: calculateZoomForRoute(originLat, originLon, destLat, destLon)
    cameraPosition = CameraPosition.Builder()
        .target(LatLng(centerLat, centerLon))
        .zoom(calculatedZoom)
        .build()
}

/**
 * Calculates an appropriate zoom level for displaying a flight route.
 *
 * Uses the angular great-circle distance between the two airports to
 * determine a zoom level that shows the entire route with some padding.
 * Zoom levels range from 1 (very far, whole world) to 7 (very close, city-level).
 *
 * Formula: zoom = log2(180 / angularDistance) * 0.85, clamped to [1.0, 7.0]
 *
 * @param originLat Latitude of departure airport
 * @param originLon Longitude of departure airport
 * @param destLat Latitude of arrival airport
 * @param destLon Longitude of arrival airport
 * @return Calculated zoom level between 1.0 and 7.0
 */
fun calculateZoomForRoute(
    originLat: Double,
    originLon: Double,
    destLat: Double,
    destLon: Double
): Double {
    val angularDistance = calculateAngularDistance(originLat, originLon, destLat, destLon)
    return calculateZoomFromAngularDistance(angularDistance)
}

/**
 * Calculates the great-circle angular distance between two points on Earth
 * using the haversine formula.
 *
 * @param lat1 Latitude of first point in degrees
 * @param lon1 Longitude of first point in degrees
 * @param lat2 Latitude of second point in degrees
 * @param lon2 Longitude of second point in degrees
 * @return Angular distance in degrees (0 to 180)
 */
private fun calculateAngularDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val φ1 = Math.toRadians(lat1)
    val φ2 = Math.toRadians(lat2)
    val Δφ = Math.toRadians(lat2 - lat1)
    val Δλ = Math.toRadians(lon2 - lon1)

    val a = sin(Δφ / 2).pow(2) + cos(φ1) * cos(φ2) * sin(Δλ / 2).pow(2)
    val c = 2 * asin(sqrt(a))

    return Math.toDegrees(c)
}

/**
 * Converts an angular distance to a MapLibre zoom level.
 *
 * The mapping is based on: at zoom 1, the world is approximately 360 degrees
 * wide, so zoom = log2(360 / angularDistance). A multiplier of 0.85 provides
 * padding so routes don't fill the entire view.
 *
 * @param angularDistance Angular distance in degrees
 * @return Zoom level clamped to range [1.0, 7.0]
 */
private fun calculateZoomFromAngularDistance(angularDistance: Double): Double {
    if (angularDistance <= 0) return 3.0

    val baseZoom = kotlin.math.log2(180.0 / angularDistance)
    val adjustedZoom = baseZoom * 0.85

    return adjustedZoom.coerceIn(1.0, 7.0)
}

/**
 * Generates intermediate points along a great-circle arc between two airports.
 *
 * Implements great-circle navigation math to create a curved line that
 * represents the shortest path between two points on Earth's surface.
 * This appears as a natural flight path rather than a straight line.
 *
 * @param lat1 Latitude of first point in degrees
 * @param lon1 Longitude of first point in degrees
 * @param lat2 Latitude of second point in degrees
 * @param lon2 Longitude of second point in degrees
 * @param numPoints Number of intermediate points to generate (default: 50)
 * @return List of LatLng points along the great-circle arc
 */
private fun greatCirclePoints(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
    numPoints: Int = 50
): List<LatLng> {
    val points = mutableListOf<LatLng>()
    val φ1 = Math.toRadians(lat1)
    val λ1 = Math.toRadians(lon1)
    val φ2 = Math.toRadians(lat2)
    val λ2 = Math.toRadians(lon2)

    val d = 2 * asin(
        sqrt(
            sin((φ2 - φ1) / 2).pow(2) + cos(φ1) * cos(φ2) * sin((λ2 - λ1) / 2).pow(2)
        )
    )

    for (i in 0..numPoints) {
        val f = i.toDouble() / numPoints
        val A = sin((1 - f) * d) / sin(d)
        val B = sin(f * d) / sin(d)
        val x = A * cos(φ1) * cos(λ1) + B * cos(φ2) * cos(λ2)
        val y = A * cos(φ1) * sin(λ1) + B * cos(φ2) * sin(λ2)
        val z = A * sin(φ1) + B * sin(φ2)
        val φ = atan2(z, sqrt(x * x + y * y))
        val λ = atan2(y, x)
        points.add(LatLng(Math.toDegrees(φ), Math.toDegrees(λ)))
    }
    return points
}
