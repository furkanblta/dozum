package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MedicationWithDose
import com.example.ui.DayItem
import com.example.ui.DozumViewModel
import com.example.ui.NavTab
import com.example.ui.theme.DozumPrimary
import com.example.ui.theme.DozumSecondary
import com.example.ui.theme.DozumSecondaryContainer
import com.example.ui.theme.DozumTertiary

@Composable
fun TodayScreen(
    viewModel: DozumViewModel,
    doses: List<MedicationWithDose>,
    selectedDay: Int,
    weekDays: List<DayItem>,
    userName: String = "",
    modifier: Modifier = Modifier
) {
    val totalCount = doses.size
    val takenCount = doses.count { it.dose.status == "TAKEN" }
    val percentCompleted = if (totalCount > 0) ((takenCount.toFloat() / totalCount) * 100).toInt() else 0
    val nextPendingDose = doses.firstOrNull { it.dose.status != "TAKEN" }
    val nextDoseInfo = when {
        nextPendingDose != null -> "Sıradaki: ${nextPendingDose.dose.scheduledTime} (${nextPendingDose.medication.name})"
        totalCount > 0 -> "Tüm dozlar tamamlandı! 🎉"
        else -> "Henüz planlı doz yok"
    }

    val (monthName, weekInfo) = viewModel.getCurrentMonthAndWeekInfo()
    val areNotificationsEnabled by viewModel.areNotificationsEnabled.collectAsStateWithLifecycle()
    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()

    val greetingText = if (userName.isNotBlank()) "Merhaba, $userName! 👋" else "Merhaba! 👋"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Greeting & Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = greetingText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DozumSecondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFF003732),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (takenCount > 0) "Aktif Takip" else "Bugün",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF003732)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (totalCount == 0)
                    "Henüz eklenmiş bir ilaç bulunmuyor. Yeni bir ilaç ekleyerek takibe başlayın."
                else if (takenCount == totalCount)
                    "Bugünkü tüm ilaçlarını aldın, harika gidiyorsun! 🎉"
                else
                    "Bugünkü ilaçlarının %$percentCompleted'sini aldın, harika gidiyorsun!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Daily Progress Gauge Card
        item {
            DailyProgressCard(
                takenCount = takenCount,
                totalCount = totalCount,
                percent = percentCompleted,
                nextDoseInfo = nextDoseInfo
            )
        }

        // Notification Muted Banner (shown when user chooses to turn off notifications)
        if (!areNotificationsEnabled) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "İlaç Bildirimleri Kapalı",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Doz alarmları sessize alınmış durumda.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        TextButton(
                            onClick = { viewModel.setNotificationsEnabled(true) }
                        ) {
                            Text(
                                text = "Aç",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DozumPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Horizontal Week Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = weekInfo,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEach { day ->
                        val isSelected = if (day.dateKey.isNotBlank()) {
                            day.dateKey == selectedDateKey
                        } else {
                            day.dayNumber == selectedDay
                        }
                        DayStripItem(
                            day = day,
                            isSelected = isSelected,
                            onClick = { viewModel.setSelectedDay(day.dayNumber, day.dateKey) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Real Doses or Empty State
        if (doses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(DozumSecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = DozumPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Bugün İçin Planlı Doz Yok",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Düzenli ilaç veya takviye saatlerinizi takip etmek için ilk ilacınızı ekleyin.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        )
                        Button(
                            onClick = { viewModel.setNavTab(NavTab.EKLE) },
                            colors = ButtonDefaults.buttonColors(containerColor = DozumPrimary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "İlaç Ekle",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        } else {
            // Group real doses by period
            val periods = listOf("Sabah", "Öğle", "Akşam", "Gece")
            val groupedDoses = doses.groupBy { it.dose.periodName }

            periods.forEach { period ->
                val periodDoses = groupedDoses[period]
                if (!periodDoses.isNullOrEmpty()) {
                    item {
                        val isPeriodCompleted = periodDoses.all { it.dose.status == "TAKEN" }
                        val firstDose = periodDoses.first()
                        val headerIcon = when (period) {
                            "Sabah" -> Icons.Default.WbSunny
                            "Öğle" -> Icons.Outlined.LightMode
                            "Akşam" -> Icons.Default.Schedule
                            else -> Icons.Default.Nightlight
                        }

                        TimeGroupHeader(
                            icon = headerIcon,
                            title = period,
                            time = firstDose.dose.scheduledTime,
                            badgeText = if (isPeriodCompleted) "Tamamlandı" else "Bekliyor",
                            isCompleted = isPeriodCompleted,
                            isNext = !isPeriodCompleted
                        )
                        val todayKey = viewModel.getTodayDateKey()
                        val isPastOrToday = if (selectedDateKey.isNotBlank()) selectedDateKey <= todayKey else selectedDay <= viewModel.realCurrentDay

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            periodDoses.forEach { medWithDose ->
                                DynamicMedicationDoseCard(
                                    medWithDose = medWithDose,
                                    canTakeDose = isPastOrToday,
                                    onTakeDose = {
                                        viewModel.markDoseTaken(
                                            medWithDose.dose.id,
                                            medWithDose.medication.id,
                                            medWithDose.medication.name
                                        )
                                    },
                                    onSnoozeDose = {
                                        viewModel.snoozeDose(
                                            medWithDose.dose.id,
                                            medWithDose.medication.name
                                        )
                                    },
                                    onUndoDose = {
                                        viewModel.undoDoseTaken(
                                            medWithDose.dose.id,
                                            medWithDose.medication.id,
                                            medWithDose.medication.name
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Link to History & Reports
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { viewModel.setNavTab(NavTab.RAPORLAR) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = DozumPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Geçmiş Günlerin Doz Takip Raporu",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    Text(
                        text = "Görüntüle →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DozumPrimary
                        )
                    )
                }
            }
        }

        // Daily Tip Card
        item {
            DailyTipCard()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DailyProgressCard(
    takenCount: Int,
    totalCount: Int,
    percent: Int,
    nextDoseInfo: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular Progress Gauge
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    drawCircle(
                        color = trackColor,
                        radius = (size.minDimension - strokeWidth) / 2,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = DozumPrimary,
                        startAngle = -90f,
                        sweepAngle = (percent / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$takenCount/$totalCount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DozumPrimary,
                            lineHeight = 16.sp
                        )
                    )
                    Text(
                        text = "Doz",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            // Info Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%$percent Tamamlandı",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DozumPrimary
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Bugün",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = if (totalCount - takenCount > 0) "${totalCount - takenCount} Doz Kaldı" else "Tüm Dozlar Alındı",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = DozumSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = nextDoseInfo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DayStripItem(
    day: DayItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clickable(onClick = onClick)
            .testTag("day_item_${day.dayNumber}"),
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
                text = day.dayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color(0xFF89F5E7) else MaterialTheme.colorScheme.outline
                )
            )
            Text(
                text = "${day.dayNumber}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> Color(0xFF6FFBBE)
                            day.isCompleted -> DozumTertiary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
        }
    }
}

@Composable
fun TimeGroupHeader(
    icon: ImageVector,
    title: String,
    time: String,
    badgeText: String,
    isCompleted: Boolean,
    isNext: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isNext) DozumPrimary else DozumSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isNext) DozumPrimary else MaterialTheme.colorScheme.outline
                )
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when {
                isNext -> DozumSecondaryContainer
                isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = DozumTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                } else if (isNext) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(DozumPrimary)
                    )
                }
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isNext -> Color(0xFF003732)
                            isCompleted -> DozumTertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun DynamicMedicationDoseCard(
    medWithDose: MedicationWithDose,
    canTakeDose: Boolean,
    onTakeDose: () -> Unit,
    onSnoozeDose: () -> Unit,
    onUndoDose: () -> Unit
) {
    val dose = medWithDose.dose
    val med = medWithDose.medication
    val isTaken = dose.status == "TAKEN"
    val isSnoozed = dose.status == "SNOOZED"

    val accentColor by animateColorAsState(
        targetValue = if (isTaken) DozumTertiary else DozumPrimary,
        label = "accent"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dose_card_${dose.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (isTaken) 78.dp else 160.dp)
                    .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = DozumPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = med.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = med.mealTiming,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = med.dosage + " • " + (med.note ?: med.frequency),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    if (isTaken) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = DozumTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = dose.takenTime ?: "Alındı",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = DozumTertiary
                                    )
                                )
                                IconButton(
                                    onClick = onUndoDose,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Undo,
                                        contentDescription = "Geri Al",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isTaken) {
                    if (med.currentStock <= med.lowStockThreshold) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Kutuda son ${med.currentStock} kaldı. Yenileme zamanı yaklaşıyor.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )
                            }
                        }
                    }

                    // Action Buttons
                    if (canTakeDose) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onTakeDose,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_take_dose_${dose.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = DozumPrimary),
                                shape = RoundedCornerShape(23.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Aldım Olarak İşaretle",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Button(
                                onClick = onSnoozeDose,
                                modifier = Modifier
                                    .height(46.dp)
                                    .testTag("btn_snooze_dose_${dose.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DozumSecondaryContainer,
                                    contentColor = Color(0xFF003732)
                                ),
                                shape = RoundedCornerShape(23.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Snooze,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSnoozed) "Ertelendi" else "15 dk Ertele",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Gelecek tarihteki dozlar henüz alınamaz.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyTipCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DozumSecondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF003732),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Günün Sağlık İpucu",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DozumPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "İlaçlarınızı her gün aynı saatte ve bol su ile almanız tedavinizin etkinliğini en üst düzeye çıkarır.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}
