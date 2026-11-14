package com.autolog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autolog.ui.export.ExportScreen
import com.autolog.ui.export.ExportViewModel
import com.autolog.ui.home.HomeScreen
import com.autolog.ui.home.HomeViewModel
import com.autolog.ui.theme.AutoLogTheme
import com.autolog.ui.trip.TripSummaryOverlay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AutoLogApp
        val rentalTripDao = app.database.rentalTripDao()
        val vehicleDataSource = app.vehicleDataSource

        setContent {
            AutoLogTheme {
                var showExport by remember { mutableStateOf(false) }

                val homeVm: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(vehicleDataSource, rentalTripDao)
                )
                val exportVm: ExportViewModel = viewModel(
                    factory = ExportViewModel.Factory(rentalTripDao)
                )

                val pendingReturn by homeVm.pendingReturn.collectAsState()

                if (showExport) {
                    ExportScreen(viewModel = exportVm, onBack = { showExport = false })
                } else {
                    HomeScreen(viewModel = homeVm, onExportClick = { showExport = true })
                }

                pendingReturn?.let { summary ->
                    TripSummaryOverlay(
                        summary = summary,
                        onSave = { destination, purpose -> homeVm.completeReturn(summary, destination, purpose) },
                        onDismiss = homeVm::dismissReturn
                    )
                }
            }
        }
    }
}
