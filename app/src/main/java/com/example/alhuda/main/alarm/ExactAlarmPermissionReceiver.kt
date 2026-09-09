package com.example.alhuda.main.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var schedulerReconciler: SchedulerReconciler

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                Log.d("ExactAlarmReceiver", "Izin SCHEDULE_EXACT_ALARM berubah! Menjalankan reconcileAll...")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        schedulerReconciler.reconcileAll()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
