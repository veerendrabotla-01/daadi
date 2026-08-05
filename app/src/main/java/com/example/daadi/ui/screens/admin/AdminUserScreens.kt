package com.example.daadi.ui.screens.admin



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
import androidx.compose.ui.draw.scale
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
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit,
    onHelpClick: (() -> Unit)? = null
) {
    val users by adminViewModel.userRepository.users.collectAsStateWithLifecycle()
    val isOnline by adminViewModel.authRepository.network.isOnline.collectAsStateWithLifecycle()
    val cachedUsers by adminViewModel.userRepository.cachedUsers.collectAsState(initial = emptyList())
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var selectedUser by remember { mutableStateOf<SupabaseUser?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showBots by remember { mutableStateOf(true) }
    
    // Filters
    var filterStatus by remember { mutableStateOf("All") } // All, Banned, Verified, Admin, Moderator, Guest
    var sortBy by remember { mutableStateOf("Newest") } // Newest, Rating, Wins, Username

    val displayUsers = (if (isOnline) users else cachedUsers.map { 
        SupabaseUser(
            id = it.id,
            username = it.username,
            email = it.email,
            role = it.role,
            createdAt = it.createdAt,
            totalGames = it.totalGames,
            wins = it.wins,
            losses = it.losses,
            coins = it.coins,
            xp = it.xp,
            rating = it.rating,
            isBanned = it.isBanned,
            isVerified = it.isVerified
        )
    }).filter { user ->
        val matchesSearch = user.username.contains(searchQuery, true) || 
                          user.email.contains(searchQuery, true) || 
                          user.id.contains(searchQuery, true) ||
                          (user.country?.contains(searchQuery, true) ?: false) ||
                          (user.deviceId?.contains(searchQuery, true) ?: false)
        
        val matchesBotFilter = if (showBots) true else !user.email.endsWith("@daadi.fake")
        
        val matchesStatus = when(filterStatus) {
            "Banned" -> user.isBanned
            "Verified" -> user.isVerified
            "Admin" -> user.role.lowercase() == "admin" || user.role.lowercase() == "superadmin"
            "Moderator" -> user.role.lowercase() == "moderator"
            "Shadow Banned" -> user.shadowBanned
            else -> true
        }

        matchesSearch && matchesBotFilter && matchesStatus
    }.let { list ->
        when(sortBy) {
            "Rating" -> list.sortedByDescending { it.rating }
            "Wins" -> list.sortedByDescending { it.wins }
            "Username" -> list.sortedBy { it.username.lowercase() }
            else -> list.sortedByDescending { it.createdAt }
        }
    }

    BoxWithConstraints {
        val isWide = maxWidth >= 900.dp
        
        if (isWide) {
            AdminWideUserManagement(
                users = displayUsers,
                isSyncing = isSyncing || !isOnline,
                selectedUser = selectedUser,
                onUserSelect = { selectedUser = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                filterStatus = filterStatus,
                onFilterStatusChange = { filterStatus = it },
                sortBy = sortBy,
                onSortByChange = { sortBy = it },
                adminViewModel = adminViewModel,
                onNavigate = onNavigate,
                onBack = onBack,
                onHelpClick = onHelpClick
            )
        } else {
            if (selectedUser == null) {
                AdminUserListScreen(
                    users = displayUsers,
                    isSyncing = isSyncing || !isOnline,
                    onUserClick = { selectedUser = it },
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    filterStatus = filterStatus,
                    onFilterStatusChange = { filterStatus = it },
                    sortBy = sortBy,
                    onSortByChange = { sortBy = it },
                    adminViewModel = adminViewModel,
                    onBack = onBack,
                    onHelpClick = onHelpClick,
                    showBots = showBots,
                    onShowBotsChange = { showBots = it }
                )
            } else {
                AdminUserDetailsScreen(
                    user = selectedUser!!,
                    adminViewModel = adminViewModel,
                    onNavigate = onNavigate,
                    onBack = { selectedUser = null },
                    onHelpClick = onHelpClick
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
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit,
    onHelpClick: (() -> Unit)? = null
) {
    var showBots by remember { mutableStateOf(true) }
    
    AdminFoundationScaffold(
        title = "User Management",
        adminViewModel = adminViewModel,
        onBack = onBack,
        onHelpClick = onHelpClick,
        showSearch = true,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                Text("Bots", style = MaterialTheme.typography.labelSmall)
                Switch(checked = showBots, onCheckedChange = { showBots = it }, modifier = Modifier.scale(0.6f))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            UserDirectoryStats(users)
            
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Left Panel: List
                Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                    UserListContent(
                        users = users,
                        isSyncing = isSyncing,
                        searchQuery = searchQuery,
                        filterStatus = filterStatus,
                        onFilterStatusChange = onFilterStatusChange,
                        sortBy = sortBy,
                        onSortByChange = onSortByChange,
                        onUserClick = onUserSelect,
                        selectedUserId = selectedUser?.id,
                        onBulkBan = { ids ->
                            ids.forEach { id -> adminViewModel.userRepository.toggleUserBan(id) }
                        }
                    )
                }
                VerticalDivider(color = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)
                
                // Right Panel: Details
                Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                    if (selectedUser != null) {
                        UserDetailsContent(user = selectedUser, adminViewModel = adminViewModel, onNavigate = onNavigate)
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
}

@Composable
fun AdminUserListScreen(
    users: List<SupabaseUser>,
    isSyncing: Boolean,
    onUserClick: (SupabaseUser) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit,
    onHelpClick: (() -> Unit)? = null,
    showBots: Boolean = true,
    onShowBotsChange: (Boolean) -> Unit = {}
) {
    AdminFoundationScaffold(
        title = "User Directory",
        adminViewModel = adminViewModel,
        onBack = onBack,
        onHelpClick = onHelpClick,
        showSearch = true,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchChange,
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                Text("Bots", style = MaterialTheme.typography.labelSmall)
                Switch(checked = showBots, onCheckedChange = onShowBotsChange, modifier = Modifier.scale(0.6f))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            UserDirectoryStats(users)
            UserListContent(
                users = users,
                isSyncing = isSyncing,
                searchQuery = searchQuery,
                filterStatus = filterStatus,
                onFilterStatusChange = onFilterStatusChange,
                sortBy = sortBy,
                onSortByChange = onSortByChange,
                onUserClick = onUserClick,
                onBulkBan = { ids ->
                    ids.forEach { id -> adminViewModel.userRepository.toggleUserBan(id) }
                }
            )
        }
    }
}

@Composable
fun UserListContent(
    users: List<SupabaseUser>,
    isSyncing: Boolean,
    searchQuery: String,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    onUserClick: (SupabaseUser) -> Unit,
    selectedUserId: String? = null,
    onBulkBan: (Set<String>) -> Unit
) {
    var isBulkMode by remember { mutableStateOf(false) }
    var bulkSelectedIds by remember { mutableStateOf(setOf<String>()) }
    
    val statuses = listOf("All", "Verified", "Banned", "Shadow Banned", "Admin", "Moderator")
    val sortOptions = listOf("Newest", "Rating", "Wins", "Username")

    Column(modifier = Modifier.fillMaxSize()) {
        // Filters Row
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AdminDesign.SpacingMedium, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(statuses) { status ->
                FilterChip(
                    selected = filterStatus == status,
                    onClick = { onFilterStatusChange(status) },
                    label = { Text(status, fontSize = 10.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AdminDesign.SpacingMedium, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${users.size} Result(s)", style = MaterialTheme.typography.labelSmall, color = AdminDesign.OnSurfaceVariant)
            
            var showSortMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sort: $sortBy", fontSize = 10.sp)
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    sortOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = { 
                                onSortByChange(opt)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        if (users.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AdminDesign.SpacingMedium, vertical = AdminDesign.SpacingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBulkMode) {
                    Text("${bulkSelectedIds.size} Selected", fontWeight = FontWeight.Bold, color = AdminDesign.Primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { bulkSelectedIds = users.map { it.id }.toSet() }) { Text("Select All") }
                        Button(
                            onClick = { 
                                onBulkBan(bulkSelectedIds)
                                isBulkMode = false
                                bulkSelectedIds = emptySet()
                            },
                            shape = AdminDesign.ButtonShape
                        ) { Text("Apply Ban") }
                    }
                } else {
                    Text("Directory", fontWeight = FontWeight.Bold, color = AdminDesign.OnSurfaceVariant)
                    TextButton(onClick = { isBulkMode = true }) { Text("Bulk Edit") }
                }
            }
        }

        if (isSyncing && users.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(10) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (users.isEmpty()) {
            AdminEmptyState(title = "No Players Found", description = "Try a different search term or check filters.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                items(users, key = { it.id }) { user ->
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
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCheckbox) {
                Checkbox(checked = isChecked, onCheckedChange = null)
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
            }
            Surface(
                modifier = Modifier.size(44.dp),
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
                    // Online indicator (simulated)
                    if (user.lastLogin?.contains("2026") == true) {
                        Surface(
                            modifier = Modifier.size(10.dp).align(Alignment.BottomEnd),
                            color = AdminDesign.Secondary,
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        ) {}
                    }
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.username, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AdminDesign.OnSurface)
                    if (user.email.endsWith("@daadi.fake")) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Badge(containerColor = AdminDesign.Secondary.copy(alpha = 0.2f)) {
                            Text("BOT", fontSize = 7.sp, color = AdminDesign.Secondary, fontWeight = FontWeight.Black)
                        }
                    }
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(14.dp), tint = AdminDesign.Primary)
                    }
                }
                Text(user.email, fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFFFFD700))
                    Text(" ${user.rating} ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("W: ${user.wins} L: ${user.losses}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val displayRole = if (user.role.isNotEmpty()) user.role else (user.roles.firstOrNull() ?: "publicuser")
                Badge(
                    containerColor = when(displayRole.lowercase()) {
                        "admin", "superadmin", "super_admin" -> AdminDesign.Secondary
                        "moderator" -> Color(0xFF8B5CF6)
                        "player" -> AdminDesign.Primary
                        else -> AdminDesign.OnSurfaceVariant
                    }
                ) {
                    Text(displayRole.uppercase(), fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (user.isBanned) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(containerColor = AdminDesign.Error) { Text("BANNED", fontSize = 8.sp, color = Color.White) }
                } else if (user.shadowBanned) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(containerColor = Color.Gray) { Text("SHADOW", fontSize = 8.sp, color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun AdminUserDetailsScreen(
    user: SupabaseUser,
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit,
    onHelpClick: (() -> Unit)? = null
) {
    AdminFoundationScaffold(
        title = "Player Profile",
        adminViewModel = adminViewModel,
        onBack = onBack,
        onHelpClick = onHelpClick
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            UserDetailsContent(user = user, adminViewModel = adminViewModel, onNavigate = onNavigate)
        }
    }
}

@Composable
fun UserDetailsContent(user: SupabaseUser, adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onNavigate: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Moderation", "Activity", "Economics", "Security", "Reports")

    var showResetAvatarConfirm by remember { mutableStateOf(false) }
    var showResetUsernameConfirm by remember { mutableStateOf(false) }
    var showForceLogoutConfirm by remember { mutableStateOf(false) }

    if (showResetAvatarConfirm) {
        AlertDialog(
            onDismissRequest = { showResetAvatarConfirm = false },
            title = { Text("Reset Avatar?") },
            text = { Text("Are you sure you want to reset the avatar for user ${user.username} to the default style?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        adminViewModel.authRepository.resetAvatar(user.id, "https://api.dicebear.com/7.x/avataaars/svg?seed=${user.username}")
                        showResetAvatarConfirm = false
                    }
                ) {
                    Text("Reset", color = AdminDesign.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAvatarConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminDesign.Surface,
            shape = AdminDesign.CardShape
        )
    }

    if (showResetUsernameConfirm) {
        AlertDialog(
            onDismissRequest = { showResetUsernameConfirm = false },
            title = { Text("Reset Username?") },
            text = { Text("Are you sure you want to reset the username for user ${user.username} to default 'User_${user.id.take(6)}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        adminViewModel.authRepository.resetUsername(user.id, "User_${user.id.take(6)}")
                        showResetUsernameConfirm = false
                    }
                ) {
                    Text("Reset", color = AdminDesign.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetUsernameConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminDesign.Surface,
            shape = AdminDesign.CardShape
        )
    }

    if (showForceLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showForceLogoutConfirm = false },
            title = { Text("Force Logout?") },
            text = { Text("Are you sure you want to force log out ${user.username} from all active devices and invalidate sessions immediately?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        adminViewModel.authRepository.forceLogout(user.id)
                        showForceLogoutConfirm = false
                    }
                ) {
                    Text("Force Logout", color = AdminDesign.Warning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForceLogoutConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminDesign.Surface,
            shape = AdminDesign.CardShape
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = AdminDesign.Primary.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user.username.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface)
                Text(user.id, style = MaterialTheme.typography.labelSmall, color = AdminDesign.OnSurfaceVariant)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = AdminDesign.OnSurfaceVariant)
            }
            
            // Quick Action Menu
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Reset Avatar") },
                        onClick = { 
                            showResetAvatarConfirm = true
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Reset Username") },
                        onClick = { 
                            showResetUsernameConfirm = true
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Force Logout") },
                        onClick = { 
                            showForceLogoutConfirm = true
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
        
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            items(tabs.size) { index ->
                val title = tabs[index]
                FilterChip(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    label = { Text(title, fontSize = 10.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                AnimatedContent(targetState = selectedTab, label = "user_details_tabs") { index ->
                    Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)) {
                        when(index) {
                            0 -> OverviewTab(user, adminViewModel, onNavigate)
                            1 -> ModerationTab(user, adminViewModel)
                            2 -> ActivityTab(user, adminViewModel)
                            3 -> EconomicsTab(user, adminViewModel)
                            4 -> SecurityTab(user, adminViewModel)
                            5 -> ReportsTab(user, adminViewModel)
                        }
                    }
                }
            }
            
            item {
                var showDeleteConfirm by remember { mutableStateOf(false) }
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Confirm Permanent Purge") },
                        text = { Text("WARNING: This will permanently delete user ${user.username} and all their statistics, games, and profile data from the database. This action is irreversible!") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    adminViewModel.authRepository.deleteUser(user.id)
                                    showDeleteConfirm = false
                                }
                            ) {
                                Text("PURGE DATA", color = AdminDesign.Error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("Cancel")
                            }
                        },
                        containerColor = AdminDesign.Surface,
                        shape = AdminDesign.CardShape
                    )
                }

                Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
                Button(
                    onClick = { showDeleteConfirm = true },
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
fun ShortcutButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = AdminDesign.Primary.copy(alpha = 0.05f),
            contentColor = AdminDesign.Primary
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun OverviewTab(user: SupabaseUser, adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onNavigate: (String) -> Unit) {
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

        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                Text("OPERATIONAL SHORTCUTS (CROSS-MODULE)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShortcutButton(
                            label = "Matches",
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                adminViewModel.clearAllFilters()
                                adminViewModel.filterUsername.value = user.username
                                onNavigate("match_archive")
                            }
                        )
                        ShortcutButton(
                            label = "Tickets",
                            icon = Icons.Default.Email,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                adminViewModel.clearAllFilters()
                                adminViewModel.filterUsername.value = user.username
                                onNavigate("feedback")
                            }
                        )
                        ShortcutButton(
                            label = "Economy",
                            icon = Icons.Default.ShoppingCart,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                adminViewModel.clearAllFilters()
                                adminViewModel.filterUserId.value = user.id
                                onNavigate("economy")
                            }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShortcutButton(
                            label = "Devices",
                            icon = Icons.Default.Smartphone,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                adminViewModel.clearAllFilters()
                                adminViewModel.filterDeviceId.value = user.deviceId
                                onNavigate("devices")
                            }
                        )
                        ShortcutButton(
                            label = "Fraud",
                            icon = Icons.Default.Security,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                adminViewModel.clearAllFilters()
                                adminViewModel.filterUserId.value = user.id
                                onNavigate("fraud")
                            }
                        )
                        ShortcutButton(
                            label = "Safety",
                            icon = Icons.Default.Warning,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                adminViewModel.clearAllFilters()
                                adminViewModel.filterUserId.value = user.id
                                onNavigate("safety")
                            }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShortcutButton(
                            label = "Audit Trail",
                            icon = Icons.Default.History,
                            modifier = Modifier.weight(1.0f),
                            onClick = {
                                adminViewModel.clearAllFilters()
                                adminViewModel.filterUserId.value = user.id
                                onNavigate("audit_logs")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModerationTab(user: SupabaseUser, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    var showBanConfirm by remember { mutableStateOf(false) }
    var showShadowBanConfirm by remember { mutableStateOf(false) }
    var showInvalidateConfirm by remember { mutableStateOf(false) }

    if (showBanConfirm) {
        AlertDialog(
            onDismissRequest = { showBanConfirm = false },
            title = { Text(if (user.isBanned) "Confirm Unban" else "Confirm Ban") },
            text = { Text("Are you sure you want to ${if (user.isBanned) "unban" else "ban"} user ${user.username}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        adminViewModel.userRepository.toggleUserBan(user.id)
                        showBanConfirm = false
                    }
                ) {
                    Text("Confirm", color = if (user.isBanned) AdminDesign.Secondary else AdminDesign.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBanConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminDesign.Surface,
            shape = AdminDesign.CardShape
        )
    }

    if (showShadowBanConfirm) {
        AlertDialog(
            onDismissRequest = { showShadowBanConfirm = false },
            title = { Text(if (user.shadowBanned) "Remove Shadow Ban" else "Apply Shadow Ban") },
            text = { Text("Are you sure you want to ${if (user.shadowBanned) "remove shadow ban from" else "apply shadow ban to"} user ${user.username}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        adminViewModel.authRepository.setShadowBan(user.id, !user.shadowBanned)
                        showShadowBanConfirm = false
                    }
                ) {
                    Text("Confirm", color = AdminDesign.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShadowBanConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminDesign.Surface,
            shape = AdminDesign.CardShape
        )
    }

    if (showInvalidateConfirm) {
        AlertDialog(
            onDismissRequest = { showInvalidateConfirm = false },
            title = { Text("Invalidate Sessions?") },
            text = { Text("Are you sure you want to immediately invalidate all active login sessions and force logout ${user.username}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        adminViewModel.authRepository.forceLogout(user.id)
                        showInvalidateConfirm = false
                    }
                ) {
                    Text("Confirm", color = AdminDesign.Warning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInvalidateConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminDesign.Surface,
            shape = AdminDesign.CardShape
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
        ActionTile(
            title = if (user.isBanned) "Unban Account" else "Ban Account",
            subtitle = if (user.isBanned) "Restore full access for this player" else "Prevents all game access and authentication",
            icon = Icons.Default.Block,
            color = if (user.isBanned) AdminDesign.Secondary else AdminDesign.Error,
            onClick = { showBanConfirm = true }
        )
        ActionTile(
            title = if (user.shadowBanned) "Remove Shadow Ban" else "Apply Shadow Ban",
            subtitle = "Player can still play but only with other toxic users",
            icon = Icons.Default.VisibilityOff,
            color = Color.Gray,
            onClick = { showShadowBanConfirm = true }
        )
        ActionTile(
            title = if (user.isVerified) "Remove Verification" else "Verify Identity",
            subtitle = "Grants the blue verification badge in profiles",
            icon = Icons.Default.Verified,
            color = AdminDesign.Primary,
            onClick = { adminViewModel.authRepository.updateUserVerification(user.id, !user.isVerified) }
        )
        ActionTile(
            title = "Invalidate Sessions",
            subtitle = "Force logout from all devices immediately",
            icon = Icons.Default.Logout,
            color = AdminDesign.Warning,
            onClick = { showInvalidateConfirm = true }
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
            onClick = { adminViewModel.authRepository.updateInternalNotes(user.id, notes) },
            modifier = Modifier.align(Alignment.End).padding(top = AdminDesign.SpacingSmall),
            shape = AdminDesign.ButtonShape,
            colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary)
        ) {
            Text("SAVE NOTES")
        }
    }
}

@Composable
fun ActivityTab(user: SupabaseUser, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    val loginHistory by adminViewModel.userRepository.userLoginHistory.collectAsStateWithLifecycle()
    LaunchedEffect(user.id) { adminViewModel.userRepository.fetchLoginHistory(user.id) }

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
fun EconomicsTab(user: SupabaseUser, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)) {
        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
            Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                Text("CURRENCY ADJUSTMENTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    Button(onClick = { adminViewModel.economyRepository.adjustUserEconomy(user.id, 500, 0) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("+500 C", fontSize = 10.sp) }
                    Button(onClick = { adminViewModel.economyRepository.adjustUserEconomy(user.id, 0, 1000) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("+1k XP", fontSize = 10.sp) }
                }
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    OutlinedButton(onClick = { adminViewModel.economyRepository.adjustUserEconomy(user.id, -500, 0) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("-500 C", fontSize = 10.sp) }
                    OutlinedButton(onClick = { adminViewModel.economyRepository.adjustUserEconomy(user.id, 0, -1000) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("-1k XP", fontSize = 10.sp) }
                }
            }
        }
        
        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
            Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                Text("RATING OVERRIDES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    Button(onClick = { adminViewModel.economyRepository.adjustUserStats(user.id, 1, 0, 50) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("+1 Win (+50 E)", fontSize = 10.sp) }
                    Button(onClick = { adminViewModel.economyRepository.adjustUserStats(user.id, 0, 1, -50) }, modifier = Modifier.weight(1f), shape = AdminDesign.ButtonShape) { Text("+1 Loss (-50 E)", fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable
fun SecurityTab(user: SupabaseUser, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)) {
        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
            Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                Text("DEVICE INTELLIGENCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                MetadataRow("Device Model", user.metadata?.get("device_model")?.toString() ?: "Android SDK 34")
                MetadataRow("OS Version", user.metadata?.get("os_version")?.toString() ?: "Android 14")
                MetadataRow("Rooted", user.metadata?.get("is_rooted")?.toString() ?: "False")
                MetadataRow("VPN/Proxy", user.metadata?.get("is_vpn")?.toString() ?: "False")
                MetadataRow("Emulator", user.metadata?.get("is_emulator")?.toString() ?: "False")
            }
        }
        
        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface), shape = AdminDesign.CardShape) {
            Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                Text("AUTHENTICATION STATE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                MetadataRow("Auth Method", if (user.email.contains("google")) "OAuth (Google)" else "Email/Pass")
                MetadataRow("Multi-Factor", "Disabled")
                MetadataRow("Recovery Status", "Verified")
            }
        }
    }
}

@Composable
fun UserDirectoryStats(users: List<SupabaseUser>) {
    val total = users.size
    val banned = users.count { it.isBanned }
    val verified = users.count { it.isVerified }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(AdminDesign.SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
    ) {
        AdminStatCard("Total Players", "$total", Icons.Default.People, modifier = Modifier.weight(1f))
        AdminStatCard("Banned", "$banned", Icons.Default.Block, AdminDesign.Error, modifier = Modifier.weight(1f))
        AdminStatCard("Verified", "$verified", Icons.Default.Verified, AdminDesign.Secondary, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ReportsTab(user: SupabaseUser, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    val reportsState = adminViewModel.analyticsRepository.reports.collectAsStateWithLifecycle()
    val reports = reportsState.value
    val userReports = reports.filter { it.reportedId == user.id }

    if (userReports.isEmpty()) {
        AdminEmptyState(title = "Clean Record", description = "This user hasn't been reported by other players yet.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
            userReports.forEach { report ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = when(report.priority.lowercase()) {
                                    "high" -> AdminDesign.Error
                                    "medium" -> AdminDesign.Warning
                                    else -> AdminDesign.Primary
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    report.priority.uppercase(),
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(report.category.uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(report.status.uppercase(), fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(report.reason, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Reporter ID: ${report.reporterId ?: "Anonymous"}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
                    }
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

