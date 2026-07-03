package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminLiveOpsCenter(supabaseManager: SupabaseManager, onBack: () -> Unit) {
    val events by supabaseManager.liveOpsEvents.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()

    AdminFoundationScaffold(
        title = "LiveOps Command",
        supabaseManager = supabaseManager,
        onBack = onBack,
        actions = {
            IconButton(onClick = { /* Create Event */ }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Schedule Event", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        if (isSyncing && events.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (events.isEmpty()) {
            AdminEmptyState(
                title = "Ops Silence", 
                description = "No LiveOps events are currently scheduled or active. Game logic is operating on baseline parameters."
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                item {
                    LiveOpsStatusBanner(events)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                }
                item {
                    Text("ACTIVE OPERATIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
                items(events) { event ->
                    LiveOpsEventCard(event)
                }
            }
        }
    }
}

@Composable
fun LiveOpsStatusBanner(events: List<com.example.daadi.data.supabase.SupabaseLiveOpsEvent>) {
    val activeCount = events.count { it.isActive }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Primary),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingLarge), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column {
                Text("GLOBAL LIVE OPERATIONS", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("$activeCount ACTIVE CLUSTERS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LiveOpsEventCard(event: com.example.daadi.data.supabase.SupabaseLiveOpsEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f), color = AdminDesign.OnSurface)
                StatusBadge(if (event.isActive) "LIVE" else "PAST")
            }
            Text(
                text = event.description ?: "No operation description provided.", 
                fontSize = 12.sp, 
                color = AdminDesign.OnSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall), color = AdminDesign.OnSurface.copy(alpha = 0.05f))
            
            Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingLarge)) {
                MultiplerIndicator("XP GAIN", event.xpMultiplier, AdminDesign.Primary)
                MultiplerIndicator("COIN DROP", event.coinMultiplier, AdminDesign.Secondary)
            }
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AdminDesign.Background,
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(modifier = Modifier.padding(AdminDesign.SpacingSmall), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Text(
                        text = "WINDOW: ${event.startTime?.take(16)} » ${event.endTime?.take(16)}", 
                        fontSize = 9.sp, 
                        color = AdminDesign.OnSurfaceVariant,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiplerIndicator(label: String, multiplier: Double, color: Color) {
    Column {
        Text(label, fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Black)
        Text("x${multiplier}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
    }
}
