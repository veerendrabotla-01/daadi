package com.example.daadi.ui.screens.admin



import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminLeaderboardManagerScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    val users by adminViewModel.userRepository.users.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var selectedScope by remember { mutableStateOf("Global") }
    var selectedRegion by remember { mutableStateOf("All Regions") }
    var showResetDialog by remember { mutableStateOf(false) }
    
    val scopes = listOf("Global", "Weekly", "Monthly", "Season")
    val regions = listOf("All Regions", "Asia", "Europe", "Americas", "Africa")
    
    val sortedUsers = remember(users, selectedScope, selectedRegion) {
        users.filter { 
            selectedRegion == "All Regions" || it.email.contains(".in") // Simple mock region filter
        }.sortedByDescending { 
            when (selectedScope) {
                "Global" -> it.rating.toFloat()
                "Weekly" -> it.wins.toFloat()
                "Monthly" -> it.xp.toFloat()
                "Season" -> (it.wins * 10 + it.xp).toFloat()
                else -> it.rating.toFloat()
            }
        }.take(50)
    }

    AdminFoundationScaffold(
        title = "Elo Rankings",
        adminViewModel = adminViewModel,
        onBack = onBack,
        actions = {
            IconButton(onClick = { /* Simulated Recalculate */ }) { Icon(Icons.Default.Autorenew, contentDescription = "Recalculate", tint = AdminDesign.Primary) }
            IconButton(onClick = { showResetDialog = true }) { Icon(Icons.Default.Refresh, contentDescription = "Reset Season", tint = AdminDesign.Error) }
            IconButton(onClick = { /* Export Logic */ }) { Icon(Icons.Default.FileUpload, contentDescription = "Export", tint = AdminDesign.Secondary) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Scope Tabs
            ScrollableTabRow(
                selectedTabIndex = scopes.indexOf(selectedScope),
                containerColor = Color.Transparent,
                contentColor = AdminDesign.Primary,
                edgePadding = AdminDesign.SpacingMedium,
                divider = {}
            ) {
                scopes.forEach { scope ->
                    Tab(
                        selected = selectedScope == scope,
                        onClick = { selectedScope = scope },
                        text = { Text(scope, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Region Selector
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = AdminDesign.SpacingMedium, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                regions.forEach { region ->
                    FilterChip(
                        selected = selectedRegion == region,
                        onClick = { selectedRegion = region },
                        label = { Text(region, fontSize = 10.sp) },
                        modifier = Modifier.padding(end = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isSyncing && users.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                        items(12) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                    }
                } else if (sortedUsers.isEmpty()) {
                    AdminEmptyState("No Rankings", "No data matches your current filters.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
                    ) {
                        itemsIndexed(sortedUsers) { index, user ->
                            LeaderboardAdminItem(index + 1, user, selectedScope)
                        }
                    }
                }
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("RESET SEASON DATA?", fontWeight = FontWeight.Black) },
                text = { Text("This will archive current rankings and reset all seasonal ELO points to baseline (1200). This action is irreversible.") },
                confirmButton = {
                    Button(onClick = { showResetDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Error)) {
                        Text("CONFIRM RESET")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("CANCEL") }
                }
            )
        }
    }
}

@Composable
fun LeaderboardAdminItem(rank: Int, user: SupabaseUser, scope: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AdminDesign.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = when(rank) {
                    1 -> AdminDesign.Secondary
                    2 -> AdminDesign.Primary.copy(alpha = 0.6f)
                    3 -> AdminDesign.Primary.copy(alpha = 0.4f)
                    else -> AdminDesign.Background
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$rank", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Black, 
                        color = if (rank <= 3) Color.White else AdminDesign.OnSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = AdminDesign.OnSurface)
                Text(
                    text = user.email.take(24) + "...", 
                    fontSize = 10.sp, 
                    color = AdminDesign.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (scope == "Weekly") "${user.wins} WINS" else "${user.rating} ELO", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 14.sp, 
                    color = AdminDesign.Primary
                )
                if (user.isBanned) {
                    StatusBadge("TERMINATED")
                }
            }
        }
    }
}
