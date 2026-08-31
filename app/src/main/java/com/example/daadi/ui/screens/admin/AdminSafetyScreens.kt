package com.example.daadi.ui.screens.admin



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

import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminSafetyHubScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onUserClick: (SupabaseUser) -> Unit,
    onBack: () -> Unit
) {
    val reports by supabaseManager.reports.collectAsStateWithLifecycle()
    val bans by supabaseManager.bans.collectAsStateWithLifecycle()
    val users by adminViewModel.userRepository.users.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedReport by remember { mutableStateOf<com.example.daadi.data.supabase.SupabaseReport?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Incident Reports", "Exclusion List", "Action Logs")

    LaunchedEffect(Unit) {
        supabaseManager.fetchReports()
        supabaseManager.fetchBans()
    }

    AdminFoundationScaffold(
        title = "Safety & Compliance",
        adminViewModel = adminViewModel,
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
                        0 -> ReportsQueue(reports, users, isSyncing) { report ->
                            selectedReport = report
                            showActionDialog = true
                        }
                        1 -> ExclusionList(bans, users, isSyncing, onUserClick)
                        2 -> ActionLogsView(isSyncing, users)
                    }
                }
            }
        }
    }

    if (showActionDialog && selectedReport != null) {
        val targetUser = users.find { it.id == selectedReport!!.reportedId }
        SafetyActionDialog(
            report = selectedReport!!,
            user = targetUser,
            onDismiss = { showActionDialog = false },
            onAction = { type, reason, duration ->
                val action = com.example.daadi.data.supabase.SupabaseModeratorAction(
                    id = UUID.randomUUID().toString(),
                    moderatorId = "admin_alok", // Placeholder for current admin
                    targetUserId = selectedReport!!.reportedId,
                    actionType = type,
                    reason = reason,
                    duration = duration?.toString(),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                )
                supabaseManager.submitModeratorAction(action) { success ->
                    if (success) {
                        showActionDialog = false
                        supabaseManager.fetchReports()
                        supabaseManager.fetchBans()
                        supabaseManager.fetchModeratorActions()
                    }
                }
            }
        )
    }
}

@Composable
fun ReportsQueue(
    reports: List<com.example.daadi.data.supabase.SupabaseReport>,
    users: List<SupabaseUser>,
    isSyncing: Boolean,
    onReportClick: (com.example.daadi.data.supabase.SupabaseReport) -> Unit
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
            items(reports, key = { it.id }) { report ->
                ReportItem(
                    report = report, 
                    reportedUser = users.find { it.id == report.reportedId }, 
                    onClick = { onReportClick(report) }
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
            items(bans, key = { it.id }) { ban ->
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
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        shape = AdminDesign.CardShape,
        modifier = Modifier.fillMaxWidth().border(1.dp, AdminDesign.OnSurface.copy(alpha = 0.05f), AdminDesign.CardShape)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface)
                Text(user.email, fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
            }
            if (showReports) {
                Text(
                    "${user.reportsCount} FLAGS", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White,
                    modifier = Modifier.background(AdminDesign.Error, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                Icon(Icons.Default.Block, contentDescription = null, tint = AdminDesign.Error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ActionLogsView(isSyncing: Boolean, users: List<SupabaseUser>) {
    val actions by supabaseManager.moderatorActions.collectAsStateWithLifecycle()
    
    if (isSyncing && actions.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            items(8) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
        }
    } else if (actions.isEmpty()) {
        AdminEmptyState("No Action History", "No moderation actions have been recorded yet.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            items(actions, key = { it.id }) { action ->
                ModeratorActionItem(action, users.find { it.id == action.targetUserId })
            }
        }
    }
}

@Composable
fun ModeratorActionItem(action: com.example.daadi.data.supabase.SupabaseModeratorAction, targetUser: SupabaseUser?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when(action.actionType) {
                    "ban" -> Icons.Default.Block
                    "warn" -> Icons.Default.Warning
                    "mute" -> Icons.Default.VolumeOff
                    else -> Icons.Default.Gavel
                },
                contentDescription = null,
                tint = AdminDesign.Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${action.actionType.uppercase()}: ${targetUser?.username ?: "User"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(action.reason, fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
            }
            Text(action.createdAt.take(10), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SafetyActionDialog(
    report: com.example.daadi.data.supabase.SupabaseReport,
    user: SupabaseUser?,
    onDismiss: () -> Unit,
    onAction: (String, String, Int?) -> Unit
) {
    var reason by remember { mutableStateOf(report.reason) }
    var selectedAction by remember { mutableStateOf("warn") }
    var duration by remember { mutableStateOf("3") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Moderation Action: ${user?.username ?: "User"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("INCIDENT: ${report.category.uppercase()}", fontWeight = FontWeight.Black, fontSize = 10.sp, color = AdminDesign.Error)
                
                Text("Select Enforcement:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("warn", "mute", "temp_ban", "perm_ban").forEach { action ->
                        FilterChip(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action },
                            label = { Text(action.replace("_", " ").uppercase(), fontSize = 9.sp) }
                        )
                    }
                }
                
                if (selectedAction == "temp_ban" || selectedAction == "mute") {
                    AdminTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = "Duration (Days)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                AdminTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Official Reason (Required)",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(selectedAction, reason, duration.toIntOrNull()) },
                colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Error),
                enabled = reason.isNotBlank()
            ) {
                Text("EXECUTE SANCTION")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
