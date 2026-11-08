package com.kardiomed.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.kardiomed.app.R

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "kardiomed_alarm_channel"
        const val NOTIFICATION_ID = 9999
        const val ACTION_DISMISS = "com.kardiomed.app.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.kardiomed.app.ACTION_SNOOZE_ALARM"
    }

    private var vibrator: Vibrator? = null
    private var isVibrating = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val action = intent.action
        if (action == ACTION_DISMISS) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        } else if (action == ACTION_SNOOZE) {
            val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: ""
            val title = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "Alarm"
            val typeName = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE) ?: "PRESSURE"
            val preset = intent.getStringExtra(AlarmScheduler.EXTRA_SOUND_PRESET) ?: "CLASSIC_ALARM"
            val customUri = intent.getStringExtra(AlarmScheduler.EXTRA_CUSTOM_SOUND_URI)
            val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)
            val targetNoteId = intent.getStringExtra(AlarmScheduler.EXTRA_TARGET_NOTE_ID)

            AlarmScheduler.scheduleSnooze(this, alarmId, title, typeName, preset, customUri, vibrate, targetNoteId)
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: ""
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "Czas na pomiar ciśnienia"
        val typeName = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE) ?: "PRESSURE"
        val preset = intent.getStringExtra(AlarmScheduler.EXTRA_SOUND_PRESET) ?: "CLASSIC_ALARM"
        val customUri = intent.getStringExtra(AlarmScheduler.EXTRA_CUSTOM_SOUND_URI)
        val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)
        val targetNoteId = intent.getStringExtra(AlarmScheduler.EXTRA_TARGET_NOTE_ID)

        // Rozpoczęcie odtwarzania dźwięku budzika
        AlarmSoundPlayer.play(this, preset, customUri, loop = true)

        // Wibracja
        if (vibrate) {
            startVibration()
        }

        // Przygotowanie pełnoekranowego okna budzika
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, typeName)
            putExtra(AlarmScheduler.EXTRA_SOUND_PRESET, preset)
            putExtra(AlarmScheduler.EXTRA_CUSTOM_SOUND_URI, customUri)
            putExtra(AlarmScheduler.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmScheduler.EXTRA_TARGET_NOTE_ID, targetNoteId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Akcja Wyłącz
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            this.action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            101,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Akcja Drzemka
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            this.action = ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, typeName)
            putExtra(AlarmScheduler.EXTRA_SOUND_PRESET, preset)
            putExtra(AlarmScheduler.EXTRA_CUSTOM_SOUND_URI, customUri)
            putExtra(AlarmScheduler.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmScheduler.EXTRA_TARGET_NOTE_ID, targetNoteId)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            102,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("ALARM: $title")
            .setContentText("Dotknij, aby otworzyć aplikację lub wyłącz poniżej")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, "Wyłącz", dismissPendingIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "Drzemka (10 min)", snoozePendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Otwórz również od razu activity budzika jeśli urządzenie jest aktywne
        startActivity(fullScreenIntent)

        return START_STICKY
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            isVibrating = true
            val pattern = longArrayOf(0, 600, 300, 600, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (ignored: Exception) {}
    }

    private fun stopAlarm() {
        AlarmSoundPlayer.stop()
        if (isVibrating) {
            try {
                vibrator?.cancel()
            } catch (ignored: Exception) {}
            isVibrating = false
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_alarm_desc)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setBypassDnd(true)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(null, audioAttributes) // Dźwięk obsługuje AlarmSoundPlayer dla pełnej kontroli wyboru dźwięku
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
