package com.example.daadi

import android.app.Application
import com.example.daadi.data.ads.AdManager
import com.example.daadi.data.multiplayer.MultiplayerManager
import com.example.daadi.audio.SoundManager
import com.example.daadi.data.repository.*
import com.example.daadi.data.supabase.SupabaseManager
import com.example.daadi.data.repository.supabase.*
import kotlinx.coroutines.*

class DaadiApplication : Application() {

    val gameRepository: GameRepository by lazy { GameRepositoryImpl(this) }
    val statsRepository: StatsRepository by lazy { StatsRepositoryImpl(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(this) }
    val soundManager: SoundManager by lazy { SoundManager(this, settingsRepository) }
    val multiplayerManager: MultiplayerManager by lazy { MultiplayerManager(this, soundManager) }
    val supabaseManager: SupabaseManager by lazy { SupabaseManager(this) }
    val database: com.example.daadi.data.local.AppDatabase by lazy {
        androidx.room.Room.databaseBuilder(
            this,
            com.example.daadi.data.local.AppDatabase::class.java,
            "daadi_cache_db"
        ).build()
    }
    val authRepository: AuthRepository by lazy { AuthRepository(supabaseManager) }
    val adminRepository: AdminRepository by lazy { AdminRepository(supabaseManager) }
    val analyticsRepository: AnalyticsRepository by lazy { AnalyticsRepository(supabaseManager) }
    val remoteGameRepository: RemoteGameRepository by lazy { RemoteGameRepository(supabaseManager, database.matchDao()) }
    val economyRepository: EconomyRepository by lazy { EconomyRepository(supabaseManager) }
    val liveOpsRepository: LiveOpsRepository by lazy { LiveOpsRepository(supabaseManager) }
    val supportRepository: SupportRepository by lazy { SupportRepository(supabaseManager) }
    val tournamentRepository: TournamentRepository by lazy { TournamentRepository(supabaseManager) }
    val remoteConfigRepository: RemoteConfigRepository by lazy { RemoteConfigRepository(supabaseManager) }
    val userRepository: UserRepository by lazy { UserRepository(supabaseManager, database.userDao()) }
    val adManager: AdManager by lazy { AdManager(this, remoteConfigRepository, analyticsRepository) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun ensureWebViewCacheDirs() {
        try {
            val webViewDir = java.io.File(cacheDir, "WebView")
            val defaultDir = java.io.File(webViewDir, "Default")
            val httpCacheDir = java.io.File(defaultDir, "HTTP Cache")
            val codeCacheDir = java.io.File(httpCacheDir, "Code Cache")
            val jsDir = java.io.File(codeCacheDir, "js")
            val wasmDir = java.io.File(codeCacheDir, "wasm")

            val dirs = listOf(webViewDir, defaultDir, httpCacheDir, codeCacheDir, jsDir, wasmDir)
            for (dir in dirs) {
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                if (dir.exists()) {
                    dir.setReadable(true, false)
                    dir.setWritable(true, false)
                    dir.setExecutable(true, false)
                }
            }

            // Clean up any stale placeholder files to prevent cache corruption
            val jsPlaceholder = java.io.File(jsDir, ".placeholder")
            if (jsPlaceholder.exists()) {
                jsPlaceholder.delete()
            }
            val wasmPlaceholder = java.io.File(wasmDir, ".placeholder")
            if (wasmPlaceholder.exists()) {
                wasmPlaceholder.delete()
            }
        } catch (e: Exception) {
            // Silence any errors since this is just an optimization
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Pre-create WebView HTTP cache directories immediately
        ensureWebViewCacheDirs()
        
        // Launch a background coroutine to continuously ensure they exist during startup race conditions
        applicationScope.launch {
            for (i in 1..300) { // 300 * 50ms = 15 seconds
                ensureWebViewCacheDirs()
                delay(50)
            }
        }
        
        // Schedule delayed checks as a fallback later on
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            ensureWebViewCacheDirs()
        }, 5000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            ensureWebViewCacheDirs()
        }, 15000)
    }

    companion object {
        lateinit var instance: DaadiApplication
            private set
    }
}
