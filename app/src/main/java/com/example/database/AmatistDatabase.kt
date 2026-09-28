package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class AmatistDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AmatistDatabase? = null

        fun getDatabase(context: Context): AmatistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AmatistDatabase::class.java,
                    "amatist_audio_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
