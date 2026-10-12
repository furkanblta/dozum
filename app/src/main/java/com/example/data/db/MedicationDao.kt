package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DoseScheduleEntity
import com.example.data.model.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY id ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY id ASC")
    suspend fun getAllMedicationsList(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id")
    fun getMedicationById(id: Int): Flow<MedicationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<MedicationEntity>)

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: Int)

    @Query("DELETE FROM dose_schedules WHERE medicationId = :medicationId")
    suspend fun deleteDosesForMedication(medicationId: Int)

    @Query("DELETE FROM medications")
    suspend fun clearAllMedications()

    @Query("DELETE FROM dose_schedules")
    suspend fun clearAllDoses()

    @Query("UPDATE medications SET currentStock = :newStock WHERE id = :id")
    suspend fun updateStock(id: Int, newStock: Int)

    @Query("SELECT * FROM dose_schedules WHERE dayNumber = :dayNumber ORDER BY scheduledTime ASC")
    fun getDosesForDay(dayNumber: Int): Flow<List<DoseScheduleEntity>>

    @Query("SELECT * FROM dose_schedules WHERE dateKey = :dateKey ORDER BY scheduledTime ASC")
    fun getDosesForDate(dateKey: String): Flow<List<DoseScheduleEntity>>

    @Query("SELECT * FROM dose_schedules WHERE medicationId = :medicationId AND dateKey = :dateKey")
    suspend fun getDosesForMedicationAndDate(medicationId: Int, dateKey: String): List<DoseScheduleEntity>

    @Query("SELECT * FROM dose_schedules ORDER BY scheduledTime ASC")
    fun getAllDoses(): Flow<List<DoseScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDose(dose: DoseScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoses(doses: List<DoseScheduleEntity>)

    @Update
    suspend fun updateDose(dose: DoseScheduleEntity)

    @Query("UPDATE dose_schedules SET status = :status, takenTime = :takenTime WHERE id = :id")
    suspend fun updateDoseStatus(id: Int, status: String, takenTime: String?)

    @Query("UPDATE dose_schedules SET status = 'SNOOZED', snoozeMinutes = snoozeMinutes + 15 WHERE id = :id")
    suspend fun snoozeDose(id: Int)

    @Query("SELECT COUNT(*) FROM medications")
    suspend fun getMedicationCount(): Int
}
