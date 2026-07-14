package com.example.daadi.ui.screens.admin



import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onNavigateToUsers: () -> Unit,
    onNavigateToSafety: () -> Unit,
    onNavigateToMatches: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSystem: () -> Unit,
    onNavigateToBulletins: () -> Unit,
    onNavigateToAntiCheat: () -> Unit,
    onNavigateToTournaments: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToBIAnalytics: () -> Unit,
    onNavigateToBIRevenue: () -> Unit,
    onNavigateToBINotifications: () -> Unit,
    onNavigateToAuditTrail: () -> Unit,
    onNavigateToPermissionMatrix: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onNavigateToEconomy: () -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToRewards: () -> Unit,
    onNavigateToLiveOps: () -> Unit,
    onNavigateToSeasonPass: () -> Unit,
    onNavigateToCMS: () -> Unit,
    onNavigateToCrashes: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToFraud: () -> Unit,
    onNavigateToAIAssistant: () -> Unit,
    onNavigateToMonitoring: () -> Unit,
    onNavigateToExports: () -> Unit,
    onNavigateToApprovals: () -> Unit,
    onNavigateToScheduler: () -> Unit,
    onNavigateToRollbacks: () -> Unit,
    onNavigateToAIEngine: () -> Unit,
    onNavigateToLeaderboards: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onBack: () -> Unit
) {
    val users by adminViewModel.userRepository.users.collectAsStateWithLifecycle()
    val matches by adminViewModel.remoteGameRepository.matches.collectAsStateWithLifecycle()
    val settings by adminViewModel.remoteConfigRepository.systemSettings.collectAsStateWithLifecycle()
    val currentUser by adminViewModel.authRepository.currentUser.collectAsStateWithLifecycle()
    val biMetrics by adminViewModel.analyticsRepository.biMetrics.collectAsStateWithLifecycle()
    
    val menuItems = remember(currentUser) {
        listOfNotNull(
            if (adminViewModel.authRepository.userHasPermission("view_users")) AdminMenuItem("User Directory", "Players & roles", Icons.Default.People, onNavigateToUsers, AdminDesign.Primary) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_admins")) AdminMenuItem("Admin Matrix", "RBAC & permissions", Icons.Default.Security, onNavigateToPermissionMatrix, Color(0xFFC2185B)) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_admins")) AdminMenuItem("Approvals", "Workflows & requests", Icons.Default.FactCheck, onNavigateToApprovals, Color(0xFF00695C)) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_config")) AdminMenuItem("Task Scheduler", "Cron & automation", Icons.Default.Schedule, onNavigateToScheduler, Color(0xFFE65100)) else null,
            if (adminViewModel.authRepository.userHasPermission("view_analytics")) AdminMenuItem("Data Exports", "CSV & Archives", Icons.Default.FileDownload, onNavigateToExports, Color(0xFF455A64)) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_config")) AdminMenuItem("Config Rollbacks", "Version history", Icons.Default.History, onNavigateToRollbacks, Color(0xFF5D4037)) else null,
            if (adminViewModel.authRepository.userHasPermission("moderate_users")) AdminMenuItem("Safety & Trust", "Reports & bans", Icons.Default.GppGood, onNavigateToSafety, AdminDesign.Error) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_matches")) AdminMenuItem("Match Control", "Live lobbies", Icons.Default.PlayArrow, onNavigateToMatches, AdminDesign.Secondary) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_config")) AdminMenuItem("Remote Config", "Engine variables", Icons.Default.Settings, onNavigateToConfig, Color(0xFF5C2D0A)) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_config")) AdminMenuItem("Economy Center", "Currency & XP", Icons.Default.AccountBalanceWallet, { onNavigateToEconomy() }, AdminDesign.Secondary) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_config")) AdminMenuItem("Store Desk", "IAP & Bundles", Icons.Default.Storefront, { onNavigateToStore() }, AdminDesign.Primary) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_config")) AdminMenuItem("Reward Editor", "Daily & Spin", Icons.Default.Redeem, { onNavigateToRewards() }, Color(0xFFC62828)) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_matches")) AdminMenuItem("LiveOps", "Events & Promos", Icons.Default.Celebration, { onNavigateToLiveOps() }, Color(0xFF4A148C)) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_config")) AdminMenuItem("Season Pass", "Battle Pass tiers", Icons.Default.ConfirmationNumber, { onNavigateToSeasonPass() }, Color(0xFFE65100)) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_announcements")) AdminMenuItem("CMS Console", "Patch notes & FAQ", Icons.Default.Article, { onNavigateToCMS() }, Color(0xFF455A64)) else null,
            if (adminViewModel.authRepository.userHasPermission("view_analytics")) AdminMenuItem("BI Analytics", "Enterprise BI", Icons.Default.Analytics, onNavigateToBIAnalytics, AdminDesign.Primary) else null,
            if (adminViewModel.authRepository.userHasPermission("view_analytics")) AdminMenuItem("Revenue Desk", "Monetization", Icons.Default.Payments, onNavigateToBIRevenue, AdminDesign.Secondary) else null,
            if (adminViewModel.authRepository.userHasPermission("view_logs")) AdminMenuItem("AI Assistant", "Predictive insights", Icons.Default.AutoAwesome, onNavigateToAIAssistant, Color(0xFF7B1FA2)) else null,
            if (adminViewModel.authRepository.userHasPermission("view_system_health")) AdminMenuItem("System Health", "Monitoring center", Icons.Default.HealthAndSafety, onNavigateToMonitoring, AdminDesign.Secondary) else null,
            if (adminViewModel.authRepository.userHasPermission("view_logs")) AdminMenuItem("Crash Center", "Exception logs", Icons.Default.BugReport, onNavigateToCrashes, AdminDesign.Error) else null,
            if (adminViewModel.authRepository.userHasPermission("moderate_users")) AdminMenuItem("Device Center", "Registered hardware", Icons.Default.Smartphone, onNavigateToDevices, AdminDesign.Primary) else null,
            if (adminViewModel.authRepository.userHasPermission("moderate_users")) AdminMenuItem("Fraud Radar", "Bot detection", Icons.Default.Security, onNavigateToFraud, AdminDesign.Warning) else null,
            if (adminViewModel.authRepository.userHasPermission("view_audit_logs")) AdminMenuItem("Audit Trail", "Security logs", Icons.Default.ListAlt, onNavigateToAuditTrail, Color(0xFF455A64)) else null,
            if (adminViewModel.authRepository.userHasPermission("view_logs")) AdminMenuItem("Admin Sessions", "Active logins", Icons.Default.DeviceUnknown, onNavigateToSessions, Color(0xFF7B1FA2)) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_matches")) AdminMenuItem("Tournaments", "Schedule & brackets", Icons.Default.EmojiEvents, onNavigateToTournaments, Color(0xFFFBC02D)) else null,
            if (adminViewModel.authRepository.userHasPermission("view_analytics")) AdminMenuItem("Elo Rankings", "Leaderboard snapshots", Icons.Default.TrendingUp, onNavigateToLeaderboards, Color(0xFFFBC02D)) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_matches")) AdminMenuItem("Global Events", "Seasonal events", Icons.Default.Celebration, onNavigateToEvents, Color(0xFF8E24AA)) else null,
            if (adminViewModel.authRepository.userHasPermission("manage_config")) AdminMenuItem("AI Strategy", "Behavior & Logic", Icons.Default.PrecisionManufacturing, onNavigateToAIEngine, Color(0xFF673AB7)) else null
        )
    }

    AdminTheme {
        BoxWithConstraints {
            val isWide = maxWidth >= 1100.dp
            
            Row(modifier = Modifier.fillMaxSize().background(AdminDesign.Background)) {
                if (isWide) {
                    AdminSidebar(
                        currentUser = currentUser,
                        menuItems = menuItems,
                        onBack = onBack
                    )
                }
                
                Scaffold(
                    modifier = Modifier.weight(1f),
                    topBar = {
                        AdminTopBar(
                            title = "Admin Control Center",
                            currentUser = currentUser,
                            onBack = onBack,
                            onHelpClick = onNavigateToHelp,
                            showBackButton = !isWide
                        )
                    },
                    containerColor = Color.Transparent
                ) { padding ->
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        contentPadding = PaddingValues(
                            start = AdminDesign.SpacingMedium,
                            top = padding.calculateTopPadding() + AdminDesign.SpacingMedium,
                            end = AdminDesign.SpacingMedium,
                            bottom = AdminDesign.SpacingLarge
                        ),
                        horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium),
                        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            QuickStatsRow(users, matches, biMetrics)
                        }
                        
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SystemOverridesCard(settings, adminViewModel)
                        }

                        items(menuItems) { item ->
                            AdminMenuCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemToggleRow(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

data class AdminMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val accentColor: Color
)

@Composable
fun AdminMenuCard(item: AdminMenuItem) {
    Card(
        onClick = item.onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = AdminDesign.CardShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.height(130.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = item.accentColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(item.icon, contentDescription = null, tint = item.accentColor, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun QuickStatCard(label: String, value: String, modifier: Modifier = Modifier, isAlert: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert && value != "0") AdminDesign.Error.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        shape = AdminDesign.CardShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAlert && value != "0") AdminDesign.Error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium), horizontalAlignment = Alignment.Start) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isAlert && value != "0") AdminDesign.Error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAlert && value != "0") AdminDesign.Error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QuickStatsRow(users: List<com.example.daadi.data.supabase.SupabaseUser>, matches: List<com.example.daadi.data.supabase.SupabaseMatch>, biMetrics: List<com.example.daadi.data.supabase.SupabaseBIMetrics>) {
    val humanCount = users.count { !it.email.endsWith("@daadi.fake") }
    val botCount = users.size - humanCount
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            QuickStatCard("Human Users", "$humanCount", Modifier.fillMaxWidth())
            Text("Total Profiles: ${users.size}", style = MaterialTheme.typography.labelSmall, color = AdminDesign.OnSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
        QuickStatCard("Active Matches", "${matches.count { it.status == "playing" }}", Modifier.weight(1f))
        QuickStatCard("Total Revenue", "$${biMetrics.sumOf { it.totalRevenue }.toInt()}", Modifier.weight(1f))
    }
}

@Composable
fun SystemOverridesCard(settings: List<com.example.daadi.data.supabase.SupabaseSystemSetting>, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = AdminDesign.CardShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Text("SYSTEM OVERRIDES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Error)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            val mMode = settings.find { it.key == "maintenance_mode" }?.value == "on"
            SystemToggleRow("Maintenance Mode", Icons.Default.Build, mMode) {
                adminViewModel.remoteConfigRepository.updateSystemSetting("maintenance_mode", if (it) "on" else "off")
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            val rMode = settings.find { it.key == "registration_enabled" }?.value == "on"
            SystemToggleRow("Registration", Icons.Default.AppRegistration, rMode) {
                adminViewModel.remoteConfigRepository.updateSystemSetting("registration_enabled", if (it) "on" else "off")
            }
        }
    }
}

@Composable
fun AdminSidebar(
    currentUser: com.example.daadi.data.supabase.SupabaseUser?,
    menuItems: List<AdminMenuItem>,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.width(260.dp).fillMaxHeight(),
        color = AdminDesign.Surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, AdminDesign.OnSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Text(
                "DAADI ENTERPRISE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = AdminDesign.Primary,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
            
            // User Profile Mini
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(40.dp), color = AdminDesign.Primary.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(currentUser?.username?.take(1)?.uppercase() ?: "A", fontWeight = FontWeight.Bold, color = AdminDesign.Primary)
                    }
                }
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                Column {
                    Text(currentUser?.username ?: "Administrator", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(currentUser?.role?.uppercase() ?: "SUPER_ADMIN", style = MaterialTheme.typography.labelSmall, color = AdminDesign.OnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(AdminDesign.SpacingExtraLarge))
            
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(menuItems.take(10)) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { item.onClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(item.icon, contentDescription = null, tint = item.accentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.OnSurface.copy(alpha = 0.05f), contentColor = AdminDesign.OnSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exit Console")
            }
        }
    }
}
