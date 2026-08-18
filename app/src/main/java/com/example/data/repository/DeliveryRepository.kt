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
        // Purge any old sample packages so the app starts completely clean (0 packages)
        db.packageDao().deleteAllPackages()

        val currentChats = chatMessagesFlow.first()
        if (currentChats.isEmpty()) {
            val modelSummary = assetManager.getModelSummaryText()
            db.chatDao().insertMessage(
                ChatMessage(
                    sender = "COPILOT",
                    text = "Halo Bro Kurir! 🚚 Saya RouteWise AI Copilot.\n" +
                            "Model ML Aktif: $modelSummary.\n" +
                            "Aplikasi siap digunakan. Daftar paket pengiriman saat ini masih kosong. Silakan tambahkan paket baru untuk memulai perencanaan rute dan navigasi 3D!"
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
