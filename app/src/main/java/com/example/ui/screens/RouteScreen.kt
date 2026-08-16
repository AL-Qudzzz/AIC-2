package com.example.ui.screens

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

    val estKm = 18.5
    val estLiters = estKm / vehicleProfile.fuelEfficiencyKmPerL
    val estCost = estLiters * vehicleProfile.fuelPricePerL
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
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
            // Minimalist Route Header
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    text = "Rute VRP Sawah",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "18.5 km • Kec. Sawah, Tangsel",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = onOptimizeRouteClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("optimize_route_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Optimasi",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Optimasi AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Concise Stats Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("BBM", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("%.2f L".format(estLiters), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Column {
                                Text("Biaya", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(currencyFormat.format(estCost).replace(",00", ""), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            Column {
                                Text("Terkirim", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$terkirimCount / ${packages.size}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }

            // Traffic Alert Banners if active
            items(trafficAlerts.filter { it.isActive }) { alert ->
                Spacer(modifier = Modifier.height(10.dp))
                TrafficAlertCard(
                    alert = alert,
                    onApplyAlternateRoute = { onApplyAlternateRoute(alert.id) },
                    onDismiss = { onDismissAlert(alert.id) }
                )
            }

            // Minimalist Filter Chips
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = (selectedFilter == "SEMUA"),
                        onClick = { selectedFilter = "SEMUA" },
                        label = { Text("Semua (${packages.size})", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_semua")
                    )

                    FilterChip(
                        selected = (selectedFilter == "BELUM"),
                        onClick = { selectedFilter = "BELUM" },
                        label = { Text("Pending (${packages.count { it.status == "BELUM_DIMULAI" || it.status == "DALAM_PERJALANAN" }})", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_belum")
                    )

                    FilterChip(
                        selected = (selectedFilter == "HIGH_RISK"),
                        onClick = { selectedFilter = "HIGH_RISK" },
                        label = { Text("Risiko (${packages.count { it.failureRiskLevel == "HIGH" }})", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF4444),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_high_risk")
                    )
                }
            }

            // Minimalist Title
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Daftar Titik Pengiriman",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Package Cards
            items(filteredPackages, key = { it.id }) { pkg ->
                PackageCard(
                    pkg = pkg,
                    onStatusUpdateClick = { onPackageStatusUpdateClick(pkg) }
                )
            }
        }
    }
}
