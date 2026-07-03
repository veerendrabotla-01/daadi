package com.example.daadi.engine.ai

import com.example.daadi.engine.BoardDefinition
import com.example.daadi.engine.GameEngine
import com.example.daadi.engine.MillDetector
import com.example.daadi.model.*

data class AiConfig(
    val maxDepth: Int = 4,
    val pincerTwoOfThreeWeight: Int = 30,
    val captureProximityWeight: Int = 200,
    val pathBlockingWeight: Int = 60,
    val pieceDifferentialWeight: Int = 400,
    val millWeight: Int = 150
)

object AIEngine {

    var currentConfig = AiConfig()
    
    // Telemetry Counters
    var totalNodesEvaluated = 0L
    var totalPruningClips = 0L
    var lastExecutionTime = 0L

    fun resetTelemetry() {
        totalNodesEvaluated = 0
        totalPruningClips = 0
        lastExecutionTime = 0
    }

    /**
     * Entry point for selecting the best move for the AI (Player 2).
     * Returns Pair(fromNode, toNode) where fromNode is null for placements.
     */
    fun selectMove(state: GameState, difficulty: AIDifficulty): Pair<Int?, Int>? {
        val legalMoves = GameEngine.getLegalMoves(state, Player.PLAYER_2)
        if (legalMoves.isEmpty()) return null

        return when (difficulty) {
            AIDifficulty.EASY -> legalMoves.random()
            AIDifficulty.MEDIUM -> selectMediumMove(state, legalMoves)
            AIDifficulty.HARD -> selectHardMove(state, legalMoves)
        }
    }

    /**
     * Entry point to compute a hint/suggestion for the current active player.
     */
    fun getHint(state: GameState): Pair<Int?, Int>? {
        val player = state.currentPlayer
        val opponent = GameEngine.getNextPlayer(player)
        val ruleSet = state.ruleSet

        if (state.isCapturePending) {
            // Suggest a capture target
            val target = selectCapture(state, opponent)
            return if (target != null) Pair(null, target) else null
        }

        val legalMoves = GameEngine.getLegalMoves(state, player)
        if (legalMoves.isEmpty()) return null

        // 1. Check if we can form an immediate mill
        for (move in legalMoves) {
            val hypotheticalBoard = applyHypotheticalMove(state.board, move, player)
            if (MillDetector.formsNewMill(hypotheticalBoard, move.second, player, ruleSet)) {
                return move
            }
        }

        // 2. Check if opponent is threatening to form a mill and block them
        val opponentPlacementsOfConcern = mutableSetOf<Int>()
        val emptyNodes = state.board.nodes.filter { it.value == null }.keys
        for (emptyNode in emptyNodes) {
            val hypotheticalBoard = state.board.copy(nodes = state.board.nodes + (emptyNode to opponent))
            if (MillDetector.formsNewMill(hypotheticalBoard, emptyNode, opponent, ruleSet)) {
                opponentPlacementsOfConcern.add(emptyNode)
            }
        }

        if (opponentPlacementsOfConcern.isNotEmpty()) {
            val blockingMove = legalMoves.find { it.second in opponentPlacementsOfConcern }
            if (blockingMove != null) {
                return blockingMove
            }
        }

        // 3. Fallback: select moves targeting higher centrality adjacency nodes
        val sortedMoves = legalMoves.sortedByDescending { BoardDefinition.getConnections(it.second, ruleSet).size }
        return sortedMoves.firstOrNull() ?: legalMoves.randomOrNull()
    }

    /**
     * Entry point for selecting which opponent piece to capture.
     */
    fun selectCapture(state: GameState, opponent: Player): Int? {
        val ruleSet = state.ruleSet
        val capturableNodes = state.board.nodes.filter { (nodeId, owner) ->
            owner == opponent && MillDetector.isPieceCapturable(state.board, nodeId, opponent, ruleSet)
        }.keys

        if (capturableNodes.isEmpty()) return null

        // Priority capture selection:
        // 1. Break opponent's active mills first if capturable (normally milled pieces aren't capturable, but if all are milled, any is fair game).
        // 2. Capture piece that is in progress to form a mill (has 2 pieces in a mill line).
        // 3. Capture piece with high adjacency/connections.
        // 4. Default: pick randomly.

        // Let's check if any capturable piece is close to forming a mill for the opponent
        val allMills = BoardDefinition.getMills(ruleSet)
        for (node in capturableNodes) {
            val opponentMillsPending = allMills.filter { mill ->
                mill.first == node || mill.second == node || mill.third == node
            }.any { mill ->
                val otherNodes = listOf(mill.first, mill.second, mill.third).filter { it != node }
                state.board.nodes[otherNodes[0]] == opponent && state.board.nodes[otherNodes[1]] == opponent
            }
            if (opponentMillsPending) {
                return node // Highly strategic capture! Blocks their prospective line
            }
        }

        // Return the node which has the most connections
        return capturableNodes.maxByOrNull { BoardDefinition.getConnections(it, ruleSet).size } ?: capturableNodes.random()
    }

    /**
     * Medium difficulty move selection:
     * 1. If we can form an immediate mill with a move/placement -> Do it!
     * 2. If the user is about to form a mill -> Block it!
     * 3. Else fallback to random.
     */
    private fun selectMediumMove(state: GameState, legalMoves: List<Pair<Int?, Int>>): Pair<Int?, Int> {
        val ai = Player.PLAYER_2
        val human = Player.PLAYER_1
        val ruleSet = state.ruleSet

        // 1. Can we form an immediate mill?
        for (move in legalMoves) {
            val hypotheticalBoard = applyHypotheticalMove(state.board, move, ai)
            val toNode = move.second
            if (MillDetector.formsNewMill(hypotheticalBoard, toNode, ai, ruleSet)) {
                return move
            }
        }

        // 2. Can the human form an immediate mill? Let's check where the human has 2 pieces and 1 empty node
        // We look for any empty node on the board where human can place and make a mill.
        // If it matches one of our legal moves' destination, we block it!
        val humanPlacementsOfConcern = mutableSetOf<Int>()
        val emptyNodes = state.board.nodes.filter { it.value == null }.keys
        for (emptyNode in emptyNodes) {
            val hypotheticalBoard = state.board.copy(nodes = state.board.nodes + (emptyNode to human))
            if (MillDetector.formsNewMill(hypotheticalBoard, emptyNode, human, ruleSet)) {
                humanPlacementsOfConcern.add(emptyNode)
            }
        }

        if (humanPlacementsOfConcern.isNotEmpty()) {
            // Find a legal move that targets this node
            val blockingMove = legalMoves.find { it.second in humanPlacementsOfConcern }
            if (blockingMove != null) {
                return blockingMove
            }
        }

        // 3. Fallback: Prefer moves that place on higher occupancy or side-centers to form mills
        val strategicMove = legalMoves.filter { BoardDefinition.getConnections(it.second, ruleSet).size >= 3 }
        if (strategicMove.isNotEmpty()) {
            return strategicMove.random()
        }

        return legalMoves.random()
    }

    /**
     * Hard difficulty: Minimax with Alpha-Beta Pruning.
     * Limit depth to 4 during placements to remain ultra-fast, and depth 5 during movement.
     */
    private fun selectHardMove(state: GameState, legalMoves: List<Pair<Int?, Int>>): Pair<Int?, Int> {
        val startTime = System.currentTimeMillis()
        val ai = Player.PLAYER_2
        val isPlacement = state.phase == GamePhase.PLACEMENT
        val maxDepth = if (isPlacement) currentConfig.maxDepth else currentConfig.maxDepth + 2 // Scale based on config
        val ruleSet = state.ruleSet

        var bestMove: Pair<Int?, Int> = legalMoves.random()
        var bestVal = Int.MIN_VALUE

        val alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        // --- DRAW CONTEMPT & REPETITION TRACKER ---
        // Identify moves that would result in a state we recently occupied
        val recentMoves = state.moveHistory.takeLast(6)
        val shuttlingNodes = mutableSetOf<Int>()
        if (recentMoves.size >= 4) {
            // Check if player has been moving the same piece back and forth
            val aiMoves = recentMoves.filter { it.player == Player.PLAYER_2 && it.type == MoveType.MOVE }
            if (aiMoves.size >= 2) {
                shuttlingNodes.add(aiMoves.last().fromNode!!)
            }
        }

        // Sort moves to optimize alpha-beta cutoff (evaluate immediate wins/mills first)
        val sortedMoves = legalMoves.sortedByDescending { move ->
            var score = 0
            val boardAfter = applyHypotheticalMove(state.board, move, ai)
            if (MillDetector.formsNewMill(boardAfter, move.second, ai, ruleSet)) score += 1000
            
            // Penalize shuttling (repetitive back-and-forth moves)
            if (move.first != null && move.second in shuttlingNodes) score -= 500
            
            score
        }

        for (move in sortedMoves) {
            val hypotheticalState = simulateGameState(state, move, ai)
            var v = minimax(hypotheticalState, maxDepth - 1, alpha, beta, false)
            
            // Inject Draw Contempt: slightly penalize repetition in evaluation
            if (move.first != null && move.second in shuttlingNodes) {
                v -= 100 // Contempt for repetition
            }

            if (v > bestVal) {
                bestVal = v
                bestMove = move
            }
        }

        lastExecutionTime = System.currentTimeMillis() - startTime
        return bestMove
    }

    private fun minimax(state: GameState, depth: Int, alphaInput: Int, betaInput: Int, isMax: Boolean): Int {
        totalNodesEvaluated++
        var alpha = alphaInput
        var beta = betaInput

        if (depth == 0 || state.winner != null) {
            return evaluateBoard(state)
        }

        val player = if (isMax) Player.PLAYER_2 else Player.PLAYER_1
        val legalMoves = GameEngine.getLegalMoves(state, player)

        if (legalMoves.isEmpty()) {
            // No moves available = loss for that player
            return if (isMax) -10000 else 10000
        }

        if (isMax) {
            var value = Int.MIN_VALUE
            for (move in legalMoves) {
                val nextState = simulateGameState(state, move, Player.PLAYER_2)
                value = maxOf(value, minimax(nextState, depth - 1, alpha, beta, false))
                alpha = maxOf(alpha, value)
                if (alpha >= beta) {
                    totalPruningClips++
                    break
                }
            }
            return value
        } else {
            var value = Int.MAX_VALUE
            for (move in legalMoves) {
                val nextState = simulateGameState(state, move, Player.PLAYER_1)
                value = minOf(value, minimax(nextState, depth - 1, alpha, beta, true))
                beta = minOf(beta, value)
                if (alpha >= beta) {
                    totalPruningClips++
                    break
                }
            }
            return value
        }
    }

    /**
     * Evaluation function for Minimax:
     * - Positive: Favors AI (PLAYER_2)
     * - Negative: Favors Human (PLAYER_1)
     */
    private fun evaluateBoard(state: GameState): Int {
        if (state.winner == Player.PLAYER_2) return 50000
        if (state.winner == Player.PLAYER_1) return -50000

        var score = 0
        val board = state.board
        val ruleSet = state.ruleSet

        // 1. Piece differential
        val p1Count = state.player1PiecesOnBoard
        val p2Count = state.player2PiecesOnBoard
        score += (p2Count - p1Count) * currentConfig.pieceDifferentialWeight

        // 2. Check Mills and Potential Mills
        var p2Mills = 0
        var p1Mills = 0
        var p2TwoInALine = 0
        var p1TwoInALine = 0

        for (mill in BoardDefinition.getMills(ruleSet)) {
            val o1 = board.nodes[mill.first]
            val o2 = board.nodes[mill.second]
            val o3 = board.nodes[mill.third]
            
            if (o1 != null && o1 == o2 && o2 == o3) {
                if (o1 == Player.PLAYER_2) p2Mills++
                else p1Mills++
            } else {
                // Check for 2-in-a-line with an empty 3rd spot (threats)
                val owners = listOf(o1, o2, o3)
                if (owners.count { it == Player.PLAYER_2 } == 2 && owners.any { it == null }) p2TwoInALine++
                if (owners.count { it == Player.PLAYER_1 } == 2 && owners.any { it == null }) p1TwoInALine++
            }
        }
        score += p2Mills * currentConfig.millWeight
        score -= p1Mills * (currentConfig.millWeight * 2) // Defensive priority
        score += p2TwoInALine * currentConfig.pincerTwoOfThreeWeight
        score -= p1TwoInALine * (currentConfig.pincerTwoOfThreeWeight * 2) // Block human "open" mills

        // 3. Piece Mobility (number of moves available)
        val p2Moves = GameEngine.getLegalMoves(state, Player.PLAYER_2)
        val p1Moves = GameEngine.getLegalMoves(state, Player.PLAYER_1)
        score += p2Moves.size * currentConfig.pathBlockingWeight / 3
        score -= p1Moves.size * currentConfig.pathBlockingWeight / 3
        
        // 4. Strategic Position occupancies
        for ((nodeId, owner) in board.nodes) {
            val connections = BoardDefinition.getConnections(nodeId, ruleSet).size
            if (owner == Player.PLAYER_2) {
                score += if (nodeId in listOf(1, 4, 7, 10, 13, 16, 19, 22)) 40 else 15
                score += connections * 10
            } else if (owner == Player.PLAYER_1) {
                score -= if (nodeId in listOf(1, 4, 7, 10, 13, 16, 19, 22)) 40 else 15
                score -= connections * 10
            }
        }

        return score
    }

    private fun applyHypotheticalMove(board: Board, move: Pair<Int?, Int>, player: Player): Board {
        val updatedMap = board.nodes.toMutableMap()
        if (move.first != null) {
            updatedMap[move.first!!] = null
        }
        updatedMap[move.second] = player
        return Board(updatedMap)
    }

    private fun simulateGameState(state: GameState, move: Pair<Int?, Int>, player: Player): GameState {
        val ruleSet = state.ruleSet
        val updatedNodes = state.board.nodes.toMutableMap()
        if (move.first != null) {
            updatedNodes[move.first!!] = null
        }
        updatedNodes[move.second] = player
        val newBoard = Board(updatedNodes)

        // Capture logic simplify for state evaluation:
        // If simulated move triggers a mill, simulate standard AI/human capture to keep the state accurate.
        val formsMill = MillDetector.formsNewMill(newBoard, move.second, player, ruleSet)
        val opponent = if (player == Player.PLAYER_1) Player.PLAYER_2 else Player.PLAYER_1

        var p1OnBoard = state.player1PiecesOnBoard
        var p2OnBoard = state.player2PiecesOnBoard

        if (player == Player.PLAYER_1) {
            if (move.first == null) p1OnBoard++
        } else {
            if (move.first == null) p2OnBoard++
        }

        if (formsMill) {
            // Find a hypothetical capture target
            val possibleTarget = updatedNodes.filter { (node, owner) ->
                owner == opponent && MillDetector.isPieceCapturable(newBoard, node, opponent, ruleSet)
            }.keys.firstOrNull()

            if (possibleTarget != null) {
                updatedNodes[possibleTarget] = null
                if (opponent == Player.PLAYER_1) {
                    p1OnBoard--
                } else {
                    p2OnBoard--
                }
            }
        }

        val nextPlayer = GameEngine.getNextPlayer(player)
        return state.copy(
            board = Board(updatedNodes),
            player1PiecesOnBoard = p1OnBoard,
            player2PiecesOnBoard = p2OnBoard,
            currentPlayer = nextPlayer
        )
    }
}
