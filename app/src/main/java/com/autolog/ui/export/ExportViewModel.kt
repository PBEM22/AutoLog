package com.autolog.ui.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autolog.data.db.RentalTripDao
import com.autolog.data.db.RentalTripEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

enum class ExportPreset { RENTAL_LOG, NTS }   // NTS = 국세청

class ExportViewModel(private val rentalTripDao: RentalTripDao) : ViewModel() {

    private val _trips = MutableStateFlow<List<RentalTripEntity>>(emptyList())
    val trips: StateFlow<List<RentalTripEntity>> = _trips.asStateFlow()

    private val _preset = MutableStateFlow(ExportPreset.RENTAL_LOG)
    val preset: StateFlow<ExportPreset> = _preset.asStateFlow()

    private val _webhookUrl = MutableStateFlow("")
    val webhookUrl: StateFlow<String> = _webhookUrl.asStateFlow()

    private val _statusMsg = MutableStateFlow<String?>(null)
    val statusMsg: StateFlow<String?> = _statusMsg.asStateFlow()

    init {
        viewModelScope.launch {
            rentalTripDao.getAllTrips().collect { _trips.value = it.filter { r -> r.isCompleted } }
        }
    }

    fun setPreset(p: ExportPreset) = _preset.update { p }
    fun setWebhookUrl(url: String) = _webhookUrl.update { url }
    fun clearStatus() = _statusMsg.update { null }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val records = rentalTripDao.getRecentTrips()
            val (csv, fileName) = buildCsv(records, _preset.value)
            runCatching { saveCsv(context, fileName, csv); "저장 완료: 다운로드/$fileName" }
                .onSuccess { _statusMsg.value = it }
                .onFailure { _statusMsg.value = "저장 실패: ${it.message}" }
        }
    }

    fun shareViaSend(context: Context) {
        viewModelScope.launch {
            val records = rentalTripDao.getRecentTrips()
            val (csv, fileName) = buildCsv(records, _preset.value)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, csv)
            }
            context.startActivity(Intent.createChooser(intent, "공유"))
        }
    }

    fun sendWebhook() {
        val url = _webhookUrl.value.trim()
        if (url.isEmpty()) { _statusMsg.value = "웹훅 URL을 입력하세요"; return }
        viewModelScope.launch {
            val records = rentalTripDao.getRecentTrips(10)   // last 10 entries
            val json = buildJson(records)
            runCatching { postWebhook(url, json) }
                .onSuccess { _statusMsg.value = it }
                .onFailure { _statusMsg.value = "웹훅 실패: ${it.message}" }
        }
    }

    // ── CSV builders ────────────────────────────────────────────────────────

    private fun buildCsv(records: List<RentalTripEntity>, preset: ExportPreset): Pair<String, String> {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.KOREA).format(Date())
        return when (preset) {
            ExportPreset.RENTAL_LOG -> {
                val header = "운전자,대여일자,빌린시각,반납시각,출발지,목적지,주행 전 계기판(km),주행 후 계기판(km),주행거리(km),대여 전 기름양(%),반납 후 기름양(%),목적/비고"
                val rows = records.filter { it.isCompleted }.joinToString("\n") { r ->
                    val df = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                    val tf = SimpleDateFormat("HH:mm", Locale.KOREA)
                    listOf(
                        r.driverName, df.format(Date(r.startTime)),
                        tf.format(Date(r.startTime)), r.endTime?.let { tf.format(Date(it)) } ?: "",
                        r.origin, r.destination,
                        "%.1f".format(r.startOdometer), "%.1f".format(r.endOdometer ?: r.startOdometer),
                        "%.1f".format(r.distanceKm),
                        "%.0f".format(r.startFuelPct), "%.0f".format(r.endFuelPct ?: r.startFuelPct),
                        r.purpose
                    ).joinToString(",") { "\"$it\"" }
                }
                "$header\n$rows" to "공용차대장_$stamp.csv"
            }
            ExportPreset.NTS -> {
                val header = "사용일자,부서/성명,주행 전 계기판(km),주행 후 계기판(km),주행거리(km),업무용 사용거리(km),연료 소모(L),비고"
                val rows = records.filter { it.isCompleted }.joinToString("\n") { r ->
                    val df = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                    val businessKm = if (r.purpose != "개인 용무") "%.1f".format(r.distanceKm) else "0"
                    listOf(
                        df.format(Date(r.startTime)), r.driverName,
                        "%.1f".format(r.startOdometer), "%.1f".format(r.endOdometer ?: r.startOdometer),
                        "%.1f".format(r.distanceKm), businessKm,
                        "%.2f".format(r.fuelConsumedLiters),
                        "${r.purpose}/${r.destination}"
                    ).joinToString(",") { "\"$it\"" }
                }
                "$header\n$rows" to "국세청운행기록부_$stamp.csv"
            }
        }
    }

    private fun buildJson(records: List<RentalTripEntity>): String {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        val entries = records.joinToString(",\n") { r ->
            """{"driver":"${r.driverName}","start":"${df.format(Date(r.startTime))}","end":"${r.endTime?.let { df.format(Date(it)) } ?: "운행중"}","km":${r.distanceKm},"purpose":"${r.purpose}","destination":"${r.destination}"}"""
        }
        return """{"source":"AutoLog","generated":"${df.format(Date())}","records":[$entries]}"""
    }

    private suspend fun postWebhook(url: String, json: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true
        conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        if (code in 200..299) "웹훅 전송 성공 (HTTP $code)" else "전송 실패 (HTTP $code)"
    }

    private fun saveCsv(context: Context, fileName: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("MediaStore insert failed")
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(dir, fileName).writeText(content, Charsets.UTF_8)
        }
    }

    class Factory(private val rentalTripDao: RentalTripDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ExportViewModel(rentalTripDao) as T
    }
}
