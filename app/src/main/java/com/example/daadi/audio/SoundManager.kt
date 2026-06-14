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

    private fun isCountdownSoundEnabled(): Boolean {
        return settingsRepository.getSettings().countdownSoundEnabled
    }

    fun playPlace() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Short, warm acoustic-like tap for placement
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_0, 60)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playMove() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Soft slide-like tone
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 70)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playCapture() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Distinct downward alert
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playMillFormed() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Harmonic double beep for success
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playWin() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                Thread.sleep(150)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 300)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playLose() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 400)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playHint() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_7, 80)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playCountdownTick() {
        if (!isSoundEnabled() || !isCountdownSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Neutral, very short tick for urgency but not annoyance
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 40)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playConnect() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playError() {
        if (!isSoundEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 100)
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
