package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.local.AppDatabase
import com.example.data.ml.VrpModelInference
import com.example.data.model.ChatMessage
import com.example.data.model.DeliveryPackage
import com.example.data.model.TrafficAlert
import com.example.data.model.VehicleProfile
import com.example.data.repository.DeliveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RouteWiseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DeliveryRepository

    val packages: StateFlow<List<DeliveryPackage>>
    val chatMessages: StateFlow<List<ChatMessage>>
    val lastVrpResult: StateFlow<VrpModelInference.VrpOptimizationResult?>

    private val _vehicleProfile = MutableStateFlow(VehicleProfile())
    val vehicleProfile: StateFlow<VehicleProfile> = _vehicleProfile.asStateFlow()

    private val _trafficAlerts = MutableStateFlow<List<TrafficAlert>>(emptyList())
    val trafficAlerts: StateFlow<List<TrafficAlert>> = _trafficAlerts.asStateFlow()

    private val _isGeneratingResponse = MutableStateFlow(false)
    val isGeneratingResponse: StateFlow<Boolean> = _isGeneratingResponse.asStateFlow()

    private val _selectedPackageForUpdate = MutableStateFlow<DeliveryPackage?>(null)
    val selectedPackageForUpdate: StateFlow<DeliveryPackage?> = _selectedPackageForUpdate.asStateFlow()

    private val _showAddPackageDialog = MutableStateFlow(false)
    val showAddPackageDialog: StateFlow<Boolean> = _showAddPackageDialog.asStateFlow()

    private val _showVehicleProfileDialog = MutableStateFlow(false)
    val showVehicleProfileDialog: StateFlow<Boolean> = _showVehicleProfileDialog.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DeliveryRepository(database, application)
        lastVrpResult = repository.lastVrpResult

        packages = repository.packagesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        chatMessages = repository.chatMessagesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.initializeSampleDataIfEmpty()
            _trafficAlerts.value = repository.getTrafficAlerts()
        }
    }

    fun selectPackageForUpdate(pkg: DeliveryPackage?) {
        _selectedPackageForUpdate.value = pkg
    }

    fun updatePackageStatus(pkgId: String, status: String, failureReason: String? = null, notes: String? = null) {
        viewModelScope.launch {
            repository.updatePackageStatus(pkgId, status, failureReason, notes)
            _selectedPackageForUpdate.value = null
        }
    }

    fun optimizeRoute() {
        viewModelScope.launch {
            val result = repository.optimizeRouteSequence()
            val modelName = result.modelUsed
            val timeMs = result.predictedComputationTimeMs
            val dist = result.totalDistanceKm

            repository.addChatMessage(
                sender = "COPILOT",
                text = "⚡ **VRP Engine Optimization Selesai!**\n" +
                        "- Model: `$modelName`\n" +
                        "- Prediksi Comp. Time: `${timeMs} ms`\n" +
                        "- Total Jarak Rute: `$dist km`\n" +
                        "Urutan pengiriman telah diperbarui dengan memprioritaskan paket EKSPRES dan rute terpendek."
            )
        }
    }

    fun toggleAddPackageDialog(show: Boolean) {
        _showAddPackageDialog.value = show
    }

    fun toggleVehicleProfileDialog(show: Boolean) {
        _showVehicleProfileDialog.value = show
    }

    fun updateVehicleProfile(newProfile: VehicleProfile) {
        _vehicleProfile.value = newProfile
        _showVehicleProfileDialog.value = false
    }

    private val osrmRoutingService = com.example.data.service.OsrmRoutingService()

    fun addNewPackage(
        recipientName: String,
        address: String,
        phone: String,
        priority: String,
        packageType: String
    ) {
        viewModelScope.launch {
            val geocodedList = osrmRoutingService.searchAddressNominatim(address)
            val point = geocodedList.firstOrNull()?.point ?: org.osmdroid.util.GeoPoint(
                -6.3050 + (0.0050 * (Math.random() - 0.5)),
                106.7400 + (0.0050 * (Math.random() - 0.5))
            )
            val resolvedAddress = geocodedList.firstOrNull()?.displayName ?: address

            val currentList = packages.value
            val nextSeq = (currentList.maxOfOrNull { it.sequence } ?: 0) + 1
            val newPkg = DeliveryPackage(
                id = "PKG-${100 + nextSeq}",
                trackingNumber = "RW-${(1000000..9999999).random()}",
                recipientName = recipientName,
                phone = phone,
                address = resolvedAddress,
                subDistrict = "Sawah, Ciputat, Tangsel",
                sequence = nextSeq,
                status = "BELUM_DIMULAI",
                priority = priority,
                packageType = packageType,
                lat = point.latitude,
                lng = point.longitude
            )
            repository.insertPackage(newPkg)
            _showAddPackageDialog.value = false

            // Automatically run ML prediction update after adding package
            repository.optimizeRouteSequence()
        }
    }

    fun sendCopilotQuery(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            repository.addChatMessage(sender = "USER", text = query)
            _isGeneratingResponse.value = true

            val currentPkgs = packages.value
            val profile = vehicleProfile.value
            val vrpInfo = lastVrpResult.value
            val totalCount = currentPkgs.size
            val delivered = currentPkgs.count { it.status == "TERKIRIM" }
            val pending = currentPkgs.count { it.status == "BELUM_DIMULAI" || it.status == "DALAM_PERJALANAN" }
            val highRisk = currentPkgs.filter { it.failureRiskLevel == "HIGH" }
            val modelSummary = repository.assetManager.getModelSummaryText()

            val shiftContext = """
                ML Models Loaded: $modelSummary.
                VRP Engine Status: ${vrpInfo?.modelUsed ?: "RandomForestRegressor (vrp_model.joblib)"} (Pred. Latency: ${vrpInfo?.predictedComputationTimeMs ?: 12} ms).
                Total Paket: $totalCount ($delivered Terkirim, $pending Belum).
                Kendaraan: ${profile.vehicleName} (${profile.fuelEfficiencyKmPerL} km/L, BBM: ${profile.fuelType}).
                Risiko Gagal Kirim (FR-7 ML Model): ${highRisk.joinToString { "${it.recipientName} (${it.address}): ${it.failureRiskReason}" }}
                Lalu Lintas: ${_trafficAlerts.value.firstOrNull()?.roadName} (${_trafficAlerts.value.firstOrNull()?.congestionLevel}).
            """.trimIndent()

            val botResponse = GeminiApiClient.queryCopilot(query, shiftContext)

            repository.addChatMessage(sender = "COPILOT", text = botResponse)
            _isGeneratingResponse.value = false
        }
    }

    fun dismissTrafficAlert(alertId: String) {
        _trafficAlerts.value = _trafficAlerts.value.map {
            if (it.id == alertId) it.copy(isActive = false) else it
        }
    }

    fun applyAlternateRoute(alertId: String) {
        viewModelScope.launch {
            _trafficAlerts.value = _trafficAlerts.value.map {
                if (it.id == alertId) it.copy(isActive = false) else it
            }
            repository.optimizeRouteSequence()
            repository.addChatMessage(
                sender = "COPILOT",
                text = "Sip! Rute alternatif sudah diterapkan ke navigasi VRP. Menghemat estimasi 8 menit waktu tempuh dan terhindar dari kemacetan Sawah Raya."
            )
        }
    }
}
