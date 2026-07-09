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
import androidx.compose.ui.layout.onGloballyPositioned
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
    hintMove: Pair<Int?, Int>? = null,
    highlightLastMove: Boolean = true,
    lastMove: com.example.daadi.model.GameMove? = null,
    ruleSet: com.example.daadi.model.RuleSet = com.example.daadi.model.RuleSet.NINE_MENS_MORRIS
) {
    // Persistent state for piece offsets to enable smooth transitions
    // key: current nodeId where piece is intended to be
    val piecePositions = remember { mutableStateMapOf<Int, Animatable<Offset, AnimationVector2D>>() }

    // Sync animation states with actual board model changes
    LaunchedEffect(board) {
        val last = lastMove
        if (last != null && last.type == com.example.daadi.model.MoveType.MOVE) {
            val from = last.fromNode!!
            val to = last.toNode
            if (piecePositions.containsKey(from) && !piecePositions.containsKey(to)) {
                // Transfer the existing animator to the brand new location
                val anim = piecePositions.remove(from)!!
                piecePositions[to] = anim
            }
        }

        // 1. Clean up stale animators (captures or unexpected drifts)
        val currentOccupied = board.nodes.filter { it.value != null }.keys
        val stale = piecePositions.keys.filter { it !in currentOccupied }
        stale.forEach { piecePositions.remove(it) }

        // 2. Initialize animators for newly placed pieces
        for (nodeId in currentOccupied) {
            if (!piecePositions.containsKey(nodeId)) {
                val pos = BoardDefinition.COORDINATES[nodeId] ?: Pair(0.5f, 0.5f)
                piecePositions[nodeId] = Animatable(Offset(pos.first, pos.second), Offset.VectorConverter)
            }
        }
    }

    // Trigger animations for any piece whose node has changed
    piecePositions.forEach { (nodeId, animatable) ->
        val target = BoardDefinition.COORDINATES[nodeId] ?: Pair(0.5f, 0.5f)
        val destination = Offset(target.first, target.second)
        LaunchedEffect(nodeId, destination) {
            if (animatable.targetValue != destination) {
                animatable.animateTo(
                    targetValue = destination,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            }
        }
    }

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
        "marble" -> Color(0xFF7F8C8D) // Polished silver-gray connections
        "charcoal" -> Color(0xFFF1C40F) // Bright golden accents on dark slate
        "saffron" -> Color(0xFFD35400) // Rich vermilion connections
        else -> Color(0xFF2C3E50) // Slate-stone graphite connections
    }
    val boardFillColor = when (boardTheme) {
        "classic_wood" -> Color(0xFFEAA65D)
        "emerald_jade" -> Color(0xFF064E3B) // Immersive emerald green jade board
        "marble" -> Color(0xFFECF0F1) // Pure white marble surface
        "charcoal" -> Color(0xFF2C3E50) // Stealth midnight-charcoal surface
        "saffron" -> Color(0xFFF39C12) // Golden saffron Indian festival surface
        else -> Color(0xFF34495E) // Matte dark slate canvas
    }
    val emptyNodeColor = when (boardTheme) {
        "classic_wood" -> Color(0xFF8B5E3C)
        "emerald_jade" -> Color(0xFFA7F3D0) // Mint-toned placeholders
        "marble" -> Color(0xFFBDC3C7) // Cool gray slots
        "charcoal" -> Color(0xFF7F8C8D) // Slate slots
        "saffron" -> Color(0xFFE67E22) // Burnt orange slots
        else -> Color(0xFFBDC3C7)
    }

    // --- TELEMETRY TRACKER ---
    val drawStartTime = System.nanoTime()
    var lastLogTime by remember { mutableLongStateOf(0L) }

    // Pre-calculate density-dependent values to avoid reading density in every draw frame
    val density = androidx.compose.ui.platform.LocalDensity.current
    val strokeWidth = remember(density) { with(density) { 5.dp.toPx() } }
    val emptyRadius = remember(density) { with(density) { 6.dp.toPx() } }
    val pieceRadius = remember(density) { with(density) { 13.5.dp.toPx() } }
    val shadowOffset = remember(density) { with(density) { 2.dp.toPx() } }

    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val nodePixels = remember(canvasSize) {
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        if (w == 0f || h == 0f) {
            BoardDefinition.COORDINATES.mapValues { (_, coords) -> Offset(0f, 0f) }
        } else {
            BoardDefinition.COORDINATES.mapValues { (_, coords) ->
                Offset(coords.first * w, coords.second * h)
            }
        }
    }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("game_board_canvas")
            .onGloballyPositioned { coordinates ->
                canvasSize = coordinates.size
            }
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

        // Capture telemetry for frame overhead with throttling
        val drawDurationMs = (System.nanoTime() - drawStartTime) / 1_000_000
        val currentTime = System.currentTimeMillis()
        if (drawDurationMs > 16 && (currentTime - lastLogTime) > 2000) { 
            // Throttled logging to avoid overhead
            com.example.daadi.util.SecureLog.w("DAADI_TELEMETRY", "Slow Frame Detected: Board Draw took ${drawDurationMs}ms")
            lastLogTime = currentTime
        }

        // Outer square
        drawLine(boardLineColor, nodePixels[0]!!, nodePixels[2]!!, strokeWidth)
        drawLine(boardLineColor, nodePixels[2]!!, nodePixels[4]!!, strokeWidth)
        drawLine(boardLineColor, nodePixels[4]!!, nodePixels[6]!!, strokeWidth)
        drawLine(boardLineColor, nodePixels[6]!!, nodePixels[0]!!, strokeWidth)

        // Middle square
        drawLine(boardLineColor, nodePixels[8]!!, nodePixels[10]!!, strokeWidth)
        drawLine(boardLineColor, nodePixels[10]!!, nodePixels[12]!!, strokeWidth)
        drawLine(boardLineColor, nodePixels[12]!!, nodePixels[14]!!, strokeWidth)
        drawLine(boardLineColor, nodePixels[14]!!, nodePixels[8]!!, strokeWidth)

        // Inner square
        drawLine(boardLineColor, nodePixels[16]!!, nodePixels[18]!!, strokeWidth)
        drawLine(boardLineColor, nodePixels[18]!!, nodePixels[20]!!, strokeWidth)
        drawLine(boardLineColor, nodePixels[20]!!, nodePixels[22]!!, strokeWidth)
        drawLine(boardLineColor, nodePixels[22]!!, nodePixels[16]!!, strokeWidth)

        // Cross central connectors
        drawLine(boardLineColor, nodePixels[1]!!, nodePixels[17]!!, strokeWidth) // Top cross
        drawLine(boardLineColor, nodePixels[3]!!, nodePixels[19]!!, strokeWidth) // Right cross
        drawLine(boardLineColor, nodePixels[5]!!, nodePixels[21]!!, strokeWidth) // Bottom cross
        drawLine(boardLineColor, nodePixels[7]!!, nodePixels[23]!!, strokeWidth) // Left cross

        // Draw diagonal connectors for 12 Men's Morris
        if (ruleSet == com.example.daadi.model.RuleSet.TWELVE_MENS_MORRIS) {
            drawLine(boardLineColor, nodePixels[0]!!, nodePixels[16]!!, strokeWidth) // Top-Left diagonal
            drawLine(boardLineColor, nodePixels[2]!!, nodePixels[18]!!, strokeWidth) // Top-Right diagonal
            drawLine(boardLineColor, nodePixels[4]!!, nodePixels[20]!!, strokeWidth) // Bottom-Right diagonal
            drawLine(boardLineColor, nodePixels[6]!!, nodePixels[22]!!, strokeWidth) // Bottom-Left diagonal
        }

        // 2. Draw empty nodes as placeholders/receptacles
        val emptyRadius = 6.dp.toPx()
        for ((nodeId, _) in BoardDefinition.COORDINATES) {
            val center = nodePixels[nodeId]!!
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
        val pieceRadius = 13.5.dp.toPx()
        val shadowOffset = 2.dp.toPx()

        for ((nodeId, animatable) in piecePositions) {
            val occupant = board.nodes[nodeId] ?: continue
            val currentPos = animatable.value
            val center = Offset(currentPos.x * w, currentPos.y * h)

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
                    style = Stroke(width = 2.5.dp.toPx())
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

        // 4. Draw Neon cyan hint aura over recommended Node(s)
        if (hintMove != null) {
            // Highlight target node
            val targetCenter = nodePixels[hintMove.second]!!
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = pieceRadius * 1.6f * scaleSelection,
                center = targetCenter,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = pieceRadius * 0.4f,
                center = targetCenter
            )

            // Highlight source node if it's a move (not placement)
            if (hintMove.first != null) {
                val sourceCenter = nodePixels[hintMove.first!!]!!
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = pieceRadius * 1.2f,
                    center = sourceCenter,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
                )
            }
        }

        // 5. Draw Gold highlight for the most recent move
        if (highlightLastMove && lastMove?.toNode != null) {
            val center = nodePixels[lastMove.toNode]!!
            drawCircle(
                color = Color(0xFFD4A55A),
                radius = pieceRadius * 1.4f,
                center = center,
                style = Stroke(width = 3.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)),
                alpha = 0.6f
            )
        }
    }
}
