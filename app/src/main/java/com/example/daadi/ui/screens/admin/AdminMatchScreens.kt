package com.example.daadi.ui.screens.admin



import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseMatch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminMatchManagementScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit = {},
    onBack: () -> Unit
) {
    val matches by adminViewModel.remoteGameRepository.matches.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    val users by adminViewModel.userRepository.users.collectAsStateWithLifecycle()
    val filterMatchId = adminViewModel.filterMatchId.value ?: ""
    val filterUsername = adminViewModel.filterUsername.value ?: ""
    
    var selectedMatch by remember { mutableStateOf<SupabaseMatch?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active, 1: Archive

    LaunchedEffect(filterMatchId) {
        if (filterMatchId.isNotEmpty()) {
            searchQuery = filterMatchId
        }
    }

    LaunchedEffect(filterUsername) {
        if (filterUsername.isNotEmpty() && filterMatchId.isEmpty()) {
            searchQuery = filterUsername
        }
    }

    val liveMatches = matches.filter { it.status == "playing" || it.status == "paused" || it.status == "waiting" }
    val archivedMatches = matches.filter { it.status == "finished" || it.status == "terminated" }

    AdminFoundationScaffold(
        title = "Live Match Center",
        adminViewModel = adminViewModel,
        onBack = onBack,
        showSearch = true,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (filterMatchId.isNotEmpty() || filterUsername.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AdminDesign.SpacingMedium, vertical = 4.dp),
                    color = AdminDesign.Primary.copy(alpha = 0.1f),
                    shape = AdminDesign.CardShape
                ) {
                    Row(
                        modifier = Modifier.padding(AdminDesign.SpacingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (filterMatchId.isNotEmpty()) "Filtering match ID: $filterMatchId" else "Filtering player: $filterUsername", 
                            fontWeight = FontWeight.Bold, 
                            color = AdminDesign.Primary, 
                            fontSize = 12.sp
                        )
                        TextButton(onClick = { 
                            adminViewModel.filterMatchId.value = ""
                            adminViewModel.filterUsername.value = ""
                            searchQuery = ""
                        }) {
                            Text("Clear Filter", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = AdminDesign.Primary) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Active Sessions (${liveMatches.size})", fontSize = 12.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Match Archive (${archivedMatches.size})", fontSize = 12.sp) })
            }

            val displayMatches = if (selectedTab == 0) liveMatches else archivedMatches

            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val isWide = maxWidth >= 900.dp
                if (isWide) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(0.4f)) {
                            MatchListContent(
                                matches = displayMatches,
                                isSyncing = isSyncing,
                                searchQuery = searchQuery,
                                onMatchClick = { selectedMatch = it },
                                selectedMatchId = selectedMatch?.id,
                                adminViewModel = adminViewModel
                            )
                        }
                        VerticalDivider(color = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f))
                        Box(modifier = Modifier.weight(0.6f)) {
                            if (selectedMatch != null) {
                                MatchDetailContent(match = selectedMatch!!, users = users, onUserClick = onUserClick, adminViewModel = adminViewModel)
                            } else {
                                AdminEmptyState("No Match Selected", "Select a session to view telemetry.")
                            }
                        }
                    }
                } else {
                    if (selectedMatch == null) {
                        MatchListContent(
                            matches = displayMatches,
                            isSyncing = isSyncing,
                            searchQuery = searchQuery,
                            onMatchClick = { selectedMatch = it },
                            adminViewModel = adminViewModel
                        )
                    } else {
                        MatchDetailContent(match = selectedMatch!!, users = users, onUserClick = onUserClick, adminViewModel = adminViewModel, onBack = { selectedMatch = null })
                    }
                }
            }
        }
    }
}

@Composable
fun MatchListContent(
    matches: List<SupabaseMatch>,
    isSyncing: Boolean,
    searchQuery: String,
    onMatchClick: (SupabaseMatch) -> Unit,
    selectedMatchId: String? = null,
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel
) {
    val filteredMatches = remember(matches, searchQuery) {
        matches.filter { 
            it.id.contains(searchQuery, true) || 
            it.hostName.contains(searchQuery, true) ||
            it.opponentName.contains(searchQuery, true)
        }
    }

    if (isSyncing && matches.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            items(10) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
        }
    } else if (filteredMatches.isEmpty()) {
        AdminEmptyState(title = "No Matches Found", description = "Try a different search term or check filters.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            items(filteredMatches) { match ->
                MatchArchiveItem(
                    match = match, 
                    onClick = { onMatchClick(match) },
                    onDelete = { adminViewModel.remoteGameRepository.deleteMatch(match.id) },
                    isSelected = match.id == selectedMatchId
                )
            }
        }
    }
}

@Composable
fun MatchArchiveItem(match: SupabaseMatch, onClick: () -> Unit, onDelete: () -> Unit, isSelected: Boolean = false) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AdminDesign.Primary.copy(alpha = 0.05f) else AdminDesign.Surface
        ),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else AdminDesign.CardElevation),
        modifier = Modifier.fillMaxWidth().border(
            width = if (isSelected) 2.dp else 0.dp,
            color = if (isSelected) AdminDesign.Primary else Color.Transparent,
            shape = AdminDesign.CardShape
        )
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("ID: ${match.id.take(8).uppercase()}", fontWeight = FontWeight.Black, fontSize = 11.sp, color = AdminDesign.Primary)
                Badge(
                    containerColor = when(match.status) {
                        "finished" -> AdminDesign.Secondary
                        "playing" -> AdminDesign.Primary
                        else -> AdminDesign.OnSurfaceVariant
                    }
                ) {
                    Text(match.status.uppercase(), fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Text("${match.hostName} VS ${match.opponentName.ifEmpty { "..." }}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = AdminDesign.OnSurface)
            Text("${match.matchType.uppercase()} • ${match.movesCount} Moves • ${match.createdAt}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
            
            if (match.winner != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(12.dp), tint = AdminDesign.Secondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Winner: ${match.winner}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdminDesign.Secondary)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AdminDesign.Error.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun MatchDetailContent(
    match: SupabaseMatch, 
    users: List<com.example.daadi.data.supabase.SupabaseUser>,
    onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit,
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isUpdating by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (onBack != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                Text("Match Details", fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("TELEMETRY & ENFORCEMENT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                            Spacer(modifier = Modifier.weight(1f))
                            if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                        
                        // Action Row
                        if (match.status != "finished" && match.status != "terminated") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { 
                                        isUpdating = true
                                        val newStatus = if (match.status == "paused") "playing" else "paused"
                                        adminViewModel.remoteGameRepository.updateMatchStatus(match.id, newStatus) { isUpdating = false }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (match.status == "paused") AdminDesign.Success else AdminDesign.Primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(if (match.status == "paused") Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (match.status == "paused") "Resume" else "Pause", fontSize = 11.sp)
                                }
                                
                                Button(
                                    onClick = { 
                                        isUpdating = true
                                        adminViewModel.remoteGameRepository.updateMatchStatus(match.id, "terminated") { isUpdating = false }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Error),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Terminate", fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        DetailRow("Match ID", match.id)
                        DetailRow("Server Region", match.serverRegion ?: "Asia-South (Mumbai)")
                        DetailRow("Status", match.status.uppercase())
                        DetailRow("Latency", "${match.latencyMs}ms")
                        DetailRow("Moves", match.movesCount.toString())
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Text("SQUAD ANALYTICS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                        val hostUser = users.find { it.username.equals(match.hostName, ignoreCase = true) }
                        PlayerDetailItem(match.hostName, "HOST", isWinner = match.winner == match.hostName, user = hostUser, onUserClick = onUserClick)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                        val oppUser = users.find { it.username.equals(match.opponentName, ignoreCase = true) }
                        PlayerDetailItem(match.opponentName.ifEmpty { "WAITING..." }, "OPPONENT", isWinner = match.winner == match.opponentName, user = oppUser, onUserClick = onUserClick)
                    }
                }
            }

            item {
                Text("LIVE MOVE FEED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
                    Box(modifier = Modifier.padding(AdminDesign.SpacingMedium).fillMaxWidth().heightIn(min = 150.dp)) {
                        Text(
                            text = match.movesJson?.ifBlank { "Awaiting match start telemetry..." } ?: "No active telemetry found.",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = AdminDesign.OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerDetailItem(
    name: String, 
    role: String, 
    isWinner: Boolean, 
    user: com.example.daadi.data.supabase.SupabaseUser? = null, 
    onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (user != null) {
                    Modifier.clickable { onUserClick(user) }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 4.dp)
    ) {
        PlayerIcon(name)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (user != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Launch, 
                        contentDescription = "View Profile", 
                        tint = AdminDesign.Primary, 
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(role, fontSize = 9.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isWinner) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface)
    }
}

@Composable
fun PlayerIcon(name: String) {
    Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = AdminDesign.Primary.copy(alpha = 0.1f)) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
        }
    }
}
