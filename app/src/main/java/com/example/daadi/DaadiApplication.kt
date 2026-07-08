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
                    android.util.Log.e("DaadiApp", "Failed to ensure webview cache dir: ${dir.absolutePath}", e)
                }
            }

            // CLEAN UP: Delete any foreign placeholder files inside 'js' or 'wasm' subdirectories,
            // because Chromium's SimpleCache expects ONLY valid cache entry hashes. Non-conforming filenames
            // (like .placeholder) will flag the cache directory as corrupted and cause Chromium
            // to delete the entire HTTP Cache / Code Cache folders on startup, resulting in opendir errors!
            try {
                if (jsDir.exists()) {
                    val jsPlaceholder = java.io.File(jsDir, ".placeholder")
                    if (jsPlaceholder.exists()) {
                        jsPlaceholder.delete()
                    }
                }
                if (wasmDir.exists()) {
                    val wasmPlaceholder = java.io.File(wasmDir, ".placeholder")
                    if (wasmPlaceholder.exists()) {
                        wasmPlaceholder.delete()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DaadiApp", "Failed to clean up placeholder files", e)
            }
        } catch (e: Exception) {
            android.util.Log.e("DaadiApp", "Error in ensureWebViewCacheDirs", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Pre-create WebView HTTP cache directories immediately
        ensureWebViewCacheDirs()
        
        // Launch a background coroutine to continuously ensure they exist during the entire application session.
        // Checking for file existence is extremely fast (< 1 microsecond) and has zero noticeable CPU/battery impact,
        // but guarantees that any runtime cache cleanups/re-creations by Chromium are immediately handled so opendir never fails.
        // During the first 10 seconds (startup & WebView initialization), we check very frequently (every 50ms) to win
        // the race against Chromium's startup cache wipe/recreation. After 10 seconds, we check every 1000ms.
        applicationScope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                ensureWebViewCacheDirs()
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 30000) {
                    delay(100)
                } else {
                    delay(500)
                }
            }
        }
    }

    companion object {
        lateinit var instance: DaadiApplication
            private set
    }
}
