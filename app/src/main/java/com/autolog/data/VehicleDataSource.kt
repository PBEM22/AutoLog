package com.autolog.data

import kotlinx.coroutines.flow.StateFlow

enum class GearPosition { P, D, R, N }

data class VehicleState(
    val gear: GearPosition = GearPosition.P,
    val odometer: Float = 45210f,
    val fuelLevel: Float = 65f,
    val isEngineOn: Boolean = false
)

interface VehicleDataSource {
    val vehicleState: StateFlow<VehicleState>
    fun startEngine()
    fun stopEngine()
    fun setGear(gear: GearPosition)
    fun simulateDrive(distanceKm: Float)
}
