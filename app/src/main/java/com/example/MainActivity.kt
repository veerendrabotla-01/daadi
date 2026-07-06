package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.daadi.viewmodel.AdminViewModel

import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.daadi.viewmodel.HapticPattern
import com.example.daadi.model.RuleSet

import android.media.AudioAttributes
import android.media.SoundPool
import java.util.concurrent.ConcurrentHashMap

import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.example.daadi.viewmodel.SoundEvent

class MainActivity : ComponentActivity() {
    private var soundPool: SoundPool? = null
    private val soundMap = ConcurrentHashMap<SoundEvent, Int>()
    private var soundsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val app = applicationContext as DaadiApplication
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                app.supabaseManager.updateConnectivity(true)
            }
            override fun onLost(network: android.net.Network) {
                app.supabaseManager.updateConnectivity(false)
            }
        })

        // 16. Prevent tapjacking / overlay attacks
        window.decorView.filterTouchesWhenObscured = true
        
        // Handle incoming deep link
        handleDeepLink(intent)
        
        // Initialize Sound Effects Engine asynchronously
        initSoundPool()

        // Gather GDPR/EEA Consent using User Messaging Platform (UMP) before initializing ads
        app.adManager.gatherConsent(this) {
            com.example.daadi.util.SecureLog.d("MainActivity", "AdMob Consent state checked. Ready for compliant placements.")
        }

        // 20. Device Integrity and Anti-Cheat Startup Verification
        lifecycleScope.launch(Dispatchers.IO) {
            val isRooted = com.example.daadi.util.SecurityUtils.isRooted()
            val isEmulator = com.example.daadi.util.SecurityUtils.isEmulator()
            val isDebugger = com.example.daadi.util.SecurityUtils.isDebuggerConnected()
            
            if (isRooted) {
                com.example.daadi.util.SecureLog.e("SECURITY", "Rooted device detected. Logging violation.")
                app.remoteGameRepository.logAntiCheatViolation("", "ROOT_DETECTION", "high", "Device is rooted")
            }
            if (isEmulator) {
                com.example.daadi.util.SecureLog.i("SECURITY", "Emulator detected.")
                app.remoteGameRepository.logAntiCheatViolation("", "EMULATOR_DETECTION", "medium", "Device is emulator")
            }
            if (isDebugger && !BuildConfig.DEBUG) {
                com.example.daadi.util.SecureLog.w("SECURITY", "Debugger connected in release build!")
                app.remoteGameRepository.logAntiCheatViolation("", "DEBUGGER_DETECTION", "high", "Debugger attached to release app")
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val sharedGameViewModel: GameViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext as DaadiApplication))
                    val maintenanceMode by sharedGameViewModel.maintenanceMode.collectAsStateWithLifecycle()
                    val globalBroadcast by sharedGameViewModel.globalBroadcast.collectAsStateWithLifecycle()
                    val currentUser by sharedGameViewModel.authRepository.currentUser.collectAsStateWithLifecycle()

                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                        DaadiAppNavigation(
                            onPlaySound = { event -> playEffect(event) }
                        )

                        // 1. GLOBAL BROADCAST BANNER (Overlays Navigation)
                        globalBroadcast?.let { message ->
                            androidx.compose.animation.AnimatedVisibility(
                                visible = true,
                                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                            ) {
                                Surface(
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().statusBarsPadding(),
                                    color = Color(0xFFC62828),
                                    tonalElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = androidx.compose.ui.Modifier.size(16.dp))
                                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                                        Text(
                                            text = message,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = androidx.compose.ui.Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. MAINTENANCE MODE KILLS-WITCH (Hard block for non-admins)
                        if (maintenanceMode && currentUser?.role != "admin") {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Black.copy(alpha = 0.95f)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(32.dp),
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = Color.Cyan, modifier = androidx.compose.ui.Modifier.size(80.dp))
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
                                    Text("SERVER MAINTENANCE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                    Text(
                                        "The Daadi command center is undergoing architectural upgrades. We will be back online shortly.",
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initSoundPool() {
        val attr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attr)
            .build()
            
        soundPool?.setOnLoadCompleteListener { _, _, _ ->
            soundsLoaded = true
        }

        // Load assets asynchronously from res/raw
        lifecycleScope.launch(Dispatchers.IO) {
            soundPool?.let { pool ->
                soundMap[SoundEvent.PLACE] = pool.load(this@MainActivity, R.raw.place_piece, 1)
                soundMap[SoundEvent.MILL] = pool.load(this@MainActivity, R.raw.mill_formed, 1)
                soundMap[SoundEvent.WIN] = pool.load(this@MainActivity, R.raw.game_over, 1)
                soundMap[SoundEvent.LOSE] = pool.load(this@MainActivity, R.raw.game_over, 1)
            }
        }
    }

    private fun playEffect(event: SoundEvent) {
        if (!soundsLoaded) return
        val soundId = soundMap[event] ?: return
        soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: android.content.Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "daadi" && uri.host == "auth-callback") {
            val app = applicationContext as DaadiApplication
            app.authRepository.network.handleAuthDeepLink(uri) { success, error ->
                com.example.daadi.util.SecureLog.d("MainActivity", "Deep Link Auth result: success=$success, error=$error")
            }
        }
    }
}

@Composable
fun DaadiAppNavigation(onPlaySound: (SoundEvent) -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as DaadiApplication
    val multiplayerManager = application.multiplayerManager
    val lifecycleOwner = LocalLifecycleOwner.current

    // Shared GameViewModel to handle global match lifecycle and backgrounding
    val sharedGameViewModel: GameViewModel = viewModel(factory = ViewModelFactory(application))
    val settingsViewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(application))
    val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()

    // Advanced Device Sound Engine integration
    DisposableEffect(sharedGameViewModel) {
        sharedGameViewModel.onPlaySound = onPlaySound
        onDispose { sharedGameViewModel.onPlaySound = null }
    }

    // Advanced Device Haptic Engine integration
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    DisposableEffect(sharedGameViewModel) {
        sharedGameViewModel.onPerformHaptic = { pattern ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                when (pattern) {
                    HapticPattern.TICK -> {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                    HapticPattern.MILL -> {
                        val timings = longArrayOf(0, 40, 100, 40)
                        val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    HapticPattern.LOSS -> {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, 255))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                when (pattern) {
                    HapticPattern.TICK -> {
                        vibrator.vibrate(50)
                    }
                    HapticPattern.MILL -> {
                        val patternArray = longArrayOf(0, 40, 100, 40)
                        vibrator.vibrate(patternArray, -1)
                    }
                    HapticPattern.LOSS -> {
                        vibrator.vibrate(200)
                    }
                }
            }
        }
        onDispose { sharedGameViewModel.onPerformHaptic = null }
    }

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    sharedGameViewModel.onAppPaused()
                }
                Lifecycle.Event.ON_RESUME -> {
                    sharedGameViewModel.onAppResumed()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // 1. Splash Screen
        composable("splash") {
            SplashScreen(
                onNavigateHome = {
                    if (settingsState.hasSeenOnboarding) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("onboarding") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    settingsViewModel.updateSettings(settingsState.copy(hasSeenOnboarding = true))
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // 2. Home Dashboard
        composable("home") {
            val activeGameState by sharedGameViewModel.gameState.collectAsStateWithLifecycle()

            // Determine if a resume-ready save state exists
            val resumeReadySave = activeGameState.takeIf {
                it.winner == null && (it.player1PiecesOnBoard > 0 || it.player2PiecesOnBoard > 0 || it.player1PiecesInHand < (if (it.ruleSet == RuleSet.TWELVE_MENS_MORRIS) 12 else 9))
            }

            HomeScreen(
                savedGameState = resumeReadySave,
                sharedGameViewModel = sharedGameViewModel,
                onPlayVsAi = { ruleSet ->
                    navController.navigate("difficulty_select?ruleSet=${ruleSet.name}")
                },
                onPlayLocal = { ruleSet ->
                    sharedGameViewModel.startNewGame(GameMode.PASS_AND_PLAY, AIDifficulty.MEDIUM, ruleSet)
                    navController.navigate("game")
                },
                onPlayMultiplayer = { ruleSet ->
                    navController.navigate("multiplayer_lobby?ruleSet=${ruleSet.name}")
                },
                onResumeGame = { navController.navigate("game") },
                onStatsClick = { navController.navigate("stats") },
                onSettingsClick = { navController.navigate("settings") },
                onSignInClick = {
                    val user = application.authRepository.currentUser.value
                    if (user == null) {
                        navController.navigate("supabase_auth")
                    } else if (application.authRepository.userHasPermission("admin_dashboard")) {
                        navController.navigate("supabase_admin")
                    } else {
                        navController.navigate("supabase_auth") 
                    }
                },
                onFeedbackClick = { navController.navigate("feedback") },
                onDiscardSave = {
                    sharedGameViewModel.discardSavedGame()
                    sharedGameViewModel.startNewGame(GameMode.VS_AI, AIDifficulty.MEDIUM)
                }
            )
        }

        composable("feedback") {
            UserFeedbackScreen(
                sharedGameViewModel = sharedGameViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 3. Difficulty Configs
        composable(
            route = "difficulty_select?ruleSet={ruleSet}",
            arguments = listOf(androidx.navigation.navArgument("ruleSet") { defaultValue = RuleSet.NINE_MENS_MORRIS.name })
        ) { backStackEntry ->
            val ruleSetName = backStackEntry.arguments?.getString("ruleSet") ?: RuleSet.NINE_MENS_MORRIS.name
            val ruleSet = RuleSet.valueOf(ruleSetName)
            DifficultySelectScreen(
                onStartGame = { difficulty ->
                    sharedGameViewModel.startNewGame(GameMode.VS_AI, difficulty, ruleSet)
                    navController.navigate("game") {
                        popUpTo("home")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 3.5 Multiplayer Matchmaker Lobby
        composable(
            route = "multiplayer_lobby?ruleSet={ruleSet}",
            arguments = listOf(androidx.navigation.navArgument("ruleSet") { defaultValue = RuleSet.NINE_MENS_MORRIS.name })
        ) { backStackEntry ->
            val ruleSetName = backStackEntry.arguments?.getString("ruleSet") ?: RuleSet.NINE_MENS_MORRIS.name
            val ruleSet = RuleSet.valueOf(ruleSetName)
            MultiplayerLobbyScreen(
                multiplayerManager = multiplayerManager,
                sharedGameViewModel = sharedGameViewModel,
                onBack = { navController.popBackStack() },
                onManageProfile = { navController.navigate("supabase_auth") },
                onPlayVsAi = { navController.navigate("difficulty_select?ruleSet=${ruleSet.name}") },
                onGameStarted = {
                    sharedGameViewModel.startNewGame(GameMode.ONLINE_MULTIPLAYER, AIDifficulty.MEDIUM, ruleSet)
                    navController.navigate("game") {
                        popUpTo("home")
                    }
                }
            )
        }

        composable("supabase_auth") {
            com.example.daadi.ui.screens.SupabaseAuthScreen(
                sharedGameViewModel = sharedGameViewModel,
                onBack = { navController.popBackStack() },
                onAuthSuccess = { 
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate("supabase_admin")
                }
            )
        }

        // 4. Main Game Board Screening
        composable("game") {
            val settingsViewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(application))

            val state by sharedGameViewModel.gameState.collectAsStateWithLifecycle()
            val selectedNodeId by sharedGameViewModel.selectedNodeId.collectAsStateWithLifecycle()
            val isAiThinking by sharedGameViewModel.isAiThinking.collectAsStateWithLifecycle()
            val recentInvalidNode by sharedGameViewModel.recentInvalidNode.collectAsStateWithLifecycle()
            val showPauseMenu by sharedGameViewModel.showPauseMenu.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()

            val turnTimeSeconds by sharedGameViewModel.turnTimeSeconds.collectAsStateWithLifecycle()
            val hintMove by sharedGameViewModel.hintMove.collectAsStateWithLifecycle()
            val aiCommentary by sharedGameViewModel.aiCommentary.collectAsStateWithLifecycle()
            val showTutorial by sharedGameViewModel.showTutorial.collectAsStateWithLifecycle()

            // Multiplayer flows
            val showRemoteDrawRequest by sharedGameViewModel.showRemoteDrawRequest.collectAsStateWithLifecycle()
            val showRemoteUndoRequest by sharedGameViewModel.showRemoteUndoRequest.collectAsStateWithLifecycle()
            val undoPendingLocal by sharedGameViewModel.undoPendingLocal.collectAsStateWithLifecycle()
            val drawOfferPendingLocal by sharedGameViewModel.drawOfferPendingLocal.collectAsStateWithLifecycle()
            val chatMessages by multiplayerManager.chatMessages.collectAsStateWithLifecycle()
            val localPlayerName by multiplayerManager.localPlayerName.collectAsStateWithLifecycle()
            val opponentPlayerName by multiplayerManager.opponentPlayerName.collectAsStateWithLifecycle()
            val opponentProfile by multiplayerManager.opponentProfile.collectAsStateWithLifecycle()
            val adsEnabled by sharedGameViewModel.adsEnabled.collectAsStateWithLifecycle()
            val tutorialWarningMessage by sharedGameViewModel.tutorialWarningMessage.collectAsStateWithLifecycle()

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
                onNodeTapped = { sharedGameViewModel.tapNode(it) },
                onPauseClick = { sharedGameViewModel.togglePauseMenu() },
                onRestartClick = {
                    sharedGameViewModel.startNewGame(state.gameMode, state.aiDifficulty)
                },
                onBackHomeClick = {
                    if (state.winner == null && state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                        sharedGameViewModel.resignGame()
                    }
                    sharedGameViewModel.clearCurrentMatch()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onUndoClick = { sharedGameViewModel.undoLastMove() },
                boardTheme = settingsState.selectedBoardTheme,
                turnTimeSeconds = turnTimeSeconds,
                hintMove = hintMove,
                aiCommentary = aiCommentary,
                showTutorial = showTutorial,
                onHintClick = { sharedGameViewModel.computeHint() },
                onTutorialToggle = { sharedGameViewModel.toggleTutorial(it) },
                onResignClick = { sharedGameViewModel.resignGame() },
                onOfferDrawClick = { sharedGameViewModel.offerDrawGame() },
                showRemoteDrawRequest = showRemoteDrawRequest,
                showRemoteUndoRequest = showRemoteUndoRequest,
                undoPendingLocal = undoPendingLocal,
                drawOfferPendingLocal = drawOfferPendingLocal,
                onRespondToRemoteDraw = { sharedGameViewModel.respondToRemoteDraw(it) },
                onRespondToRemoteUndo = { sharedGameViewModel.respondToRemoteUndo(it) },
                chatMessages = chatMessages,
                onSendChatMessage = { multiplayerManager.sendChat(it) },
                localPlayerName = localPlayerName,
                opponentPlayerName = opponentPlayerName,
                opponentProfile = opponentProfile,
                adsEnabled = adsEnabled,
                settings = settingsState,
                onSettingsChanged = { settingsViewModel.updateSettings(it) },
                onReportOpponent = {
                    sharedGameViewModel.reportOpponent { success, message ->
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                tutorialWarningMessage = tutorialWarningMessage,
                connectionStatus = state.connectionStatus,
                reconnectionCountdown = state.reconnectionCountdown
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
            val currentUser by application.authRepository.currentUser.collectAsStateWithLifecycle()
            val isAdmin = application.authRepository.userHasPermission("admin_dashboard")

            SettingsScreen(
                settings = settingsState,
                isAdmin = isAdmin,
                onSettingsChanged = { settingsViewModel.updateSettings(it) },
                onAdminClick = { navController.navigate("supabase_admin") },
                onDeleteAccount = {
                    application.authRepository.network.deleteAccountGDPR { success ->
                        if (success) {
                            android.widget.Toast.makeText(context, "Account deleted successfully.", android.widget.Toast.LENGTH_LONG).show()
                            navController.navigate("supabase_auth")
                        } else {
                            android.widget.Toast.makeText(context, "Failed to delete account. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onExportData = {
                    application.authRepository.network.requestDataExport { success ->
                        if (success) {
                            android.widget.Toast.makeText(context, "Data export request sent! Check your registered email.", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            android.widget.Toast.makeText(context, "Failed to request data export. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 7. Supabase Administration Operations Control Center
        composable("supabase_admin") {
            val adminViewModel: com.example.daadi.viewmodel.AdminViewModel = viewModel(factory = ViewModelFactory(application))
            SupabaseAdminScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
