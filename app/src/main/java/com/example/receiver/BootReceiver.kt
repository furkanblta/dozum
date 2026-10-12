package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.DozumDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = DozumDatabase.getDatabase(context)
            val dao = db.medicationDao()
            
            CoroutineScope(Dispatchers.IO).launch {
                val medications = dao.getAllMedicationsList()
                for (med in medications) {
                    AlarmScheduler.scheduleAlarmsForMedication(context, med)
                }
            }
        }
    }
}
