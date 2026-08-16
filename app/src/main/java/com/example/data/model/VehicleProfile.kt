package com.example.data.model

data class VehicleProfile(
    val vehicleName: String = "Honda Vario 125 (Motor)",
    val vehicleType: String = "Motor Matik", // "Motor Matik", "Motor Bebek", "Mobil Van / Box"
    val fuelEfficiencyKmPerL: Double = 40.0,
    val fuelType: String = "Pertalite", // "Pertalite", "Pertamax", "Solar"
    val fuelPricePerL: Double = 10000.0
)
