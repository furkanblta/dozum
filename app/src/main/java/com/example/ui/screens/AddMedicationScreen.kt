package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DozumViewModel
import com.example.ui.NavTab
import com.example.ui.theme.DozumBackground
import com.example.ui.theme.DozumPrimary
import com.example.ui.theme.DozumPrimaryContainer
import com.example.ui.theme.DozumSecondaryContainer
import com.example.ui.theme.DozumTertiary

@Composable
fun AddMedicationScreen(
    viewModel: DozumViewModel,
    modifier: Modifier = Modifier
) {
    val editMedication by viewModel.medicationToEdit.collectAsStateWithLifecycle()
    val isEditMode = editMedication != null

    var drugName by remember(editMedication) { mutableStateOf(editMedication?.name ?: "") }
    var drugDosage by remember(editMedication) { mutableStateOf(editMedication?.dosage ?: "") }
    var selectedForm by remember(editMedication) { mutableStateOf(editMedication?.form ?: "Tablet") }
    var selectedFrequency by remember(editMedication) { mutableStateOf(editMedication?.frequency ?: "Günde 1 kez") }
    var selectedMealTiming by remember(editMedication) { mutableStateOf(editMedication?.mealTiming ?: "Tok Karnına") }
    var stockCount by remember(editMedication) { mutableStateOf(editMedication?.totalStock?.toString() ?: "") }
    var isReminderEnabled by remember(editMedication) { mutableStateOf(editMedication?.reminderTimes?.isNotBlank() ?: true) }
    var isCriticalAlertEnabled by remember(editMedication) { mutableStateOf(editMedication?.lowStockThreshold?.let { it > 0 } ?: true) }
    var instructions by remember(editMedication) { mutableStateOf(editMedication?.note ?: "") }

    val doseTimes = remember(editMedication) { 
        val list = mutableStateListOf<String>()
        val reminderStr = editMedication?.reminderTimes
        if (reminderStr != null && reminderStr.isNotBlank() && !reminderStr.startsWith("OFF:")) {
            list.addAll(reminderStr.split(","))
        } else if (reminderStr != null && reminderStr.startsWith("OFF:")) {
            list.addAll(reminderStr.removePrefix("OFF:").split(","))
        } else {
            list.add("09:00")
        }
        list
    }
    val context = LocalContext.current

    val scannedMedForAdd by viewModel.scannedMedicationForAdd.collectAsStateWithLifecycle()
    LaunchedEffect(scannedMedForAdd) {
        if (scannedMedForAdd.isNotBlank()) {
            drugName = scannedMedForAdd
            viewModel.setScannedMedicationForAdd("")
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Header
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
                Text(
                    text = if (isEditMode) "İlacı Düzenle" else "Yeni İlaç veya Takviye Ekle",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dozaj ve hatırlatma detaylarını girerek düzenli kullanımınızı güvenceye alın.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Quick Scan Hero Button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.showBarcodeDialog.value = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DozumPrimaryContainer, DozumPrimary)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Kutu Barkodunu Tara",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Otomatik isim ve prospektüs bilgisi aktarımı",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF89F5E7)
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. İlaç Adı ve Dozaj Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // İlaç Adı
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "İlaç Adı",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Zorunlu",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DozumPrimary
                                )
                            )
                        }
                        OutlinedTextField(
                            value = drugName,
                            onValueChange = { drugName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drug_name_input"),
                            placeholder = { Text("İlaç veya Takviye Adı (örn. Parol, Nexium, B12)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = DozumPrimary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }

                    // Doz / Birim
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Doz / Birim Değeri",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Tek kullanım miktarı",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                        OutlinedTextField(
                            value = drugDosage,
                            onValueChange = { drugDosage = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drug_dosage_input"),
                            placeholder = { Text("Dozaj (örn. 500 mg, 1 Tablet, 10 ml)") },
                            trailingIcon = {
                                Text(
                                    text = "Birim",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = DozumPrimary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // 2. İlaç Formu Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "İlaç Formu",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Lütfen formu seçin",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    val forms = listOf(
                        Pair("Tablet", Icons.Default.Medication),
                        Pair("Kapsül", Icons.Default.Vaccines),
                        Pair("Şurup", Icons.Default.Opacity),
                        Pair("Damla", Icons.Default.WaterDrop),
                        Pair("İğne", Icons.Default.Vaccines),
                        Pair("Diğer", Icons.Default.Category)
                    )

                    // 2 rows of 3 columns
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            forms.take(3).forEach { (formName, icon) ->
                                FormButton(
                                    name = formName,
                                    icon = icon,
                                    isSelected = selectedForm == formName,
                                    onClick = { selectedForm = formName },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            forms.drop(3).forEach { (formName, icon) ->
                                FormButton(
                                    name = formName,
                                    icon = icon,
                                    isSelected = selectedForm == formName,
                                    onClick = { selectedForm = formName },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Kullanım Sıklığı
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kullanım Sıklığı",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Her Gün",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DozumPrimary
                            )
                        )
                    }

                    val freqs = listOf(
                        "Günde 1 kez",
                        "Günde 2 kez",
                        "Günde 3 kez",
                        "Özel Aralık / Gerektiğinde"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FrequencyButton(
                                text = freqs[0],
                                isSelected = selectedFrequency == freqs[0],
                                onClick = {
                                    selectedFrequency = freqs[0]
                                    doseTimes.clear()
                                    doseTimes.add("08:30")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FrequencyButton(
                                text = freqs[1],
                                isSelected = selectedFrequency == freqs[1],
                                onClick = {
                                    selectedFrequency = freqs[1]
                                    doseTimes.clear()
                                    doseTimes.addAll(listOf("08:30", "20:30"))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FrequencyButton(
                                text = freqs[2],
                                isSelected = selectedFrequency == freqs[2],
                                onClick = {
                                    selectedFrequency = freqs[2]
                                    doseTimes.clear()
                                    doseTimes.addAll(listOf("08:30", "14:00", "20:30"))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FrequencyButton(
                                text = freqs[3],
                                isSelected = selectedFrequency == freqs[3],
                                onClick = {
                                    selectedFrequency = freqs[3]
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 4. Hatırlatma Saatleri
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isReminderEnabled) Icons.Default.Schedule else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (isReminderEnabled) DozumPrimary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "İlaç Bildirimleri & Doz Saatleri",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (isReminderEnabled) "${doseTimes.size} Doz Hatırlatılacak (Açık)" else "Bildirim kapalı (İsteğe bağlı)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { isReminderEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DozumPrimary
                            )
                        )
                    }

                    // List of dose times (visible when reminders are enabled)
                    if (isReminderEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            doseTimes.forEachIndexed { idx, time ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val timeParts = time.split(":")
                                        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
                                        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 30
                                        TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                val formattedTime = selectedHour.toString().padStart(2, '0') + ":" + selectedMinute.toString().padStart(2, '0')
                                                doseTimes[idx] = formattedTime
                                            },
                                            hour,
                                            minute,
                                            true
                                        ).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(DozumSecondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF003732)
                                                    )
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = time,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                val periodLabel = when {
                                                    time < "12:00" -> "Sabah Dozu"
                                                    time < "17:00" -> "Öğle Dozu"
                                                    time < "22:00" -> "Akşam Dozu"
                                                    else -> "Gece Dozu"
                                                }
                                                Text(
                                                    text = periodLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Düzenle",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Add another time button
                            TextButton(
                                onClick = {
                                    if (doseTimes.size < 5) {
                                        TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                val formattedTime = selectedHour.toString().padStart(2, '0') + ":" + selectedMinute.toString().padStart(2, '0')
                                                doseTimes.add(formattedTime)
                                            },
                                            13,
                                            0,
                                            true
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = DozumPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+ Başka Saat Ekle",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DozumPrimary
                                    )
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Bildirim Kurulmayacak",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "Bu ilaç için alarm ve bildirimler kapalı kalacaktır. Yalnızca dolabınızda listelenir ve stok takibi yapılır.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Yemek Durumu
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Yemek Durumu",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MealButton(
                            text = "Aç Karnına",
                            isSelected = selectedMealTiming == "Aç Karnına",
                            onClick = { selectedMealTiming = "Aç Karnına" },
                            modifier = Modifier.weight(1f)
                        )
                        MealButton(
                            text = "Tok Karnına",
                            isSelected = selectedMealTiming == "Tok Karnına",
                            onClick = { selectedMealTiming = "Tok Karnına" },
                            modifier = Modifier.weight(1f)
                        )
                        MealButton(
                            text = "Fark Etmez",
                            isSelected = selectedMealTiming == "Fark Etmez",
                            onClick = { selectedMealTiming = "Fark Etmez" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 6. Stok & Kutu Takibi
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = DozumTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Stok & Kutu Takibi",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFA7F3D0).copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Aktif",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DozumTertiary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Başlangıç Stok Adedi",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Kutudaki toplam miktar",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.width(120.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = stockCount,
                                    onValueChange = { stockCount = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "adet",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }
                    }

                    // Critical stock switch row
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Kritik Stok Uyarısı",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Son 5 tablet kaldığında uyar",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Switch(
                                checked = isCriticalAlertEnabled,
                                onCheckedChange = { isCriticalAlertEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DozumPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // 7. Not ve Talimat
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Özel Talimat veya Not",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Örn: Bol ılık su ile alınacak, sütle içilmez.") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = DozumPrimary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        minLines = 2
                    )
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val parsedStock = stockCount.toIntOrNull() ?: 30
                        if (isEditMode && editMedication != null) {
                            viewModel.updateFullMedication(
                                medication = editMedication!!,
                                name = drugName,
                                dosage = drugDosage,
                                form = selectedForm,
                                frequency = selectedFrequency,
                                mealTiming = selectedMealTiming,
                                stock = parsedStock,
                                isCriticalAlert = isCriticalAlertEnabled,
                                isReminderEnabled = isReminderEnabled,
                                times = if (doseTimes.isNotEmpty()) doseTimes else listOf("08:30"),
                                note = instructions
                            )
                        } else {
                            viewModel.saveNewMedication(
                                name = drugName,
                                dosage = drugDosage,
                                form = selectedForm,
                                frequency = selectedFrequency,
                                mealTiming = selectedMealTiming,
                                stock = parsedStock,
                                isCriticalAlert = isCriticalAlertEnabled,
                                isReminderEnabled = isReminderEnabled,
                                times = if (doseTimes.isNotEmpty()) doseTimes else listOf("08:30"),
                                note = instructions
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_medication_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = DozumPrimary),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(
                        imageVector = if (isReminderEnabled) Icons.Default.CheckCircle else Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditMode) "İlacı Güncelle" else if (isReminderEnabled) "İlacı Kaydet ve Hatırlatıcıları Başlat" else "İlacı Kaydet (Bildirimsiz Takip)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                TextButton(
                    onClick = { 
                        viewModel.medicationToEdit.value = null
                        viewModel.setNavTab(NavTab.ILACLARIM) 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Vazgeç",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormButton(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) DozumSecondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (isSelected) DozumPrimary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF003732) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun FrequencyButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) DozumPrimary else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun MealButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) DozumSecondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF003732) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
