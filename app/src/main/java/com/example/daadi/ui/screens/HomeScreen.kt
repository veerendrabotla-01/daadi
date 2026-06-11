package com.example.daadi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import com.example.daadi.model.GameMode
import com.example.daadi.model.GameState
import com.example.daadi.ui.components.SimulatedAdBanner

@Composable
fun HomeScreen(
    savedGameState: GameState?,
    onPlayVsAi: () -> Unit,
    onPlayLocal: () -> Unit,
    onPlayMultiplayer: () -> Unit,
    onResumeGame: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDiscardSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFDF3E3) // Sandstone light cozy background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding() // Safe nav region
                .statusBarsPadding()
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
                    color = Color(0xFF5C2D0A),
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
            }

            // Central menu buttons or Resume cards
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A)),
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

                // PRIMARY ACTION: Play vs Computer
                Button(
                    onClick = onPlayVsAi,
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

                // REAL-TIME MULTIPLAYER (NEW)
                Button(
                    onClick = onPlayMultiplayer,
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
                    onClick = onPlayLocal,
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
            }

            // Simulated Banner Ad at Bottom
            SimulatedAdBanner(modifier = Modifier.fillMaxWidth())
        }
    }
}
