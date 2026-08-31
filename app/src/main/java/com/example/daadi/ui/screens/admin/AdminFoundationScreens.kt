package com.example.daadi.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daadi.data.supabase.*

val supabaseManager: com.example.daadi.data.supabase.SupabaseManager
    get() = com.example.daadi.DaadiApplication.instance.supabaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFoundationScaffold(
    title: String,
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel? = null,
    onBack: () -> Unit,
    onHelpClick: (() -> Unit)? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    showSearch: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    AdminTheme {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    Column {
                        TopAppBar(
                            title = { 
                                Column {
                                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Management Console", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            actions = {
                                actions()
                                if (onHelpClick != null) {
                                    IconButton(onClick = onHelpClick) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                if (supabaseManager != null) {
                                    IconButton(onClick = { supabaseManager.loadInitialData() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (showSearch) {
                            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(horizontal = AdminDesign.SpacingMedium, vertical = AdminDesign.SpacingSmall)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search records...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    trailingIcon = if (searchQuery.isNotEmpty()) {
                                        { IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                                    } else null,
                                    shape = AdminDesign.InputShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            // Wrap in a box with max width for tablet optimization
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(modifier = Modifier.widthIn(max = 800.dp).fillMaxSize()) {
                    content(PaddingValues(0.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFoundationScaffold(
    title: String,
    supabaseManager: com.example.daadi.data.supabase.SupabaseManager?,
    onBack: () -> Unit,
    onHelpClick: (() -> Unit)? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    showSearch: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    AdminFoundationScaffold(
        title = title,
        adminViewModel = null,
        onBack = onBack,
        onHelpClick = onHelpClick,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        showSearch = showSearch,
        actions = actions,
        content = content
    )
}

@Composable
fun AdminAuditTrailScreen(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val auditLogs by adminViewModel.adminRepository.auditLogs.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()

    AdminFoundationScaffold(
        title = "Audit Trail",
        adminViewModel = adminViewModel,
        onBack = onBack,
        showSearch = true,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it }
    ) { padding ->
        if (isSyncing && auditLogs.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(10) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (auditLogs.isEmpty()) {
            AdminEmptyState(title = "No Audit Logs", description = "System activity logs will appear here once events occur.")
        } else {
            val filteredLogs = remember(auditLogs, searchQuery) {
                auditLogs.filter { 
                    it.actionType.contains(searchQuery, true) || 
                    it.targetTable?.contains(searchQuery, true) == true ||
                    it.reason?.contains(searchQuery, true) == true ||
                    it.actorId?.contains(searchQuery, true) == true
                }
            }
            
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                items(filteredLogs) { log ->
                    AuditLogItem(log)
                }
            }
        }
    }
}

@Composable
fun AuditLogItem(log: SupabaseAuditLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = when(log.actionType) {
                        "DELETE", "BAN" -> AdminDesign.Error.copy(alpha = 0.1f)
                        "UPDATE" -> AdminDesign.Primary.copy(alpha = 0.1f)
                        "CREATE" -> AdminDesign.Success.copy(alpha = 0.1f)
                        else -> AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when(log.actionType) {
                                "DELETE" -> Icons.Default.Delete
                                "BAN" -> Icons.Default.Block
                                "UPDATE" -> Icons.Default.Edit
                                "CREATE" -> Icons.Default.Add
                                else -> Icons.Default.History
                            },
                            contentDescription = null,
                            tint = when(log.actionType) {
                                "DELETE", "BAN" -> AdminDesign.Error
                                "UPDATE" -> AdminDesign.Primary
                                "CREATE" -> AdminDesign.Success
                                else -> AdminDesign.OnSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(log.actionType, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AdminDesign.OnSurface)
                    Text(log.createdAt, fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                }
                Text(
                    text = log.targetTable?.uppercase() ?: "SYSTEM",
                    style = MaterialTheme.typography.labelSmall,
                    color = AdminDesign.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Text(
                text = "Target ID: ${log.targetId ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = AdminDesign.OnSurface
            )
            if (log.reason != null) {
                Surface(
                    modifier = Modifier.padding(top = AdminDesign.SpacingSmall).fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = AdminDesign.Background
                ) {
                    Text(
                        text = "Reason: ${log.reason}",
                        modifier = Modifier.padding(AdminDesign.SpacingSmall),
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminDesign.OnSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Actor: ${log.actorId ?: "System"}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
            }
        }
    }
}

@Composable
fun AdminPermissionMatrixScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit,
    onHelpClick: (() -> Unit)? = null
) {
    val roles by supabaseManager.roles.collectAsStateWithLifecycle()
    val permissions by supabaseManager.permissions.collectAsStateWithLifecycle()
    val rolePermissions by supabaseManager.rolePermissions.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()

    var selectedRoleForSimulator by remember { mutableStateOf<SupabaseRole?>(null) }

    LaunchedEffect(Unit) {
        supabaseManager.fetchRolesAndPermissions()
    }

    AdminFoundationScaffold(
        title = "RBAC Matrix",
        adminViewModel = adminViewModel,
        onBack = onBack,
        onHelpClick = onHelpClick,
        actions = {
            IconButton(onClick = { supabaseManager.fetchRolesAndPermissions() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Matrix", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        if (isSyncing && (roles.isEmpty() || permissions.isEmpty())) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                items(10) { ShimmerItem() }
            }
        } else if (roles.isEmpty() || permissions.isEmpty()) {
            AdminEmptyState(
                title = "Access Control Sync Failed",
                description = "Role-based access matrix records could not be retrieved. Please initialize your security tables.",
                icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(64.dp), tint = AdminDesign.Error) },
                actionButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { supabaseManager.fetchRolesAndPermissions() }, shape = AdminDesign.ButtonShape) { Text("RETRY") }
                        OutlinedButton(onClick = { supabaseManager.initializeSystemRoles() }, shape = AdminDesign.ButtonShape) { Text("SEED SYSTEM") }
                    }
                }
            )
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Roles Summary Bar
                RoleHierarchyBar(roles)
                
                // Horizontal Matrix Header
                Surface(
                    color = AdminDesign.Surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AdminDesign.SpacingMedium, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURITY PERMISSIONS",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = AdminDesign.OnSurfaceVariant,
                            modifier = Modifier.width(160.dp)
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(roles) { role ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                                    Text(role.name.uppercase(), fontWeight = FontWeight.Bold, fontSize = 8.sp, color = AdminDesign.OnSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                // Scrollable Matrix Items
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = AdminDesign.SpacingLarge)
                ) {
                    items(permissions) { perm ->
                        PermissionMatrixRow(perm, roles, rolePermissions)
                    }
                    
                    item {
                        PermissionSimulatorSection(roles, permissions, rolePermissions)
                    }
                }
            }
        }
    }
}

@Composable
fun RoleHierarchyBar(roles: List<SupabaseRole>) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth().padding(AdminDesign.SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
    ) {
        items(roles) { role ->
            val color = when(role.name.lowercase()) {
                "admin", "superadmin" -> AdminDesign.Secondary
                "moderator" -> Color(0xFF8B5CF6)
                "support" -> Color(0xFFF59E0B)
                "player" -> AdminDesign.Primary
                else -> AdminDesign.OnSurfaceVariant
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                shape = AdminDesign.CardShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(12.dp), tint = color)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(role.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 10.sp, color = color)
                }
            }
        }
    }
}

@Composable
fun PermissionMatrixRow(perm: SupabasePermission, roles: List<SupabaseRole>, rolePermissions: List<SupabaseRolePermission>) {
    val prettyName = perm.name.replace("_", " ").uppercase()
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AdminDesign.SpacingMedium, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AdminDesign.OnSurfaceVariant.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AdminDesign.SpacingMedium, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(160.dp)) {
                Text(text = prettyName, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = AdminDesign.OnSurface)
                Text(text = perm.description ?: "Grants ${perm.name} access", fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, maxLines = 1)
            }

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(roles) { role ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.width(60.dp)) {
                        val isChecked = rolePermissions.any { it.roleId == role.id && it.permissionId == perm.id }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    supabaseManager.addRolePermission(role.id, perm.id)
                                } else {
                                    supabaseManager.removeRolePermission(role.id, perm.id)
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = AdminDesign.Primary)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PermissionSimulatorSection(roles: List<SupabaseRole>, permissions: List<SupabasePermission>, rolePermissions: List<SupabaseRolePermission>) {
    var selectedRole by remember { mutableStateOf<SupabaseRole?>(null) }
    
    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
        Divider(modifier = Modifier.padding(vertical = AdminDesign.SpacingLarge), color = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f))
        
        Text("PERMISSIONS SIMULATOR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurface)
        Text("Visualize active capabilities for a selected authorization group.", style = MaterialTheme.typography.bodySmall, color = AdminDesign.OnSurfaceVariant)
        
        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
        
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(roles) { role ->
                FilterChip(
                    selected = selectedRole?.id == role.id,
                    onClick = { selectedRole = role },
                    label = { Text(role.name) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
        
        if (selectedRole != null) {
            val activePerms = permissions.filter { p -> rolePermissions.any { it.roleId == selectedRole!!.id && it.permissionId == p.id } }
            
            if (activePerms.isEmpty()) {
                Text("This role has zero active permissions assigned.", color = AdminDesign.Error, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            } else {
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activePerms.forEach { perm ->
                        Surface(
                            color = AdminDesign.Secondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AdminDesign.Secondary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = perm.name.replace("_", " ").uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminDesign.Secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSessionManagerScreen(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val sessions by adminViewModel.adminRepository.adminSessions.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()

    AdminFoundationScaffold("Admin Sessions", supabaseManager, onBack) { padding ->
        if (isSyncing && sessions.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (sessions.isEmpty()) {
            AdminEmptyState(title = "No Active Sessions", description = "There are no recorded admin sessions in the cloud services.")
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                items(sessions) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.CardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (session.isSuspicious) Color(0xFFFFEBEE) else AdminDesign.Surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                    ) {
                        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("ID: ${session.id.take(8)}...", fontWeight = FontWeight.Bold)
                                    if (session.isSuspicious) {
                                        Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                                        Badge(containerColor = AdminDesign.Error) { Text("SUSPICIOUS", color = Color.White) }
                                    }
                                }
                                Text("IP Address: ${session.ipAddress ?: "Unknown"}", fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
                                Text("Last Active: ${session.lastActive}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                            }
                            if (session.terminatedAt == null) {
                                Button(
                                    onClick = { adminViewModel.adminRepository.terminateAdminSession(session.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Error.copy(alpha = 0.1f), contentColor = AdminDesign.Error),
                                    shape = AdminDesign.ButtonShape,
                                    contentPadding = PaddingValues(horizontal = AdminDesign.SpacingMedium)
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                                    Text("TERMINATE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("TERMINATED", color = AdminDesign.OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthStat(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(
    title: String,
    currentUser: SupabaseUser?,
    onBack: () -> Unit,
    onHelpClick: (() -> Unit)? = null,
    showBackButton: Boolean = true
) {
    var showSearchDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (currentUser != null) {
                    Text("Session: ${currentUser.username} • ${currentUser.role}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            IconButton(onClick = { showSearchDialog = true }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = { showNotificationsDialog = true }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
            if (onHelpClick != null) {
                IconButton(onClick = onHelpClick) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Help")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )

    if (showSearchDialog) {
        var query by remember { mutableStateOf("") }
        val users by supabaseManager._users.collectAsState()
        val matches by supabaseManager._matches.collectAsState()
        val settings by supabaseManager._systemSettings.collectAsState()

        val filteredUsers = remember(users, query) {
            if (query.isBlank()) emptyList()
            else users.filter { 
                it.username.contains(query, ignoreCase = true) || 
                it.email.contains(query, ignoreCase = true) || 
                it.id.contains(query, ignoreCase = true)
            }
        }

        val filteredMatches = remember(matches, query) {
            if (query.isBlank()) emptyList()
            else matches.filter { 
                it.id.contains(query, ignoreCase = true) || 
                it.hostName.contains(query, ignoreCase = true) || 
                it.opponentName.contains(query, ignoreCase = true) || 
                (it.status ?: "").contains(query, ignoreCase = true)
            }
        }

        val filteredSettings = remember(settings, query) {
            if (query.isBlank()) emptyList()
            else settings.filter { 
                it.key.contains(query, ignoreCase = true) || 
                (it.value ?: "").contains(query, ignoreCase = true) || 
                (it.description ?: "").contains(query, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Global Enterprise Search", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search players, lobbies, configs...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear") } }
                        } else null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (query.isBlank()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Type a query to search across the system.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (filteredUsers.isEmpty() && filteredMatches.isEmpty() && filteredSettings.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No matching records found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (filteredUsers.isNotEmpty()) {
                                item {
                                    Text("PLAYERS (${filteredUsers.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                items(filteredUsers) { user ->
                                    var isBanned by remember(user.isBanned) { mutableStateOf(user.isBanned) }
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(user.username, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                Text("Email: ${user.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("Role: ${user.role} • Coins: ${user.coins ?: 0}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Button(
                                                onClick = {
                                                    isBanned = !isBanned
                                                    if (supabaseManager.isConfigured) {
                                                        supabaseManager.toggleUserBanRemote(user.id, user.isBanned)
                                                    } else {
                                                        supabaseManager._users.value = supabaseManager._users.value.map {
                                                            if (it.id == user.id) it.copy(isBanned = !user.isBanned) else it
                                                        }
                                                        supabaseManager.saveSimulatorUsers()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isBanned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text(if (isBanned) "UNBAN" else "BAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (filteredMatches.isNotEmpty()) {
                                item {
                                    Text("LOBBIES & MATCHES (${filteredMatches.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                                items(filteredMatches) { match ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Match ID: ${match.id.take(8)}...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.padding(2.dp)
                                                ) {
                                                    Text(
                                                        text = (match.status ?: "UNKNOWN").uppercase(),
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Host: ${match.hostName} vs Opponent: ${match.opponentName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Moves Count: ${match.movesCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                            
                            if (filteredSettings.isNotEmpty()) {
                                item {
                                    Text("REMOTE CONFIG (${filteredSettings.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                }
                                items(filteredSettings) { setting ->
                                    var editValue by remember { mutableStateOf(setting.value ?: "") }
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(setting.key, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE65100))
                                            if (setting.description != null) {
                                                Text(setting.description!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                if (setting.value == "on" || setting.value == "off") {
                                                    val isChecked = editValue == "on"
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Toggle state: ", style = MaterialTheme.typography.bodySmall)
                                                        Switch(
                                                            checked = isChecked,
                                                            onCheckedChange = { checked ->
                                                                val newVal = if (checked) "on" else "off"
                                                                editValue = newVal
                                                                supabaseManager.updateSystemSetting(setting.key, newVal)
                                                            }
                                                        )
                                                    }
                                                } else {
                                                    OutlinedTextField(
                                                        value = editValue,
                                                        onValueChange = { editValue = it },
                                                        modifier = Modifier.weight(1f).height(54.dp),
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                                        ),
                                                        trailingIcon = {
                                                            IconButton(
                                                                onClick = {
                                                                    supabaseManager.updateSystemSetting(setting.key, editValue)
                                                                }
                                                            ) {
                                                                Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                                                            }
                                                        }
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
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showNotificationsDialog) {
        val fraudAlerts by supabaseManager._fraudAlerts.collectAsState()
        val antiCheatLogs by supabaseManager._antiCheatLogs.collectAsState()
        val crashLogs by supabaseManager._crashLogs.collectAsState()

        val recentNotifications = remember(fraudAlerts, antiCheatLogs, crashLogs) {
            val list = mutableListOf<SystemNotificationItem>()
            fraudAlerts.take(5).forEach {
                list.add(
                    SystemNotificationItem(
                        title = "FRAUD RADAR: Suspicious Activity",
                        body = "Player ID ${it.userId.take(8)} triggered fraud pattern '${it.type}' (confidence: ${it.confidence}). Status: ${it.status}.",
                        time = it.createdAt,
                        type = NotificationType.Fraud,
                        color = Color(0xFFFF9800)
                    )
                )
            }
            antiCheatLogs.take(5).forEach {
                list.add(
                    SystemNotificationItem(
                        title = "ANTI-CHEAT: Violation Warning",
                        body = "Detected violation '${it.violationType}' for user ${it.userId?.take(8) ?: "N/A"} in match ${it.matchId?.take(8) ?: "N/A"}. Severity: ${it.severity}.",
                        time = it.createdAt,
                        type = NotificationType.AntiCheat,
                        color = Color(0xFFF44336)
                    )
                )
            }
            crashLogs.take(5).forEach {
                list.add(
                    SystemNotificationItem(
                        title = "CRASH CENTER: Exception Logged",
                        body = "${it.exception}: ${it.stacktrace.take(80)}",
                        time = it.createdAt,
                        type = NotificationType.Crash,
                        color = Color(0xFFE91E63)
                    )
                )
            }
            if (list.isEmpty()) {
                list.add(SystemNotificationItem("System Connected", "Connected to game monitoring channels successfully.", "1m ago", NotificationType.Info, Color(0xFF4CAF50)))
                list.add(SystemNotificationItem("Maintenance Schedule", "Upcoming weekly rolling database updates configured.", "15m ago", NotificationType.Info, Color(0xFF2196F3)))
                list.add(SystemNotificationItem("Rate Limiting", "API endpoint threshold normal (340 req/min).", "1h ago", NotificationType.Info, Color(0xFF2196F3)))
            }
            list.sortByDescending { it.time }
            list
        }

        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live System Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    Text("Real-time telemetry and automated system alerts:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentNotifications) { alert ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = alert.color.copy(alpha = 0.08f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, alert.color.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Icon(
                                        imageVector = when(alert.type) {
                                            NotificationType.Fraud -> Icons.Default.Warning
                                            NotificationType.AntiCheat -> Icons.Default.Warning
                                            NotificationType.Crash -> Icons.Default.BugReport
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        tint = alert.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = alert.color)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(alert.body, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(alert.time, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

data class SystemNotificationItem(
    val title: String,
    val body: String,
    val time: String,
    val type: NotificationType,
    val color: Color
)

enum class NotificationType {
    Fraud, AntiCheat, Crash, Info
}
