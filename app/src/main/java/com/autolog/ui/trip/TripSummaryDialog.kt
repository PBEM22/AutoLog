package com.autolog.ui.trip

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.autolog.ui.home.RentalSummary
import com.autolog.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val PURPOSES = listOf("거래처 미팅", "현장 방문", "자재 수령", "개인 용무")
private val timeFmt = SimpleDateFormat("HH:mm", Locale.KOREA)

@Composable
fun TripSummaryOverlay(
    summary: RentalSummary,
    onSave: (destination: String, purpose: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var countdown by remember { mutableIntStateOf(10) }
    var selectedPurpose by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }

    LaunchedEffect(summary) {
        speakTts(context, "반납 처리할까요? 방문 목적을 말씀하시거나 선택해 주세요.")
        while (countdown > 0) { delay(1000); countdown-- }
        onSave(destination.ifEmpty { "목적지 미입력" }, selectedPurpose.ifEmpty { "미분류" })
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background.copy(alpha = 0.96f)).safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.78f).wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp)
            ) {
                // ── Header ──────────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalParking, contentDescription = null, tint = Success, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("반납 완료 확인", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.weight(1f))
                    CountdownRing(countdown)
                }

                Spacer(Modifier.height(16.dp))

                // ── Rental receipt strip ─────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariant).padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ReceiptItem(
                        label = "운전자",
                        value = summary.trip.driverName.ifEmpty { "미입력" }
                    )
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Surface)
                    ReceiptItem(
                        label = "대여 → 반납",
                        value = "${timeFmt.format(Date(summary.trip.startTime))} → ${timeFmt.format(Date())}"
                    )
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Surface)
                    ReceiptItem(label = "대여 시간", value = "${summary.durationMinutes}분")
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Surface)
                    ReceiptItem(label = "주행 거리", value = "%.1f km".format(summary.distanceKm), highlight = true)
                }

                Spacer(Modifier.height(10.dp))

                // ── Odometer + Fuel row ──────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariant).padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ReceiptItem(
                        label = "대여 전 계기판",
                        value = "%,.1f km".format(summary.trip.startOdometer)
                    )
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Surface)
                    ReceiptItem(
                        label = "반납 후 계기판",
                        value = "%,.1f km".format(summary.endOdometer)
                    )
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Surface)

                    val fuelDiff = summary.trip.startFuelPct - summary.endFuelPct
                    val fuelText = if (fuelDiff > 0) {
                        "${summary.trip.startFuelPct.toInt()}%→${summary.endFuelPct.toInt()}% (-${fuelDiff.toInt()}%)"
                    } else {
                        "%.1fL (추정)".format(summary.distanceKm / 12f)
                    }
                    ReceiptItem(
                        label = if (fuelDiff > 0) "기름 소모" else "기름 소모 (추정)",
                        value = fuelText
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Destination input ────────────────────────────────────────
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    placeholder = { Text("목적지 입력 (예: 판교 OO테크)", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(Modifier.height(12.dp))
                Text("방문 목적 선택", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))

                // ── Purpose buttons ──────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PURPOSES.chunked(2).forEachIndexed { rowIdx, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEachIndexed { colIdx, purpose ->
                                val idx = rowIdx * 2 + colIdx + 1
                                val selected = selectedPurpose == purpose
                                Button(
                                    onClick = { selectedPurpose = purpose },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) Primary else SurfaceVariant,
                                        contentColor = if (selected) Color(0xFF0D1117) else TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("$idx. $purpose", fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Action row ───────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onSave(destination.ifEmpty { "미입력" }, "미분류") },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("나중에 (임시저장)", fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            onSave(
                                destination.ifEmpty { "목적지 미입력" },
                                selectedPurpose.ifEmpty { "미분류" }
                            )
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color(0xFF0D1117)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("반납 완료", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptItem(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (highlight) Primary else TextPrimary)
    }
}

@Composable
private fun CountdownRing(countdown: Int) {
    Box(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(19.dp)).background(Warning.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(countdown.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Warning)
    }
}

private fun speakTts(context: Context, text: String) {
    var tts: TextToSpeech? = null
    tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREA
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
}
