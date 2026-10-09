package com.tuxy.airo.composables

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import org.maplibre.android.geometry.LatLngBounds
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
    var isMapReady by remember { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapLibre.getInstance(ctx)
                val mapView = MapView(ctx)
                mapHolder = MapViewHolder(mapView = mapView)

                mapView.getMapAsync { map ->
                    map.setStyle(styleUrl) {
                        Handler(Looper.getMainLooper()).postDelayed({ isMapReady = true }, 200)
                    }
                    mapHolder = mapHolder?.copy(mapLibreMap = map, isStyleLoaded = true)
                    configureMapSettings(
                        map.uiSettings,
                        scrollEnabled,
                        zoomEnabled,
                        tiltEnabled,
                        rotateEnabled
                    )
                    pendingOnMapReady(map)
                }
                mapView
            }
        )

        AnimatedVisibility(!isMapReady, exit = fadeOut(tween(300))) {
            Box(Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }

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

    LaunchedEffect(
        cameraTarget,
        cameraZoom,
        scrollEnabled,
        zoomEnabled,
        tiltEnabled,
        rotateEnabled
    ) {
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
    if (lat.isNaN() || lon.isNaN()) return null
    val markerOptions = MarkerOptions()
        .position(LatLng(lat, lon))
        .title(code)
        .snippet(if (isOrigin) "Departure airport" else "Arrival airport")
    return addMarker(markerOptions)
}

/**
 * Moves the camera to fit the flight route in view.
 *
 * Rather than averaging the endpoint coordinates and guessing a zoom from the
 * angular distance (which ignores the map's aspect ratio and Mercator latitude
 * distortion, and can clip the route so only its middle is visible), this builds
 * a [LatLngBounds] from the actual great-circle arc and asks MapLibre for the
 * camera that fits those bounds with padding. MapLibre then accounts for the
 * real viewport size, so the whole route is always visible.
 *
 * @param originLat Latitude of departure airport
 * @param originLon Longitude of departure airport
 * @param destLat Latitude of arrival airport
 * @param destLon Longitude of arrival airport
 * @param zoom Optional explicit zoom level. If provided, the camera is centered
 *             on the route's bounds center at this fixed zoom instead of fitting
 * @param paddingPx Padding (in pixels) applied on every side when fitting the route
 */
fun MapLibreMap.centerOnRoute(
    originLat: Double,
    originLon: Double,
    destLat: Double,
    destLon: Double,
    zoom: Double? = null,
    paddingPx: Int = 64
) {
    if (originLat.isNaN() || originLon.isNaN() || destLat.isNaN() || destLon.isNaN()) return

    val points = greatCirclePoints(originLat, originLon, destLat, destLon)
    val bounds = routeBounds(points)

    if (zoom != null) {
        // Explicit zoom requested: just center on the route's bounds center.
        val center = bounds?.center ?: LatLng((originLat + destLat) / 2, (originLon + destLon) / 2)
        cameraPosition = CameraPosition.Builder()
            .target(center)
            .zoom(zoom)
            .build()
        return
    }

    // Fit the whole route using the viewport-aware camera for the bounds.
    val fitted = bounds?.let {
        getCameraForLatLngBounds(
            it,
            intArrayOf(paddingPx, paddingPx, paddingPx, paddingPx)
        )
    }

    if (fitted != null) {
        cameraPosition = fitted
    } else {
        // Fallback: previous heuristic if bounds/camera could not be computed.
        val centerLat = (originLat + destLat) / 2
        val centerLon = (originLon + destLon) / 2
        cameraPosition = CameraPosition.Builder()
            .target(LatLng(centerLat, centerLon))
            .zoom(calculateZoomForRoute(originLat, originLon, destLat, destLon))
            .build()
    }
}

/**
 * Builds a [LatLngBounds] that contains every point of a route.
 *
 * [greatCirclePoints] has already "unwrapped" the longitudes into a continuous,
 * monotonic sequence (each point within 180\u00b0 of the previous one) so that
 * antimeridian-crossing routes — e.g. SFO \u2192 SIN across the Pacific — form the
 * short arc instead of spanning the whole globe. As a result a naive min/max
 * (which is all [LatLngBounds.fromLatLngs] does) yields the correct span, with
 * longitudes possibly outside [-180, 180], which [LatLngBounds] supports.
 *
 * Returns null for empty input.
 */
private fun routeBounds(points: List<LatLng>): LatLngBounds? {
    if (points.isEmpty()) return null
    return LatLngBounds.fromLatLngs(points)
}

/**
 * Suggests a width:height aspect ratio for the map container so that a route's
 * bounds fit as snugly as possible.
 *
 * Long, mostly east-west routes (e.g. SFO \u2192 SIN across the Pacific) have a wide
 * bounds. Giving the map a shorter (more letterboxed) aspect ratio lets the whole
 * route fill the view, so the camera doesn't have to zoom out as far to keep the
 * route on screen and shows less area beyond it. Short routes keep the default
 * landscape ratio.
 *
 * Latitude is Mercator-corrected (~1/cos(lat)) so high-latitude arcs aren't
 * under-estimated. The result is clamped to [minRatio, maxRatio].
 *
 * @return a width:height ratio to feed into `Modifier.aspectRatio(...)`
 */
fun suggestedRouteAspectRatio(
    originLat: Double,
    originLon: Double,
    destLat: Double,
    destLon: Double,
    defaultRatio: Float = 1280f / 847f,
    minRatio: Float = 1280f / 847f,
    maxRatio: Float = 2.5f
): Float {
    if (originLat.isNaN() || originLon.isNaN() || destLat.isNaN() || destLon.isNaN()) {
        return defaultRatio
    }

    val bounds = routeBounds(greatCirclePoints(originLat, originLon, destLat, destLon))
        ?: return defaultRatio

    val lonSpan = bounds.longitudeSpan
    val latSpan = bounds.latitudeSpan
    if (lonSpan <= 0.0 || latSpan.isNaN()) return defaultRatio

    val midLat = Math.toRadians((bounds.latitudeNorth + bounds.latitudeSouth) / 2)
    // Mercator stretches vertical distance by ~1/cos(lat); approximate so the
    // ratio reflects what the map actually shows.
    val projectedLatSpan = latSpan / cos(midLat).coerceAtLeast(0.1)

    if (projectedLatSpan <= 0.0) return maxRatio

    val ratio = (lonSpan / projectedLatSpan).toFloat()
    return ratio.coerceIn(minRatio, maxRatio)
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
    if (lat1.isNaN() || lon1.isNaN() || lat2.isNaN() || lon2.isNaN()) return emptyList()
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

    // Spherical interpolation divides by sin(d). When the endpoints coincide
    // (d == 0) — e.g. a flight with no location data, where both airports default
    // to the same LatLng — this is 0/0 = NaN and would poison every point. There is
    // no arc to draw in that case, so return the single shared point instead.
    if (d.isNaN() || d < 1e-9) {
        return listOf(LatLng(lat1, lon1))
    }

    var prevLon = lon1
    for (i in 0..numPoints) {
        val f = i.toDouble() / numPoints
        val A = sin((1 - f) * d) / sin(d)
        val B = sin(f * d) / sin(d)
        val x = A * cos(φ1) * cos(λ1) + B * cos(φ2) * cos(λ2)
        val y = A * cos(φ1) * sin(λ1) + B * cos(φ2) * sin(λ2)
        val z = A * sin(φ1) + B * sin(φ2)
        val φ = Math.toDegrees(atan2(z, sqrt(x * x + y * y)))
        var lon = Math.toDegrees(atan2(y, x))

        // Guard against any residual non-finite values so LatLng never throws.
        if (φ.isNaN() || φ.isInfinite() || lon.isNaN() || lon.isInfinite()) continue

        // atan2 returns longitudes in [-180, 180], so an antimeridian-crossing
        // route (e.g. SFO -> SIN) would jump 179 -> -179 and MapLibre would draw
        // that segment the long way around the globe. Unwrap each point to stay
        // within 180 of the previous one, producing a continuous polyline.
        while (lon - prevLon > 180) lon -= 360
        while (lon - prevLon < -180) lon += 360
        prevLon = lon

        points.add(LatLng(φ, lon))
    }
    return points
}
