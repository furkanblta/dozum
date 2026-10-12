package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val activeIngredient: String = "",
    val form: String = "Tablet",
    val dosage: String = "1 Tablet",
    val mealTiming: String = "Tok Karnına",
    val category: String = "REGULAR", // "REGULAR", "SOS", "SUPPLEMENT"
    val doctor: String? = null,
    val prescriptionNo: String? = null,
    val currentStock: Int = 30,
    val totalStock: Int = 30,
    val lowStockThreshold: Int = 5,
    val note: String? = null,
    val frequency: String = "Günde 1 defa",
    val reminderTimes: String = "08:30",
    val lastTakenNote: String? = null,
    val isActive: Boolean = true
)

@Entity(tableName = "dose_schedules")
data class DoseScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val scheduledTime: String,
    val periodName: String, // "Sabah", "Öğle", "Akşam", "Gece"
    val status: String, // "TAKEN", "PENDING", "SNOOZED", "MISSED"
    val takenTime: String? = null,
    val dateKey: String = "2024-10-17",
    val dayNumber: Int = 17,
    val snoozeMinutes: Int = 0
)

data class MedicationWithDose(
    val dose: DoseScheduleEntity,
    val medication: MedicationEntity
)
