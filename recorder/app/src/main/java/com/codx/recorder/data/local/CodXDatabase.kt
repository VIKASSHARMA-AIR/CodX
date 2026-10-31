package com.codx.recorder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.codx.recorder.data.local.dao.*
import com.codx.recorder.data.local.entities.*

@Database(
    entities = [
        DeviceEntity::class,
        ScheduleEntity::class,
        RecordingSessionEntity::class,
        RecordingEntity::class,
        SyncQueueEntity::class,
        DeviceStatusEntity::class,
        SyncEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CodXDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun sessionDao(): RecordingSessionDao
    abstract fun recordingDao(): RecordingDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun deviceStatusDao(): DeviceStatusDao
    abstract fun syncEventDao(): SyncEventDao

    companion object {
        @Volatile
        private var INSTANCE: CodXDatabase? = null

        fun getDatabase(context: Context): CodXDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodXDatabase::class.java,
                    "codx_recorder.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
