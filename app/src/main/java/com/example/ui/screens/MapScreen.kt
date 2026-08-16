package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import android.preference.PreferenceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.DeliveryPackage
import com.example.data.model.PackageDestination
import com.example.data.service.GeocodedLocation
import com.example.ui.SharedRouteViewModel
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
    val isOptimizing by sharedViewModel.isOptimizing.collectAsStateWithLifecycle()
    val searchQuery by sharedViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by sharedViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by sharedViewModel.isSearching.collectAsStateWithLifecycle()

    val defaultCenter = remember { GeoPoint(-6.3025, 106.7210) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

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

    // Real-time location callback
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLoc = locationResult.lastLocation ?: return
                val newPoint = GeoPoint(lastLoc.latitude, lastLoc.longitude)
                sharedViewModel.setUserLocation(newPoint)

                // When navigating, follow user movement and keep centered
                if (navigationState.isNavigating) {
                    mapViewRef?.controller?.animateTo(newPoint)
                    if (lastLoc.hasBearing()) {
                        mapViewRef?.mapOrientation = -lastLoc.bearing
                    }
                }
            }
        }
    }

    fun requestLocationUpdates() {
        if (hasLocationPermission) {
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500L)
                    .setMinUpdateIntervalMillis(1000L)
                    .build()
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        sharedViewModel.setUserLocation(GeoPoint(loc.latitude, loc.longitude))
                    } else if (userLocation == null) {
                        sharedViewModel.setUserLocation(defaultCenter)
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
                    mv.zoomToBoundingBox(boundingBox, true, 80)
                } catch (e: Exception) {
                    // Fallback
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("map_screen_view")
    ) {
        // OpenStreetMap View Component - Preserves State across Tab Switches
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(cameraState.zoomLevel)
                    controller.setCenter(cameraState.center)
                    mapOrientation = cameraState.orientation

                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
                val currentOrigin = userLocation ?: cameraState.center

                // 1. Draw Snapped Polyline Road Route (Styled for Navigation or Overview)
                routePolyline?.points?.let { points ->
                    if (points.isNotEmpty()) {
                        val isNav = navigationState.isNavigating

                        // Dark outline polyline for high contrast
                        val bgPolyline = Polyline(mapView).apply {
                            setPoints(points)
                            outlinePaint.color = if (isNav) {
                                android.graphics.Color.parseColor("#0F172A") // Dark navy outline
                            } else {
                                android.graphics.Color.parseColor("#1E3A8A") // Deep Blue outline
                            }
                            outlinePaint.strokeWidth = if (isNav) 24f else 18f
                        }
                        mapView.overlays.add(bgPolyline)

                        // Core glowing polyline (Google Maps Driving Navigation cyan #00E5FF or #38BDF8)
                        val mainPolyline = Polyline(mapView).apply {
                            setPoints(points)
                            outlinePaint.color = if (isNav) {
                                android.graphics.Color.parseColor("#00E5FF") // High-contrast glowing cyan
                            } else {
                                android.graphics.Color.parseColor("#2563EB") // Standard Royal Blue
                            }
                            outlinePaint.strokeWidth = if (isNav) 16f else 12f
                        }
                        mapView.overlays.add(mainPolyline)
                    }
                }

                // 2. User Origin / Current GPS Location Marker
                val originMarker = Marker(mapView).apply {
                    position = currentOrigin
                    title = "Lokasi Anda (Origin)"
                    snippet = if (navigationState.isNavigating) "Navigasi Turn-by-Turn Aktif" else "Titik Awal Rute"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(originMarker)

                // 3. Destination Markers (Sequenced by ONNX Model)
                destinations.forEachIndexed { index, dest ->
                    val marker = Marker(mapView).apply {
                        position = dest.point
                        title = "Stop ${index + 1}: ${dest.title}"
                        snippet = "${dest.address} • (${dest.priority})"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(marker)
                }

                // Apply Navigation Perspective if active
                if (navigationState.isNavigating) {
                    mapView.controller.setZoom(18.0)
                    mapView.controller.animateTo(currentOrigin)
                }

                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // =========================================================================
        // NAVIGATION MODE TOP PANEL: Google Maps Driving Turn Instruction Banner
        // =========================================================================
        AnimatedVisibility(
            visible = navigationState.isNavigating,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Sleek Dark Navigation Header
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .testTag("nav_mode_top_panel")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Turn / Maneuver Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Maneuver",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lanjut %.1f km".format(navigationState.distanceToNextKm),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF059669), RoundedCornerShape(6.dp))
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
                            text = navigationState.currentDestination?.address ?: "Menuju destinasi berikutnya",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }

                    // Exit Navigation Icon Button
                    IconButton(
                        onClick = { sharedViewModel.stopNavigationMode() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
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

        // =========================================================================
        // NORMAL MODE TOP SEARCH & GEOCODING BAR
        // =========================================================================
        AnimatedVisibility(
            visible = !navigationState.isNavigating,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { sharedViewModel.searchAddress(it) },
                                placeholder = { Text("Ketik alamat destinasi (Nominatim)...", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { sharedViewModel.clearSearchResults() }) {
                                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    keyboardController?.hide()
                                    sharedViewModel.searchAddress(searchQuery)
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("map_search_input")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    sharedViewModel.searchAddress(searchQuery)
                                },
                                enabled = !isSearching && searchQuery.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("map_search_btn")
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = "Cari")
                                }
                            }
                        }

                        // Nominatim Search Suggestions Dropdown
                        AnimatedVisibility(visible = searchResults.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Hasil Nominatim Geocoding (Pilih untuk menambah):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                searchResults.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                keyboardController?.hide()
                                                sharedViewModel.addDestination(item)
                                            }
                                            .padding(vertical = 8.dp, horizontal = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddLocation,
                                            contentDescription = "Add",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.displayName,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                keyboardController?.hide()
                                                sharedViewModel.addDestination(item)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("+ Tambah", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }

                // Added Destinations List & Multi-Stop ONNX Info Card
                AnimatedVisibility(visible = destinations.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "ONNX",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Destinasi (${destinations.size}) • vrp_model.onnx",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Button(
                                    onClick = { sharedViewModel.recalculateRouteWithOnnx() },
                                    enabled = !isOptimizing,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.testTag("map_reoptimize_btn")
                                ) {
                                    if (isOptimizing) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White)
                                    } else {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Re-Optimize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(modifier = Modifier.height(100.dp)) {
                                itemsIndexed(destinations, key = { _, item -> item.id }) { idx, item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (item.priority == "EKSPRES") Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${idx + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                                if (item.priority == "EKSPRES") {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("⚡ Ekspres", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(item.address, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        IconButton(
                                            onClick = { sharedViewModel.removeDestination(item.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // MAP FLOATING ACTION BUTTONS (Zoom & GPS Recenter)
        // =========================================================================
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
                    .size(40.dp)
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
                    .size(40.dp)
                    .testTag("map_zoom_out_btn")
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }

            FloatingActionButton(
                onClick = {
                    requestLocationUpdates()
                    userLocation?.let {
                        mapViewRef?.controller?.animateTo(it)
                        sharedViewModel.updateCameraState(it, mapViewRef?.zoomLevelDouble ?: 15.0)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_recenter_btn")
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "GPS Recenter", modifier = Modifier.size(20.dp))
            }
        }

        // =========================================================================
        // BOTTOM DASHBOARD PANEL (Standard vs Google Maps Navigation Mode)
        // =========================================================================
        AnimatedVisibility(
            visible = destinations.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (navigationState.isNavigating) Color(0xFF0F172A) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (navigationState.isNavigating) Color(0xFF00E5FF).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(22.dp)
                    )
                    .testTag("map_bottom_dashboard")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total Distance Metric
                    Column {
                        Text(
                            text = String.format(
                                "%.1f km",
                                if (navigationState.isNavigating) navigationState.remainingTotalDistanceKm else (routePolyline?.totalDistanceKm ?: 0.0)
                            ),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (navigationState.isNavigating) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (navigationState.isNavigating) "Sisa Jarak" else "Jarak ONNX",
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (navigationState.isNavigating) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Est. Waktu",
                            fontSize = 11.sp,
                            color = if (navigationState.isNavigating) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // "Mulai" / "Selesai" Action Button
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
                            .height(48.dp)
                            .testTag("nav_start_stop_btn")
                    ) {
                        Icon(
                            imageVector = if (navigationState.isNavigating) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (navigationState.isNavigating) "Selesai" else "Mulai",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (navigationState.isNavigating) "Keluar" else "Mulai",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
