package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.daadi.DaadiApplication
import com.example.daadi.engine.GameEngine
import com.example.daadi.model.AIDifficulty
import com.example.daadi.model.GameMode
import com.example.daadi.model.GamePhase
import com.example.daadi.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.daadi.viewmodel.GameViewModel
import com.example.daadi.viewmodel.SettingsViewModel
import com.example.daadi.viewmodel.StatsViewModel
import com.example.daadi.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DaadiAppNavigation()
                }
            }
        }
    }
}

@Composable
fun DaadiAppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as DaadiApplication
    val multiplayerManager = application.multiplayerManager

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // 1. Splash Screen
        composable("splash") {
            SplashScreen(
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // 2. Home Dashboard
        composable("home") {
            val gameViewModel: GameViewModel = viewModel(factory = ViewModelFactory(application))
            val activeGameState by gameViewModel.gameState.collectAsStateWithLifecycle()

            // Determine if a resume-ready save state exists
            val resumeReadySave = activeGameState.takeIf {
                it.winner == null && (it.player1PiecesOnBoard > 0 || it.player2PiecesOnBoard > 0 || it.player1PiecesInHand < 9)
            }

            HomeScreen(
                savedGameState = resumeReadySave,
                supabaseManager = application.supabaseManager,
                onPlayVsAi = { navController.navigate("difficulty_select") },
                onPlayLocal = {
                    gameViewModel.startNewGame(GameMode.PASS_AND_PLAY, AIDifficulty.MEDIUM)
                    navController.navigate("game")
                },
                onPlayMultiplayer = {
                    navController.navigate("multiplayer_lobby")
                },
                onResumeGame = { navController.navigate("game") },
                onStatsClick = { navController.navigate("stats") },
                onSettingsClick = { navController.navigate("settings") },
                onSignInClick = {
                    val user = application.supabaseManager.currentUser.value
                    if (user == null) {
                        navController.navigate("supabase_auth")
                    } else if (user.role == "admin") {
                        navController.navigate("supabase_admin")
                    } else {
                        // For regular users, maybe navigate to profile or just let them stay on home
                        navController.navigate("supabase_auth") 
                    }
                },
                onFeedbackClick = { navController.navigate("feedback") },
                onDiscardSave = {
                    gameViewModel.discardSavedGame()
                    gameViewModel.startNewGame(GameMode.VS_AI, AIDifficulty.MEDIUM)
                }
            )
        }

        composable("feedback") {
            UserFeedbackScreen(
                supabaseManager = (application as com.example.daadi.DaadiApplication).supabaseManager,
                onBack = { navController.popBackStack() }
            )
        }

        // 3. Difficulty Configs
        composable("difficulty_select") {
            val gameViewModel: GameViewModel = viewModel(factory = ViewModelFactory(application))

            DifficultySelectScreen(
                onStartGame = { difficulty ->
                    gameViewModel.startNewGame(GameMode.VS_AI, difficulty)
                    navController.navigate("game") {
                        popUpTo("home") // Clear difficulty Screen from backstack
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 3.5 Multiplayer Matchmaker Lobby
        composable("multiplayer_lobby") {
            MultiplayerLobbyScreen(
                multiplayerManager = multiplayerManager,
                supabaseManager = (application as com.example.daadi.DaadiApplication).supabaseManager,
                onBack = { navController.popBackStack() },
                onManageProfile = { navController.navigate("supabase_auth") },
                onPlayVsAi = { navController.navigate("difficulty_select") },
                onGameStarted = {
                    navController.navigate("game") {
                        popUpTo("home")
                    }
                }
            )
        }

        composable("supabase_auth") {
            com.example.daadi.ui.screens.SupabaseAuthScreen(
                supabaseManager = (application as com.example.daadi.DaadiApplication).supabaseManager,
                onBack = { navController.popBackStack() },
                onAuthSuccess = { 
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        // 4. Main Game Board Screening
        composable("game") {
            val gameViewModel: GameViewModel = viewModel(factory = ViewModelFactory(application))
            val settingsViewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(application))

            val state by gameViewModel.gameState.collectAsStateWithLifecycle()
            val selectedNodeId by gameViewModel.selectedNodeId.collectAsStateWithLifecycle()
            val isAiThinking by gameViewModel.isAiThinking.collectAsStateWithLifecycle()
            val recentInvalidNode by gameViewModel.recentInvalidNode.collectAsStateWithLifecycle()
            val showPauseMenu by gameViewModel.showPauseMenu.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()

            val turnTimeSeconds by gameViewModel.turnTimeSeconds.collectAsStateWithLifecycle()
            val hintMove by gameViewModel.hintMove.collectAsStateWithLifecycle()
            val aiCommentary by gameViewModel.aiCommentary.collectAsStateWithLifecycle()
            val showTutorial by gameViewModel.showTutorial.collectAsStateWithLifecycle()

            // Multiplayer flows
            val showRemoteDrawRequest by gameViewModel.showRemoteDrawRequest.collectAsStateWithLifecycle()
            val showRemoteUndoRequest by gameViewModel.showRemoteUndoRequest.collectAsStateWithLifecycle()
            val undoPendingLocal by gameViewModel.undoPendingLocal.collectAsStateWithLifecycle()
            val drawOfferPendingLocal by gameViewModel.drawOfferPendingLocal.collectAsStateWithLifecycle()
            val chatMessages by multiplayerManager.chatMessages.collectAsStateWithLifecycle()
            val localPlayerName by multiplayerManager.localPlayerName.collectAsStateWithLifecycle()
            val adsEnabled by gameViewModel.adsEnabled.collectAsStateWithLifecycle()
            val tutorialWarningMessage by gameViewModel.tutorialWarningMessage.collectAsStateWithLifecycle()

            // Calculate valid targets dynamically for board highlights
            val validDestinations = remember(state, selectedNodeId) {
                if (state.winner != null || state.isCapturePending) {
                    emptyList()
                } else {
                    val moves = GameEngine.getLegalMoves(state, state.currentPlayer)
                    if (state.phase == GamePhase.PLACEMENT) {
                        moves.map { it.second }
                    } else {
                        moves.filter { it.first == selectedNodeId }.map { it.second }
                    }
                }
            }

            GameScreen(
                state = state,
                selectedNodeId = selectedNodeId,
                validDestinationNodes = validDestinations,
                isAiThinking = isAiThinking,
                recentInvalidNode = recentInvalidNode,
                showPauseMenu = showPauseMenu,
                onNodeTapped = { gameViewModel.tapNode(it) },
                onPauseClick = { gameViewModel.togglePauseMenu() },
                onRestartClick = {
                    gameViewModel.startNewGame(state.gameMode, state.aiDifficulty)
                },
                onBackHomeClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onUndoClick = { gameViewModel.undoLastMove() },
                boardTheme = settingsState.selectedBoardTheme,
                turnTimeSeconds = turnTimeSeconds,
                hintMove = hintMove,
                aiCommentary = aiCommentary,
                showTutorial = showTutorial,
                onHintClick = { gameViewModel.computeHint() },
                onTutorialToggle = { gameViewModel.toggleTutorial(it) },
                onResignClick = { gameViewModel.resignGame() },
                onOfferDrawClick = { gameViewModel.offerDrawGame() },
                showRemoteDrawRequest = showRemoteDrawRequest,
                showRemoteUndoRequest = showRemoteUndoRequest,
                undoPendingLocal = undoPendingLocal,
                drawOfferPendingLocal = drawOfferPendingLocal,
                onRespondToRemoteDraw = { gameViewModel.respondToRemoteDraw(it) },
                onRespondToRemoteUndo = { gameViewModel.respondToRemoteUndo(it) },
                chatMessages = chatMessages,
                onSendChatMessage = { multiplayerManager.sendChat(it) },
                localPlayerName = localPlayerName,
                adsEnabled = adsEnabled,
                settings = settingsState,
                onSettingsChanged = { settingsViewModel.updateSettings(it) },
                onReportOpponent = { gameViewModel.reportOpponent() },
                tutorialWarningMessage = tutorialWarningMessage
            )
        }

        // 5. Statistics Board
        composable("stats") {
            val context = LocalContext.current
            val application = context.applicationContext as DaadiApplication
            val statsViewModel: StatsViewModel = viewModel(factory = ViewModelFactory(application))
            val statsState by statsViewModel.stats.collectAsStateWithLifecycle()

            StatsScreen(
                stats = statsState,
                onResetStats = { statsViewModel.resetStats() },
                onBack = { navController.popBackStack() }
            )
        }

        // 6. Settings toggles
        composable("settings") {
            val context = LocalContext.current
            val application = context.applicationContext as DaadiApplication
            val settingsViewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(application))
            val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()

            SettingsScreen(
                settings = settingsState,
                onSettingsChanged = { settingsViewModel.updateSettings(it) },
                onAdminClick = { navController.navigate("supabase_admin") },
                onBack = { navController.popBackStack() }
            )
        }

        // 7. Supabase Administration Operations Control Center
        composable("supabase_admin") {
            SupabaseAdminScreen(
                supabaseManager = application.supabaseManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
