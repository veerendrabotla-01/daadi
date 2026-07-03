package com.example.daadi.engine

import com.example.daadi.model.Board
import com.example.daadi.model.Player

object MillDetector {

    /**
     * Checks if a newly formed mill exists at [lastMovedNode] for [player].
     */
    fun formsNewMill(board: Board, lastMovedNode: Int, player: Player, ruleSet: com.example.daadi.model.RuleSet): Boolean {
        // Find all mills that contain the last moved node
        val candidateMills = BoardDefinition.getMills(ruleSet).filter {
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
    fun isNodeInMill(board: Board, nodeId: Int, ruleSet: com.example.daadi.model.RuleSet): Boolean {
        val owner = board.nodes[nodeId] ?: return false

        return BoardDefinition.getMills(ruleSet).any { mill ->
            (mill.first == nodeId || mill.second == nodeId || mill.third == nodeId) &&
                    board.nodes[mill.first] == owner &&
                    board.nodes[mill.second] == owner &&
                    board.nodes[mill.third] == owner
        }
    }

    /**
     * Checks if all pieces of [player] currently on the board are inside mills.
     */
    fun areAllPlayerPiecesInMills(board: Board, player: Player, ruleSet: com.example.daadi.model.RuleSet): Boolean {
        val playerNodes = board.nodes.filter { it.value == player }.keys
        if (playerNodes.isEmpty()) return true

        return playerNodes.all { nodeId ->
            isNodeInMill(board, nodeId, ruleSet)
        }
    }

    /**
     * Returns true if a piece can be legally captured.
     * International Rules: A piece inside a mill cannot be captured unless ALL of the opponent's 
     * pieces currently on the board are part of active mills.
     * 
     * --- SAFETY AUDITOR: MILL LOCKDOWN OVERRIDE ---
     * This check prevents a soft-lock where a player forms a move but cannot capture anything.
     */
    fun isPieceCapturable(board: Board, nodeId: Int, opponent: Player, ruleSet: com.example.daadi.model.RuleSet): Boolean {
        // --- ARCHITECT ASSERTION ---
        val owner = board.nodes[nodeId]
        if (owner != opponent) {
            com.example.daadi.util.SecureLog.w("DAADI_AUDITOR", "Invalid Capture Target: Node $nodeId owned by $owner, expected $opponent")
            return false
        }
        // ---------------------------

        // Check if the specific piece is in a mill
        val inMill = isNodeInMill(board, nodeId, ruleSet)
        
        // If it is NOT in a mill, it's always capturable
        if (!inMill) return true
        
        // --- THE LOCKDOWN AUDIT ---
        // If it IS in a mill, it is only capturable if there are NO OTHER pieces of the opponent
        // that are outside of mills. (i.e. ALL opponent's pieces are in mills)
        val allInMills = areAllPlayerPiecesInMills(board, opponent, ruleSet)
        
        if (allInMills) {
            com.example.daadi.util.SecureLog.i("DAADI_AUDITOR", "Mill Lockdown Detected for $opponent. Overriding safety: Node $nodeId is now capturable.")
            return true
        }

        return false
    }
}
