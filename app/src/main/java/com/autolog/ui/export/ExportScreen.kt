package com.autolog.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autolog.data.db.RentalTripEntity
import com.autolog.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExportScreen(viewModel: ExportViewModel, onBack: () -> Unit) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val preset by viewModel.preset.collectAsStateWithLifecycle()
    val webhookUrl by viewModel.webhookUrl.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMsg.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showWebhookPanel by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().background(Surface).padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = TextPrimary)
                }
                Text("대장 내보내기", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                Text("${trips.size}건", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.width(12.dp))
                // Share
                OutlinedButton(
                    onClick = { viewModel.shareViaSend(context) },
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Secondary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("공유", fontSize = 14.sp)
                }
                Spacer(Modifier.width(6.dp))
                // Webhook toggle
                OutlinedButton(
                    onClick = { showWebhookPanel = !showWebhookPanel },
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Warning)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("웹훅/메일", fontSize = 14.sp)
                }
                Spacer(Modifier.width(6.dp))
                // Export CSV
                Button(
                    onClick = { viewModel.exportCsv(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color(0xFF0D1117)),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CSV 저장", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Preset selector ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceVariant).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("내보내기 양식:", fontSize = 13.sp, color = TextSecondary)
                PresetChip(
                    label = "공용차 대여·반납 대장",
                    selected = preset == ExportPreset.RENTAL_LOG,
                    onClick = { viewModel.setPreset(ExportPreset.RENTAL_LOG) }
                )
                PresetChip(
                    label = "국세청 운행기록부",
                    selected = preset == ExportPreset.NTS,
                    onClick = { viewModel.setPreset(ExportPreset.NTS) }
                )
            }

            // ── Webhook panel ─────────────────────────────────────────────────
            if (showWebhookPanel) {
                WebhookPanel(
                    url = webhookUrl,
                    onUrlChange = viewModel::setWebhookUrl,
                    onSend = viewModel::sendWebhook,
                    onShareEmail = { viewModel.shareViaSend(context) }
                )
            }

            // ── Table header ──────────────────────────────────────────────────
            if (preset == ExportPreset.RENTAL_LOG) RentalTableHeader() else NtsTableHeader()

            // ── Records ───────────────────────────────────────────────────────
            if (trips.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("완료된 대여 기록이 없습니다", color = TextSecondary, fontSize = 18.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(trips) { trip ->
                        if (preset == ExportPreset.RENTAL_LOG) RentalTableRow(trip) else NtsTableRow(trip)
                    }
                }
            }
        }

        // ── Status snackbar ───────────────────────────────────────────────────
        statusMsg?.let { msg ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = viewModel::clearStatus) { Text("닫기", color = Primary) } },
                containerColor = Surface
            ) { Text(msg, color = TextPrimary) }
        }
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary,
            selectedLabelColor = Color(0xFF0D1117)
        )
    )
}

@Composable
private fun WebhookPanel(
    url: String,
    onUrlChange: (String) -> Unit,
    onSend: () -> Unit,
    onShareEmail: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(SurfaceVariant).padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text("외부 업무 툴 연동", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Warning)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = { Text("Webhook URL (Slack/Notion/Discord 등)", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Warning,
                    unfocusedBorderColor = Surface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Button(
                onClick = onSend,
                colors = ButtonDefaults.buttonColors(containerColor = Warning, contentColor = Color(0xFF0D1117)),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("전송", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onShareEmail,
                modifier = Modifier.height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Secondary)
            ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("메일/슬랙 공유", fontSize = 14.sp)
            }
        }
    }
}

// ── Rental log table ─────────────────────────────────────────────────────────

@Composable
private fun RentalTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        H("운전자", Modifier.weight(0.9f))
        H("일자", Modifier.weight(0.8f))
        H("대여→반납", Modifier.weight(1.2f))
        H("출발지", Modifier.weight(1.0f))
        H("목적지", Modifier.weight(1.0f))
        H("전/후 계기판", Modifier.weight(1.4f))
        H("주행거리", Modifier.weight(0.8f))
        H("전/후 기름", Modifier.weight(0.8f))
        H("목적", Modifier.weight(1.0f))
    }
}

@Composable
private fun RentalTableRow(trip: RentalTripEntity) {
    val df = SimpleDateFormat("MM/dd", Locale.KOREA)
    val tf = SimpleDateFormat("HH:mm", Locale.KOREA)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        D(trip.driverName.ifEmpty { "-" }, Modifier.weight(0.9f), color = TextPrimary)
        D(df.format(Date(trip.startTime)), Modifier.weight(0.8f))
        D("${tf.format(Date(trip.startTime))}→${trip.endTime?.let { tf.format(Date(it)) } ?: "-"}", Modifier.weight(1.2f))
        D(trip.origin.ifEmpty { "-" }, Modifier.weight(1.0f))
        D(trip.destination.ifEmpty { "-" }, Modifier.weight(1.0f))
        D("%,.0f / %,.0f".format(trip.startOdometer, trip.endOdometer ?: trip.startOdometer), Modifier.weight(1.4f))
        D("%.1f km".format(trip.distanceKm), Modifier.weight(0.8f), color = Primary)
        D("${trip.startFuelPct.toInt()}%%/${(trip.endFuelPct ?: trip.startFuelPct).toInt()}%%", Modifier.weight(0.8f))
        D(trip.purpose, Modifier.weight(1.0f), color = if (trip.purpose != "개인 용무") Primary else TextSecondary)
    }
}

// ── NTS table ────────────────────────────────────────────────────────────────

@Composable
private fun NtsTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        H("사용일자", Modifier.weight(1.0f))
        H("부서/성명", Modifier.weight(1.0f))
        H("주행 전(km)", Modifier.weight(1.0f))
        H("주행 후(km)", Modifier.weight(1.0f))
        H("주행거리", Modifier.weight(0.9f))
        H("업무거리", Modifier.weight(0.9f))
        H("연료(L)", Modifier.weight(0.8f))
        H("비고", Modifier.weight(1.4f))
    }
}

@Composable
private fun NtsTableRow(trip: RentalTripEntity) {
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    val businessKm = if (trip.purpose != "개인 용무") trip.distanceKm else 0f
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        D(df.format(Date(trip.startTime)), Modifier.weight(1.0f))
        D(trip.driverName.ifEmpty { "-" }, Modifier.weight(1.0f), color = TextPrimary)
        D("%,.1f".format(trip.startOdometer), Modifier.weight(1.0f))
        D("%,.1f".format(trip.endOdometer ?: trip.startOdometer), Modifier.weight(1.0f))
        D("%.1f".format(trip.distanceKm), Modifier.weight(0.9f), color = Primary)
        D("%.1f".format(businessKm), Modifier.weight(0.9f))
        val fuelLabel = "%.2f%s".format(trip.fuelConsumedLiters, if (trip.isEstimatedFuel) "*" else "")
        D(fuelLabel, Modifier.weight(0.8f))
        D("${trip.purpose}/${trip.destination.ifEmpty { "-" }}", Modifier.weight(1.4f))
    }
}

@Composable private fun H(text: String, modifier: Modifier) =
    Text(text, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold, modifier = modifier)

@Composable private fun D(text: String, modifier: Modifier, color: Color = TextSecondary) =
    Text(text, fontSize = 12.sp, color = color, modifier = modifier)
