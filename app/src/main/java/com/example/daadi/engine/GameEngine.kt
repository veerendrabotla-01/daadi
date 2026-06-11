package com.example.daadi.engine

import com.example.daadi.model.*

object GameEngine {

    /**
     * Retrieves all legal moves for a player based on current state.
     * Returns a list of Pair(fromNode, toNode). If fromNode is null, it's a placement.
     */
    fun getLegalMoves(state: GameState, player: Player): List<Pair<Int?, Int>> {
        if (state.winner != null) return emptyList()
        if (state.isCapturePending) return emptyList() // Must capture, no normal moves allowed

        val isPlacement = if (player == Player.PLAYER_1) {
            state.player1PiecesInHand > 0
        } else {
            state.player2PiecesInHand > 0
        }

        val emptyNodes = state.board.nodes.filter { it.value == null }.keys

        if (isPlacement) {
            // Placement phase: any empty node is valid
            return emptyNodes.map { Pair(null, it) }
        }

        // Movement / Flying phase
        val ownNodes = state.board.nodes.filter { it.value == player }.keys
        val totalOwnPieces = ownNodes.size

        if (totalOwnPieces <= 2) {
            // Can't move if defeated
            return emptyList()
        }

        val isFlying = totalOwnPieces == 3

        val moves = mutableListOf<Pair<Int?, Int>>()

        for (from in ownNodes) {
            if (isFlying) {
                // Flying: Can jump from 'from' to ANY empty node
                for (to in emptyNodes) {
                    moves.add(Pair(from, to))
                }
            } else {
                // Regular movement: Can slide to adjacent connected empty nodes
                val adjacent = BoardDefinition.CONNECTIONS[from] ?: emptyList()
                for (to in adjacent) {
                    if (state.board.nodes[to] == null) {
                        moves.add(Pair(from, to))
                    }
                }
            }
        }

        return moves
    }

    /**
     * Executes piece placement at [nodeId] for the current player.
     */
    fun placePiece(state: GameState, nodeId: Int): GameState {
        val player = state.currentPlayer
        val handCount = if (player == Player.PLAYER_1) state.player1PiecesInHand else state.player2PiecesInHand

        // Validations
        if (state.winner != null) return state
        if (state.isCapturePending) return state
        if (state.phase != GamePhase.PLACEMENT) return state
        if (handCount <= 0) return state
        if (state.board.nodes[nodeId] != null) return state

        // Place piece
        val updatedNodes = state.board.nodes.toMutableMap()
        updatedNodes[nodeId] = player
        val newBoard = Board(updatedNodes)

        // Update counts
        val newP1Hand = if (player == Player.PLAYER_1) state.player1PiecesInHand - 1 else state.player1PiecesInHand
        val newP2Hand = if (player == Player.PLAYER_2) state.player2PiecesInHand - 1 else state.player2PiecesInHand
        val newP1OnBoard = if (player == Player.PLAYER_1) state.player1PiecesOnBoard + 1 else state.player1PiecesOnBoard
        val newP2OnBoard = if (player == Player.PLAYER_2) state.player2PiecesOnBoard + 1 else state.player2PiecesOnBoard

        // Detect mill
        val formsMill = MillDetector.formsNewMill(newBoard, nodeId, player)

        // Check phase transitions
        val nextPhase = if (newP1Hand == 0 && newP2Hand == 0) GamePhase.MOVEMENT else GamePhase.PLACEMENT

        val historyItem = GameMove(
            type = MoveType.PLACE,
            fromNode = null,
            toNode = nodeId,
            capturedNode = null,
            player = player
        )

        val nextPlayer = if (formsMill) player else getNextPlayer(player)

        return state.copy(
            board = newBoard,
            player1PiecesInHand = newP1Hand,
            player2PiecesInHand = newP2Hand,
            player1PiecesOnBoard = newP1OnBoard,
            player2PiecesOnBoard = newP2OnBoard,
            phase = nextPhase,
            isCapturePending = formsMill,
            currentPlayer = nextPlayer,
            moveHistory = state.moveHistory + historyItem
        )
    }

    /**
     * Slides/flies a piece from [fromNode] to [toNode].
     */
    fun movePiece(state: GameState, fromNode: Int, toNode: Int): GameState {
        val player = state.currentPlayer

        // Validations
        if (state.winner != null) return state
        if (state.isCapturePending) return state
        if (state.phase == GamePhase.PLACEMENT) return state
        if (state.board.nodes[fromNode] != player) return state
        if (state.board.nodes[toNode] != null) return state

        val totalOwnPieces = if (player == Player.PLAYER_1) state.player1PiecesOnBoard else state.player2PiecesOnBoard
        val isFlying = totalOwnPieces == 3

        if (!isFlying) {
            val adjacent = BoardDefinition.CONNECTIONS[fromNode] ?: emptyList()
            if (toNode !in adjacent) return state // Must be adjacent
        }

        // Apply move
        val updatedNodes = state.board.nodes.toMutableMap()
        updatedNodes[fromNode] = null
        updatedNodes[toNode] = player
        val newBoard = Board(updatedNodes)

        // Detect mill
        val formsMill = MillDetector.formsNewMill(newBoard, toNode, player)

        val historyItem = GameMove(
            type = MoveType.MOVE,
            fromNode = fromNode,
            toNode = toNode,
            capturedNode = null,
            player = player
        )

        val nextPlayer = if (formsMill) player else getNextPlayer(player)

        val finalState = state.copy(
            board = newBoard,
            isCapturePending = formsMill,
            currentPlayer = nextPlayer,
            moveHistory = state.moveHistory + historyItem
        )

        // If no mill is formed, we must evaluate win conditions for the NEXT player right away
        return if (!formsMill) {
            checkWinAndAdjustPhase(finalState)
        } else {
            finalState
        }
    }

    /**
     * Captures an opponent's piece at [nodeId].
     */
    fun capturePiece(state: GameState, nodeId: Int): GameState {
        val player = state.currentPlayer
        val opponent = getNextPlayer(player)

        // Validations
        if (state.winner != null) return state
        if (!state.isCapturePending) return state
        if (state.board.nodes[nodeId] != opponent) return state

        // Standard rules: Cannot capture milled pieces unless all opponent's pieces are in mills
        if (!MillDetector.isPieceCapturable(state.board, nodeId, opponent)) return state

        // Apply capture
        val updatedNodes = state.board.nodes.toMutableMap()
        updatedNodes[nodeId] = null
        val newBoard = Board(updatedNodes)

        // Update counts
        val newP1OnBoard = if (opponent == Player.PLAYER_1) state.player1PiecesOnBoard - 1 else state.player1PiecesOnBoard
        val newP2OnBoard = if (opponent == Player.PLAYER_2) state.player2PiecesOnBoard - 1 else state.player2PiecesOnBoard

        // Record history
        val lastMove = state.moveHistory.lastOrNull()
        val updatedHistory = if (lastMove != null && lastMove.player == player && lastMove.capturedNode == null) {
            // Attach capture to the last placement/move
            state.moveHistory.dropLast(1) + lastMove.copy(capturedNode = nodeId)
        } else {
            state.moveHistory + GameMove(
                type = MoveType.CAPTURE,
                fromNode = null,
                toNode = nodeId,
                capturedNode = nodeId,
                player = player
            )
        }

        val nextPlayer = getNextPlayer(player)

        val nextState = state.copy(
            board = newBoard,
            player1PiecesOnBoard = newP1OnBoard,
            player2PiecesOnBoard = newP2OnBoard,
            isCapturePending = false,
            currentPlayer = nextPlayer,
            moveHistory = updatedHistory
        )

        return checkWinAndAdjustPhase(nextState)
    }

    /**
     * Checks if a win/loss is triggered and adjusts the active phase (e.g. Flying).
     */
    private fun checkWinAndAdjustPhase(state: GameState): GameState {
        val resolvedWinner = checkWinner(state)

        if (resolvedWinner != null) {
            return state.copy(
                winner = resolvedWinner,
                phase = GamePhase.GAME_OVER
            )
        }

        // Adjust flying phase check
        val p1Count = state.player1PiecesOnBoard
        val p2Count = state.player2PiecesOnBoard

        // Determine next active phase (only if placement completed)
        val currentActivePhase = if (state.player1PiecesInHand == 0 && state.player2PiecesInHand == 0) {
            val piecesOfCurrentPlayer = if (state.currentPlayer == Player.PLAYER_1) p1Count else p2Count
            if (piecesOfCurrentPlayer == 3) GamePhase.FLYING else GamePhase.MOVEMENT
        } else {
            state.phase
        }

        return state.copy(phase = currentActivePhase)
    }

    /**
     * Utility to get the opposite player.
     */
    fun getNextPlayer(player: Player): Player {
        return if (player == Player.PLAYER_1) Player.PLAYER_2 else Player.PLAYER_1
    }

    /**
     * Core win detector logic.
     */
    fun checkWinner(state: GameState): Player? {
        // Win constraints only apply after Placement Phase is completed
        val p1Hand = state.player1PiecesInHand
        val p2Hand = state.player2PiecesInHand
        if (p1Hand > 0 || p2Hand > 0) return null

        val p1Board = state.player1PiecesOnBoard
        val p2Board = state.player2PiecesOnBoard

        // Check counts
        if (p1Board < 3) return Player.PLAYER_2
        if (p2Board < 3) return Player.PLAYER_1

        // Check legal moves for the current player whose turn is starting
        val current = state.currentPlayer
        val legalMoves = getLegalMoves(state, current)

        // If current player has NO legal movements, they lose! The opponent wins!
        if (legalMoves.isEmpty()) {
            return getNextPlayer(current)
        }

        return null
    }
}
