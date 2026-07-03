package com.example.daadi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.daadi.DaadiApplication
import com.example.daadi.audio.SoundManager
import com.example.daadi.data.multiplayer.MultiplayerManager
import com.example.daadi.data.repository.*
import com.example.daadi.engine.GameEngine
import com.example.daadi.engine.MillDetector
import com.example.daadi.engine.ai.AIEngine
import com.example.daadi.engine.ai.AiConfig
import com.example.daadi.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HapticPattern {
    TICK, MILL, LOSS
}

enum class SoundEvent {
    PLACE, MILL, WIN, LOSE, CAPTURE
}

class GameViewModel(
    private val gameRepository: GameRepository,
    private val statsRepository: StatsRepository,
    private val settingsRepository: SettingsRepository,
    private val soundManager: SoundManager,
    val multiplayerManager: MultiplayerManager,
    val supabaseManager: com.example.daadi.data.supabase.SupabaseManager
) : ViewModel() {

    companion object {
        private val gameStateAdapter = com.example.daadi.data.repository.SharedMoshi.moshi.adapter(GameState::class.java)
        private const val SIMULATED_FILL_RATE_THRESHOLD = 15
        private const val SIMULATED_AD_LOAD_DELAY_MS = 500L
    }

    var onPerformHaptic: ((HapticPattern) -> Unit)? = null
    var onPlaySound: ((SoundEvent) -> Unit)? = null

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<Int?>(null)
    val selectedNodeId: StateFlow<Int?> = _selectedNodeId.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _showPauseMenu = MutableStateFlow(false)
    val showPauseMenu: StateFlow<Boolean> = _showPauseMenu.asStateFlow()

    private val _recentInvalidNode = MutableStateFlow<Int?>(null)
    val recentInvalidNode: StateFlow<Int?> = _recentInvalidNode.asStateFlow()

    private val _turnTimeSeconds = MutableStateFlow(30)
    val turnTimeSeconds: StateFlow<Int> = _turnTimeSeconds.asStateFlow()

    private val _isAppPaused = MutableStateFlow(false)
    val isAppPaused: StateFlow<Boolean> = _isAppPaused.asStateFlow()

    private val _adsEnabled = MutableStateFlow(false)
    val adsEnabled: StateFlow<Boolean> = _adsEnabled.asStateFlow()

    // 3. LIVE CLIENT AD FATIGUE CONTROLLER
    private val _matchesPlayedSinceLastAd = MutableStateFlow(0)
    val matchesPlayedSinceLastAd: StateFlow<Int> = _matchesPlayedSinceLastAd.asStateFlow()

    val adConfig = supabaseManager.adConfig
    val adTelemetry = supabaseManager.adTelemetry

    fun incrementMatchAdCounter() {
        _matchesPlayedSinceLastAd.update { it + 1 }
    }

    fun resetMatchAdCounter() {
        _matchesPlayedSinceLastAd.value = 0
    }

    fun canShowInterstitial(): Boolean {
        val config = adConfig.value
        if (!config.isMonetizationGlobalOverride) return false
        return _matchesPlayedSinceLastAd.value >= config.interstitialFrequencyCap
    }

    fun updateAdConfiguration(config: com.example.daadi.data.supabase.SupabaseManager.AdConfiguration) {
        supabaseManager.updateAdConfigurationRemote(config)
    }

    fun flushAdAnalytics() {
        supabaseManager.resetAdTelemetryRemote()
    }

    fun simulateAdRequest() {
        viewModelScope.launch {
            supabaseManager.incrementAdRequests()
            delay(SIMULATED_AD_LOAD_DELAY_MS) // Simulation of SDK load time
            if ((0..100).random() > SIMULATED_FILL_RATE_THRESHOLD) { // 85% fill rate simulation
                supabaseManager.incrementAdImpressions()
            }
        }
    }

    val maintenanceMode = supabaseManager.maintenanceMode
    val multiplayerEnabled = supabaseManager.multiplayerEnabled
    val globalBroadcast = supabaseManager.globalBroadcast

    fun dispatchGlobalBroadcast(message: String) {
        supabaseManager.dispatchBroadcast(message)
    }

    fun clearGlobalBroadcast() {
        supabaseManager.clearBroadcast()
    }

    private val _aiConfig = MutableStateFlow(AIEngine.currentConfig)
    val aiConfig: StateFlow<AiConfig> = _aiConfig.asStateFlow()

    private val _stagedAiConfig = MutableStateFlow(AIEngine.currentConfig)
    val stagedAiConfig: StateFlow<AiConfig> = _stagedAiConfig.asStateFlow()

    fun stageAiConfig(config: AiConfig) {
        _stagedAiConfig.value = config
    }

    fun applyStagedAiConfig() {
        val config = _stagedAiConfig.value
        _aiConfig.value = config
        AIEngine.currentConfig = config
        supabaseManager.logAdminAction("APPLY_AI_PERSONALITY", "Depth: ${config.maxDepth}, Pincer: ${config.pincerTwoOfThreeWeight}")
    }

    fun updateAiConfig(config: AiConfig) {
        _stagedAiConfig.value = config
        _aiConfig.value = config
        AIEngine.currentConfig = config
        supabaseManager.logAdminAction("UPDATE_AI_CONFIG", "Depth: ${config.maxDepth}")
    }

    fun resetAiTelemetry() {
        AIEngine.resetTelemetry()
        _telemetryState.value = emptyMap()
        supabaseManager.logAdminAction("WIPE_TELEMETRY", "local")
    }

    enum class AlertSeverity { CRITICAL, WARNING, INFO }
    data class SystemAlert(
        val id: String = java.util.UUID.randomUUID().toString(),
        val timestamp: Long = System.currentTimeMillis(),
        val severity: AlertSeverity,
        val message: String
    )

    private val _systemAlerts = MutableStateFlow<List<SystemAlert>>(emptyList())
    val systemAlerts: StateFlow<List<SystemAlert>> = _systemAlerts.asStateFlow()

    fun logSystemAlert(severity: AlertSeverity, message: String) {
        _systemAlerts.update { listOf(SystemAlert(severity = severity, message = message)) + it.take(49) }
    }

    fun clearAllAlerts() {
        _systemAlerts.value = emptyList()
    }

    private val _hintMove = MutableStateFlow<Pair<Int?, Int>?>(null)
    val hintMove: StateFlow<Pair<Int?, Int>?> = _hintMove.asStateFlow()

    private val _aiCommentary = MutableStateFlow("Chanakya Bot: Let the clash of wits begin!")
    val aiCommentary: StateFlow<String> = _aiCommentary.asStateFlow()

    private val _showTutorial = MutableStateFlow(false)
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    private val _showRemoteDrawRequest = MutableStateFlow(false)
    val showRemoteDrawRequest: StateFlow<Boolean> = _showRemoteDrawRequest.asStateFlow()

    private val _showRemoteUndoRequest = MutableStateFlow(false)
    val showRemoteUndoRequest: StateFlow<Boolean> = _showRemoteUndoRequest.asStateFlow()

    private val _undoPendingLocal = MutableStateFlow(false)
    val undoPendingLocal: StateFlow<Boolean> = _undoPendingLocal.asStateFlow()

    private val _drawOfferPendingLocal = MutableStateFlow(false)
    val drawOfferPendingLocal: StateFlow<Boolean> = _drawOfferPendingLocal.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null
    private var aiJob: kotlinx.coroutines.Job? = null
    private var tutorialWarningJob: kotlinx.coroutines.Job? = null
    private val _tutorialWarningMessage = MutableStateFlow<String?>(null)
    val tutorialWarningMessage: StateFlow<String?> = _tutorialWarningMessage.asStateFlow()

    private var currentSettings = AppSettings()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect {
                currentSettings = it
            }
        }

        val saved = gameRepository.loadGame()
        if (saved != null && saved.winner == null) {
            _gameState.value = saved
            _aiCommentary.value = "Chanakya Bot: Welcome back! Our duel is waiting."
        }
        startTurnTimer()

        multiplayerManager.onRemoteMoveReceived = { nodeId, fromNodeId, actionType ->
            applyRemoteMove(nodeId, fromNodeId, actionType)
        }
        multiplayerManager.onRemoteDrawReceived = {
            _showRemoteDrawRequest.value = true
        }
        multiplayerManager.onRemoteDrawAccepted = {
            applyRemoteDrawAccepted()
        }
        multiplayerManager.onRemoteForfeitReceived = {
            applyRemoteForfeitReceived()
        }
        multiplayerManager.onRemoteUndoRequested = {
            _showRemoteUndoRequest.value = true
        }
        multiplayerManager.onRemoteUndoAcknowledged = { accepted ->
            _undoPendingLocal.value = false
            if (accepted) {
                undoLastMoveLocally()
            }
        }
        multiplayerManager.onGameStarted = {
            startNewGame(GameMode.ONLINE_MULTIPLAYER, AIDifficulty.MEDIUM)
            
            // Real Scenario: Sync state from DB if joining an ongoing or interrupted match
            val code = multiplayerManager.roomCode.value
            if (code.isNotEmpty()) {
                supabaseManager.fetchMatchDetails(code) { match ->
                    if (match != null && !match.movesJson.isNullOrBlank()) {
                        try {
                            val remoteState = gameStateAdapter.fromJson(match.movesJson)
                            if (remoteState != null) {
                                _gameState.update { current ->
                                    remoteState.copy(
                                        gameMode = current.gameMode,
                                        aiDifficulty = current.aiDifficulty,
                                        connectionStatus = ConnectionStatus.CONNECTED
                                    )
                                }
                                startTurnTimer()
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
        }
        multiplayerManager.onSyncRequested = {
            gameStateAdapter.toJson(_gameState.value)
        }
        multiplayerManager.onSyncReceived = { stateJson ->
            try {
                val remoteState = gameStateAdapter.fromJson(stateJson)
                if (remoteState != null) {
                    _gameState.update { remoteState }
                    startTurnTimer()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        multiplayerManager.onConnectionRestored = {
            _gameState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTED, reconnectionCountdown = null) }
        }
        multiplayerManager.onAbandonRequired = {
            handleOpponentAbandonment()
            clearCurrentMatch()
        }
        multiplayerManager.onSimulatedMoveNeeded = {
            triggerSimulatedOpponentTurn()
        }

        viewModelScope.launch {
            multiplayerManager.reconnecting.collect { isReconnecting ->
                _gameState.update { it.copy(
                    connectionStatus = if (isReconnecting) ConnectionStatus.RECONNECTING else ConnectionStatus.CONNECTED
                ) }
            }
        }
        viewModelScope.launch {
            multiplayerManager.reconnectionCountdown.collect { countdown ->
                _gameState.update { it.copy(reconnectionCountdown = countdown) }
            }
        }

        viewModelScope.launch {
            supabaseManager.systemSettings.collect { settings ->
                val adSetting = settings.find { it.key == "ads_launcher" }
                _adsEnabled.value = adSetting?.value == "on"
            }
        }
    }

    private val _telemetryState = MutableStateFlow<Map<String, Long>>(emptyMap())
    val telemetryState: StateFlow<Map<String, Long>> = _telemetryState.asStateFlow()

    /**
     * STRESS TEST RUNNER: Executes a high-velocity simulation of Bot vs Bot gameplay.
     * Validates logic stability across 100 matches in background.
     */
    fun runStressTest(matches: Int = 100) {
        if (_isAiThinking.value) return // Block if AI is already thinking or testing
        viewModelScope.launch(Dispatchers.Default) {
            _isAiThinking.value = true
            _aiCommentary.value = "SYSTEM: Initiating automated stress test ($matches matches)..."
            var successCount = 0
            var crashCount = 0

            for (i in 1..matches) {
                val ruleSet = if (i % 2 == 0) RuleSet.NINE_MENS_MORRIS else RuleSet.TWELVE_MENS_MORRIS
                val difficulty = AIDifficulty.entries[i % AIDifficulty.entries.size]
                
                startNewGame(GameMode.VS_AI, difficulty, ruleSet)
                var turns = 0
                val startTime = System.currentTimeMillis()

                try {
                    while (_gameState.value.winner == null && _gameState.value.phase != GamePhase.GAME_OVER && turns < 200) {
                        val state = _gameState.value
                        val botTurn = state.currentPlayer
                        
                        // Simulate Bot Move Calculation
                        val aiMove = AIEngine.selectMove(state, difficulty)
                        if (aiMove == null) {
                            com.example.daadi.util.SecureLog.e("STRESS_TEST", "Match $i: AI returned null move at turn $turns")
                            break
                        }

                        // Atomic state update using update block for thread safety
                        _gameState.update { currentState ->
                            if (aiMove.first == null) GameEngine.placePiece(currentState, aiMove.second)
                            else GameEngine.movePiece(currentState, aiMove.first!!, aiMove.second)
                        }

                        if (_gameState.value.isCapturePending) {
                            val target = AIEngine.selectCapture(_gameState.value, if (botTurn == Player.PLAYER_1) Player.PLAYER_2 else Player.PLAYER_1)
                            if (target != null) {
                                _gameState.update { GameEngine.capturePiece(it, target) }
                            }
                        }
                        
                        turns++
                        delay(2) // 2ms hyper-delay for simulation
                    }
                    successCount++
                } catch (e: Exception) {
                    crashCount++
                    com.example.daadi.util.SecureLog.e("STRESS_TEST", "Match $i: Exception detected: ${e.message}")
                }
                
                val duration = System.currentTimeMillis() - startTime
                // Fix memory leak by capping telemetry history to last 5 results
                _telemetryState.update { currentMap ->
                    val newMap = currentMap + ("match_$i" to duration)
                    if (newMap.size > 5) {
                        newMap.entries.sortedByDescending { it.key }.take(5).associate { it.toPair() }
                    } else newMap
                }
            }
            _isAiThinking.value = false
            _aiCommentary.value = "STRESS TEST COMPLETE: Successes: $successCount, Failures: $crashCount"
        }
    }

    fun startNewGame(mode: GameMode, difficulty: AIDifficulty, ruleSet: RuleSet = RuleSet.NINE_MENS_MORRIS) {
        if (_isAiThinking.value) return // --- BOT LOCKDOWN ---
        val newState = GameState(
            gameMode = mode,
            aiDifficulty = difficulty,
            ruleSet = ruleSet,
            currentPlayer = Player.PLAYER_1,
            phase = GamePhase.PLACEMENT,
            player1PiecesInHand = if (ruleSet == RuleSet.TWELVE_MENS_MORRIS) 12 else 9,
            player2PiecesInHand = if (ruleSet == RuleSet.TWELVE_MENS_MORRIS) 12 else 9,
            player1PiecesOnBoard = 0,
            player2PiecesOnBoard = 0,
            winner = null,
            moveHistory = emptyList()
        )
        _gameState.update { newState }
        _selectedNodeId.value = null
        _isAiThinking.value = false
        _showPauseMenu.value = false
        _recentInvalidNode.value = null
        _hintMove.value = null
        _showRemoteDrawRequest.value = false
        _showRemoteUndoRequest.value = false
        _undoPendingLocal.value = false
        _drawOfferPendingLocal.value = false
        
        _aiCommentary.value = when (mode) {
            GameMode.VS_AI -> "Chanakya Bot: I have prepared my strategy. Good luck!"
            GameMode.PASS_AND_PLAY -> "Match Started: Pass & Play Mode"
            GameMode.ONLINE_MULTIPLAYER -> "Battlefield Ready! Syncing gears..."
        }
        gameRepository.clearSavedGame()
        startTurnTimer()
    }

    fun togglePauseMenu() {
        _showPauseMenu.value = !_showPauseMenu.value
        if (_showPauseMenu.value) {
            timerJob?.cancel()
            aiJob?.cancel()
            _isAiThinking.value = false
            _aiCommentary.value = "Game Paused: Strategies on hold."
        } else {
            if (_gameState.value.currentPlayer == Player.PLAYER_1 || _gameState.value.gameMode == GameMode.PASS_AND_PLAY) {
                startTurnTimer()
            }
            if (_gameState.value.gameMode == GameMode.VS_AI && _gameState.value.currentPlayer == Player.PLAYER_2) {
                triggerAiTurn()
            }
            _aiCommentary.value = "Game Resumed: Let the dual continue!"
        }
    }

    fun onAppPaused() {
        _isAppPaused.value = true
        timerJob?.cancel()
        aiJob?.cancel()
        tutorialWarningJob?.cancel()
        soundManager.setBackgroundMuted(true)
        _isAiThinking.value = false
    }

    fun onAppResumed() {
        _isAppPaused.value = false
        soundManager.setBackgroundMuted(false)
        if (_showTutorial.value) {
            startTutorialWarningTimer()
        } else if (!_showPauseMenu.value && _gameState.value.winner == null) {
            startTurnTimer()
            if (_gameState.value.gameMode == GameMode.VS_AI && _gameState.value.currentPlayer == Player.PLAYER_2) {
                triggerAiTurn()
            }
        }
    }

    fun tapNode(nodeId: Int) {
        if (_isAiThinking.value) return
        _hintMove.value = null
        
        val currentState = _gameState.value
        val localRole = if (currentState.gameMode == GameMode.ONLINE_MULTIPLAYER) multiplayerManager.localRole.value else Player.PLAYER_1
        
        if (currentState.gameMode == GameMode.ONLINE_MULTIPLAYER && currentState.currentPlayer != localRole) return
        if (currentState.winner != null) return

        if (currentState.isCapturePending) {
            val opponent = GameEngine.getNextPlayer(currentState.currentPlayer)
            if (currentState.board.nodes[nodeId] == opponent) {
                if (MillDetector.isPieceCapturable(currentState.board, nodeId, opponent, currentState.ruleSet)) {
                    executeCapture(nodeId)
                } else {
                    flashInvalidNode(nodeId)
                }
            } else {
                flashInvalidNode(nodeId)
            }
        } else {
            val isPlacement = if (currentState.currentPlayer == Player.PLAYER_1) currentState.player1PiecesInHand > 0 else currentState.player2PiecesInHand > 0
            if (isPlacement) {
                if (currentState.board.nodes[nodeId] == null) {
                    val nextState = GameEngine.placePiece(currentState, nodeId)
                    if (nextState != currentState) {
                        if (nextState.isCapturePending) {
                            soundManager.playMillFormed()
                            onPlaySound?.invoke(SoundEvent.MILL)
                            onPerformHaptic?.invoke(HapticPattern.MILL)
                        } else {
                            soundManager.playPlace()
                            onPlaySound?.invoke(SoundEvent.PLACE)
                            onPerformHaptic?.invoke(HapticPattern.TICK)
                        }
                        updateStateAndTriggerAI(nextState)
                        if (currentState.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                            multiplayerManager.sendMove(nodeId, null, "PLACE")
                        }
                    }
                } else {
                    flashInvalidNode(nodeId)
                }
            } else {
                val currentSelection = _selectedNodeId.value
                val nodeOwner = currentState.board.nodes[nodeId]
                if (nodeId == currentSelection) {
                    _selectedNodeId.value = null
                } else if (nodeOwner == currentState.currentPlayer) {
                    _selectedNodeId.value = nodeId
                } else if (currentSelection != null && nodeOwner == null) {
                    val nextState = GameEngine.movePiece(currentState, currentSelection, nodeId)
                    if (nextState != currentState) {
                        _selectedNodeId.value = null
                        if (nextState.isCapturePending) {
                            soundManager.playMillFormed()
                            onPlaySound?.invoke(SoundEvent.MILL)
                            onPerformHaptic?.invoke(HapticPattern.MILL)
                        } else {
                            soundManager.playMove()
                            onPlaySound?.invoke(SoundEvent.PLACE)
                            onPerformHaptic?.invoke(HapticPattern.TICK)
                        }
                        updateStateAndTriggerAI(nextState)
                        if (currentState.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                            multiplayerManager.sendMove(nodeId, currentSelection, "MOVE")
                        }
                    }
                } else {
                    flashInvalidNode(nodeId)
                }
            }
        }
    }

    private fun executeCapture(nodeId: Int) {
        val state = _gameState.value
        val nextState = GameEngine.capturePiece(state, nodeId)
        if (nextState != state) {
            soundManager.playCapture()
            onPerformHaptic?.invoke(HapticPattern.LOSS)
            updateStateAndTriggerAI(nextState)
            if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                multiplayerManager.sendMove(nodeId, null, "CAPTURE")
            }
        }
    }

    private fun flashInvalidNode(nodeId: Int) {
        _recentInvalidNode.value = nodeId
        soundManager.playError()
        viewModelScope.launch {
            delay(300)
            if (_recentInvalidNode.value == nodeId) {
                _recentInvalidNode.value = null
            }
        }
    }

    private fun updateStateAndTriggerAI(nextState: GameState) {
        _gameState.update { nextState }
        _selectedNodeId.value = null
        updateAiCommentary(nextState)
        startTurnTimer()

        if (nextState.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            val code = multiplayerManager.roomCode.value
            if (code.isNotEmpty()) {
                val movesJson = gameStateAdapter.toJson(nextState)
                supabaseManager.updateMatchMoves(code, movesJson)
            }
        }

        if (nextState.winner != null || nextState.phase == GamePhase.GAME_OVER) {
            timerJob?.cancel()
            incrementMatchAdCounter() // Tracking match completion for fatigue control
            if (nextState.winner == Player.PLAYER_1) {
                soundManager.playWin()
                onPlaySound?.invoke(SoundEvent.WIN)
            } else if (nextState.winner == Player.PLAYER_2 && nextState.gameMode == GameMode.VS_AI) {
                soundManager.playLose()
                onPlaySound?.invoke(SoundEvent.LOSE)
            } else {
                soundManager.playWin()
                onPlaySound?.invoke(SoundEvent.WIN)
            }
            statsRepository.updateStats(nextState.winner, nextState.gameMode, nextState.aiDifficulty)
            if (nextState.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                checkAndRecordOnlineMatchFinished(nextState)
            }
            gameRepository.clearSavedGame()
            return
        }

        if (nextState.gameMode == GameMode.VS_AI && nextState.currentPlayer == Player.PLAYER_2) {
            triggerAiTurn()
        }
    }

    private fun triggerAiTurn() {
        if (_isAiThinking.value || _showTutorial.value || _showPauseMenu.value || _isAppPaused.value) return
        _isAiThinking.value = true

        val oldAiJob = aiJob
        aiJob = viewModelScope.launch {
            try {
                oldAiJob?.cancel()
                oldAiJob?.join()
            } catch (e: Exception) {}
            
            val aiDelay = if (currentSettings.fastAnimations) 500L else 1200L
            delay(aiDelay)

            val currentState = _gameState.value
            if (currentState.winner != null || currentState.currentPlayer != Player.PLAYER_2 || _showTutorial.value || _showPauseMenu.value) {
                _isAiThinking.value = false
                return@launch
            }

            var nextState = currentState
            if (currentState.isCapturePending) {
                val captureTarget = withContext(Dispatchers.Default) {
                    AIEngine.selectCapture(currentState, Player.PLAYER_1)
                }
                if (captureTarget != null) {
                    nextState = GameEngine.capturePiece(currentState, captureTarget)
                }
            } else {
                val aiMove = withContext(Dispatchers.Default) {
                    AIEngine.selectMove(currentState, currentState.aiDifficulty)
                }
                if (aiMove != null) {
                    nextState = if (aiMove.first == null) {
                        GameEngine.placePiece(currentState, aiMove.second)
                    } else {
                        GameEngine.movePiece(currentState, aiMove.first!!, aiMove.second)
                    }

                    if (nextState.isCapturePending) {
                        soundManager.playMillFormed()
                        onPlaySound?.invoke(SoundEvent.MILL)
                        val captureTarget = withContext(Dispatchers.Default) {
                            AIEngine.selectCapture(nextState, Player.PLAYER_1)
                        }
                        if (captureTarget != null) {
                            delay(400)
                            nextState = GameEngine.capturePiece(nextState, captureTarget)
                            soundManager.playCapture()
                        }
                    } else {
                        if (aiMove.first == null) {
                            soundManager.playPlace()
                            onPlaySound?.invoke(SoundEvent.PLACE)
                        } else {
                            soundManager.playMove()
                            onPlaySound?.invoke(SoundEvent.PLACE)
                        }
                    }
                }
            }

            _isAiThinking.value = false
            updateStateAndTriggerAI(nextState)
        }
    }

    private fun triggerSimulatedOpponentTurn() {
        if (_isAiThinking.value || _gameState.value.winner != null) return
        _isAiThinking.value = true
        viewModelScope.launch {
            val state = _gameState.value
            val difficulty = multiplayerManager.simulatorDifficulty
            
            if (state.isCapturePending) {
                val captureTarget = withContext(Dispatchers.Default) {
                    AIEngine.selectCapture(state, multiplayerManager.localRole.value)
                }
                if (captureTarget != null) {
                    _isAiThinking.value = false
                    multiplayerManager.onRemoteMoveReceived?.invoke(captureTarget, null, "CAPTURE")
                } else {
                    _isAiThinking.value = false
                }
            } else {
                val aiMove = withContext(Dispatchers.Default) {
                    AIEngine.selectMove(state, difficulty)
                }
                if (aiMove != null) {
                    _isAiThinking.value = false
                    val fromNode = aiMove.first
                    val toNode = aiMove.second
                    val actionType = if (fromNode == null) "PLACE" else "MOVE"
                    multiplayerManager.onRemoteMoveReceived?.invoke(toNode, fromNode, actionType)
                } else {
                    _isAiThinking.value = false
                }
            }
        }
    }

    fun clearCurrentMatch() {
        if (_isAiThinking.value) return // --- BOT LOCKDOWN ---
        _gameState.update { GameState() }
        _selectedNodeId.value = null
        _isAiThinking.value = false
        _hintMove.value = null
        _undoPendingLocal.value = false
        _drawOfferPendingLocal.value = false
        _showPauseMenu.value = false
        _showRemoteDrawRequest.value = false
        _showRemoteUndoRequest.value = false
        timerJob?.cancel()
        aiJob?.cancel()
        multiplayerManager.disconnect()
        gameRepository.clearSavedGame()
    }

    fun discardSavedGame() {
        clearCurrentMatch()
    }

    fun undoLastMove() {
        if (_isAiThinking.value) return // --- BOT LOCKDOWN ---
        val state = _gameState.value
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            _undoPendingLocal.value = true
            multiplayerManager.sendUndoRequest()
            return
        }
        val history = state.moveHistory
        if (history.isEmpty()) return
        var targetIndex = history.size - 1
        if (state.gameMode == GameMode.VS_AI) {
            val lastP1MoveIndex = history.indexOfLast { it.player == Player.PLAYER_1 }
            if (lastP1MoveIndex == -1) return
            targetIndex = lastP1MoveIndex
        }
        replayHistoryToIndex(targetIndex)
        _aiCommentary.value = "Chanakya Bot: Reverted state! Think wisely on this retry."
    }

    fun undoLastMoveLocally() {
        val state = _gameState.value
        val history = state.moveHistory
        if (history.isEmpty()) return
        replayHistoryToIndex(history.size - 1)
        _aiCommentary.value = "Match state and moves reverted!"
    }

    private fun replayHistoryToIndex(targetIndex: Int) {
        val state = _gameState.value
        val history = state.moveHistory
        val historyToReplay = history.take(targetIndex)
        var tempState = GameState(
            gameMode = state.gameMode,
            aiDifficulty = state.aiDifficulty,
            ruleSet = state.ruleSet,
            currentPlayer = Player.PLAYER_1,
            phase = GamePhase.PLACEMENT,
            player1PiecesInHand = if (state.ruleSet == RuleSet.TWELVE_MENS_MORRIS) 12 else 9,
            player2PiecesInHand = if (state.ruleSet == RuleSet.TWELVE_MENS_MORRIS) 12 else 9,
            player1PiecesOnBoard = 0,
            player2PiecesOnBoard = 0,
            winner = null
        )
        for (move in historyToReplay) {
            tempState = when (move.type) {
                MoveType.PLACE -> {
                    val s = GameEngine.placePiece(tempState, move.toNode)
                    if (s.isCapturePending && move.capturedNode != null) GameEngine.capturePiece(s, move.capturedNode) else s
                }
                MoveType.MOVE -> {
                    val s = GameEngine.movePiece(tempState, move.fromNode!!, move.toNode)
                    if (s.isCapturePending && move.capturedNode != null) GameEngine.capturePiece(s, move.capturedNode) else s
                }
                MoveType.CAPTURE -> tempState
            }
        }
        _gameState.update { tempState }
        _selectedNodeId.value = null
        _isAiThinking.value = false
        startTurnTimer()
    }

    private fun applyRemoteMove(nodeId: Int, fromNodeId: Int?, actionType: String) {
        _gameState.update { state ->
            val opponentRole = GameEngine.getNextPlayer(multiplayerManager.localRole.value)
            if (state.currentPlayer != opponentRole) {
                logSystemAlert(
                    AlertSeverity.WARNING,
                    "Security: Intercepted out-of-turn play attempt from opponent (${multiplayerManager.opponentPlayerName.value}). Packet discarded."
                )
                supabaseManager.logAntiCheatViolation(
                    matchId = multiplayerManager.roomCode.value,
                    violationType = "OUT_OF_TURN_PLAY",
                    severity = "high",
                    details = "Current: ${state.currentPlayer}, Opponent: $opponentRole"
                )
                // Force sync immediately to correct remote client state
                viewModelScope.launch {
                    multiplayerManager.sendSyncRequest()
                }
                return@update state
            }
            
            val nextState = when (actionType) {
                "PLACE" -> GameEngine.placePiece(state, nodeId)
                "MOVE" -> if (fromNodeId != null) GameEngine.movePiece(state, fromNodeId, nodeId) else state
                "CAPTURE" -> GameEngine.capturePiece(state, nodeId)
                else -> state
            }
            
            if (nextState == state) {
                // Illegal move attempted or fake board state detected!
                logSystemAlert(
                    AlertSeverity.CRITICAL,
                    "Security Alert: Intercepted illegal board manipulation (${actionType}) from remote opponent at node $nodeId! Syncing board."
                )
                supabaseManager.logAntiCheatViolation(
                    matchId = multiplayerManager.roomCode.value,
                    violationType = "ILLEGAL_MOVE",
                    severity = "critical",
                    details = "Action: $actionType, Node: $nodeId, From: $fromNodeId"
                )
                // Force remote client to sync and conform to validated local board state
                viewModelScope.launch {
                    multiplayerManager.sendSyncRequest()
                }
            } else {
                 viewModelScope.launch {
                    if (nextState.isCapturePending) soundManager.playMillFormed()
                    else when (actionType) {
                        "PLACE" -> soundManager.playPlace()
                        "MOVE" -> soundManager.playMove()
                        "CAPTURE" -> soundManager.playCapture()
                    }
                    startTurnTimer()
                 }
            }
            nextState
        }
    }

    private fun applyRemoteDrawAccepted() {
        _gameState.update { it.copy(winner = null, phase = GamePhase.GAME_OVER) }
        soundManager.playWin()
        _aiCommentary.value = "Match ended: Both accepted a Handshake Draw!"
        checkAndRecordOnlineMatchFinished(_gameState.value)
        gameRepository.clearSavedGame()
    }

    private fun applyRemoteForfeitReceived() {
        _gameState.update { it.copy(winner = multiplayerManager.localRole.value, phase = GamePhase.GAME_OVER) }
        soundManager.playWin()
        _aiCommentary.value = "Match ended: Opponent surrendered! You win!"
        checkAndRecordOnlineMatchFinished(_gameState.value)
        gameRepository.clearSavedGame()
    }

    private fun handleOpponentAbandonment() {
        val state = _gameState.value
        if (state.winner != null || state.phase == GamePhase.GAME_OVER) return
        
        // Prevent disconnect abuse: reward the local active player with the win by forfeit
        val localRole = multiplayerManager.localRole.value
        val updatedState = state.copy(winner = localRole, phase = GamePhase.GAME_OVER)
        
        logSystemAlert(
            AlertSeverity.WARNING,
            "Security: Opponent disconnected/abandoned. Handled opponent disconnect abuse. Win awarded to active player."
        )
        statsRepository.updateStats(localRole, state.gameMode, state.aiDifficulty)
        _gameState.update { updatedState }
        
        checkAndRecordOnlineMatchFinished(updatedState)
        gameRepository.clearSavedGame()
    }

    fun respondToRemoteDraw(accept: Boolean) {
        _showRemoteDrawRequest.value = false
        if (accept) {
            multiplayerManager.sendDrawAccept()
            applyRemoteDrawAccepted()
        }
    }

    fun respondToRemoteUndo(accept: Boolean) {
        _showRemoteUndoRequest.value = false
        multiplayerManager.sendUndoResponse(accept)
        if (accept) undoLastMoveLocally()
    }

    private fun startTurnTimer() {
        timerJob?.cancel()
        _turnTimeSeconds.value = 30
        val state = _gameState.value
        if (state.winner != null || state.phase == GamePhase.GAME_OVER || _showTutorial.value || _showPauseMenu.value || _isAppPaused.value) return
        if (state.gameMode == GameMode.VS_AI && state.currentPlayer == Player.PLAYER_2) return
        val localRole = if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) multiplayerManager.localRole.value else Player.PLAYER_1
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER && state.currentPlayer != localRole) return
        timerJob = viewModelScope.launch {
            while (_turnTimeSeconds.value > 0) {
                delay(1000)
                _turnTimeSeconds.value -= 1
                if (_turnTimeSeconds.value in 1..3) soundManager.playCountdownTick()
            }
            if (_turnTimeSeconds.value == 0) executeAutoTimedTurn()
        }
    }

    private fun executeAutoTimedTurn() {
        val state = _gameState.value
        if (state.winner != null || state.phase == GamePhase.GAME_OVER) return
        
        val timedOutPlayer = state.currentPlayer
        val opponent = GameEngine.getNextPlayer(timedOutPlayer)
        
        val currentChances = if (timedOutPlayer == Player.PLAYER_1) state.player1TimerChances else state.player2TimerChances
        
        if (currentChances > 1) {
            val updatedState = if (timedOutPlayer == Player.PLAYER_1) {
                state.copy(player1TimerChances = currentChances - 1)
            } else {
                state.copy(player2TimerChances = currentChances - 1)
            }
            _gameState.update { updatedState }
            soundManager.playError()
            
            val chancesLeft = currentChances - 1
            val chancesWord = if (chancesLeft == 1) "chance" else "chances"
            
            _aiCommentary.value = when (state.gameMode) {
                GameMode.VS_AI -> {
                    "Chanakya Bot: Warning! You ran out of time. $chancesLeft $chancesWord remaining. Resetting timer..."
                }
                GameMode.PASS_AND_PLAY -> {
                    val playerLabel = if (timedOutPlayer == Player.PLAYER_1) "Player 1 (Red)" else "Player 2 (Blue)"
                    "Warning! $playerLabel ran out of time. $chancesLeft $chancesWord remaining. Resetting timer..."
                }
                GameMode.ONLINE_MULTIPLAYER -> {
                    "Warning! You ran out of time. $chancesLeft $chancesWord remaining. Resetting timer..."
                }
            }
            startTurnTimer()
            return
        }
        
        // No chances left -> forfeit/loss by timeout
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            val localRole = multiplayerManager.localRole.value
            if (timedOutPlayer != localRole) return
            
            multiplayerManager.sendForfeit()
            val updatedState = state.copy(winner = opponent, phase = GamePhase.GAME_OVER)
            soundManager.playLose()
            statsRepository.updateStats(opponent, state.gameMode, state.aiDifficulty)
            _gameState.update { updatedState }
            _aiCommentary.value = "Match ended: You ran out of timer chances and lost by timeout!"
            checkAndRecordOnlineMatchFinished(updatedState)
            gameRepository.clearSavedGame()
        } else if (state.gameMode == GameMode.VS_AI) {
            if (timedOutPlayer == Player.PLAYER_1) {
                val updatedState = state.copy(winner = Player.PLAYER_2, phase = GamePhase.GAME_OVER)
                soundManager.playLose()
                statsRepository.updateStats(Player.PLAYER_2, state.gameMode, state.aiDifficulty)
                _gameState.update { updatedState }
                _aiCommentary.value = "Chanakya Bot: Time's up! All chances exhausted. Slow strategy leads to defeat."
                gameRepository.clearSavedGame()
            }
        } else if (state.gameMode == GameMode.PASS_AND_PLAY) {
            val winnerPlayer = opponent
            val updatedState = state.copy(winner = winnerPlayer, phase = GamePhase.GAME_OVER)
            soundManager.playLose()
            statsRepository.updateStats(winnerPlayer, state.gameMode, state.aiDifficulty)
            _gameState.update { updatedState }
            
            val timedOutName = if (timedOutPlayer == Player.PLAYER_1) "Player 1" else "Player 2"
            val winnerName = if (winnerPlayer == Player.PLAYER_1) "Player 1" else "Player 2"
            _aiCommentary.value = "Time's up! $timedOutName ran out of timer chances and lost. $winnerName wins!"
            gameRepository.clearSavedGame()
        }
    }

    private fun updateAiCommentary(nextState: GameState) {
        val commentary = when {
            nextState.winner == Player.PLAYER_2 -> "Chanakya Bot: Victory is mine! Practice more to rival Chanakya!"
            nextState.winner == Player.PLAYER_1 -> "Chanakya Bot: Outstanding move! You possess the mind of a true King."
            nextState.isCapturePending && nextState.currentPlayer == Player.PLAYER_2 -> "Chanakya Bot: DAADI! Your bead is mine! Sprung a clever trap..."
            nextState.isCapturePending && nextState.currentPlayer == Player.PLAYER_1 -> "Chanakya Bot: Ah, a well deserved capture! Nicely blocked."
            nextState.phase == GamePhase.PLACEMENT -> listOf("Chanakya Bot: Laying foundations bead by bead...", "Chanakya Bot: Watch your sides, do not invite a line!", "Chanakya Bot: A steady placement establishes control.", "Chanakya Bot: Let's see if you can defend these nine seeds.").random()
            nextState.phase == GamePhase.MOVEMENT -> listOf("Chanakya Bot: The board shifts! Slide carefully.", "Chanakya Bot: A strategic pincer of nodes is forming.", "Chanakya Bot: Can you slip past my defensive guard?", "Chanakya Bot: Each slide must serve our long battle.").random()
            nextState.phase == GamePhase.FLYING -> listOf("Chanakya Bot: Jumping active! Beads fly freely now!", "Chanakya Bot: Three beads left, but the wings of strategy are wide.", "Chanakya Bot: Complete freedom! Select any destination spot.").random()
            else -> "Chanakya Bot: The clash of wits continues..."
        }
        _aiCommentary.value = commentary
    }

    fun computeHint() {
        val state = _gameState.value
        if (state.winner != null || state.phase == GamePhase.GAME_OVER || _isAiThinking.value) return // --- BOT LOCKDOWN ---
        val hint = AIEngine.getHint(state)
        if (hint != null) {
            _hintMove.value = hint
            soundManager.playHint()
            val moveDesc = if (state.isCapturePending) "Suggested: Remove opponent piece at spot ${hint.second + 1}" else if (hint.first == null) "Suggested: Place a piece at node ${hint.second + 1}" else "Suggested: Move piece from ${hint.first!! + 1} to ${hint.second + 1}"
            _aiCommentary.value = "Chanakya Bot: $moveDesc"
        }
    }

    fun reportOpponent(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val opponentName = multiplayerManager.opponentPlayerName.value
        if (opponentName.isNotBlank()) {
            supabaseManager.reportUserByName(opponentName, "In-game behavior violation / online match report") { success ->
                if (success) {
                    _aiCommentary.value = "Report submitted. We take fair play seriously!"
                    onResult(true, "Successfully reported $opponentName.")
                } else {
                    onResult(false, "Failed to submit report. Please try again.")
                }
            }
        } else {
            onResult(false, "No opponent found to report.")
        }
    }

    fun toggleTutorial(visible: Boolean) {
        _showTutorial.value = visible
        if (visible) {
            timerJob?.cancel()
            startTutorialWarningTimer()
        } else {
            _tutorialWarningMessage.value = null
            tutorialWarningJob?.cancel()
            startTurnTimer()
            if (_gameState.value.currentPlayer == Player.PLAYER_2 && _gameState.value.gameMode == GameMode.VS_AI) triggerAiTurn()
        }
    }

    private fun startTutorialWarningTimer() {
        tutorialWarningJob?.cancel()
        tutorialWarningJob = viewModelScope.launch {
            delay(10000)
            var countdown = 10
            while (countdown > 0) {
                _tutorialWarningMessage.value = "Match begins in $countdown seconds... Close guide to play!"
                delay(1000)
                countdown--
            }
            toggleTutorial(false)
        }
    }

    fun resignGame() {
        val state = _gameState.value
        if (state.winner != null) return
        val opponent = if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            multiplayerManager.sendForfeit()
            GameEngine.getNextPlayer(multiplayerManager.localRole.value)
        } else Player.PLAYER_2
        val updatedState = state.copy(winner = opponent, phase = GamePhase.GAME_OVER)
        soundManager.playLose()
        statsRepository.updateStats(opponent, state.gameMode, state.aiDifficulty)
        _gameState.update { updatedState }
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) checkAndRecordOnlineMatchFinished(updatedState)
        gameRepository.clearSavedGame()
    }

    fun offerDrawGame() {
        val state = _gameState.value
        if (state.winner != null || state.phase == GamePhase.GAME_OVER) return
        if (state.moveHistory.size < 16) {
            _aiCommentary.value = "Too early for a draw! Let the battle progress."
            return
        }
        if (state.gameMode == GameMode.VS_AI) {
            val movesSinceCapture = state.moveHistory.takeLastWhile { it.capturedNode == null }.size
            if (movesSinceCapture > 30 || state.moveHistory.size > 100) finalizeDraw(state)
            else _aiCommentary.value = "Chanakya Bot: I believe I can still win. Let us proceed!"
            return
        }
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            _drawOfferPendingLocal.value = true
            multiplayerManager.sendDrawOffer()
            _aiCommentary.value = "Draw offer transmitted to your opponent..."
            return
        }
        finalizeDraw(state)
    }

    private fun finalizeDraw(state: GameState) {
        val updatedState = state.copy(winner = null, phase = GamePhase.GAME_OVER)
        soundManager.playPlace()
        statsRepository.updateStats(null, state.gameMode, state.aiDifficulty)
        _gameState.update { updatedState }
        _aiCommentary.value = "Match ended in a Handshake Draw!"
        gameRepository.clearSavedGame()
    }

    private fun checkAndRecordOnlineMatchFinished(nextState: GameState) {
        if (nextState.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            val hostName = if (multiplayerManager.isHost.value) multiplayerManager.localPlayerName.value else multiplayerManager.opponentPlayerName.value
            val opponentName = if (!multiplayerManager.isHost.value) multiplayerManager.localPlayerName.value else multiplayerManager.opponentPlayerName.value
            val winnerName = when (nextState.winner) {
                Player.PLAYER_1 -> hostName
                Player.PLAYER_2 -> opponentName
                else -> null
            }
            supabaseManager.registerMatchResult(multiplayerManager.roomCode.value, hostName, opponentName, winnerName, nextState.moveHistory.size)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

class StatsViewModel(private val statsRepository: StatsRepository) : ViewModel() {
    val stats: StateFlow<PlayerStats> = statsRepository.statsFlow
    fun resetStats() { statsRepository.resetStats() }
}

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
    fun updateSettings(settings: AppSettings) { settingsRepository.saveSettings(settings) }
}

class ViewModelFactory(private val application: DaadiApplication) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(GameViewModel::class.java) -> GameViewModel(application.gameRepository, application.statsRepository, application.settingsRepository, application.soundManager, application.multiplayerManager, application.supabaseManager)
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(application.statsRepository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(application.settingsRepository)
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}
