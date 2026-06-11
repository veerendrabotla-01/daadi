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
import com.example.daadi.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(
    private val gameRepository: GameRepository,
    private val statsRepository: StatsRepository,
    private val settingsRepository: SettingsRepository,
    private val soundManager: SoundManager,
    val multiplayerManager: MultiplayerManager,
    val supabaseManager: com.example.daadi.data.supabase.SupabaseManager
) : ViewModel() {

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

    private val _hintNodeId = MutableStateFlow<Int?>(null)
    val hintNodeId: StateFlow<Int?> = _hintNodeId.asStateFlow()

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

    init {
        // Try restoring saved game initially if it exists
        val saved = gameRepository.loadGame()
        if (saved != null && saved.winner == null) {
            _gameState.value = saved
            _aiCommentary.value = "Chanakya Bot: Welcome back! Our duel is waiting."
        }
        startTurnTimer()

        // Wire up real-time multiplayer callbacks
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
        }
    }

    /**
     * Start a completely new game with mode and difficulty.
     */
    fun startNewGame(mode: GameMode, difficulty: AIDifficulty) {
        _gameState.value = GameState(
            gameMode = mode,
            aiDifficulty = difficulty,
            currentPlayer = Player.PLAYER_1,
            phase = GamePhase.PLACEMENT,
            player1PiecesInHand = 9,
            player2PiecesInHand = 9,
            player1PiecesOnBoard = 0,
            player2PiecesOnBoard = 0,
            winner = null,
            moveHistory = emptyList()
        )
        _selectedNodeId.value = null
        _isAiThinking.value = false
        _showPauseMenu.value = false
        _recentInvalidNode.value = null
        _hintNodeId.value = null
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
    }

    /**
     * Tapping on a specific board node.
     */
    fun tapNode(nodeId: Int) {
        if (_isAiThinking.value) return // Block input during AI thinking
        _hintNodeId.value = null
        val state = _gameState.value
        if (state.winner != null) return

        val localRole = if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            multiplayerManager.localRole.value
        } else {
            Player.PLAYER_1
        }
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER && state.currentPlayer != localRole) {
            return // Block turn if it belongs to remote player
        }

        if (state.isCapturePending) {
            // -- Capture Phase logic --
            val opponent = GameEngine.getNextPlayer(state.currentPlayer)
            if (state.board.nodes[nodeId] == opponent) {
                if (MillDetector.isPieceCapturable(state.board, nodeId, opponent)) {
                    executeCapture(nodeId)
                } else {
                    flashInvalidNode(nodeId)
                }
            } else {
                flashInvalidNode(nodeId)
            }
        } else {
            // -- Placement or Movement/Flying Phase logic --
            val isPlacement = if (state.currentPlayer == Player.PLAYER_1) {
                state.player1PiecesInHand > 0
            } else {
                state.player2PiecesInHand > 0
            }

            if (isPlacement) {
                // Place state piece
                if (state.board.nodes[nodeId] == null) {
                    val nextState = GameEngine.placePiece(state, nodeId)
                    if (nextState != state) {
                        if (nextState.isCapturePending) {
                            soundManager.playMillFormed()
                        } else {
                            soundManager.playPlace()
                        }
                        updateStateAndTriggerAI(nextState)
                        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                            multiplayerManager.sendMove(nodeId, null, "PLACE")
                        }
                    }
                } else {
                    flashInvalidNode(nodeId)
                }
            } else {
                // Move state piece
                val currentSelection = _selectedNodeId.value
                val nodeOwner = state.board.nodes[nodeId]

                if (nodeId == currentSelection) {
                    // Deselect piece
                    _selectedNodeId.value = null
                } else if (nodeOwner == state.currentPlayer) {
                    // Selecting/switching pieces
                    _selectedNodeId.value = nodeId
                } else if (currentSelection != null && nodeOwner == null) {
                    // Execute movement
                    val nextState = GameEngine.movePiece(state, currentSelection, nodeId)
                    if (nextState != state) {
                        _selectedNodeId.value = null
                        if (nextState.isCapturePending) {
                            soundManager.playMillFormed()
                        } else {
                            soundManager.playMove()
                        }
                        updateStateAndTriggerAI(nextState)
                        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                            multiplayerManager.sendMove(nodeId, currentSelection, "MOVE")
                        }
                    } else {
                        flashInvalidNode(nodeId)
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
            updateStateAndTriggerAI(nextState)
            if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                multiplayerManager.sendMove(nodeId, null, "CAPTURE")
            }
        }
    }

    private fun flashInvalidNode(nodeId: Int) {
        _recentInvalidNode.value = nodeId
        viewModelScope.launch {
            delay(300)
            if (_recentInvalidNode.value == nodeId) {
                _recentInvalidNode.value = null
            }
        }
    }

    private fun updateStateAndTriggerAI(nextState: GameState) {
        _gameState.value = nextState
        gameRepository.saveGame(nextState)

        // Generate customized bot personality comment
        updateAiCommentary(nextState)

        // Standardize and reboot turn countdown
        startTurnTimer()

        // If game is over, save stats
        if (nextState.winner != null || nextState.phase == GamePhase.GAME_OVER) {
            timerJob?.cancel()
            if (nextState.winner == Player.PLAYER_1) {
                soundManager.playWin()
            } else if (nextState.winner == Player.PLAYER_2 && nextState.gameMode == GameMode.VS_AI) {
                soundManager.playLose()
            } else {
                soundManager.playWin() // Pass & Play ends in win
            }
            statsRepository.updateStats(
                nextState.winner,
                nextState.gameMode,
                nextState.aiDifficulty
            )
            if (nextState.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                checkAndRecordOnlineMatchFinished(nextState)
            }
            gameRepository.clearSavedGame()
            return
        }

        // Trigger AI turn if it is VS_AI and AI's turn
        if (nextState.gameMode == GameMode.VS_AI && nextState.currentPlayer == Player.PLAYER_2) {
            triggerAiTurn()
        }
    }

    private fun triggerAiTurn() {
        if (_isAiThinking.value) return
        _isAiThinking.value = true

        viewModelScope.launch {
            // Simulate brief strategic delay so AI doesn't feel instantaneous/glitchy
            delay(600)

            val currentState = _gameState.value
            if (currentState.winner != null || currentState.currentPlayer != Player.PLAYER_2) {
                _isAiThinking.value = false
                return@launch
            }

            var nextState = currentState

            // If a capture is pending, handle capture
            if (currentState.isCapturePending) {
                val captureTarget = withContext(Dispatchers.Default) {
                    AIEngine.selectCapture(currentState, Player.PLAYER_1)
                }
                if (captureTarget != null) {
                    nextState = GameEngine.capturePiece(currentState, captureTarget)
                }
            } else {
                // Regular placement or movement
                val aiMove = withContext(Dispatchers.Default) {
                    AIEngine.selectMove(currentState, currentState.aiDifficulty)
                }

                if (aiMove != null) {
                    nextState = if (aiMove.first == null) {
                        GameEngine.placePiece(currentState, aiMove.second)
                    } else {
                        GameEngine.movePiece(currentState, aiMove.first!!, aiMove.second)
                    }

                    // Handles double turn if AIforms a mill
                    if (nextState.isCapturePending) {
                        soundManager.playMillFormed()
                        // Immediate strategic calculation for what AI wants to capture
                        val captureTarget = withContext(Dispatchers.Default) {
                            AIEngine.selectCapture(nextState, Player.PLAYER_1)
                        }
                        if (captureTarget != null) {
                            delay(400) // Small visual delay to show what it is about to capture
                            nextState = GameEngine.capturePiece(nextState, captureTarget)
                            soundManager.playCapture()
                        }
                    } else {
                        if (aiMove.first == null) {
                            soundManager.playPlace()
                        } else {
                            soundManager.playMove()
                        }
                    }
                }
            }

            _isAiThinking.value = false
            updateStateAndTriggerAI(nextState)
        }
    }

    /**
     * Reset or discard saves
     */
    fun discardSavedGame() {
        gameRepository.clearSavedGame()
    }

    /**
     * Rewarded benefit: undo last turn.
     * In VS_AI: Undoes BOTH the AI's move and the player's move so they can try again.
     * In Pass & Play: Undoes 1 move.
     * In online multiplayer: sends an undo request to the opponent.
     */
    fun undoLastMove() {
        val state = _gameState.value
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            _undoPendingLocal.value = true
            multiplayerManager.sendUndoRequest()
            return
        }

        val history = state.moveHistory
        if (history.isEmpty()) return

        var targetGameMoveIndex = history.size - 1

        if (state.gameMode == GameMode.VS_AI) {
            // Need to roll back until it is PLAYER_1's turn again
            // So we find the last action that belonged to PLAYER_1 and undo from there.
            val lastP1MoveIndex = history.indexOfLast { it.player == Player.PLAYER_1 }
            if (lastP1MoveIndex == -1) return
            targetGameMoveIndex = lastP1MoveIndex
        }

        // Recreate the game state from scratch by replaying history up to targetGameMoveIndex (exclusive)
        replayHistoryToIndex(targetGameMoveIndex)
        _aiCommentary.value = "Chanakya Bot: Reverted state! Think wisely on this retry."
    }

    fun undoLastMoveLocally() {
        val state = _gameState.value
        val history = state.moveHistory
        if (history.isEmpty()) return
        
        // In online multiplayer, rollback exactly the last move
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
            currentPlayer = Player.PLAYER_1,
            phase = GamePhase.PLACEMENT,
            player1PiecesInHand = 9,
            player2PiecesInHand = 9,
            player1PiecesOnBoard = 0,
            player2PiecesOnBoard = 0,
            winner = null
        )

        for (move in historyToReplay) {
            tempState = when (move.type) {
                MoveType.PLACE -> {
                    val s = GameEngine.placePiece(tempState, move.toNode)
                    if (s.isCapturePending && move.capturedNode != null) {
                        GameEngine.capturePiece(s, move.capturedNode)
                    } else {
                        s
                    }
                }
                MoveType.MOVE -> {
                    val s = GameEngine.movePiece(tempState, move.fromNode!!, move.toNode)
                    if (s.isCapturePending && move.capturedNode != null) {
                        GameEngine.capturePiece(s, move.capturedNode)
                    } else {
                        s
                    }
                }
                MoveType.CAPTURE -> {
                    tempState // Already handled inside PLACE/MOVE
                }
            }
        }

        _gameState.value = tempState
        _selectedNodeId.value = null
        _isAiThinking.value = false
        gameRepository.saveGame(tempState)
        startTurnTimer()
    }

    private fun applyRemoteMove(nodeId: Int, fromNodeId: Int?, actionType: String) {
        val state = _gameState.value
        
        // Safety: verify that it is indeed the online opponent's turn
        val opponentRole = GameEngine.getNextPlayer(multiplayerManager.localRole.value)
        if (state.currentPlayer != opponentRole) return

        val nextState = when (actionType) {
            "PLACE" -> GameEngine.placePiece(state, nodeId)
            "MOVE" -> if (fromNodeId != null) GameEngine.movePiece(state, fromNodeId, nodeId) else state
            "CAPTURE" -> GameEngine.capturePiece(state, nodeId)
            else -> state
        }

        if (nextState != state) {
            if (nextState.isCapturePending) {
                soundManager.playMillFormed()
            } else {
                when (actionType) {
                    "PLACE" -> soundManager.playPlace()
                    "MOVE" -> soundManager.playMove()
                    "CAPTURE" -> soundManager.playCapture()
                }
            }
            _gameState.value = nextState
            gameRepository.saveGame(nextState)
            startTurnTimer()
        }
    }

    private fun applyRemoteDrawAccepted() {
        val state = _gameState.value
        val updatedState = state.copy(
            winner = null,
            phase = GamePhase.GAME_OVER
        )
        _gameState.value = updatedState
        soundManager.playWin()
        _aiCommentary.value = "Match ended: Both accepted a Handshake Draw!"
        checkAndRecordOnlineMatchFinished(updatedState)
        gameRepository.clearSavedGame()
    }

    private fun applyRemoteForfeitReceived() {
        val state = _gameState.value
        val updatedState = state.copy(
            winner = multiplayerManager.localRole.value,
            phase = GamePhase.GAME_OVER
        )
        _gameState.value = updatedState
        soundManager.playWin()
        _aiCommentary.value = "Match ended: Opponent surrendered! You win!"
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
        if (accept) {
            undoLastMoveLocally()
        }
    }

    private fun startTurnTimer() {
        timerJob?.cancel()
        _turnTimeSeconds.value = 30
        
        val state = _gameState.value
        if (state.winner != null || state.phase == GamePhase.GAME_OVER) return
        
        val localRole = if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            multiplayerManager.localRole.value
        } else {
            Player.PLAYER_1
        }
        
        // Timer only active on local player's turn
        if (state.currentPlayer != localRole) return
        
        timerJob = viewModelScope.launch {
            while (_turnTimeSeconds.value > 0) {
                delay(1000)
                _turnTimeSeconds.value -= 1
                
                // Play soft warnings in last 5 seconds
                if (_turnTimeSeconds.value in 1..5) {
                    soundManager.playCountdownTick()
                }
            }
            // Trigger automatic strategic random move if time runs out!
            if (_turnTimeSeconds.value == 0) {
                executeAutoTimedTurn()
            }
        }
    }

    private fun executeAutoTimedTurn() {
        val state = _gameState.value
        val localRole = if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            multiplayerManager.localRole.value
        } else {
            Player.PLAYER_1
        }
        if (state.winner != null || state.currentPlayer != localRole) return
        
        val opponent = GameEngine.getNextPlayer(localRole)
        
        if (state.isCapturePending) {
            // Auto capture a random capturable piece
            val capturable = state.board.nodes.filter { (nodeId, owner) ->
                owner == opponent && MillDetector.isPieceCapturable(state.board, nodeId, opponent)
            }.keys.toList()
            if (capturable.isNotEmpty()) {
                val target = capturable.random()
                executeCapture(target)
            } else {
                val nextState = state.copy(isCapturePending = false, currentPlayer = opponent)
                updateStateAndTriggerAI(nextState)
            }
        } else {
            val legalMoves = GameEngine.getLegalMoves(state, localRole)
            if (legalMoves.isNotEmpty()) {
                val move = legalMoves.random()
                val nextState = if (move.first == null) {
                    GameEngine.placePiece(state, move.second)
                } else {
                    GameEngine.movePiece(state, move.first!!, move.second)
                }
                
                val actionType = if (move.first == null) "PLACE" else "MOVE"
                val nodeId = move.second
                val fromNodeId = move.first

                if (nextState.isCapturePending) {
                    val capturable = nextState.board.nodes.filter { (n, owner) ->
                        owner == opponent && MillDetector.isPieceCapturable(nextState.board, n, opponent)
                    }.keys.toList()
                    val finalState = if (capturable.isNotEmpty()) {
                        val capTarget = capturable.random()
                        val res = GameEngine.capturePiece(nextState, capTarget)
                        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                            multiplayerManager.sendMove(nodeId, fromNodeId, actionType)
                            multiplayerManager.sendMove(capTarget, null, "CAPTURE")
                        }
                        res
                    } else {
                        val res = nextState.copy(isCapturePending = false, currentPlayer = opponent)
                        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                            multiplayerManager.sendMove(nodeId, fromNodeId, actionType)
                        }
                        res
                    }
                    updateStateAndTriggerAI(finalState)
                } else {
                    updateStateAndTriggerAI(nextState)
                    if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                        multiplayerManager.sendMove(nodeId, fromNodeId, actionType)
                    }
                }
            }
        }
    }

    private fun updateAiCommentary(nextState: GameState) {
        val phase = nextState.phase
        
        val commentary = when {
            nextState.winner == Player.PLAYER_2 -> {
                "Chanakya Bot: Victory is mine! Practice more to rival Chanakya!"
            }
            nextState.winner == Player.PLAYER_1 -> {
                "Chanakya Bot: Outstanding move! You possess the mind of a true King."
            }
            nextState.isCapturePending && nextState.currentPlayer == Player.PLAYER_2 -> {
                "Chanakya Bot: DAADI! Your bead is mine! Sprung a clever trap..."
            }
            nextState.isCapturePending && nextState.currentPlayer == Player.PLAYER_1 -> {
                "Chanakya Bot: Ah, a well deserved capture! Nicely blocked."
            }
            phase == GamePhase.PLACEMENT -> {
                listOf(
                    "Chanakya Bot: Laying foundations bead by bead...",
                    "Chanakya Bot: Watch your sides, do not invite a line!",
                    "Chanakya Bot: A steady placement establishes control.",
                    "Chanakya Bot: Let's see if you can defend these nine seeds."
                ).random()
            }
            phase == GamePhase.MOVEMENT -> {
                listOf(
                    "Chanakya Bot: The board shifts! Slide carefully.",
                    "Chanakya Bot: A strategic pincer of nodes is forming.",
                    "Chanakya Bot: Can you slip past my defensive guard?",
                    "Chanakya Bot: Each slide must serve our long battle."
                ).random()
            }
            phase == GamePhase.FLYING -> {
                listOf(
                    "Chanakya Bot: Jumping active! Beads fly freely now!",
                    "Chanakya Bot: Three beads left, but the wings of strategy are wide.",
                    "Chanakya Bot: Complete freedom! Select any destination spot."
                ).random()
            }
            else -> "Chanakya Bot: The clash of wits continues..."
        }
        _aiCommentary.value = commentary
    }

    fun computeHint() {
        val hint = AIEngine.getHint(_gameState.value)
        if (hint != null) {
            _hintNodeId.value = hint.second
            soundManager.playHint()
        }
    }

    fun toggleTutorial(visible: Boolean) {
        _showTutorial.value = visible
    }

    fun resignGame() {
        val state = _gameState.value
        if (state.winner != null) return
        
        val opponent = if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            multiplayerManager.sendForfeit()
            GameEngine.getNextPlayer(multiplayerManager.localRole.value)
        } else {
            Player.PLAYER_2
        }

        val updatedState = state.copy(
            winner = opponent, 
            phase = GamePhase.GAME_OVER
        )
        soundManager.playLose()
        statsRepository.updateStats(opponent, state.gameMode, state.aiDifficulty)
        _gameState.value = updatedState
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            checkAndRecordOnlineMatchFinished(updatedState)
        }
        gameRepository.clearSavedGame()
    }

    fun offerDrawGame() {
        val state = _gameState.value
        if (state.winner != null) return
        
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            _drawOfferPendingLocal.value = true
            multiplayerManager.sendDrawOffer()
            _aiCommentary.value = "Draw offer transmitted to your opponent..."
            return
        }

        val updatedState = state.copy(
            winner = null, 
            phase = GamePhase.GAME_OVER
        )
        soundManager.playWin()
        statsRepository.updateStats(null, state.gameMode, state.aiDifficulty)
        _gameState.value = updatedState
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
            
            supabaseManager.registerMatchResult(
                roomCode = multiplayerManager.roomCode.value,
                hostName = hostName,
                opponentName = opponentName,
                winnerName = winnerName,
                movesCount = nextState.moveHistory.size
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        // Save state defensively on clear
        if (_gameState.value.winner == null && _gameState.value.gameMode != GameMode.ONLINE_MULTIPLAYER) {
            gameRepository.saveGame(_gameState.value)
        }
    }
}

class StatsViewModel(private val statsRepository: StatsRepository) : ViewModel() {
    val stats: StateFlow<PlayerStats> = statsRepository.statsFlow

    fun resetStats() {
        statsRepository.resetStats()
    }
}

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow

    fun updateSettings(settings: AppSettings) {
        settingsRepository.saveSettings(settings)
    }
}

/**
 * Custom Factory pattern for injecting applications service locators.
 */
class ViewModelFactory(private val application: DaadiApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(GameViewModel::class.java) -> {
                GameViewModel(
                    application.gameRepository,
                    application.statsRepository,
                    application.settingsRepository,
                    application.soundManager,
                    application.multiplayerManager,
                    application.supabaseManager
                ) as T
            }
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> {
                StatsViewModel(application.statsRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application.settingsRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
