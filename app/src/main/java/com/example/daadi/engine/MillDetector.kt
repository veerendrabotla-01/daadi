package com.example.daadi.engine

import com.example.daadi.model.Board
import com.example.daadi.model.Player

object MillDetector {

    /**
     * Checks if a newly formed mill exists at [lastMovedNode] for [player].
     */
    fun formsNewMill(board: Board, lastMovedNode: Int, player: Player): Boolean {
        // Find all mills that contain the last moved node
        val candidateMills = BoardDefinition.MILLS.filter {
            it.first == lastMovedNode || it.second == lastMovedNode || it.third == lastMovedNode
        }

        // Check if any of these candidates are now fully occupied by this player
        return candidateMills.any { mill ->
            board.nodes[mill.first] == player &&
                    board.nodes[mill.second] == player &&
                    board.nodes[mill.third] == player
        }
    }

    /**
     * Checks if a specific node is part of any active mill for its owner.
     */
    fun isNodeInMill(board: Board, nodeId: Int): Boolean {
        val owner = board.nodes[nodeId] ?: return false

        return BoardDefinition.MILLS.any { mill ->
            (mill.first == nodeId || mill.second == nodeId || mill.third == nodeId) &&
                    board.nodes[mill.first] == owner &&
                    board.nodes[mill.second] == owner &&
                    board.nodes[mill.third] == owner
        }
    }

    /**
     * Checks if all pieces of [player] currently on the board are inside mills.
     */
    fun areAllPlayerPiecesInMills(board: Board, player: Player): Boolean {
        val playerNodes = board.nodes.filter { it.value == player }.keys
        if (playerNodes.isEmpty()) return true

        return playerNodes.all { nodeId ->
            isNodeInMill(board, nodeId)
        }
    }

    /**
     * Returns true if a piece can be legally captured.
     * Rule: A piece inside a mill cannot be captured unless all opponent's pieces are inside mills.
     */
    fun isPieceCapturable(board: Board, nodeId: Int, opponent: Player): Boolean {
        if (board.nodes[nodeId] != opponent) return false
        if (!isNodeInMill(board, nodeId)) return true
        // If it is in a mill, it's only capturable if ALL opponent pieces are in mills
        return areAllPlayerPiecesInMills(board, opponent)
    }
}
