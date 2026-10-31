package com.codx.controller.data.local.dao

import androidx.room.*
import com.codx.controller.data.local.entities.CachedRecordingEntity
import com.codx.controller.data.local.entities.CachedScheduleEntity
import com.codx.controller.data.local.entities.CachedStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ControllerDao {

    @Query("SELECT * FROM cached_status WHERE recorderId = :recorderId")
    fun getStatusFlow(recorderId: String): Flow<CachedStatusEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStatus(status: CachedStatusEntity)

    @Query("SELECT * FROM cached_schedules WHERE recorderId = :recorderId ORDER BY startTime ASC")
    fun getSchedulesFlow(recorderId: String): Flow<List<CachedScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSchedules(schedules: List<CachedScheduleEntity>)

    @Query("DELETE FROM cached_schedules WHERE id = :id")
    suspend fun deleteSchedule(id: String)

    @Query("SELECT * FROM cached_recordings WHERE recorderId = :recorderId ORDER BY uploadedAt DESC")
    fun getRecordingsFlow(recorderId: String): Flow<List<CachedRecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecordings(recordings: List<CachedRecordingEntity>)

    @Query("UPDATE cached_recordings SET localFilePath = :path WHERE id = :id")
    suspend fun updateLocalPath(id: String, path: String)
}
