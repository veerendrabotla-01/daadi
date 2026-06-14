package com.example.daadi.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.daadi.engine.GameEngine
import com.example.daadi.model.*
import com.example.daadi.ui.components.GameBoard
import com.example.daadi.ui.components.RewardedAdOfferDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameState,
    selectedNodeId: Int?,
    validDestinationNodes: List<Int>,
    isAiThinking: Boolean,
    recentInvalidNode: Int?,
    showPauseMenu: Boolean,
    onNodeTapped: (Int) -> Unit,
    onPauseClick: () -> Unit,
    onRestartClick: () -> Unit,
    onBackHomeClick: () -> Unit,
    onUndoClick: () -> Unit,
    boardTheme: String,
    turnTimeSeconds: Int,
    hintMove: Pair<Int?, Int>?,
    aiCommentary: String,
    showTutorial: Boolean,
    onHintClick: () -> Unit,
    onTutorialToggle: (Boolean) -> Unit,
    onResignClick: () -> Unit,
    onOfferDrawClick: () -> Unit,
    // Multiplayer integrations
    showRemoteDrawRequest: Boolean = false,
    showRemoteUndoRequest: Boolean = false,
    undoPendingLocal: Boolean = false,
    drawOfferPendingLocal: Boolean = false,
    onRespondToRemoteDraw: (Boolean) -> Unit = {},
    onRespondToRemoteUndo: (Boolean) -> Unit = {},
    chatMessages: List<com.example.daadi.data.multiplayer.ChatMessage> = emptyList(),
    onSendChatMessage: (String) -> Unit = {},
    localPlayerName: String = "You",
    adsEnabled: Boolean = false,
    settings: AppSettings = AppSettings(),
    onSettingsChanged: (AppSettings) -> Unit = {},
    onReportOpponent: () -> Unit = {},
    tutorialWarningMessage: String? = null
) {
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showQuitConfirmation by remember { mutableStateOf(false) }
    var showAdOfferDialog by remember { mutableStateOf(false) }
    var showDrawConfirmation by remember { mutableStateOf(false) }
    var showResignConfirmation by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }

    // Automatic tutorial popup on match start (first move)
    LaunchedEffect(state.moveHistory, settings.showRulesOnStart) {
        if (settings.showRulesOnStart && state.moveHistory.isEmpty() && state.winner == null) {
            onTutorialToggle(true)
        }
    }

    // Dynamic adaptive background color based on boardTheme
    val dynamicBackgroundColor = when (boardTheme) {
        "classic_wood" -> Color(0xFFFDF3E3) // Sandstone cozy light
        "emerald_jade" -> Color(0xFF042116) // Celestial emerald jade dark
        else -> Color(0xFF1B2327) // Slate obsidian dark
    }

    val dynamicTextColor = when (boardTheme) {
        "classic_wood" -> Color(0xFF5C2D0A)
        else -> Color(0xFFECEFF1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (state.currentPlayer == Player.PLAYER_1) Color(0xFFCC2222) else Color(0xFF1A5276))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAiThinking) {
                                "Computer Thinking..."
                            } else if (state.isCapturePending) {
                                "CAPTURE PENDING!"
                            } else if (state.gameMode == GameMode.VS_AI) {
                                if (state.currentPlayer == Player.PLAYER_1) "Your Turn" else "Opponent Turn"
                            } else {
                                if (state.currentPlayer == Player.PLAYER_1) "Red turn" else "Blue turn"
                            },
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackHomeClick, modifier = Modifier.testTag("game_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit to Menu", tint = dynamicTextColor)
                    }
                },
                actions = {
                    if (state.winner == null) {
                        IconButton(onClick = onPauseClick, modifier = Modifier.testTag("pause_button")) {
                            Icon(Icons.Default.Menu, contentDescription = "Pause Session", tint = dynamicTextColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = dynamicBackgroundColor,
                    titleContentColor = dynamicTextColor,
                    navigationIconContentColor = dynamicTextColor,
                    actionIconContentColor = dynamicTextColor
                )
            )
        },
        containerColor = dynamicBackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // Status banner: Pieces remaining & Phase Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Phase Title
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFAE8CE), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (state.phase) {
                                GamePhase.PLACEMENT -> "PLACEMENT PHASE • Plant 9 Seeds"
                                GamePhase.MOVEMENT -> "MOVEMENT PHASE • Slide Along Lines"
                                GamePhase.FLYING -> "FLYING PHASE • Jump Anywhere!"
                                GamePhase.GAME_OVER -> "MATCH RESOLVED"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5C2D0A),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.winner == null && state.currentPlayer == Player.PLAYER_1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(if (turnTimeSeconds <= 10) Color(0x22D32F2F) else Color(0x11000000), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Clock Icon",
                                tint = if (turnTimeSeconds <= 10) Color(0xFFD32F2F) else Color(0xFFE5A93B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Move Timer: ${turnTimeSeconds}s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (turnTimeSeconds <= 10) Color(0xFFD32F2F) else dynamicTextColor
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                // Chips layout showing detailed hand counts
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Player 1 (Red)
                    GameStatusChip(
                        label = if (state.gameMode == GameMode.VS_AI) "You (Red)" else "Red",
                        handCount = state.player1PiecesInHand,
                        boardCount = state.player1PiecesOnBoard,
                        color = Color(0xFFCC2222),
                        modifier = Modifier.weight(1f)
                    )

                    // Player 2 (Blue)
                    GameStatusChip(
                        label = if (state.gameMode == GameMode.VS_AI) "Bot (Blue)" else "Blue",
                        handCount = state.player2PiecesInHand,
                        boardCount = state.player2PiecesOnBoard,
                        color = Color(0xFF1A5276),
                        modifier = Modifier.weight(1f)
                    )
                }

                    if (state.gameMode == GameMode.VS_AI && state.winner == null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (boardTheme == "classic_wood") Color(0xFFFAE8CE) else Color(0xFF233036)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🤖", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text(
                                        text = "ANCIENT SAGE CHANAKYA",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE5A93B),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = aiCommentary,
                                        fontSize = 12.sp,
                                        color = if (boardTheme == "classic_wood") Color(0xFF5C2D0A) else Color(0xFFECEFF1),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Main GameBoard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    GameBoard(
                        board = state.board,
                        selectedNodeId = selectedNodeId,
                        validDestinationNodes = validDestinationNodes,
                        recentInvalidNode = recentInvalidNode,
                        onNodeTapped = onNodeTapped,
                        boardTheme = boardTheme,
                        hintMove = hintMove,
                        highlightLastMove = settings.highlightLastMove,
                        lastMoveNodeId = state.moveHistory.lastOrNull()?.toNode,
                        modifier = Modifier.sizeIn(maxWidth = 440.dp, maxHeight = 440.dp)
                    )
                }

                // Tutorial Countdown Warning Overlay
                AnimatedVisibility(
                    visible = tutorialWarningMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 2.dp)
                            .background(Color(0xFFC75D27), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = tutorialWarningMessage ?: "",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Dedicated instruction area for captures and status (Moved away from board overlay)
                AnimatedVisibility(
                    visible = state.isCapturePending && !isAiThinking,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val isUserCapturer = state.gameMode == GameMode.VS_AI && state.currentPlayer == Player.PLAYER_1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(Color(0xFFC62828), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (isUserCapturer) {
                                "✨ DAADI CLOSED! REMOVE A BLUE PIECE!"
                            } else if (state.gameMode == GameMode.VS_AI) {
                                "Bot is removing your Red piece..."
                            } else {
                                "✨ DAADI CLOSED! REMOVE OPPONENT PIECE!"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 1. Quick tips or hint bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .background(Color(0xFFFAE8CE), RoundedCornerShape(10.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (state.isCapturePending) {
                            "Defensive rule: You cannot target a piece which is already in a mill."
                        } else if (state.phase == GamePhase.PLACEMENT) {
                            "Objective: Click empty dots to form 3-in-a-row lines to capture bot pieces."
                        } else if (state.phase == GamePhase.FLYING) {
                            "Jumping active! Your remaining 3 beads can fly anywhere on the board."
                        } else {
                            "Movement: Click a piece, then tap any adjacent connected gold dot to slide."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF5C2D0A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Move Ledger (Dynamic Action Log ledger)
                if (state.moveHistory.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (boardTheme == "classic_wood") Color(0xFFF5EAD4) else Color(0xFF233036)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "MATCH ACTION LOGS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE5A93B),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            state.moveHistory.takeLast(2).reversed().forEach { move ->
                                val logStr = when (move.type) {
                                    MoveType.PLACE -> "Placed seed at spot ${move.toNode + 1}"
                                    MoveType.MOVE -> "Slid seed from ${move.fromNode!! + 1} to ${move.toNode + 1}"
                                    MoveType.CAPTURE -> "Captured opponent bead at spot ${move.toNode + 1}"
                                }
                                val actor = if (move.player == Player.PLAYER_1) "Red" else "Blue"
                                Text(
                                    text = "• $actor: $logStr",
                                    fontSize = 11.sp,
                                    color = if (boardTheme == "classic_wood") Color(0xFF5C2D0A) else Color(0xFFECEFF1),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 3. Control Grid: Get Hint, Tutorial, Resign, Handshake Draw
                if (state.winner == null && state.phase != GamePhase.GAME_OVER) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Get Hint
                        Button(
                            onClick = onHintClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5E3C)),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("💡 Hint", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // How to play Guide
                        Button(
                            onClick = { onTutorialToggle(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A)),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("📖 Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Offer Draw
                        Button(
                            onClick = { showDrawConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A7F71)),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("🤝 Draw", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Surrender/Resign
                        Button(
                            onClick = { showResignConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB03A2E)),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("🏳️ Forfeit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { showChatSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C81)),
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("💬 Quick Chat & Messages (${chatMessages.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // --- Dialog Overlays ---

            // Draw Confirmation Dialog
            if (showDrawConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDrawConfirmation = false },
                    title = { Text("Offer Handshake Draw?", fontWeight = FontWeight.Bold, color = Color(0xFF5C2D0A)) },
                    text = { Text("Are you sure you want to end this game with a friendly handshake Draw? Stats will register a Draw match.", fontSize = 14.sp, color = Color(0xFF8B5E3C)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDrawConfirmation = false
                                onOfferDrawClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A7F71))
                        ) {
                            Text("Accept Draw", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDrawConfirmation = false }) {
                            Text("Cancel", color = Color(0xFF8B5E3C))
                        }
                    },
                    containerColor = Color(0xFFFFFBF4)
                )
            }

            // Forfeit Confirmation Dialog
            if (showResignConfirmation) {
                AlertDialog(
                    onDismissRequest = { showResignConfirmation = false },
                    title = { Text("Forfeit Match?", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
                    text = { Text("Are you sure you want to surrender this matchup? The Opponent will receive an immediate victory.", fontSize = 14.sp, color = Color(0xFF8B5E3C)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showResignConfirmation = false
                                onResignClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB03A2E))
                        ) {
                            Text("Yes, Surrender", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResignConfirmation = false }) {
                            Text("Cancel", color = Color(0xFF8B5E3C))
                        }
                    },
                    containerColor = Color(0xFFFFFBF4)
                )
            }

            // Interactive Tutorial Paging Slides Modal
            if (showTutorial) {
                var currentSlide by remember { mutableStateOf(0) }
                val slides = listOf(
                    Pair("Welcome to Daadi!", "Daadi is an ancient Indian dynamic strategy board game of align-and-capture. You start with 9 beads to place on the board during the PLACEMENT phase."),
                    Pair("Forming Daadi Mills", "Align exactly 3 of your beads horizontally or vertically to close a Daadi trap! Closing a Daadi grants you an immediate CAPTURE of any opponent bead!"),
                    Pair("Capture Safety Rule", "Important: You are forbidden from targeting any opponent bead that is currently part of an active Daadi, unless all of their on-board beads are inside active Daadis."),
                    Pair("Sliding & Flying Wings", "After placing all 9 beads, enter the MOVEMENT phase to slide to adjacent dots along lines. Once down to exactly 3 beads, your beads gain wings to FLY to any empty dot!")
                )
                AlertDialog(
                    onDismissRequest = { onTutorialToggle(false) },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (currentSlide < slides.size - 1) {
                                    currentSlide++
                                } else {
                                    onTutorialToggle(false)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A))
                        ) {
                            Text(if (currentSlide < slides.size - 1) "Next" else "Close Guide", color = Color.White)
                        }
                    },
                    dismissButton = {
                        if (currentSlide > 0) {
                            TextButton(onClick = { currentSlide-- }) {
                                Text("Back", color = Color(0xFF5C2D0A))
                            }
                        } else {
                            TextButton(onClick = { onTutorialToggle(false) }) {
                                Text("Skip", color = Color(0xFF5C2D0A))
                            }
                        }
                    },
                    title = {
                        Text(
                            text = slides[currentSlide].first,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5C2D0A)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = slides[currentSlide].second,
                                fontSize = 13.sp,
                                color = Color(0xFF8B5E3C),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // "Don't show again" toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSettingsChanged(settings.copy(showRulesOnStart = !settings.showRulesOnStart)) }
                            ) {
                                Checkbox(
                                    checked = !settings.showRulesOnStart,
                                    onCheckedChange = { onSettingsChanged(settings.copy(showRulesOnStart = !it)) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF5C2D0A))
                                )
                                Text(
                                    text = "Don't show rules automatically",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5C2D0A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                slides.indices.forEach { idx ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (idx == currentSlide) Color(0xFFE5A93B) else Color(0x335C2D0A))
                                    )
                                }
                            }
                        }
                    },
                    containerColor = Color(0xFFFFFBF4)
                )
            }

            // 1. Remote Draw Request Dialog
            if (showRemoteDrawRequest) {
                AlertDialog(
                    onDismissRequest = { onRespondToRemoteDraw(false) },
                    title = { Text("Draw Offer Received", fontWeight = FontWeight.Bold, color = Color(0xFF5C2D0A)) },
                    text = { Text("Your opponent is proposing a friendly handshake Draw. Do you accept?", fontSize = 14.sp, color = Color(0xFF8B5E3C)) },
                    confirmButton = {
                        Button(
                            onClick = { onRespondToRemoteDraw(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A7F71))
                        ) {
                            Text("Accept Draw", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onRespondToRemoteDraw(false) }) {
                            Text("Reject", color = Color(0xFFC62828))
                        }
                    },
                    containerColor = Color(0xFFFFFBF4)
                )
            }

            // 2. Remote Undo Request Dialog
            if (showRemoteUndoRequest) {
                AlertDialog(
                    onDismissRequest = { onRespondToRemoteUndo(false) },
                    title = { Text("Undo Request Received", fontWeight = FontWeight.Bold, color = Color(0xFF5C2D0A)) },
                    text = { Text("Your opponent is requesting to undo their last move. Do you grant this permission?", fontSize = 14.sp, color = Color(0xFF8B5E3C)) },
                    confirmButton = {
                        Button(
                            onClick = { onRespondToRemoteUndo(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A7F71))
                        ) {
                            Text("Grant Permission", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onRespondToRemoteUndo(false) }) {
                            Text("Deny", color = Color(0xFFC62828))
                        }
                    },
                    containerColor = Color(0xFFFFFBF4)
                )
            }

            // 3. Local Pending Request Overlay (Undo or Draw)
            if (undoPendingLocal || drawOfferPendingLocal) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99110802)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFC75D27))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (undoPendingLocal) {
                                    "Waiting for Opponent to approve your Undo Request..."
                                } else {
                                    "Waiting for Opponent to respond to your Draw Offer..."
                                },
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF5C2D0A),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 4. Quick Chat bottom modal
            if (showChatSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99110802))
                        .clickable { showChatSheet = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF4)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.65f)
                            .clickable(enabled = false) {}
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💬 Battlefield Quick Chat",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5C2D0A)
                                )
                                TextButton(onClick = { showChatSheet = false }) {
                                    Text("Close", color = Color(0xFFC75D27), fontWeight = FontWeight.Bold)
                                }
                            }

                            Divider(color = Color(0xFFE5A93B).copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF7EA), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                if (chatMessages.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No messages yet. Send a quick phrase below!",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(chatMessages.size) { idx ->
                                            val msg = chatMessages[idx]
                                            val isMe = msg.sender == localPlayerName
                                            val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isMe) Color(0xFFC75D27) else Color(0xFFE5A93B).copy(alpha = 0.2f),
                                                            RoundedCornerShape(
                                                                topStart = 10.dp,
                                                                topEnd = 10.dp,
                                                                bottomStart = if (isMe) 10.dp else 0.dp,
                                                                bottomEnd = if (isMe) 0.dp else 10.dp
                                                            )
                                                        )
                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = msg.text,
                                                        color = if (isMe) Color.White else Color(0xFF5C2D0A),
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (isMe) "You • $timeStr" else "${msg.sender} • $timeStr",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "TAP A PHRASE TO SEND INSTANTLY:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5E3C),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            val templates = listOf(
                                "Good luck! 👍",
                                "Brilliant move! 👏",
                                "Yes! My Daadi Mill closed! 🎉",
                                "Oops! Overlooked that line 😅",
                                "Excellent game so far! 😎",
                                "Dhanyavaad! (Thank you) 🙏"
                            )

                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(templates.size) { idx ->
                                    val phrase = templates[idx]
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFD4A55A).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0xFFD4A55A).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                            .clickable {
                                                onSendChatMessage(phrase)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = phrase, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF5C2D0A))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PAUSE PANEL DRAWER MODAL
            if (showPauseMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xB3110802))
                        .clickable { onPauseClick() }, // Dismiss on outside click
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF4)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clickable(enabled = false) {} // Prevent click-through
                            .testTag("pause_menu_drawer")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Game Paused",
                                style = MaterialTheme.typography.displayMedium,
                                color = Color(0xFF5C2D0A),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Resume Button
                            Button(
                                onClick = onPauseClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("resume_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resume Match")
                            }

                            // Restart Match Button (triggers dialog)
                            Button(
                                onClick = { showResetConfirmation = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4A55A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("restart_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF1C0A00))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restart Match", color = Color(0xFF1C0A00))
                            }

                            // Back to Main Menu (triggers dialog)
                            OutlinedButton(
                                onClick = { showQuitConfirmation = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("quit_button")
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF5C2D0A))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Back to Main Menu", color = Color(0xFF5C2D0A))
                            }
                        }
                    }
                }
            }

            // GAME OVER POPUP/CARD
            if (state.winner != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC110802)),
                    contentAlignment = Alignment.Center
                ) {
                    val isWin = state.winner == Player.PLAYER_1
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF4)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(16.dp)
                            .testTag("game_over_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🏆 MATCH RESOLVED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE5A93B),
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (state.gameMode == GameMode.VS_AI) {
                                    if (isWin) "VICTORY!" else "DEFEAT"
                                } else {
                                    if (state.winner == Player.PLAYER_1) "RED WINS!" else "BLUE WINS!"
                                },
                                style = MaterialTheme.typography.displayLarge,
                                color = if (isWin) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (state.gameMode == GameMode.VS_AI) {
                                    if (isWin) "You outsmarted the Bot with superb strategic blocks!" else "The Chanakya AI has defeated you. Practice makes perfect."
                                } else {
                                    "A glorious clash of strategy. Congratulations to the victor!"
                                },
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF8B5E3C),
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            // Play again & Menu CTAs
                            Button(
                                onClick = onRestartClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5C2D0A),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("play_again_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Rematch", color = Color.White)
                            }
 
                            if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onReportOpponent,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("report_opponent_button")
                                ) {
                                    Icon(Icons.Default.Report, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Report Unfair Play", color = Color.White)
                                }
                            }
 
                            // Undo offer buttons (Loss only in single mode)
                            if (state.gameMode == GameMode.VS_AI && !isWin && adsEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { showAdOfferDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5A93B)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("undo_error_menu_button")
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Watch Ad to Undo", color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = onBackHomeClick,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("home_from_game_button")
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF5C2D0A))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Discard & Home Menu", color = Color(0xFF5C2D0A))
                            }
                        }
                    }
                }
            }

            // REWARDED AD OFFER OVERLAY DIALOG
            if (showAdOfferDialog) {
                RewardedAdOfferDialog(
                    onDismiss = { showAdOfferDialog = false },
                    onRewardEarned = onUndoClick
                )
            }

            // CONFIRMATIONS MODALS
            if (showResetConfirmation) {
                AlertDialog(
                    onDismissRequest = { showResetConfirmation = false },
                    title = { Text("Restart Match?") },
                    text = { Text("Are you sure you want to discard this board and start from scratch?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showResetConfirmation = false
                                onRestartClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) { Text("Yes, Reset") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
                    },
                    containerColor = Color(0xFFFFFBF4)
                )
            }

            if (showQuitConfirmation) {
                AlertDialog(
                    onDismissRequest = { showQuitConfirmation = false },
                    title = { Text("Exit to Main Menu?") },
                    text = { Text("Your board state will be automatically saved, and you can resume this session later!") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showQuitConfirmation = false
                                onBackHomeClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A))
                        ) { Text("Yes, Quit & Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showQuitConfirmation = false }) { Text("Stay") }
                    },
                    containerColor = Color(0xFFFFFBF4)
                )
            }
        }
    }
}

@Composable
fun GameStatusChip(
    label: String,
    handCount: Int,
    boardCount: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hand", fontSize = 8.sp, color = Color.Gray)
                    Text("$handCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C2D0A))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Board", fontSize = 8.sp, color = Color.Gray)
                    Text("$boardCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C2D0A))
                }
            }
        }
    }
}
