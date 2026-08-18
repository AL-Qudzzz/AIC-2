package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.preference.PreferenceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PackageDestination
import com.example.data.model.VehicleType
import com.example.data.service.GeocodedLocation
import com.example.ui.SharedRouteViewModel
import com.example.ui.animation.MarkerAnimator
import com.example.ui.animation.VehicleMarkerHelper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MapScreen(
    sharedViewModel: SharedRouteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Collect Activity-Scoped ViewModel StateFlows
    val destinations by sharedViewModel.destinations.collectAsStateWithLifecycle()
    val routePolyline by sharedViewModel.routePolyline.collectAsStateWithLifecycle()
    val cameraState by sharedViewModel.cameraState.collectAsStateWithLifecycle()
    val navigationState by sharedViewModel.navigationState.collectAsStateWithLifecycle()
    val userLocation by sharedViewModel.userLocation.collectAsStateWithLifecycle()
    val vehicleType by sharedViewModel.vehicleType.collectAsStateWithLifecycle()
    val isOptimizing by sharedViewModel.isOptimizing.collectAsStateWithLifecycle()
    val searchQuery by sharedViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by sharedViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by sharedViewModel.isSearching.collectAsStateWithLifecycle()

    val defaultCenter = remember { GeoPoint(-6.3025, 106.7210) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var markerAnimatorRef by remember { mutableStateOf<MarkerAnimator?>(null) }
    var userVehicleMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var isDestinationsExpanded by remember { mutableStateOf(false) }

    // Initialize OSM Configuration
    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    // Permission state & GPS Tracking
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Real-time location callback with smooth marker gliding & rotation interpolation
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLoc = locationResult.lastLocation ?: return
                val newPoint = GeoPoint(lastLoc.latitude, lastLoc.longitude)
                val bearing = if (lastLoc.hasBearing()) lastLoc.bearing else 0f
                sharedViewModel.setUserLocation(newPoint, bearing)

                // Smoothly animate vehicle marker position, bearing, and camera follow
                markerAnimatorRef?.let { animator ->
                    animator.followCamera = navigationState.isNavigating
                    animator.followCameraOrientation = navigationState.isNavigating
                    animator.onLocationUpdated(lastLoc, vehicleType)
                } ?: run {
                    userVehicleMarkerRef?.let { marker ->
                        marker.position = newPoint
                        if (bearing != 0f) marker.rotation = bearing
                        mapViewRef?.invalidate()
                    }
                }
            }
        }
    }

    fun requestLocationUpdates() {
        if (hasLocationPermission) {
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                    .setMinUpdateIntervalMillis(1000L)
                    .build()
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        val pt = GeoPoint(loc.latitude, loc.longitude)
                        val brg = if (loc.hasBearing()) loc.bearing else 0f
                        sharedViewModel.setUserLocation(pt, brg)
                        markerAnimatorRef?.setImmediate(pt, brg)
                    } else if (userLocation == null) {
                        sharedViewModel.setUserLocation(defaultCenter)
                        markerAnimatorRef?.setImmediate(defaultCenter, 0f)
                    }
                }
            } catch (e: SecurityException) {
                if (userLocation == null) sharedViewModel.setUserLocation(defaultCenter)
            }
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    DisposableEffect(Unit) {
        requestLocationUpdates()
        onDispose {
            markerAnimatorRef?.cancel()
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Auto-center camera when polyline updates if not in navigation mode
    LaunchedEffect(routePolyline) {
        val points = routePolyline?.points
        if (points != null && points.isNotEmpty() && !navigationState.isNavigating) {
            mapViewRef?.let { mv ->
                try {
                    val boundingBox = BoundingBox.fromGeoPoints(points)
                    mv.zoomToBoundingBox(boundingBox, true, 90)
                } catch (_: Exception) {}
            }
        }
    }

    // Smooth camera transition when toggling navigation mode
    LaunchedEffect(navigationState.isNavigating) {
        mapViewRef?.let { mv ->
            if (navigationState.isNavigating) {
                mv.controller.setZoom(18.0)
                val target = markerAnimatorRef?.currentPosition ?: userLocation ?: defaultCenter
                mv.setExpectedCenter(target)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("map_screen_view")
    ) {
        // -------------------------------------------------------------------------
        // FULL-BLEED OPENSTREETMAP VIEW WITH SMOOTH VEHICLE MARKER & GLOW POLYLINES
        // -------------------------------------------------------------------------
        // -------------------------------------------------------------------------
        // FULL-BLEED OPENSTREETMAP VIEW WITH CLEAN 3D TILES & VEHICLE GLIDER
        // -------------------------------------------------------------------------
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(com.example.ui.navigation.NavigationActivity.CARTO_VOYAGER_TILES)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    isHorizontalMapRepetitionEnabled = false
                    isVerticalMapRepetitionEnabled = false
                    controller.setZoom(cameraState.zoomLevel)
                    controller.setCenter(cameraState.center)
                    mapOrientation = cameraState.orientation

                    // Initialize persistent flat vehicle marker with top-down vehicle icon
                    val startPos = userLocation ?: cameraState.center
                    val vehicleMarker = Marker(this).apply {
                        position = startPos
                        title = "Posisi Anda"
                        snippet = "Navigasi Berjalan"
                        VehicleMarkerHelper.configureVehicleMarker(this, ctx, vehicleType)
                    }
                    userVehicleMarkerRef = vehicleMarker

                    // Attach smooth marker animator with ValueAnimator & LinearInterpolator
                    val animator = MarkerAnimator(
                        mapView = this,
                        marker = vehicleMarker,
                        followCamera = navigationState.isNavigating,
                        followCameraOrientation = navigationState.isNavigating,
                        animationDurationMs = 1200L
                    )
                    markerAnimatorRef = animator

                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
                val isNav = navigationState.isNavigating

                // Dynamic 3D Camera Tilt (pitch) during navigation mode
                mapView.rotationX = if (isNav) 55f else 0f
                mapView.cameraDistance = 8000f

                // 1. Dual-Layer Glowing Snapped Polyline (Only if destinations exist)
                if (destinations.isNotEmpty()) {
                    routePolyline?.points?.let { points ->
                        if (points.isNotEmpty()) {
                            // High-contrast background border
                            val bgPolyline = Polyline(mapView).apply {
                                setPoints(points)
                                outlinePaint.color = if (isNav) {
                                    android.graphics.Color.parseColor("#0F172A")
                                } else {
                                    android.graphics.Color.parseColor("#1E293B")
                                }
                                outlinePaint.strokeWidth = if (isNav) 22f else 18f
                            }
                            mapView.overlays.add(bgPolyline)

                            // Core glowing polyline (Electric Cyan / Royal Blue)
                            val mainPolyline = Polyline(mapView).apply {
                                setPoints(points)
                                outlinePaint.color = if (isNav) {
                                    android.graphics.Color.parseColor("#00E5FF")
                                } else {
                                    android.graphics.Color.parseColor("#2563EB")
                                }
                                outlinePaint.strokeWidth = if (isNav) 14f else 10f
                            }
                            mapView.overlays.add(mainPolyline)
                        }
                    }

                    // 2. Custom Sequenced Destination Badges
                    destinations.forEachIndexed { index, dest ->
                        val isEkspres = dest.priority == "EKSPRES"
                        val marker = Marker(mapView).apply {
                            position = dest.point
                            title = "${index + 1}. ${dest.title}"
                            snippet = dest.address
                            icon = createCustomMarkerDrawable(
                                context = context,
                                text = "${index + 1}",
                                isOrigin = false,
                                isEkspres = isEkspres
                            )
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                        mapView.overlays.add(marker)
                    }
                }

                // 3. Persistent User Vehicle Marker with Dynamic Top-Down Icon
                userVehicleMarkerRef?.let { marker ->
                    VehicleMarkerHelper.configureVehicleMarker(marker, context, vehicleType)
                    marker.snippet = if (isNav) "Navigasi Berjalan" else "Titik Mulai"
                    if (!mapView.overlays.contains(marker)) {
                        mapView.overlays.add(marker)
                    }
                }

                // Keep animator settings aligned with navigation state
                markerAnimatorRef?.let { animator ->
                    animator.followCamera = isNav
                    animator.followCameraOrientation = isNav
                }

                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // -------------------------------------------------------------------------
        // NAVIGATION MODE: SLEEK TURN-BY-TURN TOP HUD BANNER
        // -------------------------------------------------------------------------
        AnimatedVisibility(
            visible = navigationState.isNavigating,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nav_mode_top_panel")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Maneuver Icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Maneuver",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lanjut %.1f km".format(navigationState.distanceToNextKm),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Stop ${navigationState.nextStopIndex}/${navigationState.totalStops}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = navigationState.currentDestination?.address ?: "Menuju destinasi",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }

                    // Exit Nav Button
                    IconButton(
                        onClick = { sharedViewModel.stopNavigationMode() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("nav_exit_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------------------
        // OVERVIEW MODE: FLOATING MINIMALIST SEARCH CAPSULE & HUD PILLS
        // -------------------------------------------------------------------------
        AnimatedVisibility(
            visible = !navigationState.isNavigating,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Floating Search Pill
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { sharedViewModel.searchAddress(it) },
                            placeholder = { Text("Cari alamat...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                sharedViewModel.searchAddress(searchQuery)
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("map_search_input")
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { sharedViewModel.clearSearchResults() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Search Action Button
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                sharedViewModel.searchAddress(searchQuery)
                            },
                            enabled = !isSearching && searchQuery.isNotBlank(),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("map_search_btn")
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Cari",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Instant Geocoding Results Dropdown
                AnimatedVisibility(visible = searchResults.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            searchResults.take(4).forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            keyboardController?.hide()
                                            sharedViewModel.addDestination(item)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddLocation,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.displayName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+ Tambah",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            }
                        }
                    }
                }

                // Floating VRP Quick Status & Re-Optimize Pill
                AnimatedVisibility(visible = destinations.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 3.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "VRP",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${destinations.size} Titik • VRP AI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { sharedViewModel.recalculateRouteWithOnnx() },
                                    enabled = !isOptimizing,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("map_reoptimize_btn")
                                ) {
                                    if (isOptimizing) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Re-Optimize",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------------------
        // FLOATING ACTION CONTROLS (Zoom & Recenter)
        // -------------------------------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { mapViewRef?.controller?.zoomIn() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("map_zoom_in_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            FloatingActionButton(
                onClick = { mapViewRef?.controller?.zoomOut() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("map_zoom_out_btn")
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }

            FloatingActionButton(
                onClick = {
                    requestLocationUpdates()
                    val pos = markerAnimatorRef?.currentPosition ?: userLocation ?: defaultCenter
                    mapViewRef?.controller?.animateTo(pos)
                    sharedViewModel.updateCameraState(pos, mapViewRef?.zoomLevelDouble ?: 16.0)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("map_recenter_btn")
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "GPS Recenter", modifier = Modifier.size(20.dp))
            }
        }

        // -------------------------------------------------------------------------
        // FLOATING BOTTOM DASHBOARD & EXPANDABLE STOPS SHEET
        // -------------------------------------------------------------------------
        AnimatedVisibility(
            visible = destinations.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.BottomCenter)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (navigationState.isNavigating) Color(0xFF0F172A) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (navigationState.isNavigating) Color(0xFF00E5FF).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .testTag("map_bottom_dashboard")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Total Distance
                        Column {
                            Text(
                                text = String.format(
                                    "%.1f km",
                                    if (navigationState.isNavigating) navigationState.remainingTotalDistanceKm else (routePolyline?.totalDistanceKm ?: 0.0)
                                ),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (navigationState.isNavigating) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (navigationState.isNavigating) "Sisa Jarak" else "Total Rute",
                                fontSize = 11.sp,
                                color = if (navigationState.isNavigating) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // ETA Metric
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(
                                    "%.0f min",
                                    if (navigationState.isNavigating) navigationState.remainingDurationMin else (routePolyline?.totalDurationMin ?: 0.0)
                                ),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (navigationState.isNavigating) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Est. Waktu",
                                fontSize = 11.sp,
                                color = if (navigationState.isNavigating) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Toggle Expand Destinations (Only in non-navigating mode)
                        if (!navigationState.isNavigating) {
                            IconButton(
                                onClick = { isDestinationsExpanded = !isDestinationsExpanded },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDestinationsExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = "Toggle Stops",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Start/Stop Navigation Button
                        Button(
                            onClick = {
                                if (navigationState.isNavigating) {
                                    sharedViewModel.stopNavigationMode()
                                } else {
                                    requestLocationUpdates()
                                    sharedViewModel.startNavigationMode()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (navigationState.isNavigating) Color(0xFFEF4444) else Color(0xFF10B981)
                            ),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("nav_start_stop_btn")
                        ) {
                            Icon(
                                imageVector = if (navigationState.isNavigating) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (navigationState.isNavigating) "Keluar" else "Mulai",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (navigationState.isNavigating) "Keluar" else "Mulai",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Expandable Sequenced Stops Drawer
                    if (isDestinationsExpanded && !navigationState.isNavigating) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(modifier = Modifier.height(120.dp)) {
                            itemsIndexed(destinations, key = { _, item -> item.id }) { idx, item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (item.priority == "EKSPRES") Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${idx + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                                        Text(item.address, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    IconButton(
                                        onClick = { sharedViewModel.removeDestination(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates high-contrast custom Vector-like bitmap drawables for OpenStreetMap markers on the fly.
 */
private fun createCustomMarkerDrawable(
    context: Context,
    text: String,
    isOrigin: Boolean = false,
    isEkspres: Boolean = false
): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = if (isOrigin) (32 * density).toInt() else (36 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    if (isOrigin) {
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        // Outer glow halo
        paint.color = android.graphics.Color.parseColor("#4000E5FF")
        canvas.drawCircle(cx, cy, sizePx / 2.1f, paint)

        // Core cyan puck
        paint.color = android.graphics.Color.parseColor("#00E5FF")
        canvas.drawCircle(cx, cy, sizePx / 3.0f, paint)

        // White center dot
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(cx, cy, sizePx / 6.5f, paint)
    } else {
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val radius = sizePx / 2.3f

        // Outer border
        paint.color = android.graphics.Color.parseColor("#0F172A")
        canvas.drawCircle(cx, cy, radius, paint)

        // Solid badge fill
        val fillColor = if (isEkspres) {
            android.graphics.Color.parseColor("#EF4444")
        } else {
            android.graphics.Color.parseColor("#10B981")
        }
        paint.color = fillColor
        canvas.drawCircle(cx, cy, radius - (2 * density), paint)

        // Number Text
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 13 * density
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER

        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val textY = cy - textBounds.exactCenterY()
        canvas.drawText(text, cx, textY, paint)
    }

    return BitmapDrawable(context.resources, bitmap)
}
