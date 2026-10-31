package com.codx.recorder.data.local.dao

import androidx.room.*
import com.codx.recorder.data.local.entities.SyncEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncEventDao {
    @Query("SELECT * FROM sync_events ORDER BY timestamp DESC LIMIT 100")
    fun getAllEventsFlow(): Flow<List<SyncEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SyncEventEntity)

    @Query("DELETE FROM sync_events WHERE timestamp < :olderThanTimestamp")
    suspend fun pruneOldEvents(olderThanTimestamp: Long)
}
