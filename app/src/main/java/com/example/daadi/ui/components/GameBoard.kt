package com.example.daadi.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.daadi.engine.BoardDefinition
import com.example.daadi.model.Board
import com.example.daadi.model.Player
import kotlin.math.sqrt

@Composable
fun GameBoard(
    board: Board,
    selectedNodeId: Int?,
    validDestinationNodes: List<Int>,
    recentInvalidNode: Int?,
    onNodeTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
    boardTheme: String = "classic_wood",
    hintNodeId: Int? = null
) {
    // Pulsing animations for highlights
    val infiniteTransition = rememberInfiniteTransition(label = "board_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val scaleSelection by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_selection"
    )

    // Set colors according to theme
    val boardLineColor = when (boardTheme) {
        "classic_wood" -> Color(0xFF5C2D0A)
        "emerald_jade" -> Color(0xFFD4AF37) // Gold connections
        else -> Color(0xFF2C3E50) // Slate-stone graphite connections
    }
    val boardFillColor = when (boardTheme) {
        "classic_wood" -> Color(0xFFEAA65D)
        "emerald_jade" -> Color(0xFF064E3B) // Immersive emerald green jade board
        else -> Color(0xFF34495E) // Matte dark slate canvas
    }
    val emptyNodeColor = when (boardTheme) {
        "classic_wood" -> Color(0xFF8B5E3C)
        "emerald_jade" -> Color(0xFFA7F3D0) // Mint-toned placeholders
        else -> Color(0xFFBDC3C7)
    }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("game_board_canvas")
            .pointerInput(board) {
                detectTapGestures { offset ->
                    // Map tap coordinates (pixels) to ratios
                    val rx = offset.x / size.width
                    val ry = offset.y / size.height

                    // Find nearest node where distance is < threshold (0.07 ratio unit)
                    var nearestNodeId = -1
                    var minDistance = Float.MAX_VALUE

                    for ((nodeId, coords) in BoardDefinition.COORDINATES) {
                        val dx = rx - coords.first
                        val dy = ry - coords.second
                        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        if (dist < minDistance && dist < 0.08f) {
                            minDistance = dist
                            nearestNodeId = nodeId
                        }
                    }

                    if (nearestNodeId != -1) {
                        onNodeTapped(nearestNodeId)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw outer, middle, inner concentric squares
        // We use BoardDefinition coordinates mapping to actual pixel points
        fun getPixel(nodeId: Int): Offset {
            val coords = BoardDefinition.COORDINATES[nodeId] ?: Pair(0.5f, 0.5f)
            return Offset(coords.first * w, coords.second * h)
        }

        val strokeWidth = 5.dp.toPx()

        // Outer square
        drawLine(boardLineColor, getPixel(0), getPixel(2), strokeWidth)
        drawLine(boardLineColor, getPixel(2), getPixel(4), strokeWidth)
        drawLine(boardLineColor, getPixel(4), getPixel(6), strokeWidth)
        drawLine(boardLineColor, getPixel(6), getPixel(0), strokeWidth)

        // Middle square
        drawLine(boardLineColor, getPixel(8), getPixel(10), strokeWidth)
        drawLine(boardLineColor, getPixel(10), getPixel(12), strokeWidth)
        drawLine(boardLineColor, getPixel(12), getPixel(14), strokeWidth)
        drawLine(boardLineColor, getPixel(14), getPixel(8), strokeWidth)

        // Inner square
        drawLine(boardLineColor, getPixel(16), getPixel(18), strokeWidth)
        drawLine(boardLineColor, getPixel(18), getPixel(20), strokeWidth)
        drawLine(boardLineColor, getPixel(20), getPixel(22), strokeWidth)
        drawLine(boardLineColor, getPixel(22), getPixel(16), strokeWidth)

        // Cross central connectors
        drawLine(boardLineColor, getPixel(1), getPixel(17), strokeWidth) // Top cross
        drawLine(boardLineColor, getPixel(3), getPixel(19), strokeWidth) // Right cross
        drawLine(boardLineColor, getPixel(5), getPixel(21), strokeWidth) // Bottom cross
        drawLine(boardLineColor, getPixel(7), getPixel(23), strokeWidth) // Left cross

        // 2. Draw empty nodes as placeholders/receptacles
        val emptyRadius = 8.dp.toPx()
        for ((nodeId, _) in BoardDefinition.COORDINATES) {
            val center = getPixel(nodeId)
            if (board.nodes[nodeId] == null) {
                // If it is a valid destination node, draw pulsing gold target ring!
                if (nodeId in validDestinationNodes) {
                    drawCircle(
                        color = Color(0xFFE5A93B),
                        radius = emptyRadius * 1.8f,
                        center = center,
                        alpha = pulseAlpha
                    )
                    drawCircle(
                        color = Color.White,
                        radius = emptyRadius * 0.9f,
                        center = center
                    )
                } else {
                    // Draw normal empty marker
                    drawCircle(
                        color = emptyNodeColor,
                        radius = emptyRadius * 0.7f,
                        center = center
                    )
                }
            }
        }

        // 3. Draw active player pieces as gorgeous beads
        val pieceRadius = 18.dp.toPx()
        val shadowOffset = 3.dp.toPx()

        for ((nodeId, occupant) in board.nodes) {
            if (occupant != null) {
                val center = getPixel(nodeId)

                // Selectors/Flashing color states
                val baseColor = if (occupant == Player.PLAYER_1) Color(0xFFCC2222) else Color(0xFF1A5276)
                val glossColor = if (occupant == Player.PLAYER_1) Color(0xFFFF8A8A) else Color(0xFF5DADE2)

                // Recent invalid move flashes red
                val finalBaseColor = if (nodeId == recentInvalidNode) Color(0xFFE74C3C) else baseColor

                // 3a. Drop Shadow (for a beautiful 3D look!)
                drawCircle(
                    color = Color(0x3C000000),
                    radius = pieceRadius,
                    center = center + Offset(shadowOffset, shadowOffset)
                )

                // 3b. Highlight Selection ring if active
                if (nodeId == selectedNodeId) {
                    drawCircle(
                        color = Color(0xFFE5A93B),
                        radius = pieceRadius * scaleSelection,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // 3c. Draw master color bead
                drawCircle(
                    color = finalBaseColor,
                    radius = pieceRadius,
                    center = center
                )

                // 3d. Accent glint/gloss (Top Left highlight)
                drawCircle(
                    color = glossColor,
                    radius = pieceRadius * 0.35f,
                    center = center - Offset(pieceRadius * 0.3f, pieceRadius * 0.3f)
                )
            }
        }

        // 4. Draw Neon cyan hint aura over recommended Node
        if (hintNodeId != null) {
            val center = getPixel(hintNodeId)
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = pieceRadius * 1.6f * scaleSelection,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = pieceRadius * 0.4f,
                center = center
            )
        }
    }
}
