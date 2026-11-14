package com.autolog.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RentalStatus { ACTIVE, COMPLETED }

@Entity(tableName = "rental_trips")
data class RentalTripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val driverName: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val startOdometer: Float = 0f,
    val endOdometer: Float? = null,
    val startFuelPct: Float = 0f,
    val endFuelPct: Float? = null,
    val origin: String = "",
    val destination: String = "",
    val purpose: String = "미분류",
    val status: String = RentalStatus.ACTIVE.name
) {
    val distanceKm: Float get() = (endOdometer ?: startOdometer) - startOdometer
    val isCompleted: Boolean get() = status == RentalStatus.COMPLETED.name
    val durationMinutes: Long get() {
        val end = endTime ?: System.currentTimeMillis()
        return (end - startTime) / 60_000
    }
    // ponytail: fuelConsumedLiters uses tank-size ratio fallback; replace with actual litre sensor when available
    val fuelConsumedLiters: Float get() {
        val endFuel = endFuelPct ?: startFuelPct
        return if (startFuelPct > endFuel) (startFuelPct - endFuel) * 0.5f
        else distanceKm / 12f
    }
    val isEstimatedFuel: Boolean get() = endFuelPct == null || endFuelPct >= startFuelPct
}
