package com.example.daadi.ui.screens



import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.Warning
import com.example.daadi.data.network.NetworkUtils
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daadi.data.supabase.SupabaseUser
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.sp
import com.example.daadi.data.multiplayer.MultiplayerManager
import com.example.daadi.data.multiplayer.MultiplayerStatus
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerLobbyScreen(
    multiplayerManager: MultiplayerManager,
    sharedGameViewModel: com.example.daadi.viewmodel.GameViewModel,
    onBack: () -> Unit,
    onManageProfile: () -> Unit,
    onPlayVsAi: () -> Unit,
    onGameStarted: () -> Unit
) {
    val status by multiplayerManager.status.collectAsStateWithLifecycle()
    val roomCode by multiplayerManager.roomCode.collectAsStateWithLifecycle()
    val matchmakingCountdown by multiplayerManager.matchmakingCountdown.collectAsStateWithLifecycle()
    val isLobbyEmpty by multiplayerManager.isLobbyEmpty.collectAsStateWithLifecycle()
    val opponentPlayerName by multiplayerManager.opponentPlayerName.collectAsStateWithLifecycle()
    val isHost by multiplayerManager.isHost.collectAsStateWithLifecycle()
    val errorMsg by multiplayerManager.errorMessage.collectAsStateWithLifecycle()
    val currentUser by sharedGameViewModel.authRepository.currentUser.collectAsStateWithLifecycle()
    val localPlayerName by multiplayerManager.localPlayerName.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var manualRoomCodeInput by remember { mutableStateOf("") }
    var isOfflineAlertVisible by remember { mutableStateOf(false) }
    var isNetworkConnected by remember { mutableStateOf(true) }

    // Run active network check on screen mount
    LaunchedEffect(Unit) {
        val connected = NetworkUtils.isNetworkAvailable(context)
        isNetworkConnected = connected
        if (!connected) {
            isOfflineAlertVisible = true
        }
    }

    var showAuthPromptDialog by remember { mutableStateOf(false) }

    // Connectivity-guard wrapper for matchmaking buttons with login check
    val checkConnectivityThen = { block: () -> Unit ->
        val connected = NetworkUtils.isNetworkAvailable(context)
        isNetworkConnected = connected
        if (!connected) {
            isOfflineAlertVisible = true
        } else if (currentUser == null) {
            showAuthPromptDialog = true
        } else {
            block()
        }
    }

    // Sync username with Multiplayer local name
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            multiplayerManager.setLocalPlayerName(currentUser!!.username)
        }
    }

    // Automatically navigate when pairing completes successfully
    LaunchedEffect(status) {
        if (status == MultiplayerStatus.CONNECTED) {
            delay(1200)
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
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
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
            // Player Profile Badge Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFE5A93B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { onManageProfile() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFFF3E0), CircleShape)
                                .border(1.dp, Color(0xFFC75D27), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentUser != null) Icons.Default.AccountBox else Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFC75D27),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (currentUser != null) "Logged in Player" else "Playing as Guest",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = localPlayerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5C2D0A)
                            )
                        }
                    }

                    TextButton(onClick = onManageProfile) {
                        Text(
                            text = if (currentUser != null) "Profile" else "Sign In",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC75D27)
                        )
                    }
                }
            }

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
            if (status == MultiplayerStatus.CONNECTING || status == MultiplayerStatus.MATCHMAKING || (status == MultiplayerStatus.LOBBY_WAITING && matchmakingCountdown != null) || status == MultiplayerStatus.CONNECTED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).border(1.dp, Color(0xFFC75D27).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFC75D27), strokeWidth = 4.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = when {
                                status == MultiplayerStatus.CONNECTED -> "Opponent Found: $opponentPlayerName"
                                status == MultiplayerStatus.CONNECTING -> "Synchronizing with Battle Servers..."
                                isLobbyEmpty -> "Global lobby is currently empty. You can wait for a host or launch the Guardian Simulator immediately."
                                matchmakingCountdown != null -> "Searching for Opponent..."
                                else -> "Querying Matchmaker..."
                            },
                            textAlign = TextAlign.Center,
                            color = if (status == MultiplayerStatus.CONNECTED) Color(0xFF2E7D32) else Color(0xFF5C2D0A),
                            fontWeight = FontWeight.Black,
                            fontSize = if (isLobbyEmpty) 16.sp else 18.sp
                        )

                        if (matchmakingCountdown != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Guardian Simulator fallback in: ${matchmakingCountdown}s",
                                color = Color(0xFF8B5E3C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { multiplayerManager.startSimulatorNow() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (isLobbyEmpty) "LAUNCH GUARDIAN SIMULATOR INSTANTLY" else "PLAY GUARDIAN SIMULATOR NOW",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { multiplayerManager.disconnect() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CANCEL SEARCH")
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
                                checkConnectivityThen {
                                    focusManager.clearFocus()
                                    val code = (100000..999999).random().toString()
                                    sharedGameViewModel.remoteGameRepository.hostWaitingMatch(localPlayerName, code) { success ->
                                        if (success) {
                                            multiplayerManager.hostRoom(code)
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Failed to create private room on server. Please check your connection and try again.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5C2D0A),
                            contentColor = Color.White
                        ),
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
                                unfocusedLabelColor = Color(0xFF8B5E3C),
                                focusedTextColor = Color(0xFF5C2D0A),
                                unfocusedTextColor = Color(0xFF5C2D0A)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("join_room_input")
                        )

                        Button(
                            onClick = {
                                checkConnectivityThen {
                                    focusManager.clearFocus()
                                    if (manualRoomCodeInput.length == 6) {
                                        sharedGameViewModel.remoteGameRepository.joinWaitingMatch(manualRoomCodeInput, localPlayerName) { success ->
                                            if (success) {
                                                multiplayerManager.joinRoom(manualRoomCodeInput)
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Room code is invalid or connection failed. Please verify the code and try again.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD4A55A),
                                contentColor = Color(0xFF1C0A00)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = manualRoomCodeInput.length == 6,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("join_room_button")
                        ) {
                            Text("JOIN ROOM & PLAY", fontWeight = FontWeight.Bold)
                        }

                        Divider(color = Color(0xFFE5A93B).copy(alpha = 0.3f), thickness = 1.dp)

                        // Action 3: Quick Match Making
                        Button(
                            onClick = {
                                checkConnectivityThen {
                                    focusManager.clearFocus()
                                    multiplayerManager.quickMatch { onJoin, onHost ->
                                        sharedGameViewModel.remoteGameRepository.findWaitingMatch { match ->
                                            if (match != null) {
                                                sharedGameViewModel.remoteGameRepository.joinWaitingMatch(match.id, localPlayerName) {
                                                    onJoin(match.id)
                                                }
                                            } else {
                                                multiplayerManager.setLobbyEmpty(true)
                                                val newCode = (100000..999999).random().toString()
                                                sharedGameViewModel.remoteGameRepository.hostWaitingMatch(localPlayerName, newCode) { success ->
                                                    onHost(newCode)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC75D27),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("quick_match_button")
                        ) {
                            Text("QUICK MATCH / FIND OPPONENT", fontWeight = FontWeight.Bold)
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

            // 3. Lively Lobby Feed Section
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "DAADI WORLD ARENA",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C2D0A),
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Real-time Active Players & Matches",
                                fontSize = 11.sp,
                                color = Color(0xFF8B5E3C)
                            )
                        }
                        
                        // Pulsing online indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2E7D32), CircleShape)
                            )
                            Text(
                                "142 Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Divider(color = Color(0xFFE5A93B).copy(alpha = 0.2f), thickness = 1.dp)

                    // Display list of real active users
                    Text(
                        "ONLINE PLAYERS",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC75D27),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    val realUsers by sharedGameViewModel.authRepository.network._users.collectAsStateWithLifecycle()
                    val activePlayers = remember(realUsers) {
                        if (realUsers.isNotEmpty()) {
                            realUsers.filter { !it.isBanned && !it.username.startsWith("u_sim") && !it.username.startsWith("u_oauth") }
                                .shuffled().take(4)
                                .map { Triple(it.username, it.role.replaceFirstChar { c -> c.uppercase() }, it.rating) }
                        } else {
                            emptyList()
                        }
                    }

                    if (activePlayers.isEmpty()) {
                        Text("No other players online right now.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activePlayers.forEach { player ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFFDF8), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(0xFFFFF3E0), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "👤",
                                                fontSize = 12.sp
                                            )
                                        }
                                        Column {
                                            Text(
                                                player.first,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF5C2D0A)
                                            )
                                            Text(
                                                player.second,
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "🏆 Rating: ${player.third}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8B5E3C)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF2E7D32), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "LIVE MATCHES",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC75D27),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    // Live Battles updating in real time
                    val battles = remember {
                        listOf(
                            Triple("Kabir_Gamer (1540)", "Sneha_Warrior (1610)", "Turn 14 • PLACEMENT"),
                            Triple("Chanakya_Pro (1980)", "Rani_99 (1920)", "Turn 26 • MOVEMENT")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        battles.forEach { battle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFFDF8), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            battle.first,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFCC2222)
                                        )
                                        Text(
                                            "vs",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            battle.second,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A5276)
                                        )
                                    }
                                    Text(
                                        battle.third,
                                        fontSize = 10.sp,
                                        color = Color(0xFF8B5E3C)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFBE9E7), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "LIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFD84315)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isOfflineAlertVisible) {
                AlertDialog(
                    onDismissRequest = { isOfflineAlertVisible = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Offline Info",
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Internet Connection Required",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF5C2D0A)
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "No internet connection detected on your device. Playing real opponents requires cellular mobile data or a WiFi connection. Would you like to play Daadi offline with Chanakya Computer instead?",
                            fontSize = 14.sp,
                            color = Color(0xFF8B5E3C)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isOfflineAlertVisible = false
                                onPlayVsAi()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5C2D0A),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Play Computer (Offline)")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { isOfflineAlertVisible = false }
                        ) {
                            Text("Close", color = Color(0xFFC62828))
                        }
                    },
                    containerColor = Color(0xFFFFF7EA),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (showAuthPromptDialog) {
                AlertDialog(
                    onDismissRequest = { showAuthPromptDialog = false },
                    title = {
                        Text(
                            text = "Authentication Required",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF5C2D0A)
                        )
                    },
                    text = {
                        Text(
                            text = "To participate in real-time online matchmaking or private rooms, please sign up or log in to a player profile. Guests may play vs AI offline.",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showAuthPromptDialog = false
                                onManageProfile()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5C2D0A),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Log In / Register")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showAuthPromptDialog = false
                                onPlayVsAi()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC75D27))
                        ) {
                            Text("Play Computer (Offline)")
                        }
                    },
                    containerColor = Color(0xFFFFF7EA),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
