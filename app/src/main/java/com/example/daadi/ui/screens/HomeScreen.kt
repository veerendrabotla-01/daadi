package com.example.daadi.ui.screens

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.daadi.model.GameMode
import com.example.daadi.model.GameState
import com.example.daadi.ui.components.SimulatedAdBanner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(
    savedGameState: GameState?,
    supabaseManager: SupabaseManager,
    onPlayVsAi: (com.example.daadi.model.RuleSet) -> Unit,
    onPlayLocal: (com.example.daadi.model.RuleSet) -> Unit,
    onPlayMultiplayer: (com.example.daadi.model.RuleSet) -> Unit,
    onResumeGame: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignInClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onDiscardSave: () -> Unit
) {
    val systemSettings by supabaseManager.systemSettings.collectAsStateWithLifecycle()
    val isMaintenanceMode = systemSettings.find { it.key == "maintenance_mode" }?.value == "on"
    val currentUser by supabaseManager.currentUser.collectAsStateWithLifecycle()
    val isAdmin = supabaseManager.hasPermission("admin_dashboard")

    if (isMaintenanceMode && !isAdmin) {
        MaintenanceOverlay(onSignInClick)
        return
    }

    val announcement = systemSettings.find { it.key == "announcement_text" }?.value ?: ""

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { },
                actions = {
                    IconButton(
                        onClick = onSignInClick,
                        modifier = Modifier.testTag("home_signin_button")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.AccountCircle,
                            contentDescription = "Profile / Sign In",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
                // Header content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 42.dp)
                ) {
                    Text(
                        "DAADI",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 5.sp
                    )
                    Text(
                        "India's Traditional Board Game",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8B5E3C),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (announcement.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFC75D27).copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFC75D27), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    announcement,
                                    fontSize = 11.sp,
                                    color = Color(0xFFC75D27),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

            // Central menu buttons or Resume cards
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                // RULE VARIETY TOGGLE (Standard 9-Piece vs Advanced 12-Piece)
                var activeRuleSet by remember { mutableStateOf(com.example.daadi.model.RuleSet.NINE_MENS_MORRIS) }

                // RESUME CARD (IF SAVE STATE EXISTS)
                if (savedGameState != null && savedGameState.winner == null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAE8CE)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .testTag("resume_game_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "MATCH IN PROGRESS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (savedGameState.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                                    "Online Real-Time Multiplayer"
                                } else if (savedGameState.gameMode == GameMode.VS_AI) {
                                    "Vs Computer (${savedGameState.aiDifficulty.name})"
                                } else {
                                    "2-Player Pass & Play"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF5C2D0A)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                              ) {
                                // Resume Button
                                Button(
                                    onClick = onResumeGame,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF5C2D0A),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("resume_match_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume", color = Color.White, fontSize = 14.sp)
                                }
                                // New Game Discard button
                                OutlinedButton(
                                    onClick = onDiscardSave,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("discard_match_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFC62828))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", color = Color(0xFFC62828), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                // AUTH SECTION (Login or User Profile)
                val userState = supabaseManager.currentUser.collectAsStateWithLifecycle()
                val currentUser = userState.value
                
                if (currentUser == null) {
                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 12.dp).testTag("home_signin_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIGN IN TO PLAY ONLINE", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.dp, Color(0xFF5C2D0A).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(Color(0xFF5C2D0A).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(currentUser.username.take(1).uppercase(), fontWeight = FontWeight.Black, color = Color(0xFF5C2D0A))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(currentUser.username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(if (currentUser.role == "admin") "System Admin" else "Active Player", fontSize = 11.sp, color = Color.Gray)
                            }
                            if (currentUser.role == "admin") {
                                IconButton(onClick = onSignInClick /* This routes to admin dashboard in MainActivity for admins */) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = Color(0xFFC75D27))
                                }
                            }
                            IconButton(onClick = { supabaseManager.logout() }) {
                                Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = Color.LightGray)
                            }
                        }
                    }
                }

                // PRIMARY ACTION: Play vs Computer
                Button(
                    onClick = { onPlayVsAi(activeRuleSet) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(bottom = 12.dp)
                        .testTag("play_vs_ai_menu_button")
                ) {
                    Text(
                        "CHALLENGE COMPUTER",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).border(1.dp, Color(0xFFD4A55A).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RULES:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5E3C), modifier = Modifier.width(50.dp))
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeRuleSet == com.example.daadi.model.RuleSet.NINE_MENS_MORRIS) Color(0xFF5C2D0A) else Color(0xFFE5A93B).copy(alpha = 0.1f))
                                    .clickable { activeRuleSet = com.example.daadi.model.RuleSet.NINE_MENS_MORRIS }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("STANDARD (9)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (activeRuleSet == com.example.daadi.model.RuleSet.NINE_MENS_MORRIS) Color.White else Color(0xFF8B5E3C))
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeRuleSet == com.example.daadi.model.RuleSet.TWELVE_MENS_MORRIS) Color(0xFFC75D27) else Color(0xFFE5A93B).copy(alpha = 0.1f))
                                    .clickable { activeRuleSet = com.example.daadi.model.RuleSet.TWELVE_MENS_MORRIS }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("ADVANCED (12)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (activeRuleSet == com.example.daadi.model.RuleSet.TWELVE_MENS_MORRIS) Color.White else Color(0xFF8B5E3C))
                            }
                        }
                    }
                }

                // REAL-TIME MULTIPLAYER (NEW)
                Button(
                    onClick = { onPlayMultiplayer(activeRuleSet) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)), // Terracotta crimson
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(bottom = 12.dp)
                        .testTag("play_multiplayer_menu_button")
                ) {
                    Text(
                        "DAADI REAL-TIME MULTIPLAYER",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // SECONDARY ACTION: Pass & Play
                Button(
                    onClick = { onPlayLocal(activeRuleSet) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4A55A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(bottom = 12.dp)
                        .testTag("play_local_menu_button")
                ) {
                    Text(
                        "PASS & PLAY (LOCAL COOP)",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1C0A00),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats & Settings Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Statistics Button
                    Button(
                        onClick = onStatsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFBFA)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("stats_menu_button")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Stats", tint = Color(0xFFE5A93B))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Statistics", color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)
                    }

                    // Settings Button
                    Button(
                        onClick = onSettingsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFBFA)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_menu_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF5C2D0A))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Settings", color = Color(0xFF5C2D0A), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Feedback Button (Full Width)
                OutlinedButton(
                    onClick = onFeedbackClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("home_feedback_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5C2D0A).copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, tint = Color(0xFF8B5E3C))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Feedback / Bug Report", color = Color(0xFF8B5E3C), fontWeight = FontWeight.Medium)
                }
            }

            // Simulated Banner Ad area with fixed height to prevent layout shifts
            val isAdsOn = systemSettings.find { it.key == "ads_launcher" }?.value == "on"
            Box(modifier = Modifier.fillMaxWidth().height(54.dp)) {
                if (isAdsOn) {
                    SimulatedAdBanner(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun MaintenanceOverlay(onSignInClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFDF3E3)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint = Color(0xFFC75D27),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Under Maintenance",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF5C2D0A),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "We're currently polishing the board! The game will be back online shortly. Please check back later.",
                textAlign = TextAlign.Center,
                color = Color(0xFF8B5E3C),
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(onClick = onSignInClick) {
                Text("Admin Login", color = Color(0xFF5C2D0A))
            }
        }
    }
}
