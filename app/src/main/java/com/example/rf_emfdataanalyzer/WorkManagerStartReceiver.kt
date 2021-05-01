package com.example.rf_emfdataanalyzer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WorkManagerStartReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val workRequest = PeriodicWorkRequestBuilder<SaveDataJobService>(5, TimeUnit.MINUTES)
            .setInitialDelay(0, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("emf work",
            ExistingPeriodicWorkPolicy.KEEP,workRequest)
    }
}