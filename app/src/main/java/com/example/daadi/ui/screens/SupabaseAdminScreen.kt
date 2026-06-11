package com.example.daadi.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseAnnouncement
import com.example.daadi.data.supabase.SupabaseManager
import com.example.daadi.data.supabase.SupabaseMatch
import com.example.daadi.data.supabase.SupabaseSystemSetting
import com.example.daadi.data.supabase.SupabaseUser
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseAdminScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val users by supabaseManager.users.collectAsStateWithLifecycle()
    val matches by supabaseManager.matches.collectAsStateWithLifecycle()
    val announcements by supabaseManager.announcements.collectAsStateWithLifecycle()
    val settings by supabaseManager.systemSettings.collectAsStateWithLifecycle()
    val isLoading by supabaseManager.isLoading.collectAsStateWithLifecycle()
    val errorMsg by supabaseManager.errorMessage.collectAsStateWithLifecycle()
    val currentRole by supabaseManager.currentAdminRole.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var showAddAnnouncementDialog by remember { mutableStateOf(false) }
    var editSettingItem by remember { mutableStateOf<SupabaseSystemSetting?>(null) }

    val tabs = listOf(
        "Users" to Icons.Default.AccountBox,
        "Announcements" to Icons.Default.Notifications,
        "Matches" to Icons.Default.PlayArrow,
        "Settings" to Icons.Default.Settings
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Supabase Control Board", 
                            fontFamily = FontFamily.Serif, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (supabaseManager.isConfigured) "Connected to Cloud Database" else "Local Sandbox Simulator Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (supabaseManager.isConfigured) Color(0xFF2E7D32) else Color(0xFFC75D27)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    IconButton(onClick = { supabaseManager.loadInitialData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Identity Sandbox Switcher (Perfect for testing role permissions easily)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "TEST PRESETS", 
                            fontSize = 9.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFFC75D27), 
                            letterSpacing = 1.sp
                        )
                        Text("Simulate Authorization Role:", fontSize = 12.sp, color = Color(0xFF8B5E3C))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentRole == "admin",
                            onClick = { supabaseManager.setAdminRoleTesting("admin") },
                            label = { Text("Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF5C2D0A),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = currentRole == "user",
                            onClick = { supabaseManager.setAdminRoleTesting("user") },
                            label = { Text("Standard User", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFC75D27),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFE5A93B).copy(alpha = 0.3f))

            // 2. Main Interface Based on Authorization Checks
            if (currentRole != "admin") {
                // Denied State View for Standard Users
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFFC62828).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock, 
                            contentDescription = "Access Denied", 
                            tint = Color(0xFFC62828), 
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "ADMIN ACCESS REQUIRED",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF5C2D0A),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your simulated user account role is 'User'. Only admins are authorized to view or edit database registers.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { supabaseManager.setAdminRoleTesting("admin") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A))
                    ) {
                        Text("Elevate My Role to Admin")
                    }
                }
            } else {
                // Admin Dashboard Tab Layout
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFFFFBF4),
                    contentColor = Color(0xFF5C2D0A)
                ) {
                    tabs.forEachIndexed { idx, pair ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { 
                                Text(
                                    pair.first, 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            icon = { Icon(pair.second, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                // Database Sync Warning Banner
                if (!supabaseManager.isConfigured) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3E0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning, 
                                contentDescription = "Config Notice", 
                                tint = Color(0xFFE65100), 
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Simulation active. Add SUPABASE_URL and SUPABASE_ANON_KEY to .env to use live cloud sync.",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFFFB74D).copy(alpha = 0.4f))
                    }
                }

                // Error State Display
                if (errorMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color(0xFFC62828))
                            Text(errorMsg!!, color = Color(0xFFC62828), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF5C2D0A))
                    }
                } else {
                    when (selectedTab) {
                        0 -> UsersPanel(users = users, onRoleChange = { id, role -> supabaseManager.changeUserRole(id, role) }, onBanToggle = { id -> supabaseManager.toggleUserBan(id) }, onDelete = { id -> supabaseManager.deleteUser(id) })
                        1 -> AnnouncementsPanel(announcements = announcements, onCreateClick = { showAddAnnouncementDialog = true }, onToggle = { id -> supabaseManager.toggleAnnouncementStatus(id) }, onDelete = { id -> supabaseManager.deleteAnnouncement(id) })
                        2 -> MatchesPanel(matches = matches, onDelete = { id -> supabaseManager.deleteMatch(id) })
                        3 -> SettingsPanel(settings = settings, onEditClick = { editSettingItem = it })
                    }
                }
            }
        }
    }

    // New Announcement Dialog
    if (showAddAnnouncementDialog) {
        var annTitle by remember { mutableStateOf("") }
        var annContent by remember { mutableStateOf("") }
        var annActive by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddAnnouncementDialog = false },
            title = { Text("Publish Announcement") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = annTitle,
                        onValueChange = { annTitle = it },
                        label = { Text("Announcement Title") },
                        placeholder = { Text("e.g. Server Maintenance") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5C2D0A),
                            focusedLabelColor = Color(0xFF5C2D0A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = annContent,
                        onValueChange = { annContent = it },
                        label = { Text("Announcement Message Body") },
                        placeholder = { Text("Enter details...") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5C2D0A),
                            focusedLabelColor = Color(0xFF5C2D0A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Set Active Immediately")
                        Switch(
                            checked = annActive,
                            onCheckedChange = { annActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF5C2D0A), checkedTrackColor = Color(0xFFD4A55A))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (annTitle.isNotBlank() && annContent.isNotBlank()) {
                            supabaseManager.createAnnouncement(annTitle, annContent, annActive)
                            showAddAnnouncementDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A)),
                    enabled = annTitle.isNotBlank() && annContent.isNotBlank()
                ) {
                    Text("Publish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAnnouncementDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFFFFFBF4)
        )
    }

    // System Setting Editing Dialog
    if (editSettingItem != null) {
        var settingVal by remember { mutableStateOf(editSettingItem!!.value) }

        AlertDialog(
            onDismissRequest = { editSettingItem = null },
            title = { Text("Configure Variable") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = editSettingItem!!.key.uppercase().replace("_", " "),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C2D0A)
                    )
                    Text(editSettingItem!!.description, fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = settingVal,
                        onValueChange = { settingVal = it },
                        label = { Text("Config Value") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5C2D0A),
                            focusedLabelColor = Color(0xFF5C2D0A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (settingVal.isNotBlank()) {
                            supabaseManager.updateSystemSetting(editSettingItem!!.key, settingVal)
                            editSettingItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A))
                ) {
                    Text("Update Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { editSettingItem = null }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFFFFFBF4)
        )
    }
}

@Composable
fun UsersPanel(
    users: List<SupabaseUser>,
    onRoleChange: (String, String) -> Unit,
    onBanToggle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No registered users audited.", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(users) { usr ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = usr.username,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF5C2D0A),
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = usr.role.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (usr.role == "admin") Color(0xFF2E7D32) else Color(0xFF1976D2),
                                        modifier = Modifier
                                            .background(
                                                color = if (usr.role == "admin") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(usr.email, fontSize = 12.sp, color = Color.Gray)
                            }

                            if (usr.isBanned) {
                                Text(
                                    text = "BANNED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(Color(0xFFC62828), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Stats: ${usr.totalGames} Games • ${usr.wins}W / ${usr.losses}L",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF8B5E3C)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Toggle Role Trigger
                                TextButton(
                                    onClick = {
                                        val targetRole = if (usr.role == "admin") "user" else "admin"
                                        onRoleChange(usr.id, targetRole)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5C2D0A))
                                ) {
                                    Text(if (usr.role == "admin") "Demote" else "Promote", fontSize = 11.sp)
                                }

                                // Ban/Unban Trigger
                                TextButton(
                                    onClick = { onBanToggle(usr.id) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (usr.isBanned) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(if (usr.isBanned) "Unban" else "Ban User", fontSize = 11.sp)
                                }

                                // Delete Button
                                IconButton(
                                    onClick = { onDelete(usr.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Delete", 
                                        tint = Color.LightGray, 
                                        modifier = Modifier.size(16.dp)
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
fun AnnouncementsPanel(
    announcements: List<SupabaseAnnouncement>,
    onCreateClick: () -> Unit,
    onToggle: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CREATE NEW ANNOUNCEMENT", fontWeight = FontWeight.Bold)
            }
        }

        if (announcements.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No announcements loaded.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(announcements) { ann ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (ann.isActive) Color(0xFFFFFBFA) else Color(0xFFF2F2F2)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ann.title,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ann.isActive) Color(0xFF5C2D0A) else Color.Gray,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Published: ${ann.createdAt}", 
                                        fontSize = 10.sp, 
                                        color = Color.LightGray
                                    )
                                }

                                Switch(
                                    checked = ann.isActive,
                                    onCheckedChange = { onToggle(ann.id) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF5C2D0A), 
                                        checkedTrackColor = Color(0xFFD4A55A)
                                    ),
                                    modifier = Modifier.scaleScaleHelper()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = ann.content,
                                fontSize = 12.sp,
                                color = if (ann.isActive) Color.DarkGray else Color.Gray,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { onDelete(ann.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Delete", 
                                        tint = Color(0xFFC62828).copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
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
fun Modifier.scaleScaleHelper(): Modifier = this.padding(0.dp) // decorative

@Composable
fun MatchesPanel(
    matches: List<SupabaseMatch>,
    onDelete: (String) -> Unit
) {
    if (matches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active or historic lobbies in database.", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(matches) { match ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PlayArrow, 
                                    contentDescription = null, 
                                    tint = Color(0xFFE5A93B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lobby: ${match.id.uppercase()}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5C2D0A),
                                    fontSize = 14.sp
                                )
                            }

                            val (statusLabel, statusColor, contColor) = when (match.status) {
                                "waiting" -> Triple("AWAITING", Color(0xFFFFE082), Color(0xFF5D4037))
                                "playing" -> Triple("LIVE MATCH", Color(0xFFE3F2FD), Color(0xFF1565C0))
                                else -> Triple("FINISHED", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                            }

                            Text(
                                text = statusLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = contColor,
                                modifier = Modifier
                                    .background(statusColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${match.hostName} vs ${match.opponentName.ifEmpty { "Pending Opponent..." }}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        if (match.status == "finished" && !match.winner.isNullOrEmpty()) {
                            Text(
                                text = "🏆 Winner: ${match.winner}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Moves Played: ${match.movesCount} • Created: ${match.createdAt}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )

                            IconButton(
                                onClick = { onDelete(match.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Clear Match", 
                                    tint = Color(0xFFC62828).copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPanel(
    settings: List<SupabaseSystemSetting>,
    onEditClick: (SupabaseSystemSetting) -> Unit
) {
    if (settings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No system configurations loaded.", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    "GLOBAL ENGINE PARAMETERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF8B5E3C),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(settings) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.key.uppercase().replace("_", " "),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C2D0A),
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Current Value: ", 
                                    fontSize = 11.sp, 
                                    color = Color(0xFF8B5E3C)
                                )
                                Text(
                                    text = item.value, 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Black, 
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onEditClick(item) },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF5C2D0A))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Variable")
                        }
                    }
                }
            }
        }
    }
}
