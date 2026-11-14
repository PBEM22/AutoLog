package com.autolog.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "frequent_places")
data class FrequentPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val visitCount: Int = 0
)
