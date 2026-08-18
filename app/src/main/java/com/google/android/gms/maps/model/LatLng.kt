package com.google.android.gms.maps.model

/**
 * Compatible LatLng model enabling standalone usage of com.google.maps.android:android-maps-utils:3.4.0
 * (SphericalUtil, PolyUtil, etc.) without requiring full Google Play Services Maps SDK.
 */
class LatLng(
    @JvmField val latitude: Double,
    @JvmField val longitude: Double
) {
    constructor() : this(0.0, 0.0)

    override fun toString(): String = "lat/lng: ($latitude,$longitude)"

    override fun hashCode(): Int {
        val latBits = java.lang.Double.doubleToLongBits(latitude)
        val lngBits = java.lang.Double.doubleToLongBits(longitude)
        var result = (latBits xor (latBits ushr 32)).toInt()
        result = 31 * result + (lngBits xor (lngBits ushr 32)).toInt()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LatLng) return false
        return java.lang.Double.doubleToLongBits(latitude) == java.lang.Double.doubleToLongBits(other.latitude) &&
                java.lang.Double.doubleToLongBits(longitude) == java.lang.Double.doubleToLongBits(other.longitude)
    }
}
