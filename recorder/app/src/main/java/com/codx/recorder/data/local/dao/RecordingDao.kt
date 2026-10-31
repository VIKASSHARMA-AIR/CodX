package com.codx.recorder.data.local.dao

import androidx.room.*
import com.codx.recorder.data.local.entities.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordingsFlow(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE uploadStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingUploads(): List<RecordingEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(recording: RecordingEntity): Long

    @Update
    suspend fun update(recording: RecordingEntity)

    @Query("UPDATE recordings SET uploadStatus = :status, uploadedAt = :uploadedAt WHERE id = :id")
    suspend fun updateUploadStatus(id: String, status: String, uploadedAt: Long? = null)

    @Delete
    suspend fun delete(recording: RecordingEntity)
}
