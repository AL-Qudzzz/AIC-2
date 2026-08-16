package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "delivery_packages")
data class DeliveryPackage(
    @PrimaryKey val id: String, // e.g. "PKG-101"
    val trackingNumber: String,
    val recipientName: String,
    val phone: String,
    val address: String,
    val subDistrict: String = "Sawah, Tangerang Selatan",
    val sequence: Int, // Order index
    val status: String, // "BELUM_DIMULAI", "DALAM_PERJALANAN", "TERKIRIM", "GAGAL_KIRIM"
    val priority: String = "REGULER", // "REGULER", "EKSPRES"
    val timeSlot: String = "09:00 - 12:00",
    val failureRiskLevel: String = "LOW", // "LOW", "MEDIUM", "HIGH"
    val failureRiskReason: String? = null,
    val recommendedAction: String? = null,
    val lat: Double,
    val lng: Double,
    val deliveryProofNotes: String? = null,
    val failureReason: String? = null,
    val completedTimestamp: Long? = null,
    val packageType: String = "Paket Reguler (1-2 kg)"
)
