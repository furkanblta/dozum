package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DozumViewModel
import com.example.ui.NavTab
import com.example.ui.ToastMessage
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.DozumBottomNav
import com.example.ui.components.DozumHeader
import com.example.ui.components.DozumToast
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.PrescriptionReminderDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.components.StockUpdateDialog
import com.example.ui.components.WelcomeNameDialog
import com.example.ui.screens.AddMedicationScreen
import com.example.ui.screens.MedicationsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.DozumBackground
import com.example.ui.theme.DozumTheme
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        enableEdgeToEdge()
        setContent {
            val viewModel: DozumViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            DozumTheme(darkTheme = isDarkMode) {
                DozumApp(viewModel = viewModel, isDarkMode = isDarkMode)
            }
        }
    }
}

@Composable
fun DozumApp(viewModel: DozumViewModel, isDarkMode: Boolean) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val medications by viewModel.allMedications.collectAsStateWithLifecycle()
    val doses by viewModel.currentDayDoses.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.selectedMedFilter.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedReportPeriod.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val showOnboarding by viewModel.showOnboardingDialog.collectAsStateWithLifecycle()

    val showPrescription by viewModel.showPrescriptionDialog.collectAsStateWithLifecycle()
    val showBarcode by viewModel.showBarcodeDialog.collectAsStateWithLifecycle()
    val showProfile by viewModel.showProfileDialog.collectAsStateWithLifecycle()
    val showNotifications by viewModel.showNotificationsDialog.collectAsStateWithLifecycle()
    val stockEditMed by viewModel.stockEditMedication.collectAsStateWithLifecycle()
    val areNotificationsEnabled by viewModel.areNotificationsEnabled.collectAsStateWithLifecycle()
    val medicationToEdit by viewModel.medicationToEdit.collectAsStateWithLifecycle()

    var activeToast by remember { mutableStateOf<ToastMessage?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { toast ->
            activeToast = toast
            delay(3200)
            if (activeToast == toast) {
                activeToast = null
            }
        }
    }

    // Back handler: pop to BUGUN if currently on another tab
    if (currentTab != NavTab.BUGUN) {
        BackHandler {
            viewModel.setNavTab(NavTab.BUGUN)
        }
    }

    // Check if there are active notifications (pending doses or critical stock)
    // If the user disabled notifications, no unread red dot is displayed
    val hasUnreadNotifications = remember(medications, doses, areNotificationsEnabled) {
        if (!areNotificationsEnabled) {
            false
        } else {
            val hasCriticalStock = medications.any { it.currentStock <= it.lowStockThreshold }
            val hasPendingDoses = doses.any { it.dose.status != "TAKEN" }
            hasCriticalStock || hasPendingDoses
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DozumHeader(
                currentTab = currentTab,
                userName = userName,
                hasUnreadNotifications = hasUnreadNotifications,
                onNotificationsClick = { viewModel.showNotificationsDialog.value = true },
                onProfileClick = { viewModel.showProfileDialog.value = true }
            )
        },
        bottomBar = {
            DozumBottomNav(
                currentTab = currentTab,
                onTabSelected = { viewModel.setNavTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    NavTab.BUGUN -> {
                        TodayScreen(
                            viewModel = viewModel,
                            doses = doses,
                            selectedDay = selectedDay,
                            weekDays = viewModel.getWeekDays(),
                            userName = userName
                        )
                    }
                    NavTab.ILACLARIM -> {
                        MedicationsScreen(
                            viewModel = viewModel,
                            medications = medications,
                            searchQuery = searchQuery,
                            activeFilter = activeFilter
                        )
                    }
                    NavTab.EKLE -> {
                        AddMedicationScreen(
                            viewModel = viewModel
                        )
                    }
                    NavTab.RAPORLAR -> {
                        ReportsScreen(
                            viewModel = viewModel,
                            medications = medications,
                            selectedPeriod = selectedPeriod
                        )
                    }
                }
            }

            // Floating Toast Alert
            DozumToast(
                toast = activeToast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }
    }

    // Dialogs
    if (showPrescription) {
        PrescriptionReminderDialog(
            medicationName = "Reçeteli İlaç",
            onDismiss = { viewModel.showPrescriptionDialog.value = false },
            onConfirm = {
                viewModel.showPrescriptionDialog.value = false
                viewModel.showToast("Reçete Hatırlatıcısı Eklendi 📋", "Reçete yenileme hatırlatması takviminize kaydedildi.")
            }
        )
    }

    if (showBarcode) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val scanner = GmsBarcodeScanning.getClient(context)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    var scannedDrug = barcode.rawValue ?: "Bilinmeyen İlaç"
                    
                    if (scannedDrug.startsWith("http://") || scannedDrug.startsWith("https://")) {
                        // Eğer karekod bir URL ise (örn: https://www.recordati.com.tr/kt/stilizan1mg/stilizan1mg.pdf)
                        // Son kısmı alıp ilaç ismine çevirelim.
                        try {
                            val uri = android.net.Uri.parse(scannedDrug)
                            val lastPathSegment = uri.lastPathSegment ?: ""
                            
                            // .pdf, .html gibi uzantıları sil
                            var drugName = lastPathSegment.substringBeforeLast(".")
                            
                            // Alt çizgi, tire gibi işaretleri boşluğa çevir
                            drugName = drugName.replace("-", " ").replace("_", " ")
                            
                            // Harf ve rakamlar arasına boşluk ekle (örn: stilizan1mg -> stilizan 1mg)
                            drugName = drugName.replace(Regex("(?<=[a-zA-Z])(?=\\d)"), " ")
                            drugName = drugName.replace(Regex("(?<=\\d)(?=[a-zA-Z])"), " ") // 1mg -> 1 mg
                            
                            // İlk harfleri büyük yap
                            scannedDrug = drugName.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                        } catch (e: Exception) {
                            // URL parse edilemezse ham hali kalsın
                        }
                    } else if (scannedDrug.startsWith("010") && scannedDrug.length >= 16) {
                        // Türkiye İlaç Takip Sistemi (İTS) GS1 Karekod (DataMatrix) formatı genellikle "010869..." ile başlar.
                        // "01" den sonraki "0" atlanıp 13 haneli asıl barkod numarası alınır.
                        scannedDrug = scannedDrug.substring(3, 16)
                    }
                    
                    viewModel.showBarcodeDialog.value = false
                    
                    if (currentTab == NavTab.EKLE) {
                        viewModel.setScannedMedicationForAdd(scannedDrug)
                    } else {
                        viewModel.setSearchQuery(scannedDrug)
                        viewModel.setNavTab(NavTab.ILACLARIM)
                    }
                }
                .addOnCanceledListener {
                    viewModel.showBarcodeDialog.value = false
                }
                .addOnFailureListener {
                    viewModel.showBarcodeDialog.value = false
                }
        }
    }

    if (showProfile) {
        ProfileDialog(
            userName = userName,
            onUpdateName = { newName -> viewModel.saveUserName(newName) },
            isDarkMode = isDarkMode,
            onToggleDarkMode = { viewModel.toggleDarkMode() },
            areNotificationsEnabled = areNotificationsEnabled,
            onToggleNotifications = { viewModel.setNotificationsEnabled(!areNotificationsEnabled) },
            onDismiss = { viewModel.showProfileDialog.value = false }
        )
    }

    if (showOnboarding) {
        WelcomeNameDialog(
            onNameConfirmed = { confirmedName ->
                viewModel.saveUserName(confirmedName)
            }
        )
    }

    if (showNotifications) {
        NotificationsDialog(
            doses = doses,
            medications = medications,
            areNotificationsEnabled = areNotificationsEnabled,
            onToggleNotifications = { viewModel.setNotificationsEnabled(!areNotificationsEnabled) },
            onDismiss = { viewModel.showNotificationsDialog.value = false }
        )
    }

    stockEditMed?.let { med ->
        StockUpdateDialog(
            medication = med,
            onDismiss = { viewModel.stockEditMedication.value = null },
            onSaveStock = { newStock ->
                viewModel.updateStock(med.id, newStock)
            }
        )
    }
}
