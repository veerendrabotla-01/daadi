package com.example.daadi.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Aesthetic Simulated banner ads.
 * Displayed at the bottom of the screen on Home and Between games in non-gameplay moments.
 */
@Composable
fun SimulatedAdBanner(modifier: Modifier = Modifier) {
    var closed by remember { mutableStateOf(false) }
    if (closed) return

    val adCampaigns = listOf(
        Pair("Spices of India Coffee - Pure Sandalwood Roast", "Order now and get 25% Off! code: DAADI25"),
        Pair("Play Saffron Carrom Match Online", "Compete with 2 million players worldwide for free!"),
        Pair("Royal Taj Hotels & Resorts", "Experience the ultimate luxury of Rajasthan. Book rooms today.")
    )
    val randomCampaign = remember { adCampaigns.random() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Color(0xFFFFF9E6))
            .border(1.dp, Color(0xFFE5A93B))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE5A93B), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Ad",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    randomCampaign.first,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C2D0A),
                    maxLines = 1
                )
            }
            Text(
                randomCampaign.second,
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }

        IconButton(
            onClick = { closed = true },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close Ad",
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Dialog overlay that offers the user a Rewarded Ad to get a benefit (Undo Move).
 */
@Composable
fun RewardedAdOfferDialog(
    onDismiss: () -> Unit,
    onRewardEarned: () -> Unit
) {
    var isWatching by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(3) }

    LaunchedEffect(isWatching) {
        if (isWatching) {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
            }
            onRewardEarned()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isWatching) onDismiss() },
        confirmButton = {
            if (!isWatching) {
                Button(
                    onClick = { isWatching = true },
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
            }
        },
        dismissButton = {
            if (!isWatching) {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("skip_ad_button")) {
                    Text("No, Thanks", color = Color.Gray)
                }
            }
        },
        title = {
            Text(
                text = if (isWatching) "Loading Sponsor..." else "Undo Last Defeat?",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF5C2D0A)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                if (isWatching) {
                    CircularProgressIndicator(color = Color(0xFFE5A93B))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Granting your Undo benefit in $secondsLeft seconds...",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Gold Reward",
                        tint = Color(0xFFE5A93B),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Watch a quick 3-second sponsor message to undo your mistake and reclaim your position on the board!",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFFFFFBF4)
    )
}
