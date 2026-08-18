package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.RouteWiseViewModel
import com.example.ui.SharedRouteViewModel
import com.example.ui.components.AddPackageDialog
import com.example.ui.components.StatusUpdateDialog
import com.example.ui.components.VehicleProfileDialog
import com.example.ui.screens.DailySummaryScreen
import com.example.ui.screens.FuelProfileScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.RouteScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RouteWiseViewModel by viewModels()
    private val sharedRouteViewModel: SharedRouteViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val packages by viewModel.packages.collectAsStateWithLifecycle()
                val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
                val vehicleProfile by viewModel.vehicleProfile.collectAsStateWithLifecycle()
                val trafficAlerts by viewModel.trafficAlerts.collectAsStateWithLifecycle()
                val isGeneratingResponse by viewModel.isGeneratingResponse.collectAsStateWithLifecycle()
                val selectedPackageForUpdate by viewModel.selectedPackageForUpdate.collectAsStateWithLifecycle()
                val showAddPackageDialog by viewModel.showAddPackageDialog.collectAsStateWithLifecycle()
                val showVehicleProfileDialog by viewModel.showVehicleProfileDialog.collectAsStateWithLifecycle()

                // Synchronize DB packages with SharedRouteViewModel
                androidx.compose.runtime.LaunchedEffect(packages) {
                    sharedRouteViewModel.syncFromDatabasePackages(packages)
                }

                // Synchronize active vehicle profile with SharedRouteViewModel
                androidx.compose.runtime.LaunchedEffect(vehicleProfile) {
                    sharedRouteViewModel.setVehicleType(vehicleProfile.typeEnum)
                }

                var currentTab by remember { mutableStateOf(NavTab.RUTE) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = "Logo",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "RouteWise AI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981))
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Shift Aktif • Tangsel",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.toggleVehicleProfileDialog(true) },
                                    modifier = Modifier.testTag("top_vehicle_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TwoWheeler,
                                        contentDescription = "Profil Kendaraan",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                modifier = Modifier.testTag("bottom_navigation_bar")
                            ) {
                                NavTab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = (currentTab == tab),
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            NavTab.RUTE -> RouteScreen(
                                packages = packages,
                                trafficAlerts = trafficAlerts,
                                vehicleProfile = vehicleProfile,
                                onOptimizeRouteClick = { viewModel.optimizeRoute() },
                                onPackageStatusUpdateClick = { viewModel.selectPackageForUpdate(it) },
                                onAddPackageClick = { viewModel.toggleAddPackageDialog(true) },
                                onApplyAlternateRoute = { viewModel.applyAlternateRoute(it) },
                                onDismissAlert = { viewModel.dismissTrafficAlert(it) }
                            )

                            NavTab.MAP -> MapScreen(sharedViewModel = sharedRouteViewModel)

                            NavTab.BBM -> FuelProfileScreen(
                                vehicleProfile = vehicleProfile,
                                onEditProfileClick = { viewModel.toggleVehicleProfileDialog(true) }
                            )

                            NavTab.SUMMARY -> DailySummaryScreen(
                                packages = packages,
                                vehicleProfile = vehicleProfile
                            )
                        }
                    }
                }

                // Status Update Dialog Modal
                selectedPackageForUpdate?.let { pkg ->
                    StatusUpdateDialog(
                        pkg = pkg,
                        onDismiss = { viewModel.selectPackageForUpdate(null) },
                        onConfirm = { id, status, reason, notes ->
                            viewModel.updatePackageStatus(id, status, reason, notes)
                        }
                    )
                }

                // Add Package Dialog Modal (Two-Way Integration: Rute -> Peta & Nominatim Geocoding)
                if (showAddPackageDialog) {
                    AddPackageDialog(
                        onDismiss = { viewModel.toggleAddPackageDialog(false) },
                        onConfirm = { name, addr, phone, prio, type ->
                            viewModel.toggleAddPackageDialog(false)
                            sharedRouteViewModel.addDestinationWithGeocoding(
                                address = addr,
                                recipientName = name,
                                phone = phone,
                                priority = prio,
                                packageType = type
                            )
                        }
                    )
                }

                // Vehicle Profile Dialog Modal
                if (showVehicleProfileDialog) {
                    VehicleProfileDialog(
                        currentProfile = vehicleProfile,
                        onDismiss = { viewModel.toggleVehicleProfileDialog(false) },
                        onConfirm = { updated ->
                            viewModel.updateVehicleProfile(updated)
                        }
                    )
                }
            }
        }
    }
}

enum class NavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    RUTE("Rute", Icons.Default.Route),
    MAP("Peta", Icons.Default.Map),
    BBM("BBM", Icons.Default.LocalGasStation),
    SUMMARY("Summary", Icons.Default.Assessment)
}
