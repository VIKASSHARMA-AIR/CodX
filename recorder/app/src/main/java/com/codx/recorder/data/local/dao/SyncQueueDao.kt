package com.codx.recorder.data.local.dao

import androidx.room.*
import com.codx.recorder.data.local.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status IN ('QUEUED', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingTasks(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC LIMIT 50")
    fun getAllQueueFlow(): Flow<List<SyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(task: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status, retryCount = retryCount + 1, nextRetryTimestamp = :nextRetry WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String, nextRetry: Long)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteTask(id: String)
}
