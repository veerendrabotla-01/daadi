package com.example.daadi.model

import com.squareup.moshi.JsonClass

enum class Player {
    PLAYER_1, // Red / Participant
    PLAYER_2  // Blue / AI or Opponent
}

enum class GamePhase {
    PLACEMENT,
    MOVEMENT,
    FLYING,
    GAME_OVER
}

enum class GameMode {
    VS_AI,
    PASS_AND_PLAY,
    ONLINE_MULTIPLAYER
}

enum class AIDifficulty {
    EASY,
    MEDIUM,
    HARD
}

enum class MoveType {
    PLACE,
    MOVE,
    CAPTURE
}

@JsonClass(generateAdapter = true)
data class Board(
    val nodes: Map<Int, Player?> = (0..23).associateWith { null }
)

@JsonClass(generateAdapter = true)
data class Mill(
    val node1: Int,
    val node2: Int,
    val node3: Int,
    val owner: Player
) {
    fun containsNode(node: Int): Boolean {
        return node == node1 || node == node2 || node == node3
    }
}

@JsonClass(generateAdapter = true)
data class GameMove(
    val type: MoveType,
    val fromNode: Int?,
    val toNode: Int,
    val capturedNode: Int?,
    val player: Player,
    val notation: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class RuleSet {
    NINE_MENS_MORRIS,
    TWELVE_MENS_MORRIS
}

enum class ConnectionStatus {
    CONNECTED,
    RECONNECTING,
    DISCONNECTED
}

@JsonClass(generateAdapter = true)
data class GameState(
    val board: Board = Board(),
    val currentPlayer: Player = Player.PLAYER_1,
    val phase: GamePhase = GamePhase.PLACEMENT,
    val gameMode: GameMode = GameMode.VS_AI,
    val aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val ruleSet: RuleSet = RuleSet.NINE_MENS_MORRIS,
    val player1PiecesInHand: Int = 9,
    val player2PiecesInHand: Int = 9,
    val player1PiecesOnBoard: Int = 0,
    val player2PiecesOnBoard: Int = 0,
    val isCapturePending: Boolean = false,
    val winner: Player? = null,
    val drawReason: String? = null,
    val moveHistory: List<GameMove> = emptyList(),
    val boardHistory: List<String> = emptyList(), // Store "player:boardEncodedString" for repetition check
    val halfMoveClock: Int = 0, // Moves since last capture or placement for 50-move rule
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val reconnectionCountdown: Int? = null,
    val player1TimerChances: Int = 3,
    val player2TimerChances: Int = 3,
    val timestampSaved: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class PlayerStats(
    val totalGamesPlayed: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val totalDraws: Int = 0,
    val winsVsEasyAI: Int = 0,
    val lossesVsEasyAI: Int = 0,
    val winsVsMediumAI: Int = 0,
    val lossesVsMediumAI: Int = 0,
    val winsVsHardAI: Int = 0,
    val lossesVsHardAI: Int = 0,
    val passAndPlayGames: Int = 0
)

@JsonClass(generateAdapter = true)
data class AppSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val countdownSoundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val highlightLastMove: Boolean = true,
    val fastAnimations: Boolean = false,
    val defaultDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val selectedBoardTheme: String = "classic_wood",
    val showRulesOnStart: Boolean = true,
    val showLatestActivity: Boolean = false
)
