package com.autolog

import android.app.Application
import com.autolog.data.MockVehicleDataSource
import com.autolog.data.VehicleDataSource
import com.autolog.data.db.AppDatabase

class AutoLogApp : Application() {
    val vehicleDataSource: VehicleDataSource by lazy { MockVehicleDataSource() }
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
