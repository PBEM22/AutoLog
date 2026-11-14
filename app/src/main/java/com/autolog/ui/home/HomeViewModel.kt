package com.autolog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autolog.data.GearPosition
import com.autolog.data.VehicleDataSource
import com.autolog.data.db.RentalStatus
import com.autolog.data.db.RentalTripDao
import com.autolog.data.db.RentalTripEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Snapshot at rental-start, passed to TripSummaryDialog on return. */
data class RentalSummary(
    val trip: RentalTripEntity,
    val endOdometer: Float,
    val endFuelPct: Float
) {
    val distanceKm: Float get() = endOdometer - trip.startOdometer
    val durationMinutes: Long get() = (System.currentTimeMillis() - trip.startTime) / 60_000
}

class HomeViewModel(
    private val vehicleDataSource: VehicleDataSource,
    private val rentalTripDao: RentalTripDao
) : ViewModel() {

    val vehicleState = vehicleDataSource.vehicleState

    private val _trips = MutableStateFlow<List<RentalTripEntity>>(emptyList())
    val trips: StateFlow<List<RentalTripEntity>> = _trips.asStateFlow()

    // Currently open rental session (ACTIVE record in DB)
    private val _activeTrip = MutableStateFlow<RentalTripEntity?>(null)
    val activeTrip: StateFlow<RentalTripEntity?> = _activeTrip.asStateFlow()

    // Populated when P-gear triggers return dialog
    private val _pendingReturn = MutableStateFlow<RentalSummary?>(null)
    val pendingReturn: StateFlow<RentalSummary?> = _pendingReturn.asStateFlow()

    private val _recentDrivers = MutableStateFlow<List<String>>(emptyList())
    val recentDrivers: StateFlow<List<String>> = _recentDrivers.asStateFlow()

    private val _selectedDriver = MutableStateFlow("")
    val selectedDriver: StateFlow<String> = _selectedDriver.asStateFlow()

    private val _showSimulator = MutableStateFlow(false)
    val showSimulator: StateFlow<Boolean> = _showSimulator.asStateFlow()

    // Track whether we were driving this session (for P-gear trigger threshold)
    private var hadDrivenThisSession = false

    init {
        viewModelScope.launch {
            rentalTripDao.getAllTrips().collect { _trips.value = it }
        }
        viewModelScope.launch {
            _recentDrivers.value = rentalTripDao.getRecentDrivers()
            _selectedDriver.value = _recentDrivers.value.firstOrNull() ?: ""
        }
        // Restore any in-progress rental (e.g. app restart)
        viewModelScope.launch {
            rentalTripDao.getActiveTrip()?.let { _activeTrip.value = it }
        }
        // React to vehicle state changes
        viewModelScope.launch {
            vehicleState.collect { state ->
                val active = _activeTrip.value
                when (state.gear) {
                    GearPosition.D, GearPosition.R -> {
                        if (active == null) startRental()   // auto-start on drive if none open
                        else hadDrivenThisSession = true
                    }
                    GearPosition.P -> {
                        if (active != null && hadDrivenThisSession &&
                            state.odometer - active.startOdometer >= 0.5f
                        ) {
                            _pendingReturn.value = RentalSummary(
                                trip = active,
                                endOdometer = state.odometer,
                                endFuelPct = state.fuelLevel
                            )
                        }
                        hadDrivenThisSession = false
                    }
                    else -> {}
                }
            }
        }
    }

    fun selectDriver(name: String) = _selectedDriver.update { name }

    fun startRental() {
        if (_activeTrip.value != null) return
        val state = vehicleState.value
        viewModelScope.launch {
            val trip = RentalTripEntity(
                driverName = _selectedDriver.value,
                startOdometer = state.odometer,
                startFuelPct = state.fuelLevel,
                origin = "현재 위치",
                status = RentalStatus.ACTIVE.name
            )
            val id = rentalTripDao.insert(trip)
            _activeTrip.value = trip.copy(id = id.toInt())
            hadDrivenThisSession = false
            refreshDrivers()
        }
    }

    fun completeReturn(summary: RentalSummary, destination: String, purpose: String) {
        viewModelScope.launch {
            val completed = summary.trip.copy(
                endTime = System.currentTimeMillis(),
                endOdometer = summary.endOdometer,
                endFuelPct = summary.endFuelPct,
                destination = destination,
                purpose = purpose,
                status = RentalStatus.COMPLETED.name
            )
            rentalTripDao.update(completed)
            _activeTrip.value = null
            _pendingReturn.value = null
        }
    }

    fun dismissReturn() {
        viewModelScope.launch {
            _pendingReturn.value?.let { summary ->
                rentalTripDao.update(
                    summary.trip.copy(
                        endTime = System.currentTimeMillis(),
                        endOdometer = summary.endOdometer,
                        endFuelPct = summary.endFuelPct,
                        purpose = "미분류",
                        status = RentalStatus.COMPLETED.name
                    )
                )
            }
            _activeTrip.value = null
            _pendingReturn.value = null
        }
    }

    private suspend fun refreshDrivers() {
        _recentDrivers.value = rentalTripDao.getRecentDrivers()
    }

    // Simulator controls
    fun toggleSimulator() = _showSimulator.update { !it }
    fun simulatorStartEngine() { vehicleDataSource.startEngine(); startRental() }
    fun simulatorStopEngine() = vehicleDataSource.stopEngine()
    fun simulatorDrive() = vehicleDataSource.simulateDrive(18.4f)
    fun simulatorPark() = vehicleDataSource.setGear(GearPosition.P)

    class Factory(
        private val vehicleDataSource: VehicleDataSource,
        private val rentalTripDao: RentalTripDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(vehicleDataSource, rentalTripDao) as T
    }
}
