package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DoseScheduleEntity
import com.example.data.model.MedicationEntity

@Database(
    entities = [MedicationEntity::class, DoseScheduleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DozumDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: DozumDatabase? = null

        fun getDatabase(context: Context): DozumDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DozumDatabase::class.java,
                    "dozum_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
