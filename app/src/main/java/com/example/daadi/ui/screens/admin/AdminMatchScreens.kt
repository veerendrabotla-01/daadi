package com.example.daadi.ui.screens.admin



import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    onBack: () -> Unit
) {
    val matches by adminViewModel.remoteGameRepository.matches.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var selectedMatch by remember { mutableStateOf<SupabaseMatch?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    BoxWithConstraints {
        val isWide = maxWidth >= 900.dp
        
        if (isWide) {
            AdminWideMatchManagement(
                matches = matches,
                isSyncing = isSyncing,
                selectedMatch = selectedMatch,
                onMatchSelect = { selectedMatch = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                adminViewModel = adminViewModel,
                onBack = onBack
            )
        } else {
            if (selectedMatch == null) {
                AdminMatchArchiveScreen(
                    matches = matches,
                    isSyncing = isSyncing,
                    onMatchClick = { selectedMatch = it },
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    adminViewModel = adminViewModel,
                    onBack = onBack
                )
            } else {
                AdminMatchDetailScreen(
                    match = selectedMatch!!,
                    onBack = { selectedMatch = null }
                )
            }
        }
    }
}

@Composable
fun AdminWideMatchManagement(
    matches: List<SupabaseMatch>,
    isSyncing: Boolean,
    selectedMatch: SupabaseMatch?,
    onMatchSelect: (SupabaseMatch) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    AdminFoundationScaffold(
        title = "Match Archive",
        adminViewModel = adminViewModel,
        onBack = onBack,
        showSearch = true,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Left Panel: List
            Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                MatchListContent(
                    matches = matches,
                    isSyncing = isSyncing,
                    searchQuery = searchQuery,
                    onMatchClick = onMatchSelect,
                    selectedMatchId = selectedMatch?.id,
                    adminViewModel = adminViewModel
                )
            }
            VerticalDivider(color = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)
            
            // Right Panel: Details
            Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                if (selectedMatch != null) {
                    MatchDetailContent(match = selectedMatch)
                } else {
                    AdminEmptyState(
                        title = "No Match Selected",
                        description = "Select a game record from the archive to view technical details and move history.",
                        icon = { Icon(Icons.Default.ManageSearch, contentDescription = null, modifier = Modifier.size(64.dp), tint = AdminDesign.OnSurfaceVariant) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminMatchArchiveScreen(
    matches: List<SupabaseMatch>,
    isSyncing: Boolean,
    onMatchClick: (SupabaseMatch) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    AdminFoundationScaffold(
        title = "Match Archive",
        adminViewModel = adminViewModel,
        onBack = onBack,
        showSearch = true,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchChange
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            MatchListContent(
                matches = matches,
                isSyncing = isSyncing,
                searchQuery = searchQuery,
                onMatchClick = onMatchClick,
                adminViewModel = adminViewModel
            )
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
fun AdminMatchDetailScreen(
    match: SupabaseMatch,
    onBack: () -> Unit
) {
    AdminFoundationScaffold(
        title = "Match Details",
        supabaseManager = null, // No need for refresh here
        onBack = onBack
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            MatchDetailContent(match = match)
        }
    }
}

@Composable
fun MatchDetailContent(match: SupabaseMatch) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium), verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    Text("MATCH SUMMARY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    DetailRow("Match ID", match.id)
                    DetailRow("Status", match.status.uppercase())
                    DetailRow("Type", match.matchType.uppercase())
                    DetailRow("Created At", match.createdAt)
                    DetailRow("Moves", match.movesCount.toString())
                    DetailRow("Latency", "${match.latencyMs}ms")
                    if (match.abandonedBy != null) DetailRow("Abandoned By", match.abandonedBy!!)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    Text("PARTICIPANTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerIcon(match.hostName)
                        Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                        Text(match.hostName, fontWeight = FontWeight.Bold)
                        if (match.winner == match.hostName) {
                            Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AdminDesign.Secondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerIcon(match.opponentName.ifEmpty { "Waiting..." })
                        Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                        Text(match.opponentName.ifEmpty { "Waiting..." }, fontWeight = FontWeight.Bold)
                        if (match.winner == match.opponentName) {
                            Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AdminDesign.Secondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        
        item {
            Text("MOVE LOGS (JSON)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
                Box(modifier = Modifier.padding(AdminDesign.SpacingMedium).fillMaxWidth().heightIn(min = 200.dp)) {
                    if (match.movesJson.isNullOrBlank()) {
                        Text("No telemetry data available for this session.", color = AdminDesign.OnSurfaceVariant, fontSize = 12.sp)
                    } else {
                        Text(match.movesJson, fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                    }
                }
            }
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
private fun PlayerIcon(name: String) {
    Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = AdminDesign.Primary.copy(alpha = 0.1f)) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
        }
    }
}
