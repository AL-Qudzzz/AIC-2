package com.example.data.model

/**
 * Supported vehicle types for navigation marker representation and routing profile.
 */
enum class VehicleType(val displayName: String) {
    MOTORCYCLE("Sepeda Motor"),
    CAR("Mobil / Sedan"),
    TRUCK("Mobil Van / Truk Box");

    companion object {
        /**
         * Maps a vehicle profile string or name to a VehicleType enum.
         */
        fun fromProfileString(typeString: String?): VehicleType {
            if (typeString.isNullOrBlank()) return MOTORCYCLE
            val lower = typeString.lowercase()
            return when {
                lower.contains("truk") || lower.contains("truck") || lower.contains("box") || lower.contains("van") -> TRUCK
                lower.contains("mobil") || lower.contains("car") || lower.contains("sedan") -> CAR
                lower.contains("motor") || lower.contains("bike") || lower.contains("scooter") -> MOTORCYCLE
                else -> MOTORCYCLE
            }
        }
    }
}
