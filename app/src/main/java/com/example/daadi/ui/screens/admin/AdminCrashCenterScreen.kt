package com.example.daadi.ui.screens.admin



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
fun AdminCrashCenterScreen(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val crashLogs by adminViewModel.analyticsRepository.crashLogs.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()

    AdminFoundationScaffold("Crash Terminal", supabaseManager, onBack) { padding ->
        if (isSyncing && crashLogs.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(6) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (crashLogs.isEmpty()) {
            AdminEmptyState(
                title = "System Stable", 
                description = "Zero active crash reports detected in the current production cycle."
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                        MetricMiniCard("TOTAL INCIDENTS", crashLogs.size.toString(), AdminDesign.Error, Modifier.weight(1f))
                        MetricMiniCard("RESOLVED NODES", crashLogs.count { it.status == "resolved" }.toString(), AdminDesign.Success, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                }

                item {
                    Text("ACTIVE EXCEPTION LOGS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }

                items(crashLogs) { log ->
                    CrashLogCard(log)
                }
            }
        }
    }
}

@Composable
fun CrashLogCard(log: com.example.daadi.data.supabase.SupabaseCrashLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (log.status == "open") AdminDesign.Error.copy(alpha = 0.1f) else AdminDesign.Success.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (log.status == "open") Icons.Default.BugReport else Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = if (log.status == "open") AdminDesign.Error else AdminDesign.Success,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(log.exception, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = AdminDesign.OnSurface)
                    Text(
                        text = "NODE: ${log.deviceModel} | OS: ${log.osVersion} | BUILD: ${log.appVersion}", 
                        fontSize = 10.sp, 
                        color = AdminDesign.OnSurfaceVariant,
                        fontWeight = FontWeight.Black
                    )
                }
                StatusBadge(log.status)
            }
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AdminDesign.Background,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = log.stacktrace.take(250) + "...", 
                    fontSize = 9.sp, 
                    color = AdminDesign.Error, 
                    modifier = Modifier.padding(AdminDesign.SpacingSmall),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { /* View Full Trace */ }) {
                    Text("FULL TRACE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (log.status != "resolved") {
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Button(
                        onClick = { /* Mark Resolved */ },
                        shape = AdminDesign.ButtonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Success)
                    ) {
                        Text("MARK RESOLVED", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
