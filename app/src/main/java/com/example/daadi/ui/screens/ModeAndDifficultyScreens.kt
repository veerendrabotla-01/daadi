package com.example.daadi.ui.screens



import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.model.AIDifficulty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultySelectScreen(
    onStartGame: (AIDifficulty) -> Unit,
    onBack: () -> Unit
) {
    var selectedDifficulty by remember { mutableStateOf(AIDifficulty.MEDIUM) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Opponent Difficulty", fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A),
                    navigationIconContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Choose how skilled the computer opponent should be. Hard mode utilizes full Minimax search algorithms.",
                    fontSize = 14.sp,
                    color = Color(0xFF8B5E3C),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // EASY CARD
                DifficultyCard(
                    difficulty = AIDifficulty.EASY,
                    title = "Sanyasi (Easy Mode)",
                    desc = "Plays mostly random, playful moves. Excellent for beginners learning Daadi rules.",
                    colorLabel = Color(0xFF2E7D32),
                    isSelected = selectedDifficulty == AIDifficulty.EASY,
                    onClick = { selectedDifficulty = AIDifficulty.EASY },
                    modifier = Modifier.testTag("easy_difficulty_card")
                )

                // MEDIUM CARD
                DifficultyCard(
                    difficulty = AIDifficulty.MEDIUM,
                    title = "Mantri (Medium Mode)",
                    desc = "Plays strategic mill blockades and attacks. Highly competitive for casual play.",
                    colorLabel = Color(0xFFE65100),
                    isSelected = selectedDifficulty == AIDifficulty.MEDIUM,
                    onClick = { selectedDifficulty = AIDifficulty.MEDIUM },
                    modifier = Modifier.testTag("medium_difficulty_card")
                )

                // HARD CARD
                DifficultyCard(
                    difficulty = AIDifficulty.HARD,
                    title = "Chanakya (Hard Mode)",
                    desc = "Deep tactical minimax alpha-beta lookaheads. A true test of Indian strategy skill!",
                    colorLabel = Color(0xFFC62828),
                    isSelected = selectedDifficulty == AIDifficulty.HARD,
                    onClick = { selectedDifficulty = AIDifficulty.HARD },
                    modifier = Modifier.testTag("hard_difficulty_card")
                )
            }

            // Begin Button
            Button(
                onClick = { onStartGame(selectedDifficulty) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_match_vs_ai_button")
            ) {
                Text(
                    "START SELECTION",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun DifficultyCard(
    difficulty: AIDifficulty,
    title: String,
    desc: String,
    colorLabel: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFF5C2D0A) else Color.Transparent
    val background = if (isSelected) Color(0xFFFFF7EA) else Color(0xFFFFFBFA)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorLabel)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF5C2D0A),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8B5E3C),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
