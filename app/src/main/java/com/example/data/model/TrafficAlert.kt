package com.example.data.model

data class TrafficAlert(
    val id: String,
    val roadName: String,
    val subDistrict: String = "Kecamatan Sawah, Tangsel",
    val congestionLevel: String, // "Macet Berat", "Padat Merayap", "Konstruksi Jalan"
    val timeEstimateDelayMins: Int,
    val alternativeRoute: String,
    val timeSavedMins: Int,
    val isActive: Boolean = true
)
