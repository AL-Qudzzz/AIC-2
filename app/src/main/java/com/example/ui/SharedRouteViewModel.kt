package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.ml.ModelAssetManager
import com.example.data.model.DeliveryPackage
import com.example.data.model.MapCameraState
import com.example.data.model.NavigationGuidanceState
import com.example.data.model.PackageDestination
import com.example.data.model.RoutePolylineState
import com.example.data.model.toDeliveryPackage
import com.example.data.model.toPackageDestination
import com.example.data.repository.DeliveryRepository
import com.example.data.service.GeocodedLocation
import com.example.data.service.OsrmRoutingService
import com.example.data.service.RouteOptimizerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/**
 * Shared ViewModel scoped to the Activity to maintain state across tab transitions
 * (Peta <-> Rute) and synchronize multi-stop ONNX route optimization and geocoding.
 */
class SharedRouteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DeliveryRepository
    private val optimizerService: RouteOptimizerService
    private val osrmService: OsrmRoutingService

    // Default center in Sawah, Tangerang Selatan
    private val defaultOrigin = GeoPoint(-6.3025, 106.7210)

    // 1. Destination Coordinates List (StateFlow)
    private val _destinations = MutableStateFlow<List<PackageDestination>>(emptyList())
    val destinations: StateFlow<List<PackageDestination>> = _destinations.asStateFlow()

    // 2. Calculated Route Polyline (StateFlow)
    private val _routePolyline = MutableStateFlow<RoutePolylineState?>(null)
    val routePolyline: StateFlow<RoutePolylineState?> = _routePolyline.asStateFlow()

    // 3. Map Camera / Center / Zoom State (Preserved across tab changes)
    private val _cameraState = MutableStateFlow(MapCameraState(center = defaultOrigin, zoomLevel = 15.0))
    val cameraState: StateFlow<MapCameraState> = _cameraState.asStateFlow()

    // 4. Navigation Mode State (Google Maps driving view)
    private val _navigationState = MutableStateFlow(NavigationGuidanceState())
    val navigationState: StateFlow<NavigationGuidanceState> = _navigationState.asStateFlow()

    // 5. User Real-Time Location
    private val _userLocation = MutableStateFlow<GeoPoint?>(null)
    val userLocation: StateFlow<GeoPoint?> = _userLocation.asStateFlow()

    // 6. Loading & Search states
    private val _isOptimizing = MutableStateFlow(false)
    val isOptimizing: StateFlow<Boolean> = _isOptimizing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodedLocation>>(emptyList())
    val searchResults: StateFlow<List<GeocodedLocation>> = _searchResults.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DeliveryRepository(db, application)
        val assetManager = ModelAssetManager(application)
        osrmService = OsrmRoutingService()
        optimizerService = RouteOptimizerService(assetManager, osrmService)
    }

    /**
     * Updates real-time user location from GPS.
     */
    fun setUserLocation(point: GeoPoint) {
        _userLocation.value = point
        if (_navigationState.value.isNavigating) {
            updateNavigationProgress(point)
        }
    }

    /**
     * Updates camera zoom, center, and tilt orientation.
     */
    fun updateCameraState(center: GeoPoint, zoomLevel: Double, orientation: Float = 0f) {
        _cameraState.value = MapCameraState(center = center, zoomLevel = zoomLevel, orientation = orientation)
    }

    /**
     * Synchronizes package list from database into SharedRouteViewModel's destinations.
     */
    fun syncFromDatabasePackages(packages: List<DeliveryPackage>) {
        val current = _destinations.value
        val mapped = packages.map { it.toPackageDestination() }
        if (current.isEmpty() && mapped.isNotEmpty()) {
            _destinations.value = mapped
            recalculateRouteWithOnnx(mapped)
        }
    }

    /**
     * Adds an address with Nominatim Geocoding (Two-Way Rute <-> Peta integration).
     */
    fun addDestinationWithGeocoding(
        address: String,
        recipientName: String,
        phone: String = "",
        priority: String = "REGULER",
        packageType: String = "Paket Reguler (1 kg)",
        onComplete: ((PackageDestination) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isSearching.value = true
            val geocoded = optimizerService.geocodeAddress(address)
            val point = geocoded?.point ?: GeoPoint(-6.3025, 106.7320)
            val displayName = geocoded?.displayName ?: address

            val newId = "PKG-${System.currentTimeMillis() % 100000}"
            val newSeq = (_destinations.value.maxOfOrNull { it.sequence } ?: 0) + 1

            val newDestination = PackageDestination(
                id = newId,
                title = recipientName.ifBlank { displayName.split(",").firstOrNull() ?: "Destinasi Baru" },
                address = displayName,
                lat = point.latitude,
                lng = point.longitude,
                sequence = newSeq,
                priority = priority,
                phone = phone,
                packageType = packageType
            )

            // 1. Add to active map destinations list
            val updated = _destinations.value + newDestination
            _destinations.value = updated

            _isSearching.value = false
            _searchResults.value = emptyList()
            _searchQuery.value = ""

            // 2. Insert into Room DB
            repository.insertPackage(newDestination.toDeliveryPackage())

            // 3. Recalculate route via ONNX model
            recalculateRouteWithOnnx(updated)
            onComplete?.invoke(newDestination)
        }
    }

    /**
     * Adds a destination directly from a GeocodedLocation (Map search dropdown).
     */
    fun addDestination(geocoded: GeocodedLocation) {
        val title = geocoded.displayName.split(",").firstOrNull() ?: "Destinasi"
        val newId = "DEST-${System.currentTimeMillis() % 100000}"
        val newSeq = (_destinations.value.maxOfOrNull { it.sequence } ?: 0) + 1

        val newDestination = PackageDestination(
            id = newId,
            title = title,
            address = geocoded.displayName,
            lat = geocoded.point.latitude,
            lng = geocoded.point.longitude,
            sequence = newSeq
        )

        val updated = _destinations.value + newDestination
        _destinations.value = updated
        _searchResults.value = emptyList()
        _searchQuery.value = ""

        viewModelScope.launch {
            repository.insertPackage(newDestination.toDeliveryPackage())
            recalculateRouteWithOnnx(updated)
        }
    }

    /**
     * Removes a destination and recalculates the ONNX route.
     */
    fun removeDestination(id: String) {
        val updated = _destinations.value.filterNot { it.id == id }
        _destinations.value = updated
        if (updated.isEmpty()) {
            _routePolyline.value = null
            stopNavigationMode()
        } else {
            recalculateRouteWithOnnx(updated)
        }
    }

    /**
     * Runs multi-stop optimization using ONNX model (vrp_model.onnx) from user GPS location.
     */
    fun recalculateRouteWithOnnx(customList: List<PackageDestination>? = null) {
        val listToOptimize = customList ?: _destinations.value
        val origin = _userLocation.value ?: defaultOrigin

        if (listToOptimize.isEmpty()) {
            _routePolyline.value = null
            return
        }

        viewModelScope.launch {
            _isOptimizing.value = true
            val result = optimizerService.optimizeRouteSequence(origin, listToOptimize)
            _destinations.value = result.optimizedDestinations
            _routePolyline.value = result.polylineState
            _isOptimizing.value = false

            // Update navigation guidance if currently navigating
            if (_navigationState.value.isNavigating) {
                updateNavigationGuidance(origin, result.optimizedDestinations, result.polylineState)
            }
        }
    }

    /**
     * Start Google Maps-style 3D Navigation Mode.
     */
    fun startNavigationMode() {
        val currentOrigin = _userLocation.value ?: defaultOrigin
        val dests = _destinations.value
        if (dests.isEmpty()) return

        val nextStop = dests.firstOrNull()
        val poly = _routePolyline.value

        val distToNext = if (nextStop != null) {
            optimizerService.haversineKm(currentOrigin.latitude, currentOrigin.longitude, nextStop.lat, nextStop.lng)
        } else 0.0

        _navigationState.value = NavigationGuidanceState(
            isNavigating = true,
            currentDestination = nextStop,
            nextStopIndex = 1,
            totalStops = dests.size,
            nextInstruction = "Lanjut ke Stop 1: ${nextStop?.title ?: "Destinasi"}",
            nextStreetName = nextStop?.address?.split(",")?.firstOrNull() ?: "Jl. Rute Pengiriman",
            distanceToNextKm = distToNext,
            remainingTotalDistanceKm = poly?.totalDistanceKm ?: distToNext,
            remainingDurationMin = poly?.totalDurationMin ?: (distToNext * 2.5)
        )

        // Set camera tilt & close zoom for navigation perspective
        _cameraState.value = MapCameraState(
            center = currentOrigin,
            zoomLevel = 18.0,
            orientation = 0f
        )
    }

    /**
     * Stop Navigation Mode and return to overview.
     */
    fun stopNavigationMode() {
        _navigationState.value = NavigationGuidanceState(isNavigating = false)
        val origin = _userLocation.value ?: defaultOrigin
        _cameraState.value = MapCameraState(center = origin, zoomLevel = 15.0, orientation = 0f)
    }

    private fun updateNavigationProgress(userPos: GeoPoint) {
        val nav = _navigationState.value
        if (!nav.isNavigating) return

        val dests = _destinations.value
        val currentDest = nav.currentDestination ?: dests.firstOrNull() ?: return

        val distToNext = optimizerService.haversineKm(
            userPos.latitude, userPos.longitude,
            currentDest.lat, currentDest.lng
        )

        _navigationState.value = nav.copy(
            distanceToNextKm = distToNext,
            remainingTotalDistanceKm = (_routePolyline.value?.totalDistanceKm ?: distToNext).coerceAtLeast(distToNext)
        )
    }

    private fun updateNavigationGuidance(
        origin: GeoPoint,
        dests: List<PackageDestination>,
        poly: RoutePolylineState
    ) {
        val nextStop = dests.firstOrNull()
        val distToNext = if (nextStop != null) {
            optimizerService.haversineKm(origin.latitude, origin.longitude, nextStop.lat, nextStop.lng)
        } else 0.0

        _navigationState.value = _navigationState.value.copy(
            currentDestination = nextStop,
            totalStops = dests.size,
            distanceToNextKm = distToNext,
            remainingTotalDistanceKm = poly.totalDistanceKm,
            remainingDurationMin = poly.totalDurationMin
        )
    }

    /**
     * Search address via Nominatim API.
     */
    fun searchAddress(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            val results = osrmService.searchAddressNominatim(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _searchQuery.value = ""
    }
}
