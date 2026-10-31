package com.codx.recorder.data.local.dao

import androidx.room.*
import com.codx.recorder.data.local.entities.DeviceStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceStatusDao {
    @Query("SELECT * FROM device_status WHERE id = 'LOCAL_DEVICE'")
    fun getStatusFlow(): Flow<DeviceStatusEntity?>

    @Query("SELECT * FROM device_status WHERE id = 'LOCAL_DEVICE'")
    suspend fun getStatus(): DeviceStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStatus(status: DeviceStatusEntity)
}
