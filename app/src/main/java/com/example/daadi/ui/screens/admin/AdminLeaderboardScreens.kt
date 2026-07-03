package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


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
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val users by supabaseManager.users.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()
    var selectedScope by remember { mutableStateOf("Global") }
    val scopes = listOf("Global", "Weekly", "Monthly", "Season")
    
    val sortedUsers = remember(users, selectedScope) {
        users.sortedByDescending { 
            when (selectedScope) {
                "Global" -> it.rating
                "Weekly" -> it.wins // Simulated weekly data
                else -> it.rating
            }
        }.take(50)
    }

    AdminFoundationScaffold(
        title = "Elo Rankings",
        supabaseManager = supabaseManager,
        onBack = onBack,
        actions = {
            IconButton(onClick = { /* Recalculate Logic */ }) {
                Icon(Icons.Default.Autorenew, contentDescription = "Recalculate", tint = AdminDesign.Primary)
            }
            IconButton(onClick = { /* Freeze Logic */ }) {
                Icon(Icons.Default.AcUnit, contentDescription = "Freeze", tint = AdminDesign.Secondary)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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

            Box(modifier = Modifier.weight(1f)) {
                if (isSyncing && users.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                        items(12) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                    }
                } else if (users.isEmpty()) {
                    AdminEmptyState(
                        title = "No Data Captured", 
                        description = "Elo distribution is null. No user performance records found in this cluster."
                    )
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
