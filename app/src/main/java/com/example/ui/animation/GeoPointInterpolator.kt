package com.example.ui.animation

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import org.osmdroid.util.GeoPoint

/**
 * High-precision spherical trigonometry and interpolation utilities adapted for osmdroid's GeoPoint,
 * powered by Google Maps Android Utils (com.google.maps.android:android-maps-utils:3.4.0).
 */
object GeoPointInterpolator {

    /**
     * Converts an Osmdroid GeoPoint to a Google Maps LatLng.
     */
    fun GeoPoint.toLatLng(): LatLng = LatLng(this.latitude, this.longitude)

    /**
     * Converts a Google Maps LatLng to an Osmdroid GeoPoint.
     */
    fun LatLng.toGeoPoint(): GeoPoint = GeoPoint(this.latitude, this.longitude)

    /**
     * Returns the GeoPoint which lies the given fraction of the way between the
     * `from` and `to` points using Spherical Linear Interpolation (Slerp) via Google Maps SphericalUtil.
     *
     * @param fraction The proportion of the distance between the two points, between 0.0 and 1.0.
     * @param from The starting GeoPoint.
     * @param to The destination GeoPoint.
     * @return The interpolated GeoPoint.
     */
    fun interpolate(fraction: Float, from: GeoPoint, to: GeoPoint): GeoPoint {
        if (fraction <= 0f) return from
        if (fraction >= 1f) return to

        val fromLatLng = from.toLatLng()
        val toLatLng = to.toLatLng()
        val interpolated = SphericalUtil.interpolate(fromLatLng, toLatLng, fraction.toDouble())
        return interpolated.toGeoPoint()
    }

    /**
     * Computes the heading (bearing) from one GeoPoint to another in degrees [0.0, 360.0)
     * using Google Maps SphericalUtil.
     * North is 0°, East is 90°, South is 180°, West is 270°.
     */
    fun computeHeading(from: GeoPoint, to: GeoPoint): Float {
        val heading = SphericalUtil.computeHeading(from.toLatLng(), to.toLatLng())
        return wrapTo360(heading.toFloat())
    }

    /**
     * Computes Great-Circle distance in meters between two GeoPoints
     * using Google Maps SphericalUtil.
     */
    fun computeDistanceMeters(from: GeoPoint, to: GeoPoint): Double {
        return SphericalUtil.computeDistanceBetween(from.toLatLng(), to.toLatLng())
    }

    /**
     * Calculates the shortest angular difference in degrees between two bearings,
     * ensuring rotation takes the shortest path and avoids 360-degree spins
     * across the 0°/360° boundary.
     *
     * Example: from 350° to 10° yields +20° (clockwise), NOT -340°.
     * Example: from 10° to 350° yields -20° (counter-clockwise), NOT +340°.
     *
     * @return signed difference in range [-180.0, 180.0]
     */
    fun computeShortestRotationDelta(fromBearing: Float, toBearing: Float): Float {
        val diff = (toBearing - fromBearing + 540f) % 360f - 180f
        return diff
    }

    /**
     * Interpolates bearing from [fromBearing] to [toBearing] at [fraction],
     * following the shortest path around the circle.
     *
     * @return normalized bearing in degrees [0.0, 360.0)
     */
    fun interpolateRotation(fraction: Float, fromBearing: Float, toBearing: Float): Float {
        val delta = computeShortestRotationDelta(fromBearing, toBearing)
        val result = fromBearing + fraction * delta
        return wrapTo360(result)
    }

    /**
     * Normalizes an angle in degrees to the [0.0, 360.0) range.
     */
    fun wrapTo360(degrees: Float): Float {
        val wrapped = degrees % 360f
        return if (wrapped < 0f) wrapped + 360f else wrapped
    }
}
