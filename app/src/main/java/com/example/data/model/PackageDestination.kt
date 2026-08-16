package com.example.data.model

import org.osmdroid.util.GeoPoint

/**
 * Model representing a destination point on the map and route.
 */
data class PackageDestination(
    val id: String,
    val title: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val sequence: Int = 0,
    val priority: String = "REGULER", // "REGULER", "EKSPRES"
    val status: String = "BELUM_DIMULAI", // "BELUM_DIMULAI", "DALAM_PERJALANAN", "TERKIRIM", "GAGAL_KIRIM"
    val phone: String = "",
    val packageType: String = "Paket Reguler (1 kg)"
) {
    val point: GeoPoint
        get() = GeoPoint(lat, lng)
}

/**
 * State representing calculated polyline geometry and trip metrics.
 */
data class RoutePolylineState(
    val points: List<GeoPoint> = emptyList(),
    val totalDistanceKm: Double = 0.0,
    val totalDurationMin: Double = 0.0,
    val isOptimized: Boolean = false,
    val modelUsed: String = "vrp_model.onnx (ONNX Native)"
)

/**
 * State holding map camera position, zoom, and orientation across tab switches.
 */
data class MapCameraState(
    val center: GeoPoint = GeoPoint(-6.3025, 106.7210),
    val zoomLevel: Double = 15.0,
    val orientation: Float = 0f
)

/**
 * State for Google Maps-style Turn-by-Turn Navigation Mode.
 */
data class NavigationGuidanceState(
    val isNavigating: Boolean = false,
    val currentDestination: PackageDestination? = null,
    val nextStopIndex: Int = 0,
    val totalStops: Int = 0,
    val nextInstruction: String = "Mulai perjalanan menuju destinasi",
    val nextStreetName: String = "",
    val distanceToNextKm: Double = 0.0,
    val remainingTotalDistanceKm: Double = 0.0,
    val remainingDurationMin: Double = 0.0
)

/**
 * Extension functions to convert DeliveryPackage to PackageDestination and vice-versa.
 */
fun DeliveryPackage.toPackageDestination(): PackageDestination {
    return PackageDestination(
        id = id,
        title = recipientName,
        address = address,
        lat = lat,
        lng = lng,
        sequence = sequence,
        priority = priority,
        status = status,
        phone = phone,
        packageType = packageType
    )
}

fun PackageDestination.toDeliveryPackage(trackingNumber: String = "RW-${(1000000..9999999).random()}"): DeliveryPackage {
    return DeliveryPackage(
        id = id,
        trackingNumber = trackingNumber,
        recipientName = title,
        phone = phone,
        address = address,
        subDistrict = "Sawah, Ciputat, Tangsel",
        sequence = sequence,
        status = status,
        priority = priority,
        lat = lat,
        lng = lng,
        packageType = packageType
    )
}
