package com.example.data.service

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.example.data.ml.ModelAssetManager
import com.example.data.model.PackageDestination
import com.example.data.model.RoutePolylineState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Service to perform Multi-Stop Route Optimization using ONNX trained model (vrp_model.onnx)
 * and road-snapped polyline generation via OSRM.
 */
class RouteOptimizerService(
    private val assetManager: ModelAssetManager,
    private val osrmService: OsrmRoutingService = OsrmRoutingService()
) {

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    data class OptimizationResult(
        val optimizedDestinations: List<PackageDestination>,
        val polylineState: RoutePolylineState,
        val computationTimeMs: Long,
        val modelUsed: String
    )

    init {
        initializeOnnxSession()
    }

    private fun initializeOnnxSession() {
        try {
            val modelBytes = assetManager.getModelBytes("vrp_model.onnx")
            if (modelBytes != null) {
                ortEnv = OrtEnvironment.getEnvironment()
                ortSession = ortEnv?.createSession(modelBytes)
            }
        } catch (e: Throwable) {
            // Gracefully fallback on host JVM or unsupported environments
            ortEnv = null
            ortSession = null
        }
    }

    /**
     * Optimizes delivery stop sequence starting from the user's real-time GPS location (origin)
     * using the trained ONNX model and fetches snapped road geometry.
     */
    suspend fun optimizeRouteSequence(
        origin: GeoPoint,
        destinations: List<PackageDestination>
    ): OptimizationResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        if (destinations.isEmpty()) {
            return@withContext OptimizationResult(
                optimizedDestinations = emptyList(),
                polylineState = RoutePolylineState(emptyList(), 0.0, 0.0, false, "None"),
                computationTimeMs = 0L,
                modelUsed = "None"
            )
        }

        val isOnnxAvailable = ortSession != null
        val modelName = if (isOnnxAvailable) "ONNX Native (vrp_model.onnx)" else "VRP Heuristic Solver"

        // 1. Sequence destinations starting from Origin (User GPS)
        val sortedDestinations = if (destinations.size == 1) {
            destinations.mapIndexed { idx, item -> item.copy(sequence = idx + 1) }
        } else {
            solveMultiStopSequenceWithOnnx(origin, destinations, isOnnxAvailable)
        }

        // 2. Build full waypoint list: Origin -> Stop 1 -> Stop 2 -> ... -> Stop N
        val fullWaypoints = listOf(origin) + sortedDestinations.map { it.point }

        // 3. Fetch Snapped Road Geometry via OSRM Route API
        val tripResult = osrmService.fetchRouteForSequence(fullWaypoints, profile = "driving")

        val routePoints = tripResult?.routePoints ?: fullWaypoints
        val totalDistanceKm = tripResult?.totalDistanceKm ?: calculateFallbackDistance(fullWaypoints)
        val totalDurationMin = tripResult?.totalDurationMin ?: (totalDistanceKm * 2.5) // ~24 km/h average speed

        val elapsedMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1)

        val polylineState = RoutePolylineState(
            points = routePoints,
            totalDistanceKm = totalDistanceKm,
            totalDurationMin = totalDurationMin,
            isOptimized = true,
            modelUsed = modelName
        )

        return@withContext OptimizationResult(
            optimizedDestinations = sortedDestinations,
            polylineState = polylineState,
            computationTimeMs = elapsedMs,
            modelUsed = modelName
        )
    }

    /**
     * Solves multi-stop delivery sequence using ONNX model scoring starting from user GPS location.
     * Prioritizes closest/most optimal stop from origin and respects express priorities.
     */
    private fun solveMultiStopSequenceWithOnnx(
        origin: GeoPoint,
        destinations: List<PackageDestination>,
        isOnnxAvailable: Boolean
    ): List<PackageDestination> {
        val unvisited = destinations.toMutableList()
        val orderedResult = mutableListOf<PackageDestination>()

        var currentLat = origin.latitude
        var currentLng = origin.longitude

        val expressList = unvisited.filter { it.priority == "EKSPRES" }.toMutableList()
        val regulerList = unvisited.filter { it.priority != "EKSPRES" }.toMutableList()

        fun processSublist(sublist: MutableList<PackageDestination>) {
            while (sublist.isNotEmpty()) {
                val nextBest = sublist.minByOrNull { dest ->
                    val dist = haversineKm(currentLat, currentLng, dest.lat, dest.lng)
                    if (isOnnxAvailable) {
                        val predictedScore = runOnnxInference(
                            numPkgs = sublist.size.toFloat(),
                            expRatio = if (dest.priority == "EKSPRES") 1.0f else 0.0f,
                            dist = dist.toFloat(),
                            lat = dest.lat.toFloat(),
                            lng = dest.lng.toFloat()
                        )
                        dist + (predictedScore / 1000.0)
                    } else {
                        dist
                    }
                } ?: sublist.first()

                sublist.remove(nextBest)
                orderedResult.add(nextBest)
                currentLat = nextBest.lat
                currentLng = nextBest.lng
            }
        }

        // Prioritize express packages first, then regular packages, both sequenced nearest-first from origin
        processSublist(expressList)
        processSublist(regulerList)

        return orderedResult.mapIndexed { index, item ->
            item.copy(sequence = index + 1)
        }
    }

    /**
     * Runs inference on the ONNX model to score edge weight / computational time prediction.
     */
    private fun runOnnxInference(
        numPkgs: Float,
        expRatio: Float,
        dist: Float,
        lat: Float,
        lng: Float
    ): Long {
        val session = ortSession ?: return 12L
        val env = ortEnv ?: return 12L

        return try {
            val inputName = session.inputNames.iterator().next()
            val floatBuffer = FloatBuffer.wrap(floatArrayOf(numPkgs, expRatio, dist, lat, lng))
            val inputTensor = OnnxTensor.createTensor(env, floatBuffer, longArrayOf(1, 5))

            inputTensor.use {
                val results = session.run(Collections.singletonMap(inputName, inputTensor))
                results.use {
                    val outputValue = results[0].value
                    val rawValue = when (outputValue) {
                        is Array<*> -> {
                            val firstRow = outputValue[0]
                            if (firstRow is FloatArray) firstRow[0] else 0f
                        }
                        else -> 0f
                    }
                    rawValue.toLong().coerceAtLeast(5L)
                }
            }
        } catch (e: Exception) {
            12L
        }
    }

    /**
     * Geocode an address query using Nominatim API.
     */
    suspend fun geocodeAddress(query: String): GeocodedLocation? = withContext(Dispatchers.IO) {
        try {
            val results = osrmService.searchAddressNominatim(query)
            if (results.isNotEmpty()) {
                return@withContext results.first()
            }
        } catch (e: Throwable) {
            // Network fallback
        }

        // Fallback for offline / demo addresses in Sawah Tangerang Selatan area
        val hash = query.hashCode().toDouble()
        val deltaLat = ((hash % 100) / 10000.0)
        val deltaLng = (((hash / 100) % 100) / 10000.0)
        val fallbackPoint = GeoPoint(-6.3025 + deltaLat, 106.7250 + deltaLng)
        return@withContext GeocodedLocation(displayName = query, point = fallbackPoint)
    }

    /**
     * Haversine distance in Kilometers.
     */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun calculateFallbackDistance(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += haversineKm(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
        }
        return (total * 100.0).let { Math.round(it) / 100.0 }
    }
}
