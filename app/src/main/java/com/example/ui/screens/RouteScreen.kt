package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.data.model.DeliveryPackage
import com.example.data.model.TrafficAlert
import com.example.data.model.VehicleProfile
import com.example.ui.components.PackageCard
import com.example.ui.components.TrafficAlertCard
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RouteScreen(
    packages: List<DeliveryPackage>,
    trafficAlerts: List<TrafficAlert>,
    vehicleProfile: VehicleProfile,
    onOptimizeRouteClick: () -> Unit,
    onPackageStatusUpdateClick: (DeliveryPackage) -> Unit,
    onAddPackageClick: () -> Unit,
    onApplyAlternateRoute: (String) -> Unit,
    onDismissAlert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("SEMUA") }

    val filteredPackages = when (selectedFilter) {
        "BELUM" -> packages.filter { it.status == "BELUM_DIMULAI" || it.status == "DALAM_PERJALANAN" }
        "SUKSES" -> packages.filter { it.status == "TERKIRIM" }
        "GAGAL" -> packages.filter { it.status == "GAGAL_KIRIM" }
        "HIGH_RISK" -> packages.filter { it.failureRiskLevel == "HIGH" }
        else -> packages
    }

    val estKm = if (packages.isEmpty()) 0.0 else (packages.size * 3.1)
    val estLiters = estKm / vehicleProfile.fuelEfficiencyKmPerL
    val estCost = estLiters * vehicleProfile.fuelPricePerL
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    val terkirimCount = packages.count { it.status == "TERKIRIM" }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPackageClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_package")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Paket")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // AI Shift Overview Metric Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Status Pengiriman Shift",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${packages.size} Paket Aktif",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Button(
                                onClick = onOptimizeRouteClick,
                                enabled = packages.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_optimize_vrp")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Optimize",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "VRP AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metric Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Estimasi Jarak", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "%.1f km".format(estKm),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column {
                                Text("Est. Biaya BBM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    currencyFormat.format(estCost).replace(",00", ""),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column {
                                Text("Progres", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$terkirimCount / ${packages.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }

            // AI Traffic Alerts
            items(trafficAlerts.filter { it.isActive }) { alert ->
                Spacer(modifier = Modifier.height(8.dp))
                TrafficAlertCard(
                    alert = alert,
                    onApplyAlternateRoute = { onApplyAlternateRoute(alert.id) },
                    onDismiss = { onDismissAlert(alert.id) }
                )
            }

            // Filter Chips (Only shown if packages exist)
            if (packages.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = (selectedFilter == "SEMUA"),
                            onClick = { selectedFilter = "SEMUA" },
                            label = { Text("Semua (${packages.size})", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_chip_semua")
                        )

                        FilterChip(
                            selected = (selectedFilter == "BELUM"),
                            onClick = { selectedFilter = "BELUM" },
                            label = {
                                Text(
                                    "Pending (${packages.count { it.status == "BELUM_DIMULAI" || it.status == "DALAM_PERJALANAN" }})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_chip_belum")
                        )

                        FilterChip(
                            selected = (selectedFilter == "HIGH_RISK"),
                            onClick = { selectedFilter = "HIGH_RISK" },
                            label = {
                                Text(
                                    "Risiko (${packages.count { it.failureRiskLevel == "HIGH" }})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEF4444),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_chip_high_risk")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Package Cards List
                items(filteredPackages, key = { it.id }) { pkg ->
                    PackageCard(
                        pkg = pkg,
                        onStatusUpdateClick = { onPackageStatusUpdateClick(pkg) }
                    )
                }
            } else {
                // Sleek Empty State Card for New App state
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("empty_packages_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = "Belum Ada Paket",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Belum Ada Paket Pengiriman",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Daftar pengiriman shift Anda masih kosong. Tambahkan paket tujuan pertama Anda untuk memulai rute pengiriman.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onAddPackageClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("empty_state_add_package_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tambah Paket Pertama",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
