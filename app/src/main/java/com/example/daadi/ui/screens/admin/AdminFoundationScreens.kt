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
fun AdminPermissionMatrixScreen(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val roles by supabaseManager.roles.collectAsStateWithLifecycle()
    val permissions by supabaseManager.permissions.collectAsStateWithLifecycle()
    val rolePermissions by supabaseManager.rolePermissions.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        supabaseManager.fetchRolesAndPermissions()
    }

    AdminFoundationScaffold(
        title = "Permission Matrix",
        adminViewModel = adminViewModel,
        onBack = onBack,
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
                title = "No Roles/Permissions Found",
                description = "Role-based access matrix records could not be retrieved from the cloud services. Please initialize your roles table.",
                icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(64.dp), tint = AdminDesign.Error) },
                actionButton = {
                    Button(
                        onClick = { supabaseManager.fetchRolesAndPermissions() },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary),
                        shape = AdminDesign.ButtonShape
                    ) {
                        Text("RETRY CONNECTION")
                    }
                }
            )
        } else {
            val publicUserRole = remember(roles) { roles.find { it.name.lowercase() == "publicuser" } }
            val playerRole = remember(roles) { roles.find { it.name.lowercase() == "player" } }
            val adminRole = remember(roles) { roles.find { it.name.lowercase() == "admin" } }

            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Header Row
                Surface(
                    color = AdminDesign.Surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AdminDesign.SpacingMedium, vertical = AdminDesign.SpacingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYSTEM PERMISSIONS",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = AdminDesign.OnSurfaceVariant,
                            modifier = Modifier.weight(1.5f)
                        )
                        Row(
                            modifier = Modifier.weight(2f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PUBLIC", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text("PLAYER", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text("ADMIN", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }

                // Scrollable Matrix Items
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = AdminDesign.SpacingLarge)
                ) {
                    items(permissions) { perm ->
                        val prettyName = perm.name.replace("_", " ").uppercase()
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AdminDesign.SpacingMedium, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AdminDesign.SpacingMedium, vertical = AdminDesign.SpacingSmall),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left side: Permission Name and desc
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(
                                        text = prettyName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = AdminDesign.OnSurface
                                    )
                                    Text(
                                        text = perm.description ?: "Grants access to ${perm.name}",
                                        fontSize = 10.sp,
                                        color = AdminDesign.OnSurfaceVariant
                                    )
                                }

                                // Right side: Role column toggles
                                Row(
                                    modifier = Modifier.weight(2f),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Public User Role Checkbox
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.width(56.dp)) {
                                        if (publicUserRole != null) {
                                            val isChecked = rolePermissions.any { it.roleId == publicUserRole.id && it.permissionId == perm.id }
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        supabaseManager.addRolePermission(publicUserRole.id, perm.id)
                                                    } else {
                                                        supabaseManager.removeRolePermission(publicUserRole.id, perm.id)
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = AdminDesign.Primary,
                                                    uncheckedColor = AdminDesign.OnSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            )
                                        } else {
                                            Text("-", color = AdminDesign.OnSurfaceVariant)
                                        }
                                    }

                                    // Player Role Checkbox
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.width(56.dp)) {
                                        if (playerRole != null) {
                                            val isChecked = rolePermissions.any { it.roleId == playerRole.id && it.permissionId == perm.id }
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        supabaseManager.addRolePermission(playerRole.id, perm.id)
                                                    } else {
                                                        supabaseManager.removeRolePermission(playerRole.id, perm.id)
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = AdminDesign.Primary,
                                                    uncheckedColor = AdminDesign.OnSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            )
                                        } else {
                                            Text("-", color = AdminDesign.OnSurfaceVariant)
                                        }
                                    }

                                    // Admin Role Checkbox
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.width(56.dp)) {
                                        if (adminRole != null) {
                                            val isChecked = rolePermissions.any { it.roleId == adminRole.id && it.permissionId == perm.id }
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        supabaseManager.addRolePermission(adminRole.id, perm.id)
                                                    } else {
                                                        supabaseManager.removeRolePermission(adminRole.id, perm.id)
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = AdminDesign.Secondary,
                                                    uncheckedColor = AdminDesign.OnSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            )
                                        } else {
                                            Text("-", color = AdminDesign.OnSurfaceVariant)
                                        }
                                    }
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
    showBackButton: Boolean = true
) {
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
            IconButton(onClick = { /* Search */ }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = { /* Notifications */ }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
