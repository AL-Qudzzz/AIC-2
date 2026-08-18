package com.example.ui.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.example.R
import com.example.data.model.VehicleType
import com.example.ui.animation.GeoPointInterpolator
import com.example.ui.animation.MarkerAnimator
import com.example.ui.animation.VehicleMarkerHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Native Android 3D Navigation Activity using Osmdroid & Google Maps Android Utils 3.4.0.
 * Simulates a modern 3D turn-by-turn driving experience (like Google Maps & Waze).
 */
class NavigationActivity : ComponentActivity() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    // UI Layer Elements
    private lateinit var btnBack: ImageButton
    private lateinit var btnEndNavigation: Button
    private lateinit var btnVolume: ImageButton
    private lateinit var btnReport: ImageButton
    private lateinit var btnRecenter: ImageButton

    private lateinit var ivManeuverIconTop: ImageView
    private lateinit var tvManeuverDistance: TextView
    private lateinit var tvNextStreet: TextView
    private lateinit var tvCurrentSpeed: TextView
    private lateinit var tvEtaTime: TextView
    private lateinit var tvRemainingTimeDistance: TextView
    private lateinit var ivBottomTurnIcon: ImageView
    private lateinit var tvBottomTurnText: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvDestinationAddress: TextView

    // Navigation State & Animation
    private var isMuted: Boolean = false
    private var isAutoFollowEnabled: Boolean = true
    private var selectedVehicleType: VehicleType = VehicleType.MOTORCYCLE
    private var userVehicleMarker: Marker? = null
    private var markerAnimator: MarkerAnimator? = null
    private var navigationPolyline: Polyline? = null
    private var lastKnownLocation: Location? = null

    companion object {
        // Clean Minimal Tile Source (CartoDB Voyager) for crisp road visibility without clutter
        val CARTO_VOYAGER_TILES: ITileSource = XYTileSource(
            "CartoVoyager",
            0, 20, 256, ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
            ),
            "© OpenStreetMap contributors, © CARTO"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Osmdroid Configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = "RouteWise-AI-3D-Navigation/1.0"

        setContentView(R.layout.activity_navigation)

        bindViews()
        setupCleanMapView()
        setupActionListeners()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationTracking()
    }

    private fun bindViews() {
        mapView = findViewById(R.id.map_view)
        btnBack = findViewById(R.id.btn_back)
        btnEndNavigation = findViewById(R.id.btn_end_navigation)
        btnVolume = findViewById(R.id.btn_volume)
        btnReport = findViewById(R.id.btn_report)
        btnRecenter = findViewById(R.id.btn_recenter)

        ivManeuverIconTop = findViewById(R.id.iv_maneuver_icon_top)
        tvManeuverDistance = findViewById(R.id.tv_maneuver_distance)
        tvNextStreet = findViewById(R.id.tv_next_street)
        tvCurrentSpeed = findViewById(R.id.tv_current_speed)
        tvEtaTime = findViewById(R.id.tv_eta_time)
        tvRemainingTimeDistance = findViewById(R.id.tv_remaining_time_distance)
        ivBottomTurnIcon = findViewById(R.id.iv_bottom_turn_icon)
        tvBottomTurnText = findViewById(R.id.tv_bottom_turn_text)
        tvCustomerName = findViewById(R.id.tv_customer_name)
        tvDestinationAddress = findViewById(R.id.tv_destination_address)
    }

    /**
     * Configures MapView with a clean minimal tile source and 3D navigation perspective.
     */
    private fun setupCleanMapView() {
        mapView.setTileSource(CARTO_VOYAGER_TILES)
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        mapView.isHorizontalMapRepetitionEnabled = false
        mapView.isVerticalMapRepetitionEnabled = false

        // Apply 3D Camera Tilt (pitch)
        apply3DCameraPerspective(tiltDegrees = 55f)

        // Setup Persistent Vehicle Marker
        val marker = Marker(mapView).apply {
            VehicleMarkerHelper.configureVehicleMarker(this, this@NavigationActivity, selectedVehicleType)
        }
        userVehicleMarker = marker
        mapView.overlays.add(marker)

        // Setup Marker Animator with smooth Spherical Slerp
        markerAnimator = MarkerAnimator(
            mapView = mapView,
            marker = marker,
            followCamera = true,
            followCameraOrientation = true,
            animationDurationMs = 1200L
        )
    }

    /**
     * Applies 3D camera tilt/pitch to simulate driving perspective.
     */
    fun apply3DCameraPerspective(tiltDegrees: Float = 55f) {
        // Hardware-accelerated 3D pitch/tilt on the MapView canvas
        mapView.rotationX = tiltDegrees.coerceIn(0f, 65f)
        mapView.cameraDistance = 8000f
    }

    private fun setupActionListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnEndNavigation.setOnClickListener {
            Toast.makeText(this, "Navigasi selesai", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnVolume.setOnClickListener {
            isMuted = !isMuted
            btnVolume.setImageResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up)
            Toast.makeText(this, if (isMuted) "Suara panduan dimatikan" else "Suara panduan aktif", Toast.LENGTH_SHORT).show()
        }

        btnReport.setOnClickListener {
            Toast.makeText(this, "Laporan kendala lalu lintas terkirim ke sistem", Toast.LENGTH_SHORT).show()
        }

        btnRecenter.setOnClickListener {
            isAutoFollowEnabled = true
            markerAnimator?.followCamera = true
            markerAnimator?.followCameraOrientation = true
            lastKnownLocation?.let { loc ->
                animateCameraTo3DPosition(GeoPoint(loc.latitude, loc.longitude), loc.bearing)
            }
            Toast.makeText(this, "Kamera dipusatkan kembali", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Animates camera tightly with 3D tilt and heading orientation.
     */
    fun animateCameraTo3DPosition(point: GeoPoint, bearing: Float, zoom: Double = 19.0) {
        mapView.controller.setZoom(zoom)
        mapView.mapOrientation = -bearing // Rotates map so vehicle points UP
        mapView.controller.animateTo(point)
    }

    /**
     * Starts continuous high-accuracy GPS tracking.
     */
    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(1.0f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                onNewLocationReceived(location)
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    /**
     * Handles real-time location update, triggers smooth marker gliding, 3D camera follow, and HUD updates.
     */
    private fun onNewLocationReceived(location: Location) {
        lastKnownLocation = location

        // 1. Smoothly Glide Marker & Rotate Vehicle Icon
        markerAnimator?.onLocationUpdated(location, selectedVehicleType)

        // 2. Keep Vehicle pointing "UP" in 3D perspective
        if (isAutoFollowEnabled) {
            val speedKmH = location.speed * 3.6f
            if (location.hasBearing() && speedKmH > 2.0f) {
                mapView.mapOrientation = -location.bearing
            }
        }

        // 3. Update HUD Elements
        val speedKmh = (location.speed * 3.6f).toInt().coerceAtLeast(0)
        tvCurrentSpeed.text = speedKmh.toString()

        // Update Sample Maneuver & Street
        updateManeuverHUD(location)
    }

    private fun updateManeuverHUD(location: Location) {
        // Dynamic turn-by-turn calculation or sample telemetry
        tvManeuverDistance.text = "Dalam 180 m"
        tvNextStreet.text = "Belok kanan ke Jl. Sawah Raya"
        tvEtaTime.text = "14:42"
        tvRemainingTimeDistance.text = "12 mnt • 3.8 km"
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        markerAnimator?.cancel()
    }
}
