package com.codx.controller.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.codx.controller.data.local.dao.ControllerDao
import com.codx.controller.data.local.entities.CachedRecordingEntity
import com.codx.controller.data.local.entities.CachedScheduleEntity
import com.codx.controller.data.local.entities.CachedStatusEntity

@Database(
    entities = [
        CachedStatusEntity::class,
        CachedScheduleEntity::class,
        CachedRecordingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ControllerDatabase : RoomDatabase() {

    abstract fun controllerDao(): ControllerDao

    companion object {
        @Volatile
        private var INSTANCE: ControllerDatabase? = null

        fun getDatabase(context: Context): ControllerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ControllerDatabase::class.java,
                    "codx_controller.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
