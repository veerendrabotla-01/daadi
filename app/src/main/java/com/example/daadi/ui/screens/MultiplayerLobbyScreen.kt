package com.example.daadi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.multiplayer.MultiplayerManager
import com.example.daadi.data.multiplayer.MultiplayerStatus
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerLobbyScreen(
    multiplayerManager: MultiplayerManager,
    onBack: () -> Unit,
    onGameStarted: () -> Unit
) {
    val status by multiplayerManager.status.collectAsStateWithLifecycle()
    val roomCode by multiplayerManager.roomCode.collectAsStateWithLifecycle()
    val isHost by multiplayerManager.isHost.collectAsStateWithLifecycle()
    val errorMsg by multiplayerManager.errorMessage.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var manualRoomCodeInput by remember { mutableStateOf("") }

    // Automatically navigate when pairing completes successfully
    LaunchedEffect(status) {
        if (status == MultiplayerStatus.CONNECTED) {
            onGameStarted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Daadi Matchmaker", 
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        multiplayerManager.disconnect()
                        onBack()
                    }, modifier = Modifier.testTag("multiplayer_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A)
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Connection Status Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Server Status:", fontWeight = FontWeight.Bold, color = Color(0xFF8B5E3C))
                    
                    val (statusLabel, statusColor) = when (status) {
                        MultiplayerStatus.DISCONNECTED -> "OFFLINE / READY" to Color(0xFF8B5E3C)
                        MultiplayerStatus.CONNECTING -> "CONNECTING..." to Color(0xFFE65100)
                        MultiplayerStatus.MATCHMAKING -> "MATCHMAKING..." to Color(0xFFC75D27)
                        MultiplayerStatus.LOBBY_WAITING -> "WAITING IN LOBBY" to Color(0xFFE65100)
                        MultiplayerStatus.CONNECTED -> "CONNECTED & PAIRED" to Color(0xFF2E7D32)
                        MultiplayerStatus.ERROR -> "CONNECTION FAILED" to Color(0xFFC62828)
                    }

                    Text(
                        text = statusLabel,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Error Display Banner
            if (!errorMsg.isNullOrEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFC62828),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Active Session Card or Matching Cards
            if (status == MultiplayerStatus.CONNECTING || status == MultiplayerStatus.MATCHMAKING) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF5C2D0A))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (status == MultiplayerStatus.CONNECTING) {
                                "Connecting to Daadi Multiplay Server..."
                            } else {
                                "Searching for a random opponent online..."
                            },
                            textAlign = TextAlign.Center,
                            color = Color(0xFF5C2D0A),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { multiplayerManager.disconnect() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            } else if (status == MultiplayerStatus.LOBBY_WAITING && isHost) {
                // Hosting View
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "YOUR PRIVATE ROOM CODE",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFE5A93B),
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = roomCode,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF5C2D0A),
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Share this 6-digit code with your friend to play!",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = Color(0xFF8B5E3C)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        CircularProgressIndicator(color = Color(0xFFC75D27), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Awaiting Opponent to Join...",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC75D27)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = { multiplayerManager.disconnect() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                        ) {
                            Text("Cancel Hosting")
                        }
                    }
                }
            } else {
                // Main Lobby Choices View
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "ONLINE GAME MODES",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5C2D0A),
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                        
                        // Action 1: Host Room
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                multiplayerManager.hostRoom()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("host_room_button")
                        ) {
                            Text("CREATE PRIVATE ROOM", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Divider(color = Color(0xFFE5A93B).copy(alpha = 0.3f), thickness = 1.dp)

                        // Action 2: Join Room Input & Button
                        Text(
                            "JOIN PRIVATE ROOM",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5C2D0A),
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )

                        OutlinedTextField(
                            value = manualRoomCodeInput,
                            onValueChange = { input -> 
                                if (input.length <= 6 && input.all { it.isDigit() }) {
                                    manualRoomCodeInput = input
                                }
                            },
                            label = { Text("Enter 6-Digit Code") },
                            placeholder = { Text("e.g. 543981") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                autoCorrect = false,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5C2D0A),
                                unfocusedBorderColor = Color(0xFFE5A93B),
                                focusedLabelColor = Color(0xFF5C2D0A),
                                unfocusedLabelColor = Color(0xFF8B5E3C)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("join_room_input")
                        )

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (manualRoomCodeInput.length == 6) {
                                    multiplayerManager.joinRoom(manualRoomCodeInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4A55A)),
                            shape = RoundedCornerShape(8.dp),
                            enabled = manualRoomCodeInput.length == 6,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("join_room_button")
                        ) {
                            Text("JOIN ROOM & PLAY", fontWeight = FontWeight.Bold, color = Color(0xFF1C0A00))
                        }

                        Divider(color = Color(0xFFE5A93B).copy(alpha = 0.3f), thickness = 1.dp)

                        // Action 3: Quick Match Making
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                multiplayerManager.quickMatch()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("quick_match_button")
                        ) {
                            Text("QUICK MATCH / FIND OPPONENT", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Indian Folk Art Decorative Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFE5A93B).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFF7EA))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "🌸 PRO TIP 🌸",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC75D27),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Daadi real-time matches enforce a strict 30-second timer. If your timer runs out, a strategic random move is automatically triggered so the battlefield never hangs!",
                        fontSize = 12.sp,
                        color = Color(0xFF8B5E3C),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
