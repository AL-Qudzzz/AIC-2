package com.example.data.ml

import android.content.Context
import android.util.Log
import java.io.InputStream

/**
 * Manages loading and metadata verification for ML model files placed in `assets/models/`.
 * Handles `vrp_model.joblib`, `vrp_model.onnx`, `vrp_scaler.joblib`, and classification models according to RouteWise PRD.
 */
class ModelAssetManager(private val context: Context) {

    data class ModelMetadata(
        val filename: String,
        val sizeBytes: Long,
        val isLoaded: Boolean,
        val modelType: String
    )

    private val loadedModels = mutableMapOf<String, ModelMetadata>()

    init {
        loadModelAssetsInfo()
    }

    private fun loadModelAssetsInfo() {
        val assetFolder = "models"
        try {
            val files = context.assets.list(assetFolder) ?: emptyArray()
            for (filename in files) {
                if (filename.endsWith(".joblib") || filename.endsWith(".pkl") || 
                    filename.endsWith(".json") || filename.endsWith(".onnx")) {
                    val fullPath = "$assetFolder/$filename"
                    val size = getAssetFileSize(fullPath)
                    val modelType = when {
                        filename.contains("vrp_model") && filename.endsWith(".onnx") -> "VRP Engine (ONNX - FR-1)"
                        filename.contains("vrp_model") -> "VRP Engine (RandomForestRegressor - FR-1)"
                        filename.contains("vrp_scaler") -> "VRP Feature Scaler (StandardScaler - FR-1)"
                        filename.contains("failure") || filename.contains("risk") -> "Delivery Failure Classifier (FR-7)"
                        else -> "ML Model Asset"
                    }
                    loadedModels[filename] = ModelMetadata(
                        filename = filename,
                        sizeBytes = size,
                        isLoaded = size > 0,
                        modelType = modelType
                    )
                    Log.i("ModelAssetManager", "Loaded ML Asset: $filename (${size / 1024} KB) - $modelType")
                }
            }
        } catch (e: Exception) {
            Log.e("ModelAssetManager", "Error reading asset models folder", e)
        }
    }

    private fun getAssetFileSize(assetPath: String): Long {
        return try {
            context.assets.openFd(assetPath).use { it.length }
        } catch (e: Exception) {
            try {
                context.assets.open(assetPath).use { it.available().toLong() }
            } catch (ex: Exception) {
                0L
            }
        }
    }

    fun isModelAvailable(filename: String): Boolean {
        return loadedModels[filename]?.isLoaded == true
    }

    fun getModelMetadata(filename: String): ModelMetadata? {
        return loadedModels[filename]
    }

    fun getAllModelMetadata(): List<ModelMetadata> {
        return loadedModels.values.toList()
    }

    fun openAssetStream(filename: String): InputStream? {
        return try {
            context.assets.open("models/$filename")
        } catch (e: Exception) {
            Log.e("ModelAssetManager", "Failed to open model stream for $filename", e)
            null
        }
    }

    fun getModelBytes(filename: String): ByteArray? {
        return try {
            context.assets.open("models/$filename").use { it.readBytes() }
        } catch (e: Exception) {
            Log.e("ModelAssetManager", "Failed to read model bytes for $filename", e)
            null
        }
    }

    fun getModelSummaryText(): String {
        if (loadedModels.isEmpty()) return "Model Asset: Default Internal Rules (Assets empty)"
        return loadedModels.values.joinToString("; ") {
            "${it.filename} (${it.sizeBytes / 1024} KB - ${it.modelType})"
        }
    }
}
