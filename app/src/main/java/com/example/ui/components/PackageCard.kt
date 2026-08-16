package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryPackage
import com.example.ui.theme.AlertWarningBg
import com.example.ui.theme.AlertWarningBorder
import com.example.ui.theme.HighRiskBg
import com.example.ui.theme.HighRiskText
import com.example.ui.theme.StatusBelum
import com.example.ui.theme.StatusDalamPerjalanan
import com.example.ui.theme.StatusGagal
import com.example.ui.theme.StatusTerkirim

@Composable
fun PackageCard(
    pkg: DeliveryPackage,
    onStatusUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (statusColor, statusBg, statusText) = when (pkg.status) {
        "TERKIRIM" -> Triple(StatusTerkirim, Color(0xFFD1FAE5), "Terkirim 🟢")
        "DALAM_PERJALANAN" -> Triple(StatusDalamPerjalanan, Color(0xFFFEF3C7), "Dalam Perjalanan 🟡")
        "GAGAL_KIRIM" -> Triple(StatusGagal, Color(0xFFFEE2E2), "Gagal Kirim 🔴")
        else -> Triple(StatusBelum, Color(0xFFF1F5F9), "Belum Dimulai ⚪")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("package_card_${pkg.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Sequence Badge, Priority, Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${pkg.sequence}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (pkg.priority == "EKSPRES") {
                        Surface(
                            color = Color(0xFFDC2626),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "EKSPRES ⚡",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recipient & Address Info
            Text(
                text = pkg.recipientName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Alamat",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${pkg.address}, ${pkg.subDistrict}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Jam",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Slot Waktu: ${pkg.timeSlot} • ${pkg.packageType}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // High Risk Warning Box if applicable
            if (pkg.failureRiskLevel == "HIGH" || pkg.failureRiskLevel == "MEDIUM") {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (pkg.failureRiskLevel == "HIGH") HighRiskBg else AlertWarningBg)
                        .border(
                            1.dp,
                            if (pkg.failureRiskLevel == "HIGH") Color(0xFFFCA5A5) else AlertWarningBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Peringatan Risiko",
                                tint = if (pkg.failureRiskLevel == "HIGH") HighRiskText else Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Prediksi AI Risk: ${pkg.failureRiskReason ?: "Risiko Gagal Kirim"}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (pkg.failureRiskLevel == "HIGH") HighRiskText else Color(0xFFB45309)
                            )
                        }
                        if (!pkg.recommendedAction.isNullOrEmpty()) {
                            Text(
                                text = "💡 Saran AI: ${pkg.recommendedAction}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Proof notes if completed or failed
            if (!pkg.deliveryProofNotes.isNullOrEmpty() || !pkg.failureReason.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Catatan Status: ${pkg.deliveryProofNotes ?: pkg.failureReason}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Call & WhatsApp
                Row {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pkg.phone}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .testTag("call_button_${pkg.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Telepon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val waText = "Halo Bpk/Ibu ${pkg.recipientName}, saya kurir RouteWise mengonfirmasi pengiriman paket #${pkg.id} ke ${pkg.address}. Apakah Bpk/Ibu ada di lokasi?"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=${pkg.phone.replace("^0".toRegex(), "62")}&text=${Uri.encode(waText)}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape)
                            .testTag("whatsapp_button_${pkg.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Update Status Button
                Button(
                    onClick = onStatusUpdateClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("update_status_btn_${pkg.id}")
                ) {
                    Text(
                        text = "Update Status",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
