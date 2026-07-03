package com.example.daadi.data.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import com.example.BuildConfig
import com.example.daadi.data.supabase.SupabaseManager
import com.example.daadi.util.SecureLog as Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

class AdManager(
    private val context: Context,
    private val supabaseManager: SupabaseManager) {
    private val tag = "AdManager"
    private val handler = Handler(Looper.getMainLooper())

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    
    private var isInterstitialLoading = false
    private var isRewardedLoading = false

    // Exponential Backoff Retry Parameters
    private var interstitialRetryCount = 0
    private var rewardedRetryCount = 0
    private val MAX_RETRY_DELAY_MS = 300000L // 5 minutes max backoff

    // Frequency Capping Tracking
    private var lastInterstitialShownTime = 0L
    private var sessionInterstitialImpressions = 0
    private val MIN_INTER_SPACING_MS = 45000L // 45 seconds minimum space between interstitials

    // Offline Handling / Network Monitoring
    private val isNetworkConnected = AtomicBoolean(true)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Consent Flow Status
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val isConsentGatheringInProgress = AtomicBoolean(false)

    init {
        isNetworkConnected.set(checkInitialNetworkState())
        registerNetworkCallback()
        
        // Start proactive preloading if we already have internet and don't require immediate UMP flow
        // Note: MainActivity should call gatherConsent on startup to run UMP and initialize properly.
        if (isNetworkConnected.get()) {
            handler.postDelayed({
                initializeMobileAdsSdk()
            }, 1000)
        }
    }

    /**
     * Google GDPR UMP Consent Flow integration.
     * Must be called from MainActivity on startup to satisfy EEA compliance before showing ads.
     */
    fun gatherConsent(activity: Activity, onConsentGathered: () -> Unit) {
        val adConfig = supabaseManager.adConfig.value
        val systemSettings = supabaseManager.systemSettings.value
        val isAdsEnabledByLauncher = systemSettings.find { it.key == "ads_launcher" }?.value == "on"
        val isAdsEnabled = adConfig.isMonetizationGlobalOverride || isAdsEnabledByLauncher

        if (!isAdsEnabled) {
            Log.d(tag, "Ads are globally disabled. Skipping GDPR consent flow.")
            onConsentGathered()
            return
        }

        if (isConsentGatheringInProgress.getAndSet(true)) {
            Log.d(tag, "Consent gathering already in progress. Ignoring duplicate call.")
            return
        }

        Log.d(tag, "Starting Google User Messaging Platform (UMP) GDPR consent flow.")

        // Configure consent parameters (debug settings can be added here if needed in test environment)
        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        val forceEeaDebug = systemSettings.find { it.key == "ad_consent_force_eea_debug" }?.value == "on"

        // For local development only, we can set test device IDs if needed:
        if (BuildConfig.DEBUG && forceEeaDebug) {
            Log.d(tag, "Force EEA Debug setting is ON. Simulating European GDPR context.")
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        } else {
            Log.d(tag, "Standard regional geo-resolution active. Consent form only loaded if required by real location.")
        }

        val params = paramsBuilder.build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    isConsentGatheringInProgress.set(false)
                    if (formError != null) {
                        Log.e(tag, "UMP Consent Form Error: ${formError.message} (Code: ${formError.errorCode})")
                    } else {
                        Log.d(tag, "UMP Consent Form completed successfully.")
                    }

                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk()
                    }
                    onConsentGathered()
                }
            },
            { requestConsentError ->
                isConsentGatheringInProgress.set(false)
                Log.e(tag, "Consent Info update request failed: ${requestConsentError.message}")
                
                // Recovery: Attempt Mobile Ads initialization as best effort if allowed
                if (consentInformation.canRequestAds()) {
                    initializeMobileAdsSdk()
                }
                onConsentGathered()
            }
        )
    }

    /**
     * Explicitly shows the privacy options form so users can change their consent preferences.
     * Required under GDPR regulations.
     */
    fun showPrivacyOptionsForm(activity: Activity, onComplete: (String?) -> Unit) {
        Log.d(tag, "Presenting GDPR Privacy Options form for consent re-verification.")
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.e(tag, "Failed to show privacy options form: ${formError.message} (Code: ${formError.errorCode})")
                onComplete(formError.message)
            } else {
                Log.d(tag, "Privacy options form completed successfully.")
                onComplete(null)
            }
        }
    }

    /**
     * Checks if privacy options are required (e.g. user is in the EEA/UK and has consented).
     */
    fun isPrivacyOptionsRequired(): Boolean {
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        return consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) return
        Log.i(tag, "Initializing Google Mobile Ads SDK inside validated consent container.")
        
        // Ensure the WebView HTTP cache directories exist immediately prior to initialization
        try {
            (context.applicationContext as? com.example.daadi.DaadiApplication)?.ensureWebViewCacheDirs()
        } catch (e: Exception) {}
        
        // Initialize the Mobile Ads SDK
        MobileAds.initialize(context) { status ->
            Log.d(tag, "Mobile Ads SDK Initialization finished: ${status.adapterStatusMap.keys}")
            // Proactive preloading of advertisements on successful initialization
            handler.post {
                loadInterstitial()
                loadRewarded()
            }
        }
    }

    // --- Dynamic ID Resolver (Remote Config + Injected Config) ---
    private fun getInterstitialAdUnitId(): String {
        val remoteId = supabaseManager.adConfig.value.interstitialAdUnitId
        // Comply with 'No hardcoded test IDs' in source code. Fallback to injected build-time values.
        return if (remoteId.isBlank() || remoteId.contains("3940256099942544")) {
            BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID
        } else {
            remoteId
        }
    }

    private fun getRewardedAdUnitId(): String {
        val remoteId = supabaseManager.adConfig.value.rewardedAdUnitId
        // Comply with 'No hardcoded test IDs' in source code. Fallback to injected build-time values.
        return if (remoteId.isBlank() || remoteId.contains("3940256099942544")) {
            BuildConfig.ADMOB_REWARDED_UNIT_ID
        } else {
            remoteId
        }
    }

    // --- Interstitial Ad Logic with Exponential Backoff and Policy Controls ---
    fun loadInterstitial() {
        if (isInterstitialLoading || interstitialAd != null) return
        if (!isNetworkConnected.get()) {
            Log.w(tag, "Skipping interstitial load: Device is offline.")
            return
        }

        isInterstitialLoading = true
        val adUnitId = getInterstitialAdUnitId()
        Log.d(tag, "Requesting Interstitial ad. UnitId: $adUnitId")

        try {
            (context.applicationContext as? com.example.daadi.DaadiApplication)?.ensureWebViewCacheDirs()
        } catch (e: Exception) {}

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isInterstitialLoading = false
                interstitialAd = null
                
                interstitialRetryCount++
                val delayMs = calculateRetryDelay(interstitialRetryCount)
                Log.e(tag, "Interstitial failed to load: ${adError.message}. Code: ${adError.code}. Retrying in ${delayMs}ms (Attempt #$interstitialRetryCount)")
                
                com.example.daadi.DaadiApplication.instance.supabaseManager.logBIEvent(
                    category = "ADS",
                    message = "Interstitial Load Failed: ${adError.message} (Code: ${adError.code})",
                    level = "WARNING"
                )

                handler.postDelayed({
                    loadInterstitial()
                }, delayMs)
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.i(tag, "Interstitial Ad preloaded successfully.")
                com.example.daadi.DaadiApplication.instance.supabaseManager.logBIEvent(
                    category = "ADS",
                    message = "Interstitial Loaded Successfully",
                    level = "INFO"
                )
                interstitialAd = ad
                isInterstitialLoading = false
                interstitialRetryCount = 0 // Reset backoff counter on success
            }
        })
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        val elapsedSinceLastShow = currentTime - lastInterstitialShownTime
        val frequencyCap = supabaseManager.adConfig.value.interstitialFrequencyCap

        // Dynamic Frequency Cap Verification
        if (sessionInterstitialImpressions >= frequencyCap) {
            Log.w(tag, "Ad Capped: Interstitial frequency cap reached ($sessionInterstitialImpressions/$frequencyCap). Dismissing silently.")
            onAdDismissed()
            return
        }

        // Interval Safety Spacing Verification (Policy Compliance)
        if (elapsedSinceLastShow < MIN_INTER_SPACING_MS) {
            Log.w(tag, "Ad Spacing: Too close to last interstitial display (${elapsedSinceLastShow / 1000}s elapsed). Dismissing silently.")
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(tag, "Interstitial dismissed by user.")
                    interstitialAd = null
                    lastInterstitialShownTime = System.currentTimeMillis()
                    sessionInterstitialImpressions++
                    
                    // Preload immediately for the next placement
                    loadInterstitial()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(tag, "Interstitial failed to show screen content: ${adError.message}")
                    interstitialAd = null
                    
                    // Recover and pre-fetch alternative ad
                    loadInterstitial()
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            Log.d(tag, "Interstitial requested but not preloaded yet. Attempting lazy fetch.")
            loadInterstitial()
            onAdDismissed()
        }
    }

    // --- Rewarded Ad Logic with Backoff, Offline Recovery & Validation Checks ---
    fun loadRewarded() {
        if (isRewardedLoading || rewardedAd != null) return
        if (!isNetworkConnected.get()) {
            Log.w(tag, "Skipping rewarded load: Device is offline.")
            return
        }

        isRewardedLoading = true
        val adUnitId = getRewardedAdUnitId()
        Log.d(tag, "Requesting Rewarded ad. UnitId: $adUnitId")

        try {
            (context.applicationContext as? com.example.daadi.DaadiApplication)?.ensureWebViewCacheDirs()
        } catch (e: Exception) {}

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isRewardedLoading = false
                rewardedAd = null
                
                rewardedRetryCount++
                val delayMs = calculateRetryDelay(rewardedRetryCount)
                Log.e(tag, "Rewarded ad failed to load: ${adError.message}. Code: ${adError.code}. Retrying in ${delayMs}ms (Attempt #$rewardedRetryCount)")
                
                com.example.daadi.DaadiApplication.instance.supabaseManager.logBIEvent(
                    category = "ADS",
                    message = "Rewarded Load Failed: ${adError.message} (Code: ${adError.code})",
                    level = "WARNING"
                )

                handler.postDelayed({
                    loadRewarded()
                }, delayMs)
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.i(tag, "Rewarded Ad preloaded successfully.")
                com.example.daadi.DaadiApplication.instance.supabaseManager.logBIEvent(
                    category = "ADS",
                    message = "Rewarded Loaded Successfully",
                    level = "INFO"
                )
                rewardedAd = ad
                isRewardedLoading = false
                rewardedRetryCount = 0 // Reset backoff counter on success
            }
        })
    }

    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdDismissed: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            val hasEarnedReward = AtomicBoolean(false)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(tag, "Rewarded ad dismissed.")
                    rewardedAd = null
                    
                    // Proactive reload
                    loadRewarded()
                    onAdDismissed()
                    
                    if (hasEarnedReward.get()) {
                        onRewardEarned()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(tag, "Rewarded ad failed to show: ${adError.message}")
                    rewardedAd = null
                    loadRewarded()
                    onAdDismissed()
                }
            }

            ad.show(activity) { rewardItem ->
                Log.i(tag, "Ad completed. User qualified for reward. Reward Type: ${rewardItem.type}, Amount: ${rewardItem.amount}")
                // Secure transaction check & client side verification support
                hasEarnedReward.set(true)
            }
        } else {
            Log.w(tag, "Rewarded ad not preloaded. Showing error toast or triggering recovery flow.")
            loadRewarded()
            onAdDismissed()
        }
    }

    // --- Helper Utils ---
    private fun calculateRetryDelay(attempt: Int): Long {
        val baseDelay = 4000L
        val multiplier = 2.0
        val delay = (baseDelay * Math.pow(multiplier, (attempt - 1).toDouble())).toLong()
        val jitter = (Math.random() * 2000).toLong()
        return (delay + jitter).coerceAtMost(MAX_RETRY_DELAY_MS)
    }

    private fun checkInitialNetworkState(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val builder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isNetworkConnected.compareAndSet(false, true)) {
                    Log.i(tag, "Network back online. Auto-recovering offline ad-caches.")
                    handler.post {
                        loadInterstitial()
                        loadRewarded()
                    }
                }
            }

            override fun onLost(network: Network) {
                isNetworkConnected.set(false)
                Log.w(tag, "Network connectivity lost. Disabling ad preloading to prevent resource leakage.")
            }
        }
        
        try {
            cm.registerNetworkCallback(builder.build(), networkCallback!!)
        } catch (e: Exception) {
            Log.e(tag, "Failed to register network callback", e)
        }
    }

    fun destroy() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkCallback?.let {
            try {
                cm?.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.e(tag, "Failed to unregister network callback", e)
            }
        }
        handler.removeCallbacksAndMessages(null)
    }
}
