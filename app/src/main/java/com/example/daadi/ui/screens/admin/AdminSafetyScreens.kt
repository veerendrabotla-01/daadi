package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.AnimatedContent
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
import com.example.daadi.data.supabase.SupabaseUser
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminSafetyHubScreen(
    supabaseManager: SupabaseManager,
    onUserClick: (SupabaseUser) -> Unit,
    onBack: () -> Unit
) {
    val reports by supabaseManager.reports.collectAsStateWithLifecycle()
    val bans by supabaseManager.bans.collectAsStateWithLifecycle()
    val users by supabaseManager.users.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Incident Reports", "Exclusion List")

    LaunchedEffect(Unit) {
        supabaseManager.fetchReports()
        supabaseManager.fetchBans()
    }

    AdminFoundationScaffold(
        title = "Safety & Compliance",
        supabaseManager = supabaseManager,
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AdminDesign.Primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(targetState = selectedTab, label = "safety_tabs") { tabIndex ->
                    when (tabIndex) {
                        0 -> ReportsQueue(reports, users, isSyncing, onUserClick)
                        1 -> ExclusionList(bans, users, isSyncing, onUserClick)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsQueue(
    reports: List<com.example.daadi.data.supabase.SupabaseReport>,
    users: List<SupabaseUser>,
    isSyncing: Boolean,
    onUserClick: (SupabaseUser) -> Unit
) {
    if (isSyncing && reports.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
        }
    } else if (reports.isEmpty()) {
        AdminEmptyState(
            title = "Zero Incident Reports", 
            description = "The community is currently behaving well. No active reports require attention."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            items(reports) { report ->
                ReportItem(
                    report = report, 
                    reportedUser = users.find { it.id == report.reportedId }, 
                    onClick = { 
                        users.find { it.id == report.reportedId }?.let { onUserClick(it) }
                    }
                )
            }
        }
    }
}

@Composable
fun ExclusionList(
    bans: List<com.example.daadi.data.supabase.SupabaseBan>,
    users: List<SupabaseUser>,
    isSyncing: Boolean,
    onUserClick: (SupabaseUser) -> Unit
) {
    if (isSyncing && bans.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
        }
    } else if (bans.isEmpty()) {
        AdminEmptyState(
            title = "Empty Exclusion List", 
            description = "No players are currently restricted from accessing the platform."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            items(bans) { ban ->
                BanItem(
                    ban = ban, 
                    bannedUser = users.find { it.id == ban.userId }, 
                    onClick = {
                        users.find { it.id == ban.userId }?.let { onUserClick(it) }
                    }
                )
            }
        }
    }
}

@Composable
fun ReportItem(report: com.example.daadi.data.supabase.SupabaseReport, reportedUser: SupabaseUser?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = when(report.priority) {
                    "high", "critical" -> AdminDesign.Error.copy(alpha = 0.1f)
                    "medium" -> AdminDesign.Secondary.copy(alpha = 0.1f)
                    else -> AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = when(report.priority) {
                            "high", "critical" -> AdminDesign.Error
                            "medium" -> AdminDesign.Secondary
                            else -> AdminDesign.OnSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(reportedUser?.username ?: "Anonymous User", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AdminDesign.OnSurface)
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Badge(
                        containerColor = when(report.priority) {
                            "high", "critical" -> AdminDesign.Error
                            "medium" -> AdminDesign.Secondary
                            else -> AdminDesign.OnSurfaceVariant
                        }
                    ) {
                        Text(report.priority.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
                Text("Type: ${report.category}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                Text(report.reason, fontSize = 11.sp, color = AdminDesign.OnSurface, maxLines = 1)
            }
            Surface(
                color = if (report.status == "pending") AdminDesign.Error.copy(alpha = 0.1f) else AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = report.status.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (report.status == "pending") AdminDesign.Error else AdminDesign.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun BanItem(ban: com.example.daadi.data.supabase.SupabaseBan, bannedUser: SupabaseUser?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = AdminDesign.Error.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = AdminDesign.Error, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(bannedUser?.username ?: "User: ${ban.userId.take(8)}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AdminDesign.OnSurface)
                Text("REASON: ${ban.reason}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                val isPermanent = ban.expiresAt == null
                Text(
                    text = if (isPermanent) "LIFETIME RESTRICTION" else "EXPIRATION: ${ban.expiresAt}", 
                    fontSize = 11.sp, 
                    color = AdminDesign.Error, 
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdminDesign.OnSurfaceVariant)
        }
    }
}

@Composable
fun SafetyUserItem(user: SupabaseUser, showReports: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.Bold, color = Color(0xFF5C2D0A))
                Text(user.email, fontSize = 11.sp, color = Color.Gray)
            }
            if (showReports) {
                Text(
                    "${user.reportsCount} FLAGS", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White,
                    modifier = Modifier.background(Color(0xFFC62828), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32).copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = Color.Gray, fontSize = 14.sp)
        }
    }
}
