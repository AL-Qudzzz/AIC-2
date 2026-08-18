package com.example.ui.animation

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.VehicleType
import org.osmdroid.views.overlay.Marker

/**
 * Helper utility to manage dynamic vehicle marker icons and configuration for Osmdroid.
 */
object VehicleMarkerHelper {

    /**
     * Returns the appropriate top-down vector drawable resource ID for the vehicle type.
     */
    fun getDrawableRes(vehicleType: VehicleType): Int {
        return when (vehicleType) {
            VehicleType.MOTORCYCLE -> R.drawable.ic_nav_motorcycle_top_down
            VehicleType.CAR -> R.drawable.ic_nav_car_top_down
            VehicleType.TRUCK -> R.drawable.ic_nav_truck_top_down
        }
    }

    /**
     * Retrieves the vector drawable for the specified vehicle type.
     */
    fun getVehicleDrawable(context: Context, vehicleType: VehicleType): Drawable? {
        return ContextCompat.getDrawable(context, getDrawableRes(vehicleType))
    }

    /**
     * Configures an osmdroid Marker as a flat, dynamically-oriented navigation vehicle marker.
     *
     * @param marker The osmdroid Marker to configure.
     * @param context Android context for resource resolution.
     * @param vehicleType The type of vehicle (Motorcycle, Car, Truck).
     */
    fun configureVehicleMarker(
        marker: Marker,
        context: Context,
        vehicleType: VehicleType
    ) {
        marker.isFlat = true // Ensures marker rotates flat on the map plane
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER) // (0.5f, 0.5f) center anchor
        getVehicleDrawable(context, vehicleType)?.let {
            marker.icon = it
        }
    }
}
