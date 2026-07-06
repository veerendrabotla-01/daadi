package com.example.daadi.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import com.example.daadi.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundManager(private val context: Context, private val settingsRepository: SettingsRepository) {
    private var soundPool: SoundPool? = null
    private var mediaPlayer: MediaPlayer? = null
    private val soundMap = mutableMapOf<Int, Int>()
    private var isLoaded = false
    private var musicEnabled = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        initializeSoundPool()
        observeSettings()
    }

    private fun observeSettings() {
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.settingsFlow.collect { settings ->
                musicEnabled = settings.musicEnabled
                if (musicEnabled && !isBackgroundMuted) {
                    resumeMusic()
                } else {
                    pauseMusic()
                }
            }
        }
    }

    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, _ ->
            isLoaded = true
        }

        // Asynchronously load sound samples from res/raw
        loadSounds()
    }

    private fun loadSounds() {
        CoroutineScope(Dispatchers.IO).launch {
            soundPool?.let { pool ->
                soundMap[R.raw.place_piece] = pool.load(context, R.raw.place_piece, 1)
                soundMap[R.raw.mill_formed] = pool.load(context, R.raw.mill_formed, 1)
                soundMap[R.raw.game_over] = pool.load(context, R.raw.game_over, 1)
            }
        }
    }

    private fun startMusic() {
        // mediaPlayer = MediaPlayer.create(context, R.raw.bg_music)
        // mediaPlayer?.isLooping = true
        if (musicEnabled && !isBackgroundMuted) {
            mediaPlayer?.start()
        }
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
    }

    private fun resumeMusic() {
        if (mediaPlayer == null) startMusic()
        else if (musicEnabled && !isBackgroundMuted) mediaPlayer?.start()
    }

    private fun playSound(resId: Int) {
        if (!isSoundEnabled() || isBackgroundMuted) return
        val soundId = soundMap[resId]
        if (soundId != null && isLoaded) {
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun isSoundEnabled(): Boolean {
        return settingsRepository.getSettings().soundEnabled
    }

    private fun isVibrationEnabled(): Boolean {
        return settingsRepository.getSettings().vibrationEnabled
    }

    private fun isCountdownSoundEnabled(): Boolean {
        return settingsRepository.getSettings().countdownSoundEnabled
    }

    private fun triggerHaptic(duration: Long = 50, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        if (!isVibrationEnabled()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var isBackgroundMuted = false

    fun setBackgroundMuted(muted: Boolean) {
        isBackgroundMuted = muted
        if (muted) {
            pauseMusic()
        } else if (musicEnabled) {
            resumeMusic()
        }
    }

    fun playPlace() {
        if (isBackgroundMuted) return
        triggerHaptic(40)
        playSound(R.raw.place_piece)
    }

    fun playMove() {
        if (isBackgroundMuted) return
        triggerHaptic(30)
        playSound(R.raw.place_piece) // Same acoustic tap for movement
    }

    fun playCapture() {
        if (isBackgroundMuted) return
        triggerHaptic(100, VibrationEffect.DEFAULT_AMPLITUDE)
        playSound(R.raw.place_piece) // Fallback or distinct capture if provided
    }

    fun playMillFormed() {
        if (isBackgroundMuted) return
        triggerHaptic(80, 200)
        playSound(R.raw.mill_formed)
    }

    fun playWin() {
        if (isBackgroundMuted) return
        playSound(R.raw.game_over)
    }

    fun playLose() {
        if (isBackgroundMuted) return
        playSound(R.raw.game_over)
    }

    fun playHint() {
        if (isBackgroundMuted) return
        playSound(R.raw.place_piece)
    }

    fun playCountdownTick() {
        if (isBackgroundMuted) return
        if (!isCountdownSoundEnabled()) return
        playSound(R.raw.place_piece)
    }

    fun playConnect() {
        if (isBackgroundMuted) return
        playSound(R.raw.place_piece)
    }

    fun playError() {
        if (isBackgroundMuted) return
        triggerHaptic(150, 255)
        playSound(R.raw.place_piece)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}
