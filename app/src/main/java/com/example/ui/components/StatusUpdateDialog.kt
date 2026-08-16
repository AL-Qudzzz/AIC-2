package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryPackage
import com.example.ui.theme.StatusDalamPerjalanan
import com.example.ui.theme.StatusGagal
import com.example.ui.theme.StatusTerkirim

@Composable
fun StatusUpdateDialog(
    pkg: DeliveryPackage,
    onDismiss: () -> Unit,
    onConfirm: (pkgId: String, status: String, failureReason: String?, notes: String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(pkg.status) }
    var failureReason by remember { mutableStateOf(pkg.failureReason ?: "Pelanggan Tidak di Tempat") }
    var notes by remember { mutableStateOf(pkg.deliveryProofNotes ?: "") }

    val failureReasonsList = listOf(
        "Pelanggan Tidak di Tempat",
        "Alamat Tidak Ditemukan / Salah",
        "Rumah / Toko Tutup",
        "Ditolak Penerima",
        "Gagal Dihubungi via Telepon / WA"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("status_update_dialog"),
        title = {
            Text(
                text = "Update Status Paket #${pkg.sequence}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${pkg.recipientName} (${pkg.address})",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pilih Status Terbaru:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                // Status Radio Options
                listOf(
                    "TERKIRIM" to "🟢 Terkirim (Sukses)",
                    "DALAM_PERJALANAN" to "🟡 Dalam Perjalanan",
                    "GAGAL_KIRIM" to "🔴 Gagal Kirim"
                ).forEach { (statusKey, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = statusKey }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedStatus == statusKey),
                            onClick = { selectedStatus = statusKey },
                            modifier = Modifier.testTag("radio_status_$statusKey")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (selectedStatus == statusKey) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // If Failed, select Failure Reason
                if (selectedStatus == "GAGAL_KIRIM") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Alasan Gagal Kirim:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = StatusGagal
                    )
                    failureReasonsList.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { failureReason = reason }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (failureReason == reason),
                                onClick = { failureReason = reason }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = reason, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes / Proof
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Bukti / Keterangan Tambahan") },
                    placeholder = { Text(if (selectedStatus == "TERKIRIM") "Misal: Dititipkan ke Mbak Satpam" else "Misal: Telp x3 tidak diangkat") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_notes_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedStatus == "GAGAL_KIRIM") failureReason else null
                    onConfirm(pkg.id, selectedStatus, finalReason, notes.ifBlank { null })
                },
                modifier = Modifier.testTag("confirm_status_update_btn")
            ) {
                Text("Simpan Status")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_status_update_btn")
            ) {
                Text("Batal")
            }
        }
    )
}
