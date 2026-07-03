package com.example.daadi

import android.app.Application
import com.example.daadi.data.ads.AdManager
import com.example.daadi.data.multiplayer.MultiplayerManager
import com.example.daadi.audio.SoundManager
import com.example.daadi.data.repository.*
import com.example.daadi.data.supabase.SupabaseManager
import kotlinx.coroutines.*

class DaadiApplication : Application() {

    val gameRepository: GameRepository by lazy { GameRepositoryImpl(this) }
    val statsRepository: StatsRepository by lazy { StatsRepositoryImpl(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(this) }
    val soundManager: SoundManager by lazy { SoundManager(this, settingsRepository) }
    val multiplayerManager: MultiplayerManager by lazy { MultiplayerManager(this, soundManager) }
    val supabaseManager: SupabaseManager by lazy { SupabaseManager(this) }
    val adManager: AdManager by lazy { AdManager(this, supabaseManager) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun ensureWebViewCacheDirs() {
        try {
            val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            val jsDir = java.io.File(webViewCacheDir, "js")
            val wasmDir = java.io.File(webViewCacheDir, "wasm")
            
            if (!jsDir.exists()) {
                jsDir.mkdirs()
            }
            val jsPlaceholder = java.io.File(jsDir, ".placeholder")
            if (!jsPlaceholder.exists()) {
                jsPlaceholder.createNewFile()
            }

            if (!wasmDir.exists()) {
                wasmDir.mkdirs()
            }
            val wasmPlaceholder = java.io.File(wasmDir, ".placeholder")
            if (!wasmPlaceholder.exists()) {
                wasmPlaceholder.createNewFile()
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
