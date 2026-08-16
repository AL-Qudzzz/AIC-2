package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.ml.DeliveryFailureModelInference
import com.example.data.ml.ModelAssetManager
import com.example.data.ml.VrpModelInference
import com.example.data.model.ChatMessage
import com.example.data.model.DeliveryPackage
import com.example.data.model.TrafficAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class DeliveryRepository(
    private val db: AppDatabase,
    context: Context
) {

    val assetManager = ModelAssetManager(context)
    val vrpInference = VrpModelInference(assetManager)
    val failureInference = DeliveryFailureModelInference(assetManager)

    val packagesFlow: Flow<List<DeliveryPackage>> = db.packageDao().getAllPackages()
    val chatMessagesFlow: Flow<List<ChatMessage>> = db.chatDao().getAllMessages()

    private val _lastVrpResult = MutableStateFlow<VrpModelInference.VrpOptimizationResult?>(null)
    val lastVrpResult: StateFlow<VrpModelInference.VrpOptimizationResult?> = _lastVrpResult.asStateFlow()

    suspend fun initializeSampleDataIfEmpty() {
        val currentPackages = packagesFlow.first()
        if (currentPackages.isEmpty()) {
            val rawSamplePackages = listOf(
                DeliveryPackage(
                    id = "PKG-101",
                    trackingNumber = "RW-8839201",
                    recipientName = "Bpk. Hendra Kurniawan",
                    phone = "081298765432",
                    address = "Jl. Sawah Raya No. 12, RT 02/03",
                    subDistrict = "Sawah, Ciputat, Tangsel",
                    sequence = 1,
                    status = "TERKIRIM",
                    priority = "REGULER",
                    timeSlot = "08:30 - 10:30",
                    failureRiskLevel = "LOW",
                    lat = -6.2985,
                    lng = 106.7321,
                    deliveryProofNotes = "Diterima Ybs langsung",
                    completedTimestamp = System.currentTimeMillis() - 3600000
                ),
                DeliveryPackage(
                    id = "PKG-102",
                    trackingNumber = "RW-8839202",
                    recipientName = "Ibu Siska Amelia",
                    phone = "081388112233",
                    address = "Jl. Cendrawasih I No. 45",
                    subDistrict = "Sawah, Ciputat, Tangsel",
                    sequence = 2,
                    status = "DALAM_PERJALANAN",
                    priority = "EKSPRES",
                    timeSlot = "10:00 - 12:00",
                    failureRiskLevel = "MEDIUM",
                    failureRiskReason = "Pelanggan sering istirahat jam 12:00 - 13:00",
                    recommendedAction = "Konfirmasi via WhatsApp sebelum menuju lokasi",
                    lat = -6.3012,
                    lng = 106.7350
                ),
                DeliveryPackage(
                    id = "PKG-103",
                    trackingNumber = "RW-8839203",
                    recipientName = "Bpk. Agus Setiawan",
                    phone = "085711223344",
                    address = "Gang Ki Hajar Dewantara No. 8",
                    subDistrict = "Sawah, Ciputat, Tangsel",
                    sequence = 3,
                    status = "BELUM_DIMULAI",
                    priority = "EKSPRES",
                    timeSlot = "10:30 - 12:30",
                    failureRiskLevel = "LOW",
                    lat = -6.3035,
                    lng = 106.7382
                ),
                DeliveryPackage(
                    id = "PKG-104",
                    trackingNumber = "RW-8839204",
                    recipientName = "Ibu Rina Wati (Paket #04)",
                    phone = "081233445566",
                    address = "Jl. Nuansa Elok No. 18",
                    subDistrict = "Sawah, Ciputat, Tangsel",
                    sequence = 4,
                    status = "BELUM_DIMULAI",
                    priority = "REGULER",
                    timeSlot = "13:00 - 15:00",
                    failureRiskLevel = "HIGH",
                    failureRiskReason = "Alamat dalam gang sempit & penerima sering kosong di atas jam 14:00",
                    recommendedAction = "Minta draf WA konfirmasi patokan atau titip satpam perumahan",
                    lat = -6.3060,
                    lng = 106.7410
                ),
                DeliveryPackage(
                    id = "PKG-105",
                    trackingNumber = "RW-8839205",
                    recipientName = "Bpk. Doni Pratama",
                    phone = "081900998877",
                    address = "Jl. Merak Raya No. 3",
                    subDistrict = "Sawah, Ciputat, Tangsel",
                    sequence = 5,
                    status = "BELUM_DIMULAI",
                    priority = "REGULER",
                    timeSlot = "14:00 - 16:00",
                    failureRiskLevel = "LOW",
                    lat = -6.3088,
                    lng = 106.7435
                ),
                DeliveryPackage(
                    id = "PKG-106",
                    trackingNumber = "RW-8839206",
                    recipientName = "Ibu Maya Rosita",
                    phone = "082155667788",
                    address = "Jl. Ki Hajar Dewantara No. 88",
                    subDistrict = "Sawah, Ciputat, Tangsel",
                    sequence = 6,
                    status = "BELUM_DIMULAI",
                    priority = "REGULER",
                    timeSlot = "15:00 - 17:00",
                    failureRiskLevel = "LOW",
                    lat = -6.3110,
                    lng = 106.7460
                )
            )

            // Evaluate failure risk using ML inference engine (FR-7)
            val evaluatedPackages = failureInference.evaluatePackageList(rawSamplePackages)
            db.packageDao().insertPackages(evaluatedPackages)
        }

        val currentChats = chatMessagesFlow.first()
        if (currentChats.isEmpty()) {
            val modelSummary = assetManager.getModelSummaryText()
            db.chatDao().insertMessage(
                ChatMessage(
                    sender = "COPILOT",
                    text = "Halo Bro Kurir! 🚚 Saya RouteWise AI Copilot.\n" +
                            "Model ML Aktif: $modelSummary.\n" +
                            "Ada ${currentPackages.size.ifZero(6)} paket hari ini. Tanyakan apa saja seperti rekomendasi rute (VRP Engine), prediksi kegagalan (FR-7), atau cek bensin!"
                )
            )
        }
    }

    private fun Int.ifZero(default: Int) = if (this == 0) default else this

    suspend fun updatePackageStatus(
        id: String,
        status: String,
        failureReason: String? = null,
        notes: String? = null
    ) {
        val timestamp = if (status == "TERKIRIM" || status == "GAGAL_KIRIM") System.currentTimeMillis() else null
        db.packageDao().updatePackageStatus(id, status, timestamp, failureReason, notes)
    }

    suspend fun insertPackage(pkg: DeliveryPackage) {
        // Run Failure Risk Model prediction on new package before insertion
        val pred = failureInference.predictFailureRisk(
            address = pkg.address,
            timeSlot = pkg.timeSlot,
            recipientName = pkg.recipientName,
            priority = pkg.priority
        )
        val enrichedPkg = pkg.copy(
            failureRiskLevel = pred.riskLevel,
            failureRiskReason = pkg.failureRiskReason ?: pred.riskReason,
            recommendedAction = pkg.recommendedAction ?: pred.recommendedAction
        )
        db.packageDao().insertPackage(enrichedPkg)
    }

    suspend fun addChatMessage(sender: String, text: String) {
        db.chatDao().insertMessage(ChatMessage(sender = sender, text = text))
    }

    suspend fun clearChatHistory() {
        db.chatDao().clearHistory()
    }

    /**
     * VRP Engine Optimization (FR-1)
     * Calls VrpModelInference using `vrp_model.joblib` (RandomForestRegressor) & `vrp_scaler.joblib`.
     */
    suspend fun optimizeRouteSequence(): VrpModelInference.VrpOptimizationResult {
        val currentList = db.packageDao().getAllPackages().first()
        val result = vrpInference.optimizeRoute(currentList)
        db.packageDao().insertPackages(result.optimizedPackages)
        _lastVrpResult.value = result
        return result
    }

    fun getTrafficAlerts(): List<TrafficAlert> {
        return listOf(
            TrafficAlert(
                id = "TA-01",
                roadName = "Jl. Sawah Raya - Simpang Tiga",
                congestionLevel = "Padat Merayap",
                timeEstimateDelayMins = 12,
                alternativeRoute = "Putar lewat Jl. Cendrawasih I -> Gang Masjid",
                timeSavedMins = 8,
                isActive = true
            ),
            TrafficAlert(
                id = "TA-02",
                roadName = "Jl. Ki Hajar Dewantara (Depan Pasar)",
                congestionLevel = "Macet Berat (Bongkar Muat)",
                timeEstimateDelayMins = 15,
                alternativeRoute = "Gunakan rute tikus Jl. Nuansa Elok",
                timeSavedMins = 10,
                isActive = true
            )
        )
    }
}
