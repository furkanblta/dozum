package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.model.MedicationEntity
import java.util.Calendar

object AlarmScheduler {
    fun scheduleAlarmsForMedication(context: Context, medication: MedicationEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        cancelAlarmsForMedication(context, medication)

        if (!medication.isActive) return
        if (medication.reminderTimes.startsWith("OFF:")) return
        if (medication.reminderTimes.isBlank()) return

        val times = medication.reminderTimes.split(",")
        times.forEachIndexed { index, timeStr ->
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1].toIntOrNull() ?: 0

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                val requestCode = medication.id * 100 + index
                val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                    putExtra("MEDICATION_ID", medication.id)
                    putExtra("MEDICATION_NAME", medication.name)
                    putExtra("MEDICATION_TIME", timeStr)
                    putExtra("REQUEST_CODE", requestCode)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    // Ignored
                }
            }
        }
    }

    fun cancelAlarmsForMedication(context: Context, medication: MedicationEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (index in 0..10) {
            val requestCode = medication.id * 100 + index
            val intent = Intent(context, MedicationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
