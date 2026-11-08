package com.kardiomed.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kardiomed.app.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = AppRepository.getInstance(context)
                    val activeAlarms = repo.getAlarms().filter { it.isEnabled }
                    for (alarm in activeAlarms) {
                        AlarmScheduler.schedule(context, alarm)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
