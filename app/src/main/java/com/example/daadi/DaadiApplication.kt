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
        applicationScope.launch(Dispatchers.IO) {
            // Repeat a few times during startup to combat race conditions with Chromium
            repeat(3) {
                try {
                    val cacheBase = cacheDir.absolutePath
                    val webViewDirs = listOf(
                        "$cacheBase/WebView",
                        "$cacheBase/WebView/Default",
                        "$cacheBase/WebView/Default/HTTP Cache",
                        "$cacheBase/WebView/Default/HTTP Cache/Code Cache",
                        "$cacheBase/WebView/Default/HTTP Cache/Code Cache/js",
                        "$cacheBase/WebView/Default/HTTP Cache/Code Cache/wasm",
                        "$cacheBase/WebView/Default/Code Cache",
                        "$cacheBase/WebView/Default/Code Cache/js",
                        "$cacheBase/WebView/Default/Code Cache/wasm"
                    )

                    for (path in webViewDirs) {
                        val dir = java.io.File(path)
                        try {
                            var isNewlyCreated = false
                            if (!dir.exists()) {
                                isNewlyCreated = dir.mkdirs()
                            }
                            if (dir.exists() && (isNewlyCreated || !dir.canRead() || !dir.canWrite())) {
                                dir.setReadable(true, false)
                                dir.setWritable(true, false)
                                dir.setExecutable(true, false)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DaadiApp", "Failed to ensure webview cache dir: $path", e)
                        }
                    }

                    // Clean up placeholder files
                    listOf("HTTP Cache/Code Cache/js", "HTTP Cache/Code Cache/wasm", "Code Cache/js", "Code Cache/wasm").forEach { subPath ->
                        val dir = java.io.File(cacheDir, "WebView/Default/$subPath")
                        if (dir.exists()) {
                            val placeholder = java.io.File(dir, ".placeholder")
                            if (placeholder.exists()) placeholder.delete()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DaadiApp", "Error in ensureWebViewCacheDirs pass", e)
                }
                delay(3000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Start WebView HTTP cache maintenance (handles its own background retries)
        ensureWebViewCacheDirs()
    }

    companion object {
        lateinit var instance: DaadiApplication
            private set
    }
}
