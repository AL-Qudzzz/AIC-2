package com.example.ui.components

import androidx.compose.foundation.clickable
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
import com.example.data.model.VehicleProfile

@Composable
fun VehicleProfileDialog(
    currentProfile: VehicleProfile,
    onDismiss: () -> Unit,
    onConfirm: (VehicleProfile) -> Unit
) {
    var vehicleName by remember { mutableStateOf(currentProfile.vehicleName) }
    var vehicleType by remember { mutableStateOf(currentProfile.vehicleType) }
    var fuelEfficiencyText by remember { mutableStateOf(currentProfile.fuelEfficiencyKmPerL.toString()) }
    var fuelType by remember { mutableStateOf(currentProfile.fuelType) }
    var fuelPriceText by remember { mutableStateOf(currentProfile.fuelPricePerL.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("vehicle_profile_dialog"),
        title = {
            Text(text = "Pengaturan Profil Kendaraan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = vehicleName,
                    onValueChange = { vehicleName = it },
                    label = { Text("Nama Kendaraan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_vehicle_name"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Jenis Kendaraan:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                listOf("Motor Matik", "Motor Bebek/Sport", "Mobil Van / Box").forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vehicleType = type
                                if (type.contains("Motor")) {
                                    fuelEfficiencyText = if (type.contains("Matik")) "40" else "48"
                                } else {
                                    fuelEfficiencyText = "12"
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (vehicleType == type), onClick = { vehicleType = type })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = type, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fuelEfficiencyText,
                    onValueChange = { fuelEfficiencyText = it },
                    label = { Text("Konsumsi BBM (km/Liter)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fuel_efficiency"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Jenis Bahan Bakar:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                listOf(
                    "Pertalite" to 10000,
                    "Pertamax" to 12500,
                    "Solar" to 6800
                ).forEach { (fuel, price) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                fuelType = fuel
                                fuelPriceText = price.toString()
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (fuelType == fuel), onClick = {
                            fuelType = fuel
                            fuelPriceText = price.toString()
                        })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "$fuel (Rp $price/L)", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val eff = fuelEfficiencyText.toDoubleOrNull() ?: 40.0
                    val price = fuelPriceText.toDoubleOrNull() ?: 10000.0
                    onConfirm(
                        VehicleProfile(
                            vehicleName = vehicleName.ifBlank { "Motor Kurir" },
                            vehicleType = vehicleType,
                            fuelEfficiencyKmPerL = eff,
                            fuelType = fuelType,
                            fuelPricePerL = price
                        )
                    )
                },
                modifier = Modifier.testTag("save_vehicle_profile_btn")
            ) {
                Text("Simpan Profil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_vehicle_profile_btn")) {
                Text("Batal")
            }
        }
    )
}
