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
                            if (!dir.exists()) {
                                dir.mkdirs()
                            }
                            if (dir.exists()) {
                                dir.setReadable(true, false)
                                dir.setWritable(true, false)
                                dir.setExecutable(true, false)
                                
                                // Create a persistent placeholder to ensure directory is non-empty
                                // and simple_file_enumerator doesn't freak out
                                val placeholder = java.io.File(dir, ".placeholder")
                                if (!placeholder.exists()) {
                                    placeholder.createNewFile()
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DaadiApp", "Failed to ensure webview cache dir: $path", e)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DaadiApp", "Error in ensureWebViewCacheDirs pass", e)
                }
                delay(3000)
            }
        }
    }

    private fun prepareWebViewCacheSync() {
        try {
            val cacheBase = cacheDir.absolutePath
            val webViewDirs = listOf(
                "WebView",
                "WebView/Default",
                "WebView/Default/HTTP Cache",
                "WebView/Default/HTTP Cache/Code Cache",
                "WebView/Default/HTTP Cache/Code Cache/js",
                "WebView/Default/HTTP Cache/Code Cache/wasm",
                "WebView/Default/Code Cache",
                "WebView/Default/Code Cache/js",
                "WebView/Default/Code Cache/wasm"
            )

            for (subPath in webViewDirs) {
                val dir = java.io.File(cacheDir, subPath)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                if (dir.exists()) {
                    dir.setReadable(true, false)
                    dir.setWritable(true, false)
                    dir.setExecutable(true, false)
                    
                    // Create synchronous placeholder
                    val placeholder = java.io.File(dir, ".placeholder")
                    if (!placeholder.exists()) {
                        placeholder.createNewFile()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DaadiApp", "Critical error preparing cache dirs", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Fix for Chromium opendir errors: Create directories synchronously before background passes
        prepareWebViewCacheSync()
        
        // Start background maintenance for redundancy
        ensureWebViewCacheDirs()
    }

    companion object {
        lateinit var instance: DaadiApplication
            private set
    }
}
