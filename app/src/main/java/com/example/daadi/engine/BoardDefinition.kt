package com.example.daadi.engine

object BoardDefinition {
    // 24 Node coordinates as (x, y) ratio between 0f and 1f
    val COORDINATES: Map<Int, Pair<Float, Float>> = mapOf(
        // Outer square (0 to 7)
        0 to Pair(0.02f, 0.02f),
        1 to Pair(0.50f, 0.02f),
        2 to Pair(0.98f, 0.02f),
        3 to Pair(0.98f, 0.50f),
        4 to Pair(0.98f, 0.98f),
        5 to Pair(0.50f, 0.98f),
        6 to Pair(0.02f, 0.98f),
        7 to Pair(0.02f, 0.50f),

        // Middle square (8 to 15)
        8 to Pair(0.18f, 0.18f),
        9 to Pair(0.50f, 0.18f),
        10 to Pair(0.82f, 0.18f),
        11 to Pair(0.82f, 0.50f),
        12 to Pair(0.82f, 0.82f),
        13 to Pair(0.50f, 0.82f),
        14 to Pair(0.18f, 0.82f),
        15 to Pair(0.18f, 0.50f),

        // Inner square (16 to 23)
        16 to Pair(0.34f, 0.34f),
        17 to Pair(0.50f, 0.34f),
        18 to Pair(0.66f, 0.34f),
        19 to Pair(0.66f, 0.50f),
        20 to Pair(0.66f, 0.66f),
        21 to Pair(0.50f, 0.66f),
        22 to Pair(0.34f, 0.66f),
        23 to Pair(0.34f, 0.50f)
    )

    // Adjacency connections for sliding pieces in MOVEMENT phase
    val CONNECTIONS: Map<Int, List<Int>> = mapOf(
        // Outer corners & side-centers
        0 to listOf(1, 7),
        1 to listOf(0, 2, 9),
        2 to listOf(1, 3),
        3 to listOf(2, 4, 11),
        4 to listOf(3, 5),
        5 to listOf(4, 6, 13),
        6 to listOf(5, 7),
        7 to listOf(6, 0, 15),

        // Middle corners & side-centers
        8 to listOf(9, 15),
        9 to listOf(8, 10, 1, 17),
        10 to listOf(9, 11),
        11 to listOf(10, 12, 3, 19),
        12 to listOf(11, 13),
        13 to listOf(12, 14, 5, 21),
        14 to listOf(13, 15),
        15 to listOf(14, 8, 7, 23),

        // Inner corners & side-centers
        16 to listOf(17, 23),
        17 to listOf(16, 18, 9),
        18 to listOf(17, 19),
        19 to listOf(18, 20, 11),
        20 to listOf(19, 21),
        21 to listOf(20, 22, 13),
        22 to listOf(21, 23),
        23 to listOf(22, 16, 15)
    )

    val DIAGONAL_CONNECTIONS: Map<Int, List<Int>> = mapOf(
        0 to listOf(8), 8 to listOf(0, 16), 16 to listOf(8),
        2 to listOf(10), 10 to listOf(2, 18), 18 to listOf(10),
        4 to listOf(12), 12 to listOf(4, 20), 20 to listOf(12),
        6 to listOf(14), 14 to listOf(6, 22), 22 to listOf(14)
    )

    fun getConnections(nodeId: Int, ruleSet: com.example.daadi.model.RuleSet): List<Int> {
        val base = CONNECTIONS[nodeId] ?: emptyList()
        if (ruleSet == com.example.daadi.model.RuleSet.TWELVE_MENS_MORRIS) {
            val diag = DIAGONAL_CONNECTIONS[nodeId] ?: emptyList()
            return base + diag
        }
        return base
    }

    // The 16 possible mills (3-in-a-row straight lines)
    val MILLS: List<Triple<Int, Int, Int>> = listOf(
        // Outer Horizontal
        Triple(0, 1, 2),
        Triple(6, 5, 4),
        // Outer Vertical
        Triple(0, 7, 6),
        Triple(2, 3, 4),

        // Middle Horizontal
        Triple(8, 9, 10),
        Triple(14, 13, 12),
        // Middle Vertical
        Triple(8, 15, 14),
        Triple(10, 11, 12),

        // Inner Horizontal
        Triple(16, 17, 18),
        Triple(22, 21, 20),
        // Inner Vertical
        Triple(16, 23, 22),
        Triple(18, 19, 20),

        // Side Center Connectors (Crosses)
        Triple(1, 9, 17),  // Top Cross
        Triple(3, 11, 19), // Right Cross
        Triple(5, 13, 21), // Bottom Cross
        Triple(7, 15, 23)  // Left Cross
    )

    val DIAGONAL_MILLS: List<Triple<Int, Int, Int>> = listOf(
        Triple(0, 8, 16),
        Triple(2, 10, 18),
        Triple(4, 12, 20),
        Triple(6, 14, 22)
    )

    // Readable names for nodes (e.g., "Outer Top-Left")
    fun getNodeName(nodeId: Int): String {
        val ring = when (nodeId) {
            in 0..7 -> "Outer"
            in 8..15 -> "Middle"
            in 16..23 -> "Inner"
            else -> "Unknown"
        }
        val posInRing = nodeId % 8
        val position = when (posInRing) {
            0 -> "Top-Left"
            1 -> "Top-Center"
            2 -> "Top-Right"
            3 -> "Middle-Right"
            4 -> "Bottom-Right"
            5 -> "Bottom-Center"
            6 -> "Bottom-Left"
            7 -> "Middle-Left"
            else -> "Unknown"
        }
        return "$ring $position"
    }

    fun getMills(ruleSet: com.example.daadi.model.RuleSet): List<Triple<Int, Int, Int>> {
        if (ruleSet == com.example.daadi.model.RuleSet.TWELVE_MENS_MORRIS) {
            return MILLS + DIAGONAL_MILLS
        }
        return MILLS
    }
}
