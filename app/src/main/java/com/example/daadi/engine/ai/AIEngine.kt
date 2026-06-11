package com.example.daadi.engine.ai

import com.example.daadi.engine.BoardDefinition
import com.example.daadi.engine.GameEngine
import com.example.daadi.engine.MillDetector
import com.example.daadi.model.*

object AIEngine {

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
        val legalMoves = GameEngine.getLegalMoves(state, player)
        if (legalMoves.isEmpty()) return null

        val opponent = GameEngine.getNextPlayer(player)

        // 1. Check if we can form an immediate mill
        for (move in legalMoves) {
            val hypotheticalBoard = applyHypotheticalMove(state.board, move, player)
            if (MillDetector.formsNewMill(hypotheticalBoard, move.second, player)) {
                return move
            }
        }

        // 2. Check if opponent is threatening to form a mill and block them
        val opponentPlacementsOfConcern = mutableSetOf<Int>()
        val emptyNodes = state.board.nodes.filter { it.value == null }.keys
        for (emptyNode in emptyNodes) {
            val hypotheticalBoard = state.board.copy(nodes = state.board.nodes + (emptyNode to opponent))
            if (MillDetector.formsNewMill(hypotheticalBoard, emptyNode, opponent)) {
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
        val sortedMoves = legalMoves.sortedByDescending { BoardDefinition.CONNECTIONS[it.second]?.size ?: 0 }
        return sortedMoves.firstOrNull() ?: legalMoves.randomOrNull()
    }

    /**
     * Entry point for selecting which opponent piece to capture.
     */
    fun selectCapture(state: GameState, opponent: Player): Int? {
        val capturableNodes = state.board.nodes.filter { (nodeId, owner) ->
            owner == opponent && MillDetector.isPieceCapturable(state.board, nodeId, opponent)
        }.keys

        if (capturableNodes.isEmpty()) return null

        // Priority capture selection:
        // 1. Break opponent's active mills first if capturable (normally milled pieces aren't capturable, but if all are milled, any is fair game).
        // 2. Capture piece that is in progress to form a mill (has 2 pieces in a mill line).
        // 3. Capture piece with high adjacency/connections.
        // 4. Default: pick randomly.

        // Let's check if any capturable piece is close to forming a mill for the opponent
        for (node in capturableNodes) {
            val opponentMillsPending = BoardDefinition.MILLS.filter { mill ->
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
        return capturableNodes.maxByOrNull { BoardDefinition.CONNECTIONS[it]?.size ?: 0 } ?: capturableNodes.random()
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

        // 1. Can we form an immediate mill?
        for (move in legalMoves) {
            val hypotheticalBoard = applyHypotheticalMove(state.board, move, ai)
            val toNode = move.second
            if (MillDetector.formsNewMill(hypotheticalBoard, toNode, ai)) {
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
            if (MillDetector.formsNewMill(hypotheticalBoard, emptyNode, human)) {
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
        val strategicMove = legalMoves.filter { BoardDefinition.CONNECTIONS[it.second]?.size ?: 0 >= 3 }
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
        val ai = Player.PLAYER_2
        val isPlacement = state.phase == GamePhase.PLACEMENT
        val maxDepth = if (isPlacement) 3 else 5

        var bestMove: Pair<Int?, Int> = legalMoves.random()
        var bestVal = Int.MIN_VALUE

        val alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        // Sort moves to optimize alpha-beta cutoff (evaluate immediate wins first)
        val sortedMoves = legalMoves.sortedByDescending { move ->
            val boardAfter = applyHypotheticalMove(state.board, move, ai)
            if (MillDetector.formsNewMill(boardAfter, move.second, ai)) 100 else 0
        }

        for (move in sortedMoves) {
            val hypotheticalState = simulateGameState(state, move, ai)
            val v = minimax(hypotheticalState, maxDepth - 1, alpha, beta, false)
            if (v > bestVal) {
                bestVal = v
                bestMove = move
            }
        }

        return bestMove
    }

    private fun minimax(state: GameState, depth: Int, alphaInput: Int, betaInput: Int, isMax: Boolean): Int {
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
                if (alpha >= beta) break
            }
            return value
        } else {
            var value = Int.MAX_VALUE
            for (move in legalMoves) {
                val nextState = simulateGameState(state, move, Player.PLAYER_1)
                value = minOf(value, minimax(nextState, depth - 1, alpha, beta, true))
                beta = minOf(beta, value)
                if (alpha >= beta) break
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
        if (state.winner == Player.PLAYER_2) return 10000
        if (state.winner == Player.PLAYER_1) return -10000

        var score = 0

        // 1. Piece differential
        val p1Count = state.player1PiecesOnBoard
        val p2Count = state.player2PiecesOnBoard
        score += (p2Count - p1Count) * 200

        // 2. Check Mills on board
        val board = state.board
        var p2Mills = 0
        var p1Mills = 0
        for (mill in BoardDefinition.MILLS) {
            val o1 = board.nodes[mill.first]
            val o2 = board.nodes[mill.second]
            val o3 = board.nodes[mill.third]
            if (o1 == o2 && o2 == o3) {
                if (o1 == Player.PLAYER_2) p2Mills++
                else if (o1 == Player.PLAYER_1) p1Mills++
            }
        }
        score += p2Mills * 150
        score -= p1Mills * 180 // Penalize human mills heavily to prioritize blocking

        // 3. Piece Mobility (number of moves available)
        val p2Mobility = GameEngine.getLegalMoves(state, Player.PLAYER_2).size
        val p1Mobility = GameEngine.getLegalMoves(state, Player.PLAYER_1).size
        score += p2Mobility * 10
        score -= p1Mobility * 10

        // 4. Strategic Position occupancies (side-centers are great to form mills)
        for ((nodeId, owner) in board.nodes) {
            if (owner == Player.PLAYER_2) {
                score += if (nodeId in listOf(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23)) 15 else 5
            } else if (owner == Player.PLAYER_1) {
                score -= if (nodeId in listOf(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23)) 15 else 5
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
        val updatedNodes = state.board.nodes.toMutableMap()
        if (move.first != null) {
            updatedNodes[move.first!!] = null
        }
        updatedNodes[move.second] = player
        val newBoard = Board(updatedNodes)

        // Capture logic simplify for state evaluation:
        // If simulated move triggers a mill, simulate standard AI/human capture to keep the state accurate.
        val formsMill = MillDetector.formsNewMill(newBoard, move.second, player)
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
                owner == opponent && MillDetector.isPieceCapturable(newBoard, node, opponent)
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
