package com.codx.recorder.data.local.dao

import androidx.room.*
import com.codx.recorder.data.local.entities.RecordingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingSessionDao {
    @Query("SELECT * FROM recording_sessions ORDER BY startedAt DESC")
    fun getAllSessionsFlow(): Flow<List<RecordingSessionEntity>>

    @Query("SELECT * FROM recording_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): RecordingSessionEntity?

    @Query("SELECT * FROM recording_sessions WHERE status = 'RECORDING' LIMIT 1")
    suspend fun getActiveSession(): RecordingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: RecordingSessionEntity)

    @Query("UPDATE recording_sessions SET endedAt = :endedAt, status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun completeSession(id: String, endedAt: Long, status: String, errorMessage: String? = null)
}
