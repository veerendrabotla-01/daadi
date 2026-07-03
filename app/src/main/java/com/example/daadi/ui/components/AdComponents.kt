package com.example.daadi.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.daadi.DaadiApplication
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    
    // Use remember(adUnitId) to ensure a fresh AdView if the unit ID changes
    key(adUnitId) {
        val adView = remember {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
            }
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_DESTROY -> adView.destroy()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }

        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { 
                adView.apply {
                    try {
                        (context.applicationContext as? DaadiApplication)?.ensureWebViewCacheDirs()
                    } catch (e: Exception) {}
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}

/**
 * Top level composable to show an Ad Banner based on configuration.
 */
@Composable
fun AdaptiveAdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as DaadiApplication
    val adConfig by app.supabaseManager.adConfig.collectAsState()
    val systemSettings by app.supabaseManager.systemSettings.collectAsState()
    
    val isAdsEnabledByLauncher = systemSettings.find { it.key == "ads_launcher" }?.value == "on"
    
    if (adConfig.isMonetizationGlobalOverride || isAdsEnabledByLauncher) {
        BannerAdView(modifier = modifier, adUnitId = adConfig.bannerAdUnitId)
    }
}

/**
 * Animated simulated banner used during development or as a fallback.
 */
@Composable
fun SimulatedAdBanner(modifier: Modifier = Modifier) {
    AdaptiveAdBanner(modifier = modifier)
}

/**
 * Dialog overlay that offers the user a Rewarded Ad to get a benefit (Undo Move).
 */
@Composable
fun RewardedAdOfferDialog(
    onDismiss: () -> Unit,
    onRewardEarned: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as DaadiApplication
    val adManager = app.adManager

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (activity != null) {
                        adManager.showRewarded(
                            activity = activity,
                            onRewardEarned = {
                                app.supabaseManager.incrementAdImpressions()
                                onRewardEarned()
                            },
                            onAdDismissed = {
                                onDismiss()
                            }
                        )
                    } else {
                        onRewardEarned()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE5A93B),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("watch_ad_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Watch Ad & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("skip_ad_button")) {
                Text("No, Thanks", color = Color.Gray)
            }
        },
        title = {
            Text(
                text = "Undo Last Defeat?",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF5C2D0A)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Gold Reward",
                    tint = Color(0xFFE5A93B),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Watch a short sponsored video to undo your mistake and reclaim your position on the board!",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFFFFFBF4)
    )
}
