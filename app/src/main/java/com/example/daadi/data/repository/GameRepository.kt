package com.example.daadi.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.daadi.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface GameRepository {
    fun saveGame(state: GameState)
    fun loadGame(): GameState?
    fun clearSavedGame()
}

interface StatsRepository {
    val statsFlow: StateFlow<PlayerStats>
    fun getStats(): PlayerStats
    fun saveStats(stats: PlayerStats)
    fun updateStats(winner: Player?, mode: GameMode, difficulty: AIDifficulty)
    fun resetStats()
}

interface SettingsRepository {
    val settingsFlow: StateFlow<AppSettings>
    fun getSettings(): AppSettings
    fun saveSettings(settings: AppSettings)
}

class GameRepositoryImpl(context: Context) : GameRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("daadi_game_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(GameState::class.java)

    override fun saveGame(state: GameState) {
        try {
            val json = adapter.toJson(state)
            prefs.edit().putString("saved_state", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun loadGame(): GameState? {
        val json = prefs.getString("saved_state", null) ?: return null
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun clearSavedGame() {
        prefs.edit().remove("saved_state").apply()
    }
}

class StatsRepositoryImpl(context: Context) : StatsRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("daadi_stats_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(PlayerStats::class.java)

    private val _statsFlow = MutableStateFlow(getStats())
    override val statsFlow: StateFlow<PlayerStats> = _statsFlow.asStateFlow()

    override fun getStats(): PlayerStats {
        val json = prefs.getString("player_stats", null) ?: return PlayerStats()
        return try {
            adapter.fromJson(json) ?: PlayerStats()
        } catch (e: Exception) {
            PlayerStats()
        }
    }

    override fun saveStats(stats: PlayerStats) {
        try {
            val json = adapter.toJson(stats)
            prefs.edit().putString("player_stats", json).apply()
            _statsFlow.value = stats
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun updateStats(winner: Player?, mode: GameMode, difficulty: AIDifficulty) {
        val current = getStats()
        val isWin = winner == Player.PLAYER_1
        val isLoss = winner == Player.PLAYER_2
        val isDraw = winner == null

        val updated = current.copy(
            totalGamesPlayed = current.totalGamesPlayed + 1,
            totalWins = if (isWin) current.totalWins + 1 else current.totalWins,
            totalLosses = if (isLoss) current.totalLosses + 1 else current.totalLosses,
            totalDraws = if (isDraw) current.totalDraws + 1 else current.totalDraws,
            winsVsEasyAI = if (mode == GameMode.VS_AI && difficulty == AIDifficulty.EASY && isWin) current.winsVsEasyAI + 1 else current.winsVsEasyAI,
            lossesVsEasyAI = if (mode == GameMode.VS_AI && difficulty == AIDifficulty.EASY && isLoss) current.lossesVsEasyAI + 1 else current.lossesVsEasyAI,
            winsVsMediumAI = if (mode == GameMode.VS_AI && difficulty == AIDifficulty.MEDIUM && isWin) current.winsVsMediumAI + 1 else current.winsVsMediumAI,
            lossesVsMediumAI = if (mode == GameMode.VS_AI && difficulty == AIDifficulty.MEDIUM && isLoss) current.lossesVsMediumAI + 1 else current.lossesVsMediumAI,
            winsVsHardAI = if (mode == GameMode.VS_AI && difficulty == AIDifficulty.HARD && isWin) current.winsVsHardAI + 1 else current.winsVsHardAI,
            lossesVsHardAI = if (mode == GameMode.VS_AI && difficulty == AIDifficulty.HARD && isLoss) current.lossesVsHardAI + 1 else current.lossesVsHardAI,
            passAndPlayGames = if (mode == GameMode.PASS_AND_PLAY) current.passAndPlayGames + 1 else current.passAndPlayGames
        )
        saveStats(updated)
    }

    override fun resetStats() {
        saveStats(PlayerStats())
    }
}

class SettingsRepositoryImpl(context: Context) : SettingsRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("daadi_settings_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(AppSettings::class.java)

    private val _settingsFlow = MutableStateFlow(getSettings())
    override val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    override fun getSettings(): AppSettings {
        val json = prefs.getString("app_settings", null) ?: return AppSettings()
        return try {
            adapter.fromJson(json) ?: AppSettings()
        } catch (e: Exception) {
            AppSettings()
        }
    }

    override fun saveSettings(settings: AppSettings) {
        try {
            val json = adapter.toJson(settings)
            prefs.edit().putString("app_settings", json).apply()
            _settingsFlow.value = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
