package com.kardiomed.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kardiomed.app.MainActivity
import com.kardiomed.app.model.AlarmItem
import java.util.Calendar

object AlarmScheduler {

    const val ACTION_TRIGGER_ALARM = "com.kardiomed.app.ACTION_TRIGGER_ALARM"
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_TITLE = "extra_alarm_title"
    const val EXTRA_ALARM_TYPE = "extra_alarm_type"
    const val EXTRA_SOUND_PRESET = "extra_sound_preset"
    const val EXTRA_CUSTOM_SOUND_URI = "extra_custom_sound_uri"
    const val EXTRA_VIBRATE = "extra_vibrate"
    const val EXTRA_TARGET_NOTE_ID = "extra_target_note_id"

    fun schedule(context: Context, alarm: AlarmItem) {
        if (!alarm.isEnabled) {
            cancel(context, alarm.id)
            return
        }

        val triggerTimeMs = computeNextTriggerTime(alarm) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_TITLE, alarm.title)
            putExtra(EXTRA_ALARM_TYPE, alarm.type.name)
            putExtra(EXTRA_SOUND_PRESET, alarm.soundPreset)
            putExtra(EXTRA_CUSTOM_SOUND_URI, alarm.customSoundUri)
            putExtra(EXTRA_VIBRATE, alarm.vibrate)
            putExtra(EXTRA_TARGET_NOTE_ID, alarm.targetNoteId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent do pokazania aplikacji po kliknięciu ikonki budzika na pasku stanu
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode() + 100000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                }
            } else {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Fallback
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
    }

    fun scheduleSnooze(context: Context, alarmId: String, title: String, typeName: String, preset: String, customUri: String?, vibrate: Boolean, noteId: String?, snoozeMinutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTimeMs = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, "$title (Drzemka)")
            putExtra(EXTRA_ALARM_TYPE, typeName)
            putExtra(EXTRA_SOUND_PRESET, preset)
            putExtra(EXTRA_CUSTOM_SOUND_URI, customUri)
            putExtra(EXTRA_VIBRATE, vibrate)
            putExtra(EXTRA_TARGET_NOTE_ID, noteId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + 50000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode() + 150000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    fun cancel(context: Context, alarmId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun computeNextTriggerTime(alarm: AlarmItem): Long? {
        val now = Calendar.getInstance()

        // Jeśli alarm nie jest cykliczny (jednorazowy)
        if (!alarm.isRecurring || alarm.daysOfWeek.isEmpty()) {
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1) // jutro
            }
            return target.timeInMillis
        }

        // Alarm cykliczny dla wybranych dni tygodnia
        // W Java Calendar: SUNDAY=1, MONDAY=2, ... SATURDAY=7
        // W naszym modelu: 1=Pn, 2=Wt, 3=Śr, 4=Cz, 5=Pt, 6=Sb, 7=Nd
        fun dayOfWeekToCalendar(d: Int): Int = when (d) {
            7 -> Calendar.SUNDAY
            else -> d + 1
        }

        val targetCalDays = alarm.daysOfWeek.map { dayOfWeekToCalendar(it) }

        var nearestTime = Long.MAX_VALUE
        for (calDay in targetCalDays) {
            val candidate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            var daysToAdd = (calDay - candidate.get(Calendar.DAY_OF_WEEK) + 7) % 7
            if (daysToAdd == 0 && candidate.timeInMillis <= now.timeInMillis) {
                daysToAdd = 7
            }
            candidate.add(Calendar.DAY_OF_YEAR, daysToAdd)

            if (candidate.timeInMillis < nearestTime) {
                nearestTime = candidate.timeInMillis
            }
        }

        return if (nearestTime == Long.MAX_VALUE) null else nearestTime
    }
}
