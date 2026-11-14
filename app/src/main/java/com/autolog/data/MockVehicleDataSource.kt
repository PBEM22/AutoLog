package com.autolog.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockVehicleDataSource : VehicleDataSource {
    private val _state = MutableStateFlow(VehicleState())
    override val vehicleState: StateFlow<VehicleState> = _state.asStateFlow()

    override fun startEngine() = _state.update { it.copy(isEngineOn = true) }

    override fun stopEngine() = _state.update { it.copy(isEngineOn = false, gear = GearPosition.P) }

    override fun setGear(gear: GearPosition) = _state.update { it.copy(gear = gear) }

    override fun simulateDrive(distanceKm: Float) = _state.update {
        it.copy(
            gear = GearPosition.D,
            odometer = it.odometer + distanceKm,
            fuelLevel = (it.fuelLevel - distanceKm / 12f).coerceAtLeast(0f)
        )
    }
}
