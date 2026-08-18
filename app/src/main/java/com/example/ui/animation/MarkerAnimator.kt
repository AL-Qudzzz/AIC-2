package com.example.ui.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.location.Location
import android.view.animation.LinearInterpolator
import com.example.data.model.VehicleType
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Controller for smooth marker position and rotation animations with spherical interpolation for Osmdroid.
 * Emulates the continuous, silky-smooth vehicle movement found in Google Maps and Waze.
 */
class MarkerAnimator(
    private val mapView: MapView,
    private val marker: Marker,
    var followCamera: Boolean = true,
    var followCameraOrientation: Boolean = false,
    var animationDurationMs: Long = 1500L
) {

    private var currentAnimator: ValueAnimator? = null
    var currentPosition: GeoPoint = marker.position ?: GeoPoint(0.0, 0.0)
        private set
    var currentBearing: Float = marker.rotation
        private set

    init {
        marker.isFlat = true
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    }

    /**
     * Updates marker for a new GPS location event with dynamic vehicle type styling.
     *
     * @param location The new Android Location received from FusedLocationProviderClient.
     * @param vehicleType The user's active vehicle profile type.
     * @param targetHeading Optional explicit heading; if null and location has no bearing,
     *                      heading is computed from previous position to new position.
     */
    fun onLocationUpdated(
        location: Location,
        vehicleType: VehicleType? = null,
        targetHeading: Float? = null
    ) {
        val newPoint = GeoPoint(location.latitude, location.longitude)
        
        // Update vehicle icon if specified
        if (vehicleType != null) {
            VehicleMarkerHelper.configureVehicleMarker(marker, mapView.context, vehicleType)
        }

        // Determine target bearing
        val resolvedBearing = when {
            targetHeading != null -> targetHeading
            location.hasBearing() && location.bearing != 0f -> location.bearing
            GeoPointInterpolator.computeDistanceMeters(currentPosition, newPoint) > 1.5 -> {
                GeoPointInterpolator.computeHeading(currentPosition, newPoint)
            }
            else -> currentBearing
        }

        animateTo(newPoint, resolvedBearing, animationDurationMs)
    }

    /**
     * Animates marker smoothly from its current position & bearing to [targetPosition] and [targetBearing].
     *
     * @param targetPosition Destination GeoPoint.
     * @param targetBearing Destination bearing in degrees [0..360].
     * @param duration Duration in milliseconds (typically 1000ms - 2000ms matching GPS frequency).
     */
    fun animateTo(
        targetPosition: GeoPoint,
        targetBearing: Float = currentBearing,
        duration: Long = animationDurationMs
    ) {
        // Cancel any active animation and capture its current interpolated state
        currentAnimator?.cancel()

        val startPosition = currentPosition
        val startBearing = currentBearing

        // Check if movement is negligible
        val distanceMeters = GeoPointInterpolator.computeDistanceMeters(startPosition, targetPosition)
        val bearingDelta = GeoPointInterpolator.computeShortestRotationDelta(startBearing, targetBearing)

        if (distanceMeters < 0.05 && kotlin.math.abs(bearingDelta) < 0.5f) {
            currentPosition = targetPosition
            currentBearing = GeoPointInterpolator.wrapTo360(targetBearing)
            marker.position = targetPosition
            marker.rotation = currentBearing
            mapView.invalidate()
            return
        }

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            this.interpolator = LinearInterpolator()

            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float

                // 1. Slerp Position Interpolation
                val interpolatedPoint = GeoPointInterpolator.interpolate(fraction, startPosition, targetPosition)
                currentPosition = interpolatedPoint
                marker.position = interpolatedPoint

                // 2. Shortest-Path Rotation Interpolation (Avoids 360-degree flips across 0/360)
                val interpolatedRotation = GeoPointInterpolator.interpolateRotation(fraction, startBearing, targetBearing)
                currentBearing = interpolatedRotation
                marker.rotation = interpolatedRotation

                // 3. Smooth Camera Follow
                if (followCamera) {
                    mapView.setExpectedCenter(interpolatedPoint)
                    if (followCameraOrientation) {
                        mapView.mapOrientation = -interpolatedRotation
                    }
                }

                // Refresh Map Rendering
                mapView.postInvalidate()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentPosition = targetPosition
                    currentBearing = GeoPointInterpolator.wrapTo360(targetBearing)
                    marker.position = targetPosition
                    marker.rotation = currentBearing
                    if (followCamera) {
                        mapView.setExpectedCenter(targetPosition)
                    }
                    mapView.postInvalidate()
                    currentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })

            start()
        }
    }

    /**
     * Instantly sets the marker position and rotation without animation.
     */
    fun setImmediate(position: GeoPoint, bearing: Float = currentBearing) {
        currentAnimator?.cancel()
        currentPosition = position
        currentBearing = GeoPointInterpolator.wrapTo360(bearing)
        marker.position = position
        marker.rotation = currentBearing
        if (followCamera) {
            mapView.controller.setCenter(position)
        }
        mapView.invalidate()
    }

    /**
     * Cancels any ongoing animation and frees resources.
     */
    fun cancel() {
        currentAnimator?.cancel()
        currentAnimator = null
    }
}

/**
 * Extension function on osmdroid Marker to easily animate to a new GeoPoint and bearing.
 */
fun Marker.animateTo(
    mapView: MapView,
    targetPosition: GeoPoint,
    targetBearing: Float = this.rotation,
    durationMs: Long = 1500L,
    followCamera: Boolean = false
): ValueAnimator {
    this.isFlat = true
    this.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

    val startPosition = this.position ?: targetPosition
    val startBearing = this.rotation

    return ValueAnimator.ofFloat(0f, 1f).apply {
        this.duration = durationMs
        this.interpolator = LinearInterpolator()

        addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val point = GeoPointInterpolator.interpolate(fraction, startPosition, targetPosition)
            val rot = GeoPointInterpolator.interpolateRotation(fraction, startBearing, targetBearing)

            this@animateTo.position = point
            this@animateTo.rotation = rot

            if (followCamera) {
                mapView.setExpectedCenter(point)
            }
            mapView.postInvalidate()
        }

        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                this@animateTo.position = targetPosition
                this@animateTo.rotation = GeoPointInterpolator.wrapTo360(targetBearing)
                if (followCamera) {
                    mapView.setExpectedCenter(targetPosition)
                }
                mapView.postInvalidate()
            }
        })

        start()
    }
}
