package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MedicationEntity
import com.example.data.model.MedicationWithDose
import com.example.ui.DozumViewModel
import com.example.ui.theme.DozumPrimary
import com.example.ui.theme.DozumPrimaryContainer
import com.example.ui.theme.DozumSecondary
import com.example.ui.theme.DozumSecondaryContainer
import com.example.ui.theme.DozumTertiary
import com.example.ui.util.ReportExportHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DayReportStatus {
    NONE,
    ALL_TAKEN,
    PARTIAL,
    MISSED,
    UPCOMING
}

data class WeekDayReportModel(
    val dayName: String,
    val dayNumber: Int,
    val dateKey: String,
    val isToday: Boolean,
    val status: DayReportStatus,
    val fraction: Float,
    val barLabel: String,
    val hasSnooze: Boolean,
    val totalCount: Int,
    val takenCount: Int
)

@Composable
fun ReportsScreen(
    viewModel: DozumViewModel,
    medications: List<MedicationEntity>,
    selectedPeriod: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allMedWithDoses by viewModel.allMedicationsWithDoses.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayKey = remember { dateFormat.format(Date()) }

    // Week offset from current week: 0 = Bu Hafta, -1 = Geçen Hafta, -2 = 2 Hafta Önce vb.
    var weekOffset by remember { mutableIntStateOf(0) }

    // Selected date in the strip for read-only detailed inspection
    var selectedReportDateKey by remember { mutableStateOf(todayKey) }

    // Calculate the 7 days of the currently viewed week offset
    val weekCalculation = remember(weekOffset, allMedWithDoses, todayKey) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("tr"))
        val monthTitle = monthFormat.format(cal.time).replaceFirstChar { it.uppercase() }
        val weekNumber = cal.get(Calendar.WEEK_OF_YEAR)
        val weekLabel = if (weekOffset == 0) "Bu Hafta (Hafta $weekNumber)" else if (weekOffset == -1) "Geçen Hafta (Hafta $weekNumber)" else "Hafta $weekNumber"

        val dayNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        val days = mutableListOf<WeekDayReportModel>()

        for (i in 0..6) {
            val dKey = dateFormat.format(cal.time)
            val dNum = cal.get(Calendar.DAY_OF_MONTH)
            val dayDoses = allMedWithDoses.filter { it.dose.dateKey == dKey }
            val countTotal = dayDoses.size
            val countTaken = dayDoses.count { it.dose.status == "TAKEN" }
            val hasSnooze = dayDoses.any { it.dose.status == "SNOOZED" }
            val isToday = dKey == todayKey

            val status = when {
                countTotal == 0 -> DayReportStatus.NONE
                countTaken == countTotal -> DayReportStatus.ALL_TAKEN
                countTaken > 0 -> DayReportStatus.PARTIAL
                dKey < todayKey -> DayReportStatus.MISSED
                else -> DayReportStatus.UPCOMING
            }

            val fraction = if (countTotal > 0) (countTaken.toFloat() / countTotal) else 0f
            val barLabel = when {
                countTotal == 0 -> "-"
                countTaken == countTotal -> "%100"
                countTaken > 0 -> "%${(fraction * 100).toInt()}"
                else -> "0"
            }

            days.add(
                WeekDayReportModel(
                    dayName = dayNames[i],
                    dayNumber = dNum,
                    dateKey = dKey,
                    isToday = isToday,
                    status = status,
                    fraction = fraction,
                    barLabel = barLabel,
                    hasSnooze = hasSnooze,
                    totalCount = countTotal,
                    takenCount = countTaken
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        Triple(days, monthTitle, weekLabel)
    }

    val (currentWeekDays, monthTitle, weekLabel) = weekCalculation

    // Filter doses strictly for the viewed week
    val weekDoses = remember(currentWeekDays, allMedWithDoses) {
        val weekKeys = currentWeekDays.map { it.dateKey }.toSet()
        allMedWithDoses.filter { it.dose.dateKey in weekKeys }
    }

    val weekTotalDoses = weekDoses.size
    val weekTakenDoses = weekDoses.count { it.dose.status == "TAKEN" }
    val weekCompliancePercent = if (weekTotalDoses > 0) ((weekTakenDoses.toFloat() / weekTotalDoses) * 100).toInt() else 100

    // Selected day doses (READ-ONLY)
    val selectedDayDoses = remember(selectedReportDateKey, allMedWithDoses) {
        allMedWithDoses.filter { it.dose.dateKey == selectedReportDateKey }
            .sortedBy { it.dose.scheduledTime }
    }

    val selectedDateFormatted = remember(selectedReportDateKey) {
        try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedReportDateKey)
            if (d != null) {
                SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr")).format(d)
            } else {
                selectedReportDateKey
            }
        } catch (e: Exception) {
            selectedReportDateKey
        }
    }

    val selectedDayTakenCount = selectedDayDoses.count { it.dose.status == "TAKEN" }
    val selectedDayTotalCount = selectedDayDoses.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Sleek Horizontal Week Calendar (Home-page inspired design with week navigation)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header with Month and Week Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = weekLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(34.dp)
                        ) {
                            IconButton(onClick = { weekOffset-- }) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Önceki Hafta",
                                    tint = DozumPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (weekOffset != 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DozumSecondaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        weekOffset = 0
                                        selectedReportDateKey = todayKey
                                    }
                            ) {
                                Text(
                                    text = "Bu Hafta",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF003732)
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(34.dp)
                        ) {
                            IconButton(onClick = { weekOffset++ }) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Sonraki Hafta",
                                    tint = DozumPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Horizontal 7-Day Pill Strip (Identical to Main Page DayStripItem aesthetic)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    currentWeekDays.forEach { dayModel ->
                        val isSelected = dayModel.dateKey == selectedReportDateKey
                        ReportDayStripItem(
                            model = dayModel,
                            isSelected = isSelected,
                            onClick = {
                                selectedReportDateKey = dayModel.dateKey
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. WORKING REAL DAILY COMPLIANCE BAR CHART (Gerçek Verilerle Çalışan Çubuk Grafik)
        item {
            DailyComplianceBarChart(
                days = currentWeekDays,
                selectedDateKey = selectedReportDateKey,
                onSelectDay = { dateKey ->
                    selectedReportDateKey = dateKey
                }
            )
        }

        // 3. SEÇİLEN GÜNÜN İLAÇ DETAYI (TAMAMEN SALT OKUNUR - BUTONSUZ RAPOR)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(DozumPrimary)
                            )
                            Column {
                                Text(
                                    text = if (selectedReportDateKey == todayKey) "$selectedDateFormatted (Bugün)" else selectedDateFormatted,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Günlük İlaç Raporu (Salt Okunur)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                selectedDayTotalCount == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                selectedDayTakenCount == selectedDayTotalCount -> DozumSecondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = if (selectedDayTotalCount > 0)
                                    "$selectedDayTakenCount / $selectedDayTotalCount Alındı"
                                else
                                    "Plan Yok",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedDayTakenCount == selectedDayTotalCount && selectedDayTotalCount > 0)
                                        Color(0xFF003732)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (selectedDayDoses.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Bu tarih için planlanmış ilaç dozu bulunmuyor.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // READ-ONLY DOSE LIST (NO ACTION BUTTONS)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedDayDoses.forEach { item ->
                                ReadOnlyDoseReportItem(
                                    medWithDose = item,
                                    isPast = selectedReportDateKey < todayKey
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Haftalık Genel Uyum Skoru Kartı
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "$weekLabel UYUM PERFORMANSI".uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                    letterSpacing = 1.sp
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "%$weekCompliancePercent",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (weekCompliancePercent >= 80) "Başarılı Takip" else if (weekCompliancePercent >= 50) "Düzenli Takip" else "Takip Başlangıcı",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DozumPrimary
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF6FFBBE)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF002113),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${medications.size} İlaç",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF002113)
                                    )
                                )
                            }
                        }
                    }

                    // Gauge and Breakdown
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 8.dp.toPx()
                                    drawCircle(
                                        color = Color(0xFF89F5E7).copy(alpha = 0.3f),
                                        radius = (size.minDimension - strokeWidth) / 2,
                                        style = Stroke(width = strokeWidth)
                                    )
                                    drawArc(
                                        color = DozumPrimary,
                                        startAngle = -90f,
                                        sweepAngle = (weekCompliancePercent / 100f) * 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$weekTakenDoses/$weekTotalDoses",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (weekTotalDoses > 0)
                                        "$weekLabel içinde planlanan $weekTotalDoses dozdan $weekTakenDoses tanesi alındı."
                                    else
                                        "Bu hafta için kayıtlı ilaç dozlarınız takip ediliyor.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. İlaç Bazında Uyum Listesi
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "İlaç Bazında Haftalık Uyum",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                medications.forEach { med ->
                    val medDoses = weekDoses.filter { it.dose.medicationId == med.id }
                    val mTotal = medDoses.size
                    val mTaken = medDoses.count { it.dose.status == "TAKEN" }
                    val mPercent = if (mTotal > 0) ((mTaken.toFloat() / mTotal) * 100).toInt() else 100

                    MedicationAdherenceItem(
                        name = med.name,
                        subtitle = "${med.dosage} • ${med.mealTiming}",
                        percent = mPercent,
                        doseStats = if (mTotal > 0) "$mTaken/$mTotal Alındı • Kalan Stok: ${med.currentStock}" else "Kalan Stok: ${med.currentStock}",
                        color = DozumPrimary
                    )
                }
            }
        }

        // 6. Gerçek PDF Rapor Çıktısı & Paylaşım
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DozumSecondaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = DozumPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "DOKTOR VE KİŞİSEL TAKİP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DozumPrimary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "PDF Raporu Oluştur",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "$weekLabel kullanım verilerinizi resmi PDF formatında dışa aktarın.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }

                    // PDF Button
                    Button(
                        onClick = {
                            val pdfFile = ReportExportHelper.generatePdfReport(
                                context = context,
                                userName = userName,
                                period = "$monthTitle - $weekLabel",
                                medications = medications,
                                doses = weekDoses
                            )
                            if (pdfFile != null) {
                                viewModel.showToast(
                                    title = "PDF Raporu Hazırlandı 📄",
                                    message = "Kişisel ilaç takip raporunuz PDF olarak açılıyor."
                                )
                                ReportExportHelper.openOrSharePdf(context, pdfFile)
                            } else {
                                viewModel.showToast(
                                    title = "PDF Oluşturulamadı ⚠️",
                                    message = "Rapor hazırlanırken bir sorun oluştu."
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_export_pdf"),
                        colors = ButtonDefaults.buttonColors(containerColor = DozumPrimary),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PDF Raporunu İndir / Aç",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Share Button
                    Button(
                        onClick = {
                            ReportExportHelper.shareReportAsText(
                                context = context,
                                userName = userName,
                                period = "$monthTitle - $weekLabel",
                                medications = medications,
                                doses = weekDoses
                            )
                            viewModel.showToast(
                                title = "Rapor Paylaşılıyor 📤",
                                message = "Paylaşım seçenekleri açıldı."
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = DozumPrimary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Haftalık Kullanım Geçmişini Paylaş",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ReportDayStripItem(
    model: WeekDayReportModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clickable(onClick = onClick)
            .testTag("report_day_${model.dayNumber}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) DozumPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = model.dayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color(0xFF89F5E7) else MaterialTheme.colorScheme.outline
                )
            )
            Text(
                text = "${model.dayNumber}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )

            // Dynamic Status Dot
            val dotColor = when {
                isSelected -> Color(0xFF6FFBBE)
                model.status == DayReportStatus.ALL_TAKEN -> DozumTertiary
                model.status == DayReportStatus.PARTIAL -> Color(0xFFD97706)
                model.status == DayReportStatus.MISSED -> MaterialTheme.colorScheme.error
                model.status == DayReportStatus.UPCOMING -> MaterialTheme.colorScheme.outlineVariant
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun DailyComplianceBarChart(
    days: List<WeekDayReportModel>,
    selectedDateKey: String,
    onSelectDay: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Günlük Uyum Grafiği",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Gerçek verilerle hesaplanan alım oranları",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DozumPrimary)
                        )
                        Text(
                            text = "Alındı",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DozumSecondaryContainer)
                        )
                        Text(
                            text = "Erteleme",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // 7 Days Chart Container
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    days.forEach { dayModel ->
                        BarColumn(
                            day = dayModel.dayName,
                            fraction = dayModel.fraction,
                            label = dayModel.barLabel,
                            hasSnooze = dayModel.hasSnooze,
                            isCurrent = dayModel.isToday,
                            isSelected = dayModel.dateKey == selectedDateKey,
                            onClick = { onSelectDay(dayModel.dateKey) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarColumn(
    day: String,
    fraction: Float,
    label: String,
    hasSnooze: Boolean,
    isCurrent: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (hasSnooze) DozumSecondary else DozumPrimary
            )
        )

        // The bar pillar
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(if (isSelected) DozumPrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (hasSnooze) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(DozumSecondaryContainer)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((fraction * 78).dp.coerceAtLeast(if (fraction > 0f) 8.dp else 0.dp))
                        .clip(
                            if (hasSnooze) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                            else RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                        .background(if (isSelected) DozumTertiary else DozumPrimary)
                )
            }
        }

        Text(
            text = day,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isCurrent || isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isSelected) DozumPrimary else if (isCurrent) DozumPrimary else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun ReadOnlyDoseReportItem(
    medWithDose: MedicationWithDose,
    isPast: Boolean
) {
    val dose = medWithDose.dose
    val med = medWithDose.medication
    val isTaken = dose.status == "TAKEN"
    val isSnoozed = dose.status == "SNOOZED"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTaken)
                                Color(0xFF6FFBBE).copy(alpha = 0.35f)
                            else if (isPast)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.Medication,
                        contentDescription = null,
                        tint = if (isTaken) DozumTertiary else if (isPast) MaterialTheme.colorScheme.error else DozumPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column {
                    Text(
                        text = med.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Plan: ${dose.scheduledTime} • ${med.dosage} (${med.mealTiming})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // PURELY READ-ONLY BADGES - NO INTERACTIVE ACTION BUTTONS
            if (isTaken) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF6FFBBE).copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DozumTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Alındı (${dose.takenTime ?: "Kayıtlı"})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DozumTertiary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else if (isPast) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "✗ Alınmadı / Atlandı",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isSnoozed) "Ertelendi" else "Bekliyor",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationAdherenceItem(
    name: String,
    subtitle: String,
    percent: Int,
    doseStats: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DozumSecondaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Text(
                    text = "%$percent",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                )
            }

            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = doseStats,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}
