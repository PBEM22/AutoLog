package com.autolog.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RentalTripDao {
    @Query("SELECT * FROM rental_trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<RentalTripEntity>>

    @Query("SELECT * FROM rental_trips WHERE status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveTrip(): RentalTripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: RentalTripEntity): Long

    @Update
    suspend fun update(trip: RentalTripEntity)

    @Delete
    suspend fun delete(trip: RentalTripEntity)

    @Query("SELECT DISTINCT driverName FROM rental_trips WHERE driverName != '' ORDER BY startTime DESC LIMIT 5")
    suspend fun getRecentDrivers(): List<String>

    @Query("SELECT * FROM rental_trips ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentTrips(limit: Int = 200): List<RentalTripEntity>
}
