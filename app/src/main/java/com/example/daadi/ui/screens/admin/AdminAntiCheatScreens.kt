package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminAntiCheatDashboard(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val logs by supabaseManager.antiCheatLogs.collectAsStateWithLifecycle()
    val users by supabaseManager.users.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        supabaseManager.fetchAntiCheatLogs()
    }

    AdminFoundationScaffold(
        title = "Sentinel Shield",
        supabaseManager = supabaseManager,
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Stats Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(AdminDesign.SpacingMedium),
                horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                RiskCard("TOTAL FLAGS", "${logs.size}", AdminDesign.Primary, Modifier.weight(1f))
                RiskCard("CRITICAL", "${logs.count { it.severity == "critical" }}", AdminDesign.Error, Modifier.weight(1f))
                RiskCard("UNIQUE SUSPECTS", "${logs.distinctBy { it.userId }.size}", AdminDesign.Secondary, Modifier.weight(1f))
            }

            Text(
                text = "HEURISTIC VIOLATION FEED", 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Black, 
                color = AdminDesign.OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = AdminDesign.SpacingMedium)
            )
            
            if (isSyncing && logs.isEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                    items(10) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                }
            } else if (logs.isEmpty()) {
                AdminEmptyState(
                    title = "Perimeter Secure", 
                    description = "No heuristic violations detected by Sentinel in the current observation window."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
                ) {
                    items(logs) { log ->
                        AntiCheatLogItem(log, users.find { it.id == log.userId })
                    }
                }
            }
        }
    }
}

@Composable
fun RiskCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        modifier = modifier,
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurfaceVariant)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun AntiCheatLogItem(log: SupabaseAntiCheatLog, user: SupabaseUser?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            val severityColor = when(log.severity) {
                "critical" -> AdminDesign.Error
                "high" -> AdminDesign.Secondary
                else -> AdminDesign.OnSurfaceVariant
            }
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = severityColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (log.severity == "critical") Icons.Default.GppBad else Icons.Default.SecurityUpdateWarning,
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(user?.username ?: "Anonymous Node", fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface, fontSize = 15.sp)
                Text(log.violationType.uppercase(), color = severityColor, fontWeight = FontWeight.Black, fontSize = 10.sp)
                Text("MATCH_REF: ${log.matchId ?: "SYS_PROBE"}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
            }
            Text(log.createdAt.takeLast(8), fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Black)
        }
    }
}
