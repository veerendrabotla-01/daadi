package com.example.daadi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daadi.data.supabase.SupabaseUser
import com.example.daadi.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    gameViewModel: GameViewModel,
    onBack: () -> Unit,
    onSignInClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val usersState by gameViewModel.userRepository.users.collectAsStateWithLifecycle()
    val currentUser by gameViewModel.authRepository.currentUser.collectAsStateWithLifecycle()

    // Trigger user fetch on entering screen
    LaunchedEffect(Unit) {
        gameViewModel.userRepository.fetchRemoteUsers()
    }

    // Filter and sort users
    val sortedUsers = remember(usersState, selectedTab) {
        val nonBanned = usersState.filter { !it.isBanned }
        when (selectedTab) {
            0 -> nonBanned.sortedByDescending { it.rating }
            1 -> nonBanned.sortedByDescending { it.xp }
            else -> nonBanned.sortedByDescending { it.wins }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFE5A93B),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Daadi Ladder",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color(0xFF5C2D0A)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onBack()
                    }, modifier = Modifier.testTag("leaderboard_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF5C2D0A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Info Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFC75D27),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Win matches against online opponents or challenging computer levels to accumulate rating and rise on the global ladder!",
                            fontSize = 11.sp,
                            color = Color(0xFF8B5E3C),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Aesthetic Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color(0xFF5C2D0A).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("GLOBAL ELO", "ELITE XP", "WAR WINS")
                    tabs.forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedTab == index) Color(0xFF5C2D0A) else Color.Transparent
                                )
                                .clickable { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    selectedTab = index 
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == index) Color.White else Color(0xFF8B5E3C)
                            )
                        }
                    }
                }

                if (sortedUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFC75D27))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Assembling the arena, fetching strategists...",
                                fontSize = 13.sp,
                                color = Color(0xFF8B5E3C)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("leaderboard_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(sortedUsers) { index, user ->
                            val isMe = user.id == currentUser?.id
                            val rank = index + 1
                            LeaderboardRow(
                                rank = rank,
                                user = user,
                                isMe = isMe,
                                selectedTab = selectedTab
                            )
                        }
                    }
                }
            }

            // Persistent bottom card for current user standing
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFFDF3E3).copy(alpha = 0.95f), Color(0xFFFDF3E3))
                        )
                    )
                    .padding(16.dp)
            ) {
                if (currentUser != null) {
                    val myRank = usersState.sortedByDescending { 
                        when (selectedTab) {
                            0 -> it.rating.toFloat()
                            1 -> it.xp.toFloat()
                            2 -> it.wins.toFloat()
                            else -> it.rating.toFloat()
                        }
                    }.indexOfFirst { it.id == currentUser?.id } + 1

                    val myUser = usersState.find { it.id == currentUser?.id } ?: SupabaseUser(
                        id = currentUser!!.id,
                        username = currentUser!!.username,
                        email = currentUser!!.email,
                        role = currentUser!!.role,
                        createdAt = currentUser!!.createdAt,
                        rating = currentUser!!.rating,
                        coins = currentUser!!.coins,
                        xp = currentUser!!.xp,
                        wins = currentUser!!.wins,
                        losses = currentUser!!.losses
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF5C2D0A)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .testTag("leaderboard_my_standing")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFE5A93B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (myRank > 0) "#$myRank" else "-",
                                    color = Color(0xFF5C2D0A),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${myUser.username} (You)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (myUser.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge(containerColor = Color(0xFF4CAF50), contentColor = Color.White) {
                                            Text("✓", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                val rankTier = getRankTier(myUser.rating)
                                Text(
                                    text = "$rankTier • ${myUser.wins}W / ${myUser.losses}L",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = when (selectedTab) {
                                        0 -> "${myUser.rating} Elo"
                                        1 -> "${myUser.xp} XP"
                                        else -> "${myUser.wins} Wins"
                                    },
                                    color = Color(0xFFE5A93B),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF5C2D0A).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .clickable { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onSignInClick() 
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = Color(0xFFC75D27)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "SIGN IN TO JOIN THE LEADERBOARD LADDER",
                                color = Color(0xFF5C2D0A),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardRow(
    rank: Int,
    user: SupabaseUser,
    isMe: Boolean,
    selectedTab: Int
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFD4AF37) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.Transparent
    }

    val itemBg = if (isMe) Color(0xFF5C2D0A).copy(alpha = 0.08f) else Color.White
    val borderStroke = if (isMe) Modifier.border(1.5.dp, Color(0xFFC75D27), RoundedCornerShape(16.dp)) else Modifier

    Card(
        colors = CardDefaults.cardColors(containerColor = itemBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(borderStroke)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (rank <= 3) rankColor else Color(0xFF5C2D0A).copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (rank <= 3) {
                    val emoji = when(rank) {
                        1 -> "🥇"
                        2 -> "🥈"
                        else -> "🥉"
                    }
                    Text(emoji, fontSize = 16.sp)
                } else {
                    Text(
                        text = "#$rank",
                        color = Color(0xFF5C2D0A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFC75D27).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFC75D27),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Username and Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF5C2D0A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Badge(containerColor = Color(0xFF4CAF50), contentColor = Color.White) {
                            Text("✓", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isMe) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "(YOU)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFC75D27)
                        )
                    }
                }
                val tier = getRankTier(user.rating)
                val total = user.wins + user.losses
                val winRatio = if (total > 0) "${(user.wins * 100) / total}% winrate" else "No matches"
                Text(
                    text = "$tier • $winRatio",
                    color = Color(0xFF8B5E3C),
                    fontSize = 11.sp
                )
            }

            // Stat Value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when (selectedTab) {
                        0 -> "${user.rating} Elo"
                        1 -> "${user.xp} XP"
                        else -> "${user.wins} Wins"
                    },
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF5C2D0A),
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun getRankTier(rating: Int): String {
    return when {
        rating >= 2400 -> "Grandmaster 👑"
        rating >= 2000 -> "Master 🌟"
        rating >= 1700 -> "Strategist ⚔️"
        rating >= 1400 -> "Adept 🛡️"
        rating >= 1100 -> "Apprentice 📖"
        else -> "Rookie 🌱"
    }
}
