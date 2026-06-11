package com.example.daadi.audio

import android.media.AudioManager
import android.media.ToneGenerator
import com.example.daadi.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundManager(private val settingsRepository: SettingsRepository) {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isSoundEnabled(): Boolean {
        return settingsRepository.getSettings().soundEnabled
    }

    fun playPlace() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Short, warm high pitch beep for placement
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playMove() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Quick prompt tone for movement
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playCapture() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Low warning buzz tone for capture
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 180)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playMillFormed() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // High success beep-beep
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playWin() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
                Thread.sleep(100)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playLose() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playHint() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
                Thread.sleep(60)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 80)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playCountdownTick() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
