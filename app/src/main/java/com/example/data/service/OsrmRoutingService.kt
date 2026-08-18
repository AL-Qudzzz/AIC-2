package com.example.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class GeocodedLocation(
    val displayName: String,
    val point: GeoPoint
)

data class TripResult(
    val routePoints: List<GeoPoint>,
    val orderedWaypoints: List<GeoPoint>,
    val totalDistanceKm: Double,
    val totalDurationMin: Double
)

/**
 * Service to calculate driving routes using OSRM (Open Source Routing Machine) Public API
 * and geocode addresses using OpenStreetMap Nominatim API.
 */
class OsrmRoutingService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    /**
     * Search address via Nominatim API targeting Indonesia (countrycodes=id)
     * and prioritizing the Java Island corridor (viewbox=105.0,-5.5,114.5,-8.8).
     */
    suspend fun searchAddressNominatim(query: String): List<GeocodedLocation> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val trimmedQuery = query.trim()
        val encodedQuery = try {
            URLEncoder.encode(trimmedQuery, "UTF-8")
        } catch (e: Exception) {
            trimmedQuery
        }

        // Bounded to Indonesia (countrycodes=id) with Pulau Jawa bounding box bias (viewbox)
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&countrycodes=id&viewbox=105.0,-5.5,114.5,-8.8&bounded=0&limit=8&addressdetails=1&accept-language=id,en"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RouteWise-AI-App/1.0 (Indonesia Delivery; support@routewise.ai)")
                .header("Accept", "application/json")
                .header("Accept-Language", "id, en-US, en;q=0.9")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()

                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(responseBody)
                val results = mutableListOf<GeocodedLocation>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val lat = obj.optDouble("lat", 0.0)
                    val lon = obj.optDouble("lon", 0.0)
                    val displayName = obj.optString("display_name", trimmedQuery)

                    if (lat != 0.0 && lon != 0.0) {
                        results.add(GeocodedLocation(displayName, GeoPoint(lat, lon)))
                    }
                }
                return@withContext results
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    /**
     * OSRM Route API: Fetches road geometry for a given pre-sorted sequence of waypoints.
     */
    suspend fun fetchRouteForSequence(sortedWaypoints: List<GeoPoint>, profile: String = "driving"): TripResult? = withContext(Dispatchers.IO) {
        if (sortedWaypoints.size < 2) return@withContext null

        val coordinatesString = sortedWaypoints.joinToString(";") { point ->
            "${point.longitude},${point.latitude}"
        }

        val osrmProfile = if (profile.lowercase().contains("motor")) "driving" else profile.lowercase()
        val url = "https://router.project-osrm.org/route/v1/$osrmProfile/$coordinatesString?overview=full&geometries=geojson"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RouteWise-Android-App/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val responseBody = response.body?.string() ?: return@withContext null
                val json = JSONObject(responseBody)

                if (json.optString("code") != "Ok") return@withContext null

                val routes = json.optJSONArray("routes") ?: return@withContext null
                if (routes.length() == 0) return@withContext null

                val route0 = routes.getJSONObject(0)
                val geometry = route0.optJSONObject("geometry") ?: return@withContext null
                val coordinates = geometry.optJSONArray("coordinates") ?: return@withContext null

                val routePoints = mutableListOf<GeoPoint>()
                for (i in 0 until coordinates.length()) {
                    val coordPair = coordinates.getJSONArray(i)
                    val lon = coordPair.getDouble(0)
                    val lat = coordPair.getDouble(1)
                    routePoints.add(GeoPoint(lat, lon))
                }

                val totalDistMeters = route0.optDouble("distance", 0.0)
                val totalDurationSec = route0.optDouble("duration", 0.0)

                return@withContext TripResult(
                    routePoints = routePoints,
                    orderedWaypoints = sortedWaypoints,
                    totalDistanceKm = totalDistMeters / 1000.0,
                    totalDurationMin = totalDurationSec / 60.0
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    /**
     * OSRM Trip API: Solves Traveling Salesperson Problem (TSP) starting from waypoints[0] (Origin).
     */
    suspend fun fetchOptimizedTrip(waypoints: List<GeoPoint>, profile: String = "driving"): TripResult? = withContext(Dispatchers.IO) {
        val routeResult = fetchRouteForSequence(waypoints, profile)
        if (routeResult != null) return@withContext routeResult

        if (waypoints.size < 2) return@withContext null

        val coordinatesString = waypoints.joinToString(";") { point ->
            "${point.longitude},${point.latitude}"
        }

        val osrmProfile = if (profile.lowercase().contains("motor")) "driving" else profile.lowercase()
        val url = "https://router.project-osrm.org/trip/v1/$osrmProfile/$coordinatesString?overview=full&geometries=geojson&source=first"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RouteWise-Android-App/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val responseBody = response.body?.string() ?: return@withContext null
                val json = JSONObject(responseBody)

                if (json.optString("code") != "Ok") return@withContext null

                val trips = json.optJSONArray("trips") ?: return@withContext null
                if (trips.length() == 0) return@withContext null

                val trip0 = trips.getJSONObject(0)
                val geometry = trip0.optJSONObject("geometry") ?: return@withContext null
                val coordinates = geometry.optJSONArray("coordinates") ?: return@withContext null

                val routePoints = mutableListOf<GeoPoint>()
                for (i in 0 until coordinates.length()) {
                    val coordPair = coordinates.getJSONArray(i)
                    val lon = coordPair.getDouble(0)
                    val lat = coordPair.getDouble(1)
                    routePoints.add(GeoPoint(lat, lon))
                }

                val totalDistMeters = trip0.optDouble("distance", 0.0)
                val totalDurationSec = trip0.optDouble("duration", 0.0)

                return@withContext TripResult(
                    routePoints = routePoints,
                    orderedWaypoints = waypoints,
                    totalDistanceKm = totalDistMeters / 1000.0,
                    totalDurationMin = totalDurationSec / 60.0
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    /**
     * Legacy fetchRoute fallback.
     */
    suspend fun fetchRoute(waypoints: List<GeoPoint>): List<GeoPoint> = withContext(Dispatchers.IO) {
        val trip = fetchOptimizedTrip(waypoints)
        return@withContext trip?.routePoints ?: waypoints
    }
}
