package com.autolog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RentalTripEntity::class, FrequentPlaceEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rentalTripDao(): RentalTripDao
    abstract fun frequentPlaceDao(): FrequentPlaceDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "autolog.db")
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
