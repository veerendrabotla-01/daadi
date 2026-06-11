package com.example.daadi

import android.app.Application
import com.example.daadi.data.multiplayer.MultiplayerManager
import com.example.daadi.audio.SoundManager
import com.example.daadi.data.repository.*
import com.example.daadi.data.supabase.SupabaseManager

class DaadiApplication : Application() {

    lateinit var gameRepository: GameRepository
    lateinit var statsRepository: StatsRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var soundManager: SoundManager
    lateinit var multiplayerManager: MultiplayerManager
    lateinit var supabaseManager: SupabaseManager

    override fun onCreate() {
        super.onCreate()
        instance = this

        gameRepository = GameRepositoryImpl(this)
        statsRepository = StatsRepositoryImpl(this)
        settingsRepository = SettingsRepositoryImpl(this)
        soundManager = SoundManager(settingsRepository)
        multiplayerManager = MultiplayerManager(this, soundManager)
        supabaseManager = SupabaseManager(this)
    }

    companion object {
        lateinit var instance: DaadiApplication
            private set
    }
}
