package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.animation.*
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseUser
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminUserManagementScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val users by supabaseManager.users.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()
    var selectedUser by remember { mutableStateOf<SupabaseUser?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    BoxWithConstraints {
        val isWide = maxWidth >= 900.dp
        
        if (isWide) {
            AdminWideUserManagement(
                users = users,
                isSyncing = isSyncing,
                selectedUser = selectedUser,
                onUserSelect = { selectedUser = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                supabaseManager = supabaseManager,
                onBack = onBack
            )
        } else {
            if (selectedUser == null) {
                AdminUserListScreen(
                    users = users,
                    isSyncing = isSyncing,
                    onUserClick = { selectedUser = it },
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    supabaseManager = supabaseManager,
                    onBack = onBack
                )
            } else {
                AdminUserDetailsScreen(
                    user = selectedUser!!,
                    supabaseManager = supabaseManager,
                    onBack = { selectedUser = null }
                )
            }
        }
    }
}

@Composable
fun AdminWideUserManagement(
    users: List<SupabaseUser>,
    isSyncing: Boolean,
    selectedUser: SupabaseUser?,
    onUserSelect: (SupabaseUser) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    AdminFoundationScaffold(
        title = "User Management",
        supabaseManager = supabaseManager,
        onBack = onBack,
        showSearch = true,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Left Panel: List
            Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                UserListContent(
                    users = users,
                    isSyncing = isSyncing,
                    searchQuery = searchQuery,
                    onUserClick = onUserSelect,
                    selectedUserId = selectedUser?.id
                )
            }
            VerticalDivider(color = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)
            
            // Right Panel: Details
            Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                if (selectedUser != null) {
                    UserDetailsContent(user = selectedUser, supabaseManager = supabaseManager)
                } else {
                    AdminEmptyState(
                        title = "No User Selected",
                        description = "Select a player from the directory to view detailed profile and moderation tools.",
                        icon = { Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(64.dp), tint = AdminDesign.OnSurfaceVariant) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminUserListScreen(
    users: List<SupabaseUser>,
    isSyncing: Boolean,
    onUserClick: (SupabaseUser) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    AdminFoundationScaffold(
        title = "User Directory",
        supabaseManager = supabaseManager,
        onBack = onBack,
        showSearch = true,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchChange
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            UserListContent(
                users = users,
                isSyncing = isSyncing,
                searchQuery = searchQuery,
                onUserClick = onUserClick
            )
        }
    }
}

@Composable
fun UserListContent(
    users: List<SupabaseUser>,
    isSyncing: Boolean,
    searchQuery: String,
    onUserClick: (SupabaseUser) -> Unit,
    selectedUserId: String? = null
) {
    var isBulkMode by remember { mutableStateOf(false) }
    var bulkSelectedIds by remember { mutableStateOf(setOf<String>()) }
    
    val filteredUsers = remember(users, searchQuery) {
        users.filter { 
            it.username.contains(searchQuery, true) || 
            it.email.contains(searchQuery, true) ||
            it.id.contains(searchQuery, true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (filteredUsers.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AdminDesign.SpacingMedium, vertical = AdminDesign.SpacingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBulkMode) {
                    Text("${bulkSelectedIds.size} Selected", fontWeight = FontWeight.Bold, color = AdminDesign.Primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { bulkSelectedIds = filteredUsers.map { it.id }.toSet() }) { Text("Select All") }
                        Button(
                            onClick = { /* Handle Bulk Ban/Action */ isBulkMode = false; bulkSelectedIds = emptySet() },
                            shape = AdminDesign.ButtonShape
                        ) { Text("Apply Action") }
                    }
                } else {
                    Text("${filteredUsers.size} Users", fontWeight = FontWeight.Bold, color = AdminDesign.OnSurfaceVariant)
                    TextButton(onClick = { isBulkMode = true }) { Text("Bulk Edit") }
                }
            }
        }

        if (isSyncing && users.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(10) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (filteredUsers.isEmpty()) {
            AdminEmptyState(title = "No Players Found", description = "Try a different search term or check filters.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                items(filteredUsers) { user ->
                    val isChecked = bulkSelectedIds.contains(user.id)
                    UserListItem(
                        user = user, 
                        onClick = { 
                            if (isBulkMode) {
                                bulkSelectedIds = if (isChecked) bulkSelectedIds - user.id else bulkSelectedIds + user.id
                            } else {
                                onUserClick(user)
                            }
                        },
                        isSelected = if (isBulkMode) isChecked else user.id == selectedUserId,
                        showCheckbox = isBulkMode,
                        isChecked = isChecked
                    )
                }
            }
        }
    }
}

@Composable
fun UserListItem(user: SupabaseUser, onClick: () -> Unit, isSelected: Boolean = false, showCheckbox: Boolean = false, isChecked: Boolean = false) {
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
        Row(
            modifier = Modifier.padding(AdminDesign.SpacingMedium).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCheckbox) {
                Checkbox(checked = isChecked, onCheckedChange = null)
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
            }
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = AdminDesign.Primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.username.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        color = AdminDesign.Primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AdminDesign.OnSurface)
                Text(user.email, fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                user.roles.take(1).forEach { role ->
                    Badge(
                        containerColor = when(role) {
                            "admin" -> AdminDesign.Secondary
                            "moderator" -> AdminDesign.Primary
                            else -> AdminDesign.OnSurfaceVariant
                        }
                    ) {
                        Text(role.uppercase(), fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                if (user.isBanned) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(containerColor = AdminDesign.Error) { Text("BANNED", fontSize = 8.sp, color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun AdminUserDetailsScreen(
    user: SupabaseUser,
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    AdminFoundationScaffold(
        title = "Player Profile",
        supabaseManager = supabaseManager,
        onBack = onBack
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            UserDetailsContent(user = user, supabaseManager = supabaseManager)
        }
    }
}

@Composable
fun UserDetailsContent(user: SupabaseUser, supabaseManager: SupabaseManager) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Moderation", "Activity", "Economics")

    Column(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = AdminDesign.Primary.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user.username.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column {
                Text(user.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface)
                Text(user.id, style = MaterialTheme.typography.labelSmall, color = AdminDesign.OnSurfaceVariant)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = AdminDesign.OnSurfaceVariant)
            }
        }
        
        Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
        
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
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                AnimatedContent(targetState = selectedTab, label = "user_details_tabs") { index ->
                    Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)) {
                        when(index) {
                            0 -> OverviewTab(user)
                            1 -> ModerationTab(user, supabaseManager)
                            2 -> ActivityTab(user, supabaseManager)
                            3 -> EconomicsTab(user, supabaseManager)
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
                Button(
                    onClick = { supabaseManager.deleteUser(user.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Error.copy(alpha = 0.1f), contentColor = AdminDesign.Error),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.ButtonShape
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Text("PERMANENTLY PURGE USER DATA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OverviewTab(user: SupabaseUser) {
    Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
            QuickStatCard("Games", user.totalGames.toString(), Modifier.weight(1f))
            QuickStatCard("Wins", user.wins.toString(), Modifier.weight(1f))
            QuickStatCard("Rating", user.rating.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
            QuickStatCard("Coins", user.coins.toString(), Modifier.weight(1f))
            QuickStatCard("XP", user.xp.toString(), Modifier.weight(1f))
            QuickStatCard("Reports", user.reportsCount.toString(), Modifier.weight(1f), isAlert = user.reportsCount > 0)
        }
        
        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
            Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                Text("ACCOUNT METADATA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                MetadataRow("Registered", user.createdAt)
                MetadataRow("Last Login", user.lastLogin ?: "N/A")
                MetadataRow("Country", user.country ?: "Unknown")
                MetadataRow("Device ID", user.deviceId ?: "N/A")
                MetadataRow("App Version", user.appVersion ?: "Unknown")
            }
        }
    }
}

@Composable
fun ModerationTab(user: SupabaseUser, supabaseManager: SupabaseManager) {
    Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
        ActionTile(
            title = if (user.isBanned) "Unban Account" else "Ban Account",
            subtitle = if (user.isBanned) "Restore full access for this player" else "Prevents all game access and authentication",
            icon = Icons.Default.Block,
            color = if (user.isBanned) AdminDesign.Secondary else AdminDesign.Error,
            onClick = { supabaseManager.toggleUserBan(user.id) }
        )
        ActionTile(
            title = if (user.shadowBanned) "Remove Shadow Ban" else "Apply Shadow Ban",
            subtitle = "Player can still play but only with other toxic users",
            icon = Icons.Default.VisibilityOff,
            color = Color.Gray,
            onClick = { supabaseManager.setShadowBan(user.id, !user.shadowBanned) }
        )
        ActionTile(
            title = if (user.isVerified) "Remove Verification" else "Verify Identity",
            subtitle = "Grants the blue verification badge in profiles",
            icon = Icons.Default.Verified,
            color = AdminDesign.Primary,
            onClick = { supabaseManager.updateUserVerification(user.id, !user.isVerified) }
        )
        ActionTile(
            title = "Invalidate Sessions",
            subtitle = "Force logout from all devices immediately",
            icon = Icons.Default.Logout,
            color = AdminDesign.Warning,
            onClick = { supabaseManager.forceLogout(user.id) }
        )
        
        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
        Text("ADMINISTRATIVE NOTES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
        var notes by remember { mutableStateOf(user.internalNotes ?: "") }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text("Add confidential staff notes here...") },
            shape = AdminDesign.InputShape
        )
        Button(
            onClick = { supabaseManager.updateInternalNotes(user.id, notes) },
            modifier = Modifier.align(Alignment.End).padding(top = AdminDesign.SpacingSmall),
            shape = AdminDesign.ButtonShape,
            colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary)
        ) {
            Text("SAVE NOTES")
        }
    }
}

@Composable
fun ActivityTab(user: SupabaseUser, supabaseManager: SupabaseManager) {
    val loginHistory by supabaseManager.userLoginHistory.collectAsStateWithLifecycle()
    LaunchedEffect(user.id) { supabaseManager.fetchLoginHistory(user.id) }

    if (loginHistory.isEmpty()) {
        AdminEmptyState(title = "No Recent Activity", description = "User hasn't logged in recently or history was purged.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
            loginHistory.forEach { log ->
                Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp), tint = AdminDesign.Primary)
                            Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                            Text("Session Started", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(log.createdAt.take(10), fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Location: ${log.location ?: "Unknown"}", fontSize = 12.sp)
                        Text("IP: ${log.ipAddress} | Device: ${log.deviceId?.take(8)}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun EconomicsTab(user: SupabaseUser, supabaseManager: SupabaseManager) {
    Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)) {
        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
            Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                Text("CURRENCY ADJUSTMENTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    Button(onClick = { supabaseManager.adjustUserEconomy(user.id, 500, 0) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("+500 C", fontSize = 10.sp) }
                    Button(onClick = { supabaseManager.adjustUserEconomy(user.id, 0, 1000) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("+1k XP", fontSize = 10.sp) }
                }
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    OutlinedButton(onClick = { supabaseManager.adjustUserEconomy(user.id, -500, 0) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("-500 C", fontSize = 10.sp) }
                    OutlinedButton(onClick = { supabaseManager.adjustUserEconomy(user.id, 0, -1000) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("-1k XP", fontSize = 10.sp) }
                }
            }
        }
        
        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
            Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                Text("RATING OVERRIDES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    Button(onClick = { supabaseManager.adjustUserStats(user.id, 1, 0, 50) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("+1 Win (+50 E)", fontSize = 10.sp) }
                    Button(onClick = { supabaseManager.adjustUserStats(user.id, 0, 1, -50) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("+1 Loss (-50 E)", fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AdminDesign.OnSurface)
    }
}

@Composable
fun ActionTile(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
            }
        }
    }
}

