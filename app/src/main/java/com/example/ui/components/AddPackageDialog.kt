package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

@Composable
fun AddPackageDialog(
    onDismiss: () -> Unit,
    onConfirm: (recipientName: String, address: String, phone: String, priority: String, packageType: String) -> Unit
) {
    var recipientName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("REGULER") }
    var packageType by remember { mutableStateOf("Paket Reguler (1 kg)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_package_dialog"),
        title = {
            Text(text = "Tambah Titik Pengiriman Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Nama Penerima") },
                    placeholder = { Text("Contoh: Bpk. Rizky") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_recipient_name"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat (Area Kec. Sawah Tangsel)") },
                    placeholder = { Text("Contoh: Jl. Sawah Raya Gang Buntu No. 5") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_address"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. Telepon / WhatsApp") },
                    placeholder = { Text("Contoh: 081234567890") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_phone"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Prioritas Pengiriman:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (priority == "REGULER"),
                            onClick = { priority = "REGULER" },
                            modifier = Modifier.testTag("radio_priority_reguler")
                        )
                        Text(text = "Reguler", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (priority == "EKSPRES"),
                            onClick = { priority = "EKSPRES" },
                            modifier = Modifier.testTag("radio_priority_ekspres")
                        )
                        Text(text = "Ekspres ⚡", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (recipientName.isNotBlank() && address.isNotBlank()) {
                        onConfirm(recipientName, address, phone.ifBlank { "081200000000" }, priority, packageType)
                    }
                },
                modifier = Modifier.testTag("save_package_btn")
            ) {
                Text("Tambah Paket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_add_package_btn")) {
                Text("Batal")
            }
        }
    )
}
