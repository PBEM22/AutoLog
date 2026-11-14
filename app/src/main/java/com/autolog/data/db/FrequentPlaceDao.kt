package com.autolog.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FrequentPlaceDao {
    @Query("SELECT * FROM frequent_places ORDER BY visitCount DESC")
    fun getAllPlaces(): Flow<List<FrequentPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: FrequentPlaceEntity)

    @Delete
    suspend fun delete(place: FrequentPlaceEntity)
}
