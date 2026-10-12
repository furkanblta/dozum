package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.DozumDatabase
import com.example.data.model.MedicationEntity
import com.example.data.model.MedicationWithDose
import com.example.data.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.receiver.AlarmScheduler

enum class NavTab {
    BUGUN,
    ILACLARIM,
    EKLE,
    RAPORLAR
}

data class DayItem(
    val dayName: String,
    val dayNumber: Int,
    val isToday: Boolean,
    val hasDose: Boolean,
    val isCompleted: Boolean,
    val dateKey: String = ""
)

data class ToastMessage(
    val title: String,
    val message: String
)

class DozumViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicationRepository

    private val _currentTab = MutableStateFlow(NavTab.BUGUN)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    // Real current date dynamic getters
    val realCurrentDay: Int
        get() = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private val _selectedDay = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _selectedDateKey = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMedFilter = MutableStateFlow("all") // "all", "regular", "sos", "critical"
    val selectedMedFilter: StateFlow<String> = _selectedMedFilter.asStateFlow()

    private val _scannedMedicationForAdd = MutableStateFlow("")
    val scannedMedicationForAdd: StateFlow<String> = _scannedMedicationForAdd.asStateFlow()

    private val _selectedReportPeriod = MutableStateFlow("Bu Hafta")
    val selectedReportPeriod: StateFlow<String> = _selectedReportPeriod.asStateFlow()

    val medicationToEdit = MutableStateFlow<com.example.data.model.MedicationEntity?>(null)

    private val prefs = application.getSharedPreferences("dozum_prefs", android.content.Context.MODE_PRIVATE)

    // Dark Mode persisted setting
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // User name persisted setting - asks once on first run
    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // Notification master toggle persisted setting
    private val _areNotificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val areNotificationsEnabled: StateFlow<Boolean> = _areNotificationsEnabled.asStateFlow()

    // Onboarding dialog shown once if userName is blank
    val showOnboardingDialog = MutableStateFlow(prefs.getString("user_name", "").isNullOrBlank())

    private val _toastEvent = MutableSharedFlow<ToastMessage>()
    val toastEvent = _toastEvent.asSharedFlow()

    // Dialog states
    val showPrescriptionDialog = MutableStateFlow(false)
    val showBarcodeDialog = MutableStateFlow(false)
    val showProfileDialog = MutableStateFlow(false)
    val showNotificationsDialog = MutableStateFlow(false)
    val stockEditMedication = MutableStateFlow<MedicationEntity?>(null)

    init {
        val db = DozumDatabase.getDatabase(application)
        repository = MedicationRepository(db.medicationDao())

        // Wipe demo data on first start of production mode so the app starts clean and ready to use
        val isCleaned = prefs.getBoolean("demo_data_cleaned_production_v2", false)
        if (!isCleaned) {
            viewModelScope.launch {
                repository.clearAllData()
                prefs.edit().putBoolean("demo_data_cleaned_production_v2", true).apply()
            }
        } else {
            viewModelScope.launch {
                repository.ensureWeeklyDosesForAllMedications()
            }
        }
    }

    val allMedications: StateFlow<List<MedicationEntity>> = repository.allMedications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentDayDoses: StateFlow<List<MedicationWithDose>> = _selectedDateKey
        .flatMapLatest { dateKey ->
            repository.getCombinedDosesForDate(dateKey)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDoses: StateFlow<List<com.example.data.model.DoseScheduleEntity>> = repository.allDoses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allMedicationsWithDoses: StateFlow<List<MedicationWithDose>> = combine(repository.allDoses, repository.allMedications) { doses, meds ->
        val medMap = meds.associateBy { it.id }
        doses.mapNotNull { dose ->
            medMap[dose.medicationId]?.let { med ->
                MedicationWithDose(dose = dose, medication = med)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun showToast(title: String, message: String) {
        viewModelScope.launch {
            _toastEvent.emit(ToastMessage(title = title, message = message))
        }
    }

    // Dynamic 7-day week based on current calendar week
    fun getWeekDays(): List<DayItem> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = getTodayDateKey()

        val dayNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        val days = mutableListOf<DayItem>()

        for (i in 0..6) {
            val dNum = cal.get(Calendar.DAY_OF_MONTH)
            val dKey = dateFormat.format(cal.time)
            val isToday = dKey == todayKey
            days.add(
                DayItem(
                    dayName = dayNames[i],
                    dayNumber = dNum,
                    isToday = isToday,
                    hasDose = true,
                    isCompleted = dKey < todayKey,
                    dateKey = dKey
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return days
    }

    fun getCurrentMonthAndWeekInfo(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("tr"))
        val monthStr = monthFormat.format(cal.time).replaceFirstChar { it.uppercase() }
        val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
        return Pair(monthStr, "Hafta $weekOfYear")
    }

    fun setNavTab(tab: NavTab) {
        _currentTab.value = tab
        if (tab != NavTab.EKLE) {
            medicationToEdit.value = null
        }
        if (tab == NavTab.BUGUN) {
            viewModelScope.launch {
                repository.ensureWeeklyDosesForAllMedications()
            }
        }
    }

    fun setSelectedDay(day: Int, dateKey: String? = null) {
        _selectedDay.value = day
        if (dateKey != null) {
            _selectedDateKey.value = dateKey
        } else {
            val found = getWeekDays().firstOrNull { it.dayNumber == day }
            _selectedDateKey.value = found?.dateKey ?: getTodayDateKey()
        }
        viewModelScope.launch {
            repository.ensureWeeklyDosesForAllMedications()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMedFilter(filter: String) {
        _selectedMedFilter.value = filter
    }

    fun setScannedMedicationForAdd(name: String) {
        _scannedMedicationForAdd.value = name
    }

    fun setReportPeriod(period: String) {
        _selectedReportPeriod.value = period
    }

    fun toggleDarkMode() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        prefs.edit().putBoolean("dark_mode", newMode).apply()
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun toggleNotificationsEnabled() {
        val newState = !_areNotificationsEnabled.value
        _areNotificationsEnabled.value = newState
        prefs.edit().putBoolean("notifications_enabled", newState).apply()
        viewModelScope.launch {
            _toastEvent.emit(
                ToastMessage(
                    title = if (newState) "Bildirimler Açıldı 🔔" else "Bildirimler Kapatıldı 🔕",
                    message = if (newState) "İlaç ve doz hatırlatıcıları aktif." else "İlaç hatırlatıcı bildirimleri duraklatıldı."
                )
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _areNotificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun saveUserName(name: String) {
        val trimmed = name.trim()
        _userName.value = trimmed
        prefs.edit().putString("user_name", trimmed).apply()
        showOnboardingDialog.value = false
        if (trimmed.isNotEmpty()) {
            viewModelScope.launch {
                _toastEvent.emit(
                    ToastMessage(
                        title = "Hoş Geldiniz, $trimmed! 👋",
                        message = "Dozum ilaç takip asistanınız hazır."
                    )
                )
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _toastEvent.emit(
                ToastMessage(
                    title = "Tüm Veriler Temizlendi 🗑️",
                    message = "Demo veriler silindi. Uygulama kişisel kullanımınız için hazır!"
                )
            )
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleDataIfRequested()
            _toastEvent.emit(
                ToastMessage(
                    title = "Örnek Veriler Yüklendi 💊",
                    message = "Örnek ilaçlar eklendi."
                )
            )
        }
    }

    fun deleteMedication(id: Int, name: String) {
        viewModelScope.launch {
            val med = repository.getMedicationById(id).firstOrNull()
            if (med != null) {
                AlarmScheduler.cancelAlarmsForMedication(getApplication(), med)
            }
            repository.deleteMedication(id)
            _toastEvent.emit(
                ToastMessage(
                    title = "İlaç Silindi 🗑️",
                    message = "$name dolabınızdan başarıyla kaldırıldı."
                )
            )
        }
    }

    fun markDoseTaken(doseId: Int, medicationId: Int, medName: String) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            repository.markDoseTaken(doseId, medicationId, nowStr)
            _toastEvent.emit(
                ToastMessage(
                    title = "Doz Alındı! ✓",
                    message = "$medName günlüğe başarıyla kaydedildi."
                )
            )
        }
    }

    fun undoDoseTaken(doseId: Int, medicationId: Int, medName: String) {
        viewModelScope.launch {
            repository.undoDoseTaken(doseId, medicationId)
            _toastEvent.emit(
                ToastMessage(
                    title = "Geri Alındı ↩️",
                    message = "$medName dozu alınmadı olarak işaretlendi."
                )
            )
        }
    }

    fun snoozeDose(doseId: Int, medName: String) {
        viewModelScope.launch {
            repository.snoozeDose(doseId)
            _toastEvent.emit(
                ToastMessage(
                    title = "Doz Ertelendi ⏰",
                    message = "$medName hatırlatıcısı 15 dakika ertelendi."
                )
            )
        }
    }

    fun updateStock(medicationId: Int, newStock: Int) {
        viewModelScope.launch {
            repository.updateStock(medicationId, newStock)
            stockEditMedication.value = null
            _toastEvent.emit(
                ToastMessage(
                    title = "Stok Güncellendi 📦",
                    message = "Kalan stok $newStock olarak kaydedildi."
                )
            )
        }
    }

    fun updateMedicationDetails(medication: com.example.data.model.MedicationEntity, newName: String, newDosage: String, newNote: String) {
        viewModelScope.launch {
            val updated = medication.copy(
                name = newName,
                dosage = newDosage,
                note = newNote
            )
            repository.updateMedication(updated)
            medicationToEdit.value = null
            _toastEvent.emit(
                ToastMessage(
                    title = "İlaç Güncellendi ✓",
                    message = "$newName detayları başarıyla kaydedildi."
                )
            )
        }
    }

    fun logSosDose(medication: MedicationEntity) {
        viewModelScope.launch {
            repository.logSosDose(medication.id)
            _toastEvent.emit(
                ToastMessage(
                    title = "S.O.S Doz Kaydedildi 💊",
                    message = "${medication.name} kullanımı kaydedildi, stoktan düşüldü."
                )
            )
        }
    }

    fun toggleMedicationReminder(medication: MedicationEntity) {
        viewModelScope.launch {
            val enabled = repository.toggleMedicationReminder(medication)
            if (enabled) {
                val updatedMed = repository.getMedicationById(medication.id).firstOrNull()
                if (updatedMed != null) {
                    AlarmScheduler.scheduleAlarmsForMedication(getApplication(), updatedMed)
                }
            } else {
                AlarmScheduler.cancelAlarmsForMedication(getApplication(), medication)
            }
            _toastEvent.emit(
                ToastMessage(
                    title = if (enabled) "Bildirimler Açıldı 🔔" else "Bildirimler Kapatıldı 🔕",
                    message = if (enabled)
                        "${medication.name} için doz alarmları aktif edildi."
                    else
                        "${medication.name} için bildirimler kapatıldı (yalnızca dolap/stok takibi)."
                )
            )
        }
    }

    fun saveNewMedication(
        name: String,
        dosage: String,
        form: String,
        frequency: String,
        mealTiming: String,
        stock: Int,
        isCriticalAlert: Boolean,
        isReminderEnabled: Boolean = true,
        times: List<String>,
        note: String
    ) {
        viewModelScope.launch {
            val effectiveTimes = if (times.isNotEmpty()) times else listOf("08:30")
            val formattedReminderTimes = if (isReminderEnabled) {
                effectiveTimes.joinToString(",")
            } else {
                "OFF:${effectiveTimes.joinToString(",")}"
            }

            val newMed = MedicationEntity(
                name = name.ifBlank { "Yeni İlaç" },
                activeIngredient = "Aktif Bileşen",
                form = form,
                dosage = dosage.ifBlank { "1 $form" },
                mealTiming = mealTiming,
                category = if (frequency.contains("Gerektiğinde")) "SOS" else "REGULAR",
                doctor = null,
                prescriptionNo = null,
                currentStock = stock,
                totalStock = stock,
                lowStockThreshold = if (isCriticalAlert) 5 else 0,
                note = note.ifBlank { null },
                frequency = frequency,
                reminderTimes = formattedReminderTimes
            )
            val insertedId = repository.insertMedication(newMed)
            AlarmScheduler.scheduleAlarmsForMedication(getApplication(), newMed.copy(id = insertedId.toInt()))
            _toastEvent.emit(
                ToastMessage(
                    title = "İlaç Kaydedildi! ✨",
                    message = if (isReminderEnabled)
                        "$name için hatırlatıcılar başarıyla başlatıldı."
                    else
                        "$name dolabınıza eklendi (bildirimler isteğiniz üzerine kapalı)."
                )
            )
            _currentTab.value = NavTab.ILACLARIM
        }
    }
    fun updateFullMedication(
        medication: MedicationEntity,
        name: String,
        dosage: String,
        form: String,
        frequency: String,
        mealTiming: String,
        stock: Int,
        isCriticalAlert: Boolean,
        isReminderEnabled: Boolean = true,
        times: List<String>,
        note: String
    ) {
        viewModelScope.launch {
            val effectiveTimes = if (times.isNotEmpty()) times else listOf("08:30")
            val formattedReminderTimes = if (isReminderEnabled) {
                effectiveTimes.joinToString(",")
            } else {
                "OFF:${effectiveTimes.joinToString(",")}"
            }

            val updatedMed = medication.copy(
                name = name.ifBlank { medication.name },
                form = form,
                dosage = dosage.ifBlank { medication.dosage },
                mealTiming = mealTiming,
                category = if (frequency.contains("Gerektiğinde")) "SOS" else "REGULAR",
                currentStock = stock,
                totalStock = stock.coerceAtLeast(medication.totalStock),
                lowStockThreshold = if (isCriticalAlert) 5 else 0,
                note = note.ifBlank { null },
                frequency = frequency,
                reminderTimes = formattedReminderTimes
            )
            repository.updateMedication(updatedMed)
            AlarmScheduler.scheduleAlarmsForMedication(getApplication(), updatedMed)
            medicationToEdit.value = null
            _toastEvent.emit(
                ToastMessage(
                    title = "İlaç Güncellendi ✓",
                    message = "$name başarıyla güncellendi."
                )
            )
            _currentTab.value = NavTab.ILACLARIM
        }
    }
}
