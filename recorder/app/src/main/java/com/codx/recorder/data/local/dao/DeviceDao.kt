package com.codx.recorder.data.local.dao

import androidx.room.*
import com.codx.recorder.data.local.entities.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices LIMIT 1")
    suspend fun getLocalDevice(): DeviceEntity?

    @Query("SELECT * FROM devices LIMIT 1")
    fun getLocalDeviceFlow(): Flow<DeviceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: DeviceEntity)

    @Query("DELETE FROM devices")
    suspend fun clear()
}
