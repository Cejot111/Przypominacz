package com.kardiomed.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.kardiomed.app.model.SoundPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object AlarmSoundPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var toneJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun play(
        context: Context,
        preset: String,
        customUriStr: String? = null,
        loop: Boolean = true
    ) {
        stop()

        try {
            when (preset) {
                SoundPresets.CUSTOM_SYSTEM -> {
                    val uri = if (!customUriStr.isNullOrEmpty()) {
                        Uri.parse(customUriStr)
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    }
                    playUri(context, uri, loop)
                }

                SoundPresets.SYSTEM_DEFAULT -> {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    playUri(context, uri, loop)
                }

                SoundPresets.CLASSIC_ALARM -> {
                    playSyntheticPattern(
                        toneType1 = ToneGenerator.TONE_CDMA_HIGH_L,
                        toneType2 = ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK,
                        durationMs = 280,
                        pauseMs = 120,
                        loop = loop
                    )
                }

                SoundPresets.MEDICAL_BEEP -> {
                    // Pulsometr / sygnał medyczny: podwójny pik
                    playSyntheticPattern(
                        toneType1 = ToneGenerator.TONE_PROP_BEEP,
                        toneType2 = ToneGenerator.TONE_PROP_BEEP2,
                        durationMs = 140,
                        pauseMs = 350,
                        loop = loop
                    )
                }

                SoundPresets.GENTLE_CHIME -> {
                    // Łagodny gong
                    playSyntheticPattern(
                        toneType1 = ToneGenerator.TONE_PROP_PROMPT,
                        toneType2 = ToneGenerator.TONE_PROP_ACK,
                        durationMs = 350,
                        pauseMs = 600,
                        loop = loop
                    )
                }

                SoundPresets.DIGITAL_WAKE -> {
                    // Szybki zegarek cyfrowy
                    playSyntheticPattern(
                        toneType1 = ToneGenerator.TONE_DTMF_D,
                        toneType2 = ToneGenerator.TONE_DTMF_0,
                        durationMs = 180,
                        pauseMs = 150,
                        loop = loop
                    )
                }

                else -> {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    playUri(context, uri, loop)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Awaryjny fallback do domyślnego dzwonka systemowego
            fallbackSystemAlarm(context)
        }
    }

    private fun playUri(context: Context, uri: Uri?, loop: Boolean) {
        if (uri == null) {
            fallbackSystemAlarm(context)
            return
        }
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = loop
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Spróbuj przez Ringtone API jeśli MediaPlayer zawiedzie
            ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                play()
            }
        }
    }

    private fun playSyntheticPattern(
        toneType1: Int,
        toneType2: Int,
        durationMs: Long,
        pauseMs: Long,
        loop: Boolean
    ) {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        }

        toneJob = CoroutineScope(Dispatchers.Default).launch {
            var counter = 0
            while (isActive && (loop || counter < 6)) {
                try {
                    toneGenerator?.startTone(if (counter % 2 == 0) toneType1 else toneType2, durationMs.toInt())
                    delay(durationMs + pauseMs)
                    counter++
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun fallbackSystemAlarm(context: Context) {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(context, alertUri)
            ringtone?.play()
        } catch (ignored: Exception) {}
    }

    /**
     * Odtwarza próbkę dźwięku na kilka sekund i automatycznie wyłącza (dla selektora w UI)
     */
    fun previewSound(context: Context, preset: String, customUriStr: String? = null, onFinish: () -> Unit = {}) {
        play(context, preset, customUriStr, loop = false)
        mainHandler.postDelayed({
            stop()
            onFinish()
        }, 3500)
    }

    fun stop() {
        mainHandler.removeCallbacksAndMessages(null)
        toneJob?.cancel()
        toneJob = null

        try {
            toneGenerator?.release()
        } catch (ignored: Exception) {}
        toneGenerator = null

        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (ignored: Exception) {}
        mediaPlayer = null

        try {
            ringtone?.let {
                if (it.isPlaying) it.stop()
            }
        } catch (ignored: Exception) {}
        ringtone = null
    }
}
