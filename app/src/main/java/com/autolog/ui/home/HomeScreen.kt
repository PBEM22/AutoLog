package com.autolog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autolog.data.GearPosition
import com.autolog.data.db.RentalTripEntity
import com.autolog.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onExportClick: () -> Unit
) {
    val vehicleState by viewModel.vehicleState.collectAsStateWithLifecycle()
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val activeTrip by viewModel.activeTrip.collectAsStateWithLifecycle()
    val recentDrivers by viewModel.recentDrivers.collectAsStateWithLifecycle()
    val selectedDriver by viewModel.selectedDriver.collectAsStateWithLifecycle()
    val showSimulator by viewModel.showSimulator.collectAsStateWithLifecycle()
    val isDriving = vehicleState.gear == GearPosition.D || vehicleState.gear == GearPosition.R

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                isDriving = isDriving,
                onExportClick = onExportClick,
                onSimulatorToggle = viewModel::toggleSimulator
            )
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                VehicleRentalCard(
                    activeTrip = activeTrip,
                    gear = vehicleState.gear,
                    odometer = vehicleState.odometer,
                    fuelLevel = vehicleState.fuelLevel,
                    isEngineOn = vehicleState.isEngineOn,
                    isDriving = isDriving,
                    recentDrivers = recentDrivers,
                    selectedDriver = selectedDriver,
                    onDriverSelect = viewModel::selectDriver,
                    onRentalStart = viewModel::startRental,
                    modifier = Modifier.weight(0.4f).fillMaxHeight()
                )
                Spacer(Modifier.width(16.dp))
                RentalListPanel(
                    trips = trips,
                    modifier = Modifier.weight(0.6f).fillMaxHeight()
                )
            }
        }

        if (isDriving && activeTrip != null) {
            DrivingBadge(
                startTime = activeTrip!!.startTime,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp)
            )
        }

        if (showSimulator) {
            SimulatorPanel(
                gear = vehicleState.gear,
                isEngineOn = vehicleState.isEngineOn,
                hasActiveTrip = activeTrip != null,
                onStartEngine = viewModel::simulatorStartEngine,
                onStopEngine = viewModel::simulatorStopEngine,
                onDrive = viewModel::simulatorDrive,
                onPark = viewModel::simulatorPark,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun TopBar(isDriving: Boolean, onExportClick: () -> Unit, onSimulatorToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Surface).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Text("AutoLog", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(" 공용차 대여·반납 대장", fontSize = 15.sp, color = TextSecondary)
        Spacer(Modifier.weight(1f))
        if (!isDriving) {
            OutlinedButton(
                onClick = onExportClick,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("대장 내보내기 / 공유", fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onSimulatorToggle, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.BugReport, contentDescription = "시뮬레이터", tint = TextSecondary)
        }
    }
}

@Composable
private fun VehicleRentalCard(
    activeTrip: RentalTripEntity?,
    gear: GearPosition,
    odometer: Float,
    fuelLevel: Float,
    isEngineOn: Boolean,
    isDriving: Boolean,
    recentDrivers: List<String>,
    selectedDriver: String,
    onDriverSelect: (String) -> Unit,
    onRentalStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newDriverInput by remember { mutableStateOf("") }
    var showInput by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Rental status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(active = activeTrip != null)
                if (activeTrip != null) {
                    val df = SimpleDateFormat("HH:mm", Locale.KOREA)
                    Text(
                        "대여: ${df.format(Date(activeTrip.startTime))}",
                        fontSize = 12.sp, color = TextSecondary
                    )
                }
            }

            // Driver selection (only when not actively driving)
            if (!isDriving) {
                Column {
                    Text("운전자", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(recentDrivers) { driver ->
                            FilterChip(
                                selected = selectedDriver == driver,
                                onClick = { onDriverSelect(driver) },
                                label = { Text(driver, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color(0xFF0D1117)
                                )
                            )
                        }
                        item {
                            if (!showInput) {
                                InputChip(
                                    selected = false,
                                    onClick = { showInput = true },
                                    label = { Text("+", fontSize = 14.sp) }
                                )
                            }
                        }
                    }
                    if (showInput) {
                        OutlinedTextField(
                            value = newDriverInput,
                            onValueChange = { newDriverInput = it },
                            placeholder = { Text("운전자 이름", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = SurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newDriverInput.isNotBlank()) {
                                    onDriverSelect(newDriverInput.trim())
                                    newDriverInput = ""
                                }
                                showInput = false
                            })
                        )
                    }
                }
            }

            // Gear row
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("기어", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GearPosition.entries.forEach { g -> GearBadge(g, gear == g) }
                }
            }

            // Odometer
            Column {
                Text("계기판 주행거리", fontSize = 12.sp, color = TextSecondary)
                Text("%,.1f km".format(odometer), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                activeTrip?.let {
                    Text(
                        "+%.1f km (대여 시작 이후)".format(odometer - it.startOdometer),
                        fontSize = 12.sp, color = Primary
                    )
                }
            }

            // Fuel bar
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("연료 잔량", fontSize = 12.sp, color = TextSecondary)
                    Text("%.0f%%".format(fuelLevel), fontSize = 12.sp, color = fuelColor(fuelLevel))
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fuelLevel / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = fuelColor(fuelLevel),
                    trackColor = SurfaceVariant
                )
                activeTrip?.let {
                    val diff = it.startFuelPct - fuelLevel
                    if (diff > 0.5f) Text(
                        "-%.1f%% (대여 이후)".format(diff),
                        fontSize = 11.sp, color = Warning
                    )
                }
            }

            // 대여 시작 button (when no active trip and not driving)
            if (activeTrip == null && !isDriving) {
                Button(
                    onClick = onRentalStart,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color(0xFF0D1117)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("대여 시작", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(active: Boolean) {
    val (label, color) = if (active) "대여 운행 중" to Warning else "대기 중" to TextSecondary
    Row(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 13.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GearBadge(gear: GearPosition, selected: Boolean) {
    val bg = when {
        !selected -> SurfaceVariant
        gear == GearPosition.D -> Primary
        gear == GearPosition.R -> Danger
        gear == GearPosition.P -> Success
        else -> Warning
    }
    Box(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(gear.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (selected) Color(0xFF0D1117) else TextSecondary)
    }
}

@Composable
private fun RentalListPanel(trips: List<RentalTripEntity>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("대여·반납 기록", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        if (trips.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Surface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("대여 기록이 없습니다", color = TextSecondary, fontSize = 16.sp)
                    Text("시뮬레이터로 테스트해보세요", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Surface),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(trips) { RentalRow(it) }
            }
        }
    }
}

@Composable
private fun RentalRow(trip: RentalTripEntity) {
    val timeFmt = SimpleDateFormat("MM/dd HH:mm", Locale.KOREA)
    val shortFmt = SimpleDateFormat("HH:mm", Locale.KOREA)
    val startStr = timeFmt.format(Date(trip.startTime))
    val endStr = trip.endTime?.let { shortFmt.format(Date(it)) } ?: "운행 중"

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(trip.driverName.ifEmpty { "미입력" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.width(8.dp))
                Text("$startStr → $endStr", fontSize = 12.sp, color = TextSecondary)
            }
            if (trip.destination.isNotEmpty() && trip.destination != "도착지") {
                Text(trip.destination, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("%.1f km".format(trip.distanceKm), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Primary)
            PurposeBadge(trip.purpose, trip.isCompleted)
        }
    }
}

@Composable
private fun PurposeBadge(purpose: String, completed: Boolean) {
    val color = if (purpose == "개인 용무") TextSecondary else Primary
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f)).padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(if (!completed) "운행 중" else purpose, fontSize = 11.sp, color = if (!completed) Warning else color)
    }
}

@Composable
private fun DrivingBadge(startTime: Long, modifier: Modifier = Modifier) {
    val elapsed by produceState(initialValue = "") {
        while (true) {
            val diff = (System.currentTimeMillis() - startTime) / 1000
            value = "%02d:%02d 경과".format(diff / 60, diff % 60)
            kotlinx.coroutines.delay(1000)
        }
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(Warning.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(6.dp))
            Text("대여 운행 기록 중 · $elapsed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun SimulatorPanel(
    gear: GearPosition,
    isEngineOn: Boolean,
    hasActiveTrip: Boolean,
    onStartEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onDrive: () -> Unit,
    onPark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = Warning, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("시뮬레이터 (디버그)", fontSize = 13.sp, color = Warning)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimBtn(
                    text = if (hasActiveTrip) "대여 중" else "대여 시작",
                    color = if (hasActiveTrip) TextSecondary else Success,
                    onClick = onStartEngine,
                    enabled = !hasActiveTrip
                )
                SimBtn(text = "주행 +18.4km", color = Primary, onClick = onDrive, enabled = hasActiveTrip)
                SimBtn(
                    text = "P단 반납",
                    color = Warning,
                    onClick = onPark,
                    enabled = hasActiveTrip && (gear == GearPosition.D || gear == GearPosition.R)
                )
            }
        }
    }
}

@Composable
private fun SimBtn(text: String, color: Color, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = if (enabled) 0.2f else 0.08f),
            contentColor = if (enabled) color else color.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(text, fontSize = 13.sp)
    }
}

private fun fuelColor(level: Float): Color = when {
    level < 20f -> Danger
    level < 40f -> Warning
    else -> Success
}
