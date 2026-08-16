package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DeliveryPackage
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageDao {
    @Query("SELECT * FROM delivery_packages ORDER BY sequence ASC")
    fun getAllPackages(): Flow<List<DeliveryPackage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackages(packages: List<DeliveryPackage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: DeliveryPackage)

    @Update
    suspend fun updatePackage(pkg: DeliveryPackage)

    @Query("UPDATE delivery_packages SET status = :status, completedTimestamp = :timestamp, failureReason = :failureReason, deliveryProofNotes = :notes WHERE id = :id")
    suspend fun updatePackageStatus(id: String, status: String, timestamp: Long?, failureReason: String?, notes: String?)

    @Query("DELETE FROM delivery_packages WHERE id = :id")
    suspend fun deletePackage(id: String)

    @Query("DELETE FROM delivery_packages")
    suspend fun deleteAllPackages()
}
