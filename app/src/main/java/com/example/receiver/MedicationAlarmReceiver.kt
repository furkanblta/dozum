package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Calendar

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getIntExtra("MEDICATION_ID", -1)
        val medName = intent.getStringExtra("MEDICATION_NAME") ?: "İlacınız"
        val medTime = intent.getStringExtra("MEDICATION_TIME") ?: ""
        val requestCode = intent.getIntExtra("REQUEST_CODE", medId * 100)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "dozum_channel",
                "İlaç Hatırlatıcı",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "İlaç alma zamanı bildirimleri"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            medId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "dozum_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("İlaç Zamanı: $medName")
            .setContentText("Saat $medTime - $medName ilacınızı alma zamanı geldi.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(medId, notification)

        // Reschedule for the next day
        val parts = medTime.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1].toIntOrNull() ?: 0

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1) // Schedule for tomorrow
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextIntent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                putExtra("MEDICATION_ID", medId)
                putExtra("MEDICATION_NAME", medName)
                putExtra("MEDICATION_TIME", medTime)
                putExtra("REQUEST_CODE", requestCode)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    nextPendingIntent
                )
            } catch (e: SecurityException) {
                // Handle permission gracefully
            }
        }
    }
}
