package com.example.data.repository

import com.example.data.db.MedicationDao
import com.example.data.model.DoseScheduleEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.MedicationWithDose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicationRepository(private val dao: MedicationDao) {

    val allMedications: Flow<List<MedicationEntity>> = dao.getAllMedications()
    val allDoses: Flow<List<DoseScheduleEntity>> = dao.getAllDoses()

    fun getMedicationById(id: Int): Flow<MedicationEntity?> = dao.getMedicationById(id)

    fun getDosesForDay(dayNumber: Int): Flow<List<DoseScheduleEntity>> = dao.getDosesForDay(dayNumber)

    fun getDosesForDate(dateKey: String): Flow<List<DoseScheduleEntity>> = dao.getDosesForDate(dateKey)

    fun getCombinedDosesForDay(dayNumber: Int): Flow<List<MedicationWithDose>> {
        return combine(dao.getDosesForDay(dayNumber), dao.getAllMedications()) { doses, meds ->
            val medMap = meds.associateBy { it.id }
            doses.mapNotNull { dose ->
                medMap[dose.medicationId]?.let { med ->
                    MedicationWithDose(dose = dose, medication = med)
                }
            }
        }
    }

    fun getCombinedDosesForDate(dateKey: String): Flow<List<MedicationWithDose>> {
        return combine(dao.getDosesForDate(dateKey), dao.getAllMedications()) { doses, meds ->
            val medMap = meds.associateBy { it.id }
            doses.mapNotNull { dose ->
                medMap[dose.medicationId]?.let { med ->
                    MedicationWithDose(dose = dose, medication = med)
                }
            }
        }
    }

    private fun getMondayOfCurrentWeek(): Calendar {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    suspend fun ensureWeeklyDosesForAllMedications() {
        val medications = dao.getAllMedicationsList()
        if (medications.isEmpty()) return

        val calendar = getMondayOfCurrentWeek()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val newSchedules = mutableListOf<DoseScheduleEntity>()

        for (i in 0..6) {
            val dNum = calendar.get(Calendar.DAY_OF_MONTH)
            val dKey = dateFormat.format(calendar.time)

            for (med in medications) {
                val actualTimes = if (med.reminderTimes.startsWith("OFF:")) {
                    med.reminderTimes.removePrefix("OFF:")
                } else {
                    med.reminderTimes
                }
                val times = actualTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    .ifEmpty { listOf("09:00") }

                val existingDoses = dao.getDosesForMedicationAndDate(med.id, dKey)
                val existingTimes = existingDoses.map { it.scheduledTime }.toSet()

                times.forEach { time ->
                    if (!existingTimes.contains(time)) {
                        val period = when {
                            time < "12:00" -> "Sabah"
                            time < "17:00" -> "Öğle"
                            time < "22:00" -> "Akşam"
                            else -> "Gece"
                        }
                        newSchedules.add(
                            DoseScheduleEntity(
                                medicationId = med.id,
                                scheduledTime = time,
                                periodName = period,
                                status = "PENDING",
                                dateKey = dKey,
                                dayNumber = dNum
                            )
                        )
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (newSchedules.isNotEmpty()) {
            dao.insertDoses(newSchedules)
        }
    }

    suspend fun insertMedication(medication: MedicationEntity): Long {
        val medId = dao.insertMedication(medication)
        generateWeeklyDosesForMedication(medId.toInt(), medication.reminderTimes)
        return medId
    }

    suspend fun toggleMedicationReminder(medication: MedicationEntity): Boolean {
        val isCurrentlyOff = medication.reminderTimes.startsWith("OFF:") || medication.reminderTimes.isBlank()
        if (isCurrentlyOff) {
            val restoredTimes = if (medication.reminderTimes.startsWith("OFF:")) {
                medication.reminderTimes.removePrefix("OFF:").ifBlank { "09:00" }
            } else {
                "09:00"
            }
            val updated = medication.copy(reminderTimes = restoredTimes)
            dao.updateMedication(updated)
            return true
        } else {
            val updated = medication.copy(reminderTimes = "OFF:${medication.reminderTimes}")
            dao.updateMedication(updated)
            return false
        }
    }

    suspend fun generateWeeklyDosesForMedication(medId: Int, reminderTimes: String) {
        val actualTimes = if (reminderTimes.startsWith("OFF:")) reminderTimes.removePrefix("OFF:") else reminderTimes
        val times = actualTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("09:00") }

        val calendar = getMondayOfCurrentWeek()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val schedules = mutableListOf<DoseScheduleEntity>()
        for (i in 0..6) {
            val dNum = calendar.get(Calendar.DAY_OF_MONTH)
            val dKey = dateFormat.format(calendar.time)

            val existingDoses = dao.getDosesForMedicationAndDate(medId, dKey)
            val existingTimes = existingDoses.map { it.scheduledTime }.toSet()

            times.forEach { time ->
                if (!existingTimes.contains(time)) {
                    val period = when {
                        time < "12:00" -> "Sabah"
                        time < "17:00" -> "Öğle"
                        time < "22:00" -> "Akşam"
                        else -> "Gece"
                    }
                    schedules.add(
                        DoseScheduleEntity(
                            medicationId = medId,
                            scheduledTime = time,
                            periodName = period,
                            status = "PENDING",
                            dateKey = dKey,
                            dayNumber = dNum
                        )
                    )
                }
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        if (schedules.isNotEmpty()) {
            dao.insertDoses(schedules)
        }
    }

    suspend fun updateMedication(medication: MedicationEntity) {
        dao.updateMedication(medication)
        generateWeeklyDosesForMedication(medication.id, medication.reminderTimes)
    }

    suspend fun deleteMedication(id: Int) {
        dao.deleteDosesForMedication(id)
        dao.deleteMedication(id)
    }

    suspend fun clearAllData() {
        dao.clearAllDoses()
        dao.clearAllMedications()
    }

    suspend fun markDoseTaken(doseId: Int, medicationId: Int, takenTime: String) {
        dao.updateDoseStatus(doseId, "TAKEN", takenTime)
        val med = dao.getMedicationById(medicationId).first()
        if (med != null && med.currentStock > 0) {
            dao.updateStock(medicationId, med.currentStock - 1)
        }
    }

    suspend fun undoDoseTaken(doseId: Int, medicationId: Int) {
        dao.updateDoseStatus(doseId, "PENDING", null)
        val med = dao.getMedicationById(medicationId).first()
        if (med != null) {
            dao.updateStock(medicationId, med.currentStock + 1)
        }
    }

    suspend fun snoozeDose(doseId: Int) {
        dao.snoozeDose(doseId)
    }

    suspend fun updateStock(medicationId: Int, newStock: Int) {
        dao.updateStock(medicationId, newStock)
    }

    suspend fun logSosDose(medicationId: Int) {
        val med = dao.getMedicationById(medicationId).first()
        if (med != null && med.currentStock > 0) {
            dao.updateStock(medicationId, med.currentStock - 1)
        }
    }

    suspend fun seedSampleDataIfRequested() {
        val initialMeds = listOf(
            MedicationEntity(
                id = 1,
                name = "Glifor 1000 mg",
                activeIngredient = "Metformin HCI",
                form = "Tablet",
                dosage = "1 Tablet",
                mealTiming = "Tok Karnına",
                category = "REGULAR",
                doctor = "Dr. Mehmet Kaya (Endokrinoloji)",
                prescriptionNo = "e-Reçete #29482",
                currentStock = 42,
                totalStock = 60,
                lowStockThreshold = 10,
                frequency = "Günde 2 defa (08:30, 20:30) • Tok karnına",
                reminderTimes = "08:30,20:30",
                lastTakenNote = "Son alım: Bugün 08:30"
            ),
            MedicationEntity(
                id = 2,
                name = "Vitamin D3 K2 Damla",
                activeIngredient = "Kolekalsiferol & Menaquinon",
                form = "Damla",
                dosage = "4 Damla",
                mealTiming = "Kahvaltı ile",
                category = "SUPPLEMENT",
                doctor = "Reçetesiz Destek Ürünü",
                prescriptionNo = "Günlük Takviye",
                currentStock = 180,
                totalStock = 200,
                lowStockThreshold = 20,
                frequency = "Günde 1 defa • Kahvaltı ile",
                reminderTimes = "08:30",
                lastTakenNote = "Son alım: Bugün 08:36"
            ),
            MedicationEntity(
                id = 3,
                name = "Omega-3 Balık Yağı",
                activeIngredient = "1200 mg EPA/DHA",
                form = "Softgel",
                dosage = "1 Kapsül",
                mealTiming = "Yemek esnasında",
                category = "REGULAR",
                doctor = "Reçetesiz Destek Ürünü (EPA / DHA)",
                prescriptionNo = "Günlük Rutin",
                currentStock = 22,
                totalStock = 30,
                lowStockThreshold = 5,
                frequency = "Günde 1 defa • Öğle yemeği ile",
                reminderTimes = "13:00",
                lastTakenNote = "Günde 1 kapsül planlandı"
            ),
            MedicationEntity(
                id = 4,
                name = "Nexium 40 mg",
                activeIngredient = "Esomeprazol",
                form = "Kapsül",
                dosage = "1 Kapsül",
                mealTiming = "Aç Karna",
                category = "REGULAR",
                doctor = "Uzm. Dr. Selin Doğan (Gastroenteroloji)",
                prescriptionNo = "SGK Kapsamında",
                currentStock = 4,
                totalStock = 28,
                lowStockThreshold = 7,
                note = "Yatmadan 1 saat önce bol su ile",
                frequency = "Günde 1 defa (21:00) • Aç karnına",
                reminderTimes = "21:00",
                lastTakenNote = "Kutuda son 4 tablet kaldı"
            )
        )

        dao.insertMedications(initialMeds)
    }
}
