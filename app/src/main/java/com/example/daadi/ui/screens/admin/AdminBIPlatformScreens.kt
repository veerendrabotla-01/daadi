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
fun AdminBIPlatformSuite(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    type: String, // "analytics", "revenue", "notifications", "logs", "monitoring", "database"
    onBack: () -> Unit
) {
    val title = when(type) {
        "analytics" -> "Intelligence Hub"
        "revenue" -> "Revenue Console"
        "notifications" -> "Push Matrix"
        "logs" -> "Event Explorer"
        "monitoring" -> "Vitals Monitor"
        "database" -> "Data Orchestrator"
        else -> "Business Intelligence"
    }

    AdminFoundationScaffold(
        title = title,
        adminViewModel = adminViewModel,
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when(type) {
                "analytics" -> AdminAnalyticsDashboard(adminViewModel)
                "revenue" -> AdminRevenueDashboard(adminViewModel)
                "notifications" -> AdminNotificationCenter(adminViewModel)
                "logs" -> AdminLogsExplorer(adminViewModel)
                "monitoring" -> AdminRealtimeMonitoring(adminViewModel)
                "database" -> AdminDatabaseTools(adminViewModel)
            }
        }
    }
}

@Composable
fun AdminAnalyticsDashboard(adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    val metrics by adminViewModel.analyticsRepository.biDailyMetrics.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        adminViewModel.analyticsRepository.fetchBIDailyMetrics()
    }

    if (isSyncing && metrics.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            items(6) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(AdminDesign.SpacingMedium), 
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
        ) {
            item {
                Text("RETENTION & USAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    BIGraphCard("DAU", metrics.firstOrNull()?.dau?.toString() ?: "0", AdminDesign.Success, Modifier.weight(1f))
                    BIGraphCard("WAU", metrics.firstOrNull()?.wau?.toString() ?: "0", AdminDesign.Primary, Modifier.weight(1f))
                    BIGraphCard("MAU", metrics.firstOrNull()?.mau?.toString() ?: "0", AdminDesign.Secondary, Modifier.weight(1f))
                }
            }
            
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Text("USER RETENTION COHORTS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                        repeat(5) { i ->
                            val progress = 1f - (i * 0.15f)
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Day $i", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(AdminDesign.Background)) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(AdminDesign.Success))
                                }
                                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                                Text("${(progress * 100).toInt()}%", fontSize = 10.sp, color = AdminDesign.OnSurface, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BIGraphCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), 
        modifier = modifier,
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurfaceVariant)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun AdminRevenueDashboard(adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    val metrics by adminViewModel.analyticsRepository.biDailyMetrics.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    
    if (isSyncing && metrics.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            items(4) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(AdminDesign.SpacingMedium), 
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
        ) {
            item {
                RevenueHighlightCard("ESTIMATED GROSS REVENUE", "$${metrics.firstOrNull()?.revenueUsd ?: 0.00}", AdminDesign.Primary)
            }
            item {
                Text("AD PERFORMANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    BIGraphCard("IMPRESSIONS", metrics.firstOrNull()?.adImpressions?.toString() ?: "0", AdminDesign.Primary, Modifier.weight(1f))
                    BIGraphCard("CLICKS", metrics.firstOrNull()?.adClicks?.toString() ?: "0", AdminDesign.Error, Modifier.weight(1f))
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Text("ECPM CHANNEL ANALYSIS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                        ECPMRow("Rewarded Video", "$1.24", AdminDesign.Success)
                        ECPMRow("Interstitial", "$0.85", AdminDesign.Primary)
                        ECPMRow("Native Banner", "$0.12", AdminDesign.Secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ECPMRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
            Text(label, fontSize = 13.sp, color = AdminDesign.OnSurface, fontWeight = FontWeight.Bold)
        }
        Text(value, fontSize = 13.sp, color = AdminDesign.OnSurface, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun RevenueHighlightCard(label: String, value: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(AdminDesign.SpacingLarge), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
            Text(value, fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
fun AdminNotificationCenter(adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    val notifications by adminViewModel.analyticsRepository.biNotifications.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminViewModel.analyticsRepository.fetchBINotifications()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(AdminDesign.SpacingMedium),
            shape = AdminDesign.ButtonShape,
            color = AdminDesign.Primary,
            onClick = { showCreate = true }
        ) {
            Row(
                modifier = Modifier.padding(AdminDesign.SpacingMedium), 
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                Text("DISPATCH NEW PUSH CAMPAIGN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }

        if (isSyncing && notifications.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (notifications.isEmpty()) {
            AdminEmptyState(title = "No Campaigns", description = "Targeted push notifications will appear here once dispatched.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(AdminDesign.SpacingMedium), 
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                item {
                    Text("CAMPAIGN HISTORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
                items(notifications) { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                        shape = AdminDesign.CardShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                    ) {
                        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(note.title, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface, modifier = Modifier.weight(1f), fontSize = 15.sp)
                                StatusBadge(note.status)
                            }
                            Text(note.body, fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
                            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp), tint = AdminDesign.Success)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Opens: ${note.openCount}", fontSize = 11.sp, color = AdminDesign.Success, fontWeight = FontWeight.Bold)
                                }
                                Surface(color = AdminDesign.Background, shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = note.targetSegment.uppercase(), 
                                        fontSize = 10.sp, 
                                        color = AdminDesign.OnSurfaceVariant, 
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Compose Push Matrix", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Campaign Subject") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Broadcast Payload") }, minLines = 3, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
                }
            },
            confirmButton = {
                Button(
                    onClick = { adminViewModel.analyticsRepository.scheduleNotification(title, body, "all"); showCreate = false },
                    shape = AdminDesign.ButtonShape
                ) {
                    Text("DEPLOY TO ALL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("DISCARD") }
            }
        )
    }
}

@Composable
fun AdminLogsExplorer(adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    val logs by adminViewModel.analyticsRepository.biAppLogs.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = listOf("NETWORK", "ADS", "SECURITY", "CRASH", "UI")

    LaunchedEffect(selectedCategory) {
        adminViewModel.analyticsRepository.fetchBIAppLogs(selectedCategory)
    }

    Column {
        ScrollableTabRow(
            selectedTabIndex = if (selectedCategory == null) 0 else categories.indexOf(selectedCategory) + 1,
            containerColor = Color.Transparent,
            contentColor = AdminDesign.Primary,
            divider = {},
            edgePadding = AdminDesign.SpacingMedium
        ) {
            Tab(selected = selectedCategory == null, onClick = { selectedCategory = null }, text = { Text("ALL EVENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
            categories.forEach { cat ->
                Tab(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, text = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) })
            }
        }

        if (isSyncing && logs.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(10) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (logs.isEmpty()) {
            AdminEmptyState(title = "Logs Clear", description = "No system events matched the selected filter.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(AdminDesign.SpacingMedium), 
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                items(logs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                        shape = AdminDesign.CardShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                    ) {
                        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(
                                    when(log.level) {
                                        "ERROR", "CRITICAL" -> AdminDesign.Error
                                        "WARNING" -> AdminDesign.Secondary
                                        else -> AdminDesign.Success
                                    }
                                ))
                                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                                Text(log.category, fontWeight = FontWeight.Black, fontSize = 10.sp, color = AdminDesign.Primary)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(log.createdAt, fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.message, fontSize = 13.sp, color = AdminDesign.OnSurface, fontWeight = FontWeight.Medium)
                            if (log.stackTrace != null) {
                                Surface(
                                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                                    color = AdminDesign.Error.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = log.stackTrace.take(150) + "...", 
                                        fontSize = 9.sp, 
                                        color = AdminDesign.Error, 
                                        modifier = Modifier.padding(AdminDesign.SpacingSmall),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRealtimeMonitoring(adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    val metrics by adminViewModel.analyticsRepository.biHealthMetrics.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        adminViewModel.analyticsRepository.fetchBIHealthMetrics()
    }

    if (isSyncing && metrics.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            items(6) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(AdminDesign.SpacingMedium), 
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
        ) {
            item {
                Text("SERVICE HEALTH MATRIX", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            }
            items(metrics) { metric ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                ) {
                    Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = if (metric.status == "HEALTHY") AdminDesign.Success.copy(alpha = 0.1f) else AdminDesign.Error.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (metric.status == "HEALTHY") Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (metric.status == "HEALTHY") AdminDesign.Success else AdminDesign.Error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(metric.serviceName, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface, fontSize = 15.sp)
                            Text(metric.status, color = if (metric.status == "HEALTHY") AdminDesign.Success else AdminDesign.Error, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${metric.latencyMs ?: 0}ms", fontWeight = FontWeight.Black, color = AdminDesign.OnSurface, fontSize = 18.sp)
                            Text("LATENCY", fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Text("SERVER CLUSTER UTILIZATION", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                        MonitoringBar("CPU LOAD", 0.12f, Color.Cyan)
                        MonitoringBar("MEMORY HEAP", 0.45f, Color.Magenta)
                        MonitoringBar("NODE STORAGE", 0.78f, Color.Yellow)
                    }
                }
            }
        }
    }
}

@Composable
fun MonitoringBar(label: String, progress: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 10.sp, color = AdminDesign.OnSurface, fontWeight = FontWeight.Bold)
            Text("${(progress * 100).toInt()}%", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress, 
            color = color, 
            trackColor = AdminDesign.Background, 
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
fun AdminDatabaseTools(adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(AdminDesign.SpacingMedium), 
        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
    ) {
        item {
            Text("MAINTENANCE COMMANDS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
        }
        item {
            DBToolItem("Full Backup", "Trigger a manual snapshot of all tables", Icons.Default.Backup) { /* Logic */ }
        }
        item {
            DBToolItem("Vacuum Cleanup", "Optimize storage and reclaim dead space", Icons.Default.CleaningServices) { /* Logic */ }
        }
        item {
            DBToolItem("Index Rebuild", "Re-index all tables for query performance", Icons.Default.Bolt) { /* Logic */ }
        }
        item {
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Text("DATA EXPORT & PORTABILITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
        }
        item {
            DBToolItem("Export Raw CSV", "Dump players and matches to cold storage", Icons.Default.FileDownload) { /* Logic */ }
        }
    }
}

@Composable
fun DBToolItem(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick, 
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = AdminDesign.Primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface, fontSize = 15.sp)
                Text(desc, fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
            }
        }
    }
}
