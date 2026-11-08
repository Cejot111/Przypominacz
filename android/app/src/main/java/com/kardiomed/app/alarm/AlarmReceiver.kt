package com.kardiomed.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kardiomed.app.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "Przypomnienie medyczne"
        val typeName = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE) ?: "PRESSURE"
        val preset = intent.getStringExtra(AlarmScheduler.EXTRA_SOUND_PRESET) ?: "CLASSIC_ALARM"
        val customUri = intent.getStringExtra(AlarmScheduler.EXTRA_CUSTOM_SOUND_URI)
        val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)
        val targetNoteId = intent.getStringExtra(AlarmScheduler.EXTRA_TARGET_NOTE_ID)

        // Uruchomienie serwisu pierwszoplanowego odtwarzającego głośny alarm
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, typeName)
            putExtra(AlarmScheduler.EXTRA_SOUND_PRESET, preset)
            putExtra(AlarmScheduler.EXTRA_CUSTOM_SOUND_URI, customUri)
            putExtra(AlarmScheduler.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmScheduler.EXTRA_TARGET_NOTE_ID, targetNoteId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // Aktualizacja stanu alarmu (jeśli jednorazowy -> wyłącz w bazie, jeśli cykliczny -> zaplanuj kolejny)
        CoroutineScope(Dispatchers.IO).launch {
            val repository = AppRepository.getInstance(context)
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                if (alarm.isRecurring) {
                    AlarmScheduler.schedule(context, alarm)
                } else {
                    repository.updateAlarm(alarm.copy(isEnabled = false))
                }
            }
        }
    }
}
