package com.example.daadi.ui.screens



import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.model.AIDifficulty
import com.example.daadi.model.AppSettings
import com.example.daadi.model.PlayerStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    stats: PlayerStats,
    onResetStats: () -> Unit,
    onBack: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    val winRate = if (stats.totalGamesPlayed > 0) {
        (stats.totalWins * 100f / stats.totalGamesPlayed).toInt()
    } else {
        0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Honor & Statistics", fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    if (stats.totalGamesPlayed > 0) {
                        IconButton(onClick = { showResetDialog = true }, modifier = Modifier.testTag("reset_stats_button")) {
                            Icon(Icons.Default.Delete, contentDescription = "Reset All Stats", tint = Color(0xFFC62828))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A),
                    navigationIconContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // General Stats Hub Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("STRATEGY PERFORMANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE5A93B), letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("$winRate%", fontSize = 54.sp, fontWeight = FontWeight.Black, color = Color(0xFF5C2D0A))
                    Text("Overall Win Rate", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(label = "Played", value = stats.totalGamesPlayed.toString())
                        StatItem(label = "Victory", value = stats.totalWins.toString(), color = Color(0xFF2E7D32))
                        StatItem(label = "Defeat", value = stats.totalLosses.toString(), color = Color(0xFFC62828))
                    }
                }
            }

            Text(
                "BOT DIFFICULTY CHALLENGES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5E3C),
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            // AI break-downs
            StatsBreakdownRow(title = "Sanyasi (Easy AI)", wins = stats.winsVsEasyAI, losses = stats.lossesVsEasyAI)
            StatsBreakdownRow(title = "Mantri (Medium AI)", wins = stats.winsVsMediumAI, losses = stats.lossesVsMediumAI)
            StatsBreakdownRow(title = "Chanakya (Hard AI)", wins = stats.winsVsHardAI, losses = stats.lossesVsHardAI)

            Spacer(modifier = Modifier.height(8.dp))

            // Pass & play local stats count
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pass & Play Local Cooperatives", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)
                    Text("${stats.passAndPlayGames}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFD4A55A))
                }
            }

            // Empty state helper text
            if (stats.totalGamesPlayed == 0) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No games registered yet! Launch a match from main dashboard to accumulate honors.",
                    textAlign = TextAlign.Center,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Dialog for stats wipes
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Reset Honor Statistics?") },
                    text = { Text("This will permanently wipe all record counts and set win-rates back to zero. This cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showResetDialog = false
                                onResetStats()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) { Text("Yes, Reset") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
                    },
                    containerColor = Color(0xFFFFFBF4)
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color = Color(0xFF5C2D0A)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun StatsBreakdownRow(title: String, wins: Int, losses: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Wins: $wins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Text(text = "Losses: $losses", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            }
        }
    }
}

/* ----------------- SETTINGS SCREEN ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    isAdmin: Boolean = false,
    onSettingsChanged: (AppSettings) -> Unit,
    onAdminClick: () -> Unit,
    onDeleteAccount: () -> Unit,
    onExportData: () -> Unit,
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val legalUrl = "https://daadi-legal.vercel.app"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Custom", fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A),
                    navigationIconContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("AUDIO & FEEDBACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5E3C), letterSpacing = 1.sp)

            // Dynamic synthetic sound effects toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Traditional Audio Beeps", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)
                        Text("Synthesized beep-ack for placements, mills, and wins", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { onSettingsChanged(settings.copy(soundEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF5C2D0A), checkedTrackColor = Color(0xFFD4A55A)),
                        modifier = Modifier.testTag("sound_toggle")
                    )
                }
            }

            // Background Music toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Music", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)
                        Text("Play ambient traditional music during the game", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = settings.musicEnabled,
                        onCheckedChange = { onSettingsChanged(settings.copy(musicEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF5C2D0A), checkedTrackColor = Color(0xFFD4A55A)),
                        modifier = Modifier.testTag("music_toggle")
                    )
                }
            }

            // Countdown Sound effects toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Turn Timer Warnings", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)
                        Text("Soft audio feedback during the final 3 seconds of your turn", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = settings.countdownSoundEnabled,
                        onCheckedChange = { onSettingsChanged(settings.copy(countdownSoundEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF5C2D0A), checkedTrackColor = Color(0xFFD4A55A)),
                        modifier = Modifier.testTag("countdown_sound_toggle")
                    )
                }
            }

            // Haptic/Vibration toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tactile Vibration", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)
                        Text("Gentle haptic feedback during placement and mills", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { onSettingsChanged(settings.copy(vibrationEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF5C2D0A), checkedTrackColor = Color(0xFFD4A55A)),
                        modifier = Modifier.testTag("vibration_toggle")
                    )
                }
            }

            Text("GAMEPLAY & VISUALS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5E3C), letterSpacing = 1.sp)

            // Last Move Highlight toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Highlight Last Move", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)
                        Text("Show a gold aura around the most recent move", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = settings.highlightLastMove,
                        onCheckedChange = { onSettingsChanged(settings.copy(highlightLastMove = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF5C2D0A), checkedTrackColor = Color(0xFFD4A55A)),
                        modifier = Modifier.testTag("highlight_move_toggle")
                    )
                }
            }

            // Fast Animations toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Blitz Mode Styles", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Reduce visual delays for faster board transitions", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = settings.fastAnimations,
                        onCheckedChange = { onSettingsChanged(settings.copy(fastAnimations = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF5C2D0A), checkedTrackColor = Color(0xFFD4A55A)),
                        modifier = Modifier.testTag("fast_anims_toggle")
                    )
                }
            }

            // Show Rules on Match Start toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Rules on Match Start", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Automatically popup the How-to-Play guide before every new game", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = settings.showRulesOnStart,
                        onCheckedChange = { onSettingsChanged(settings.copy(showRulesOnStart = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("rules_toggle")
                    )
                }
            }

            // Show Latest Activity toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Latest Activity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Display a live match notation log during gameplay", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = settings.showLatestActivity,
                        onCheckedChange = { onSettingsChanged(settings.copy(showLatestActivity = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("activity_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("BOARD COSMETIC SKINS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5E3C), letterSpacing = 1.sp)

            // Select Skin 1 (Classic Wood)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.selectedBoardTheme == "classic_wood") Color(0xFFFFF7EA) else Color(0xFFFFFBFA)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (settings.selectedBoardTheme == "classic_wood") 2.dp else 1.dp,
                        color = if (settings.selectedBoardTheme == "classic_wood") Color(0xFF5C2D0A) else Color.LightGray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSettingsChanged(settings.copy(selectedBoardTheme = "classic_wood")) }
                    .testTag("theme_wood_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEAA65D)) // Sandalwood color preview
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Royal Sandalwood Skin", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Traditional yellow-gold sandalwood with dark lines", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // Select Skin 2 (Dark Obsidian)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.selectedBoardTheme == "dark_stone") Color(0xFFFFF7EA) else Color(0xFFFFFBFA)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (settings.selectedBoardTheme == "dark_stone") 2.dp else 1.dp,
                        color = if (settings.selectedBoardTheme == "dark_stone") Color(0xFF5C2D0A) else Color.LightGray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSettingsChanged(settings.copy(selectedBoardTheme = "dark_stone")) }
                    .testTag("theme_stone_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF7F8C8D)) // Slate stone preview
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Dark Stone Slate Skin", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Steel obsidian slate board with dark graphite lines", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(18.dp))

                Text("CLOUD SERVICES & ROLES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5E3C), letterSpacing = 1.sp)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE5A93B).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Cloud Operations Portal", 
                            style = MaterialTheme.typography.titleMedium, 
                            color = Color(0xFF5C2D0A), 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Manage registered game players, historic matches, publish announcements, and adjust real-time strategic settings.", 
                            fontSize = 11.sp, 
                            color = Color.Gray,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAdminClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("open_admin_portal_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("OPEN ADMIN PANEL", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("LEGAL & COMPLIANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5E3C), letterSpacing = 1.sp)
            
            // GDPR Ad Consent Settings Card
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            val app = context.applicationContext as? com.example.daadi.DaadiApplication
            if (app != null && activity != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ad Consent & Privacy", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)
                            Text("Manage personalized advertising preferences and GDPR choices", fontSize = 11.sp, color = Color.Gray)
                        }
                        TextButton(
                            onClick = {
                                app.adManager.showPrivacyOptionsForm(activity) { error ->
                                    if (error != null) {
                                        android.widget.Toast.makeText(context, "Consent form currently unavailable: $error", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Preferences updated successfully.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("UPDATE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // GDPR Data Export
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Download My Data", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)
                        Text("Request a copy of your game history and profile data", fontSize = 11.sp, color = Color.Gray)
                    }
                    TextButton(onClick = onExportData) {
                        Text("REQUEST", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // GDPR Account Deletion
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Delete Account", style = MaterialTheme.typography.titleMedium, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        Text("Permanently remove all data and identity", fontSize = 11.sp, color = Color.Gray)
                    }
                    TextButton(onClick = onDeleteAccount) {
                        Text("DELETE", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Policies & Digital Safety", 
                        style = MaterialTheme.typography.titleSmall, 
                        color = Color.DarkGray, 
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Review our commitment to DPDP Act (India) and Fair Play guidelines via the official web portal.", 
                        fontSize = 11.sp, 
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { uriHandler.openUri(legalUrl) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Privacy Policy", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { uriHandler.openUri(legalUrl) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Terms of Use", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Version Display
            Text(
                "Daadi Game Version ${com.example.BuildConfig.VERSION_NAME} (Production)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
    }
}
