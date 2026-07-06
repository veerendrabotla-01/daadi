package com.example.daadi.ui.screens.admin



import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.daadi.data.supabase.SupabaseAnnouncement
import com.example.daadi.data.supabase.SupabaseSystemSetting
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daadi.data.supabase.AdminAuditLog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminSystemConfigScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    val settings by adminViewModel.remoteConfigRepository.systemSettings.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var editingItem by remember { mutableStateOf<SupabaseSystemSetting?>(null) }
    
    val categories = listOf("SYSTEM", "MULTIPLIERS", "FEATURES", "ADS", "VERSION")
    var selectedCategory by remember { mutableStateOf("SYSTEM") }

    AdminFoundationScaffold(
        title = "Remote Variables",
        adminViewModel = adminViewModel,
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                containerColor = Color.Transparent,
                contentColor = AdminDesign.Primary,
                divider = {},
                edgePadding = AdminDesign.SpacingMedium
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isSyncing && settings.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                        items(8) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                    }
                } else {
                    val filteredSettings = remember(settings, selectedCategory) {
                        settings.filter { item ->
                            when(selectedCategory) {
                                "SYSTEM" -> item.key.contains("maintenance") || item.key.contains("broadcast") || item.key.contains("setting")
                                "MULTIPLIERS" -> item.key.contains("multiplier") || item.key.contains("rate")
                                "FEATURES" -> item.key.contains("enabled") || item.key.contains("active") || item.key.contains("toggle")
                                "ADS" -> item.key.contains("ads") || item.key.contains("monetization")
                                "VERSION" -> item.key.contains("version") || item.key.contains("update")
                                else -> true
                            }
                        }
                    }

                    if (filteredSettings.isEmpty()) {
                        AdminEmptyState(title = "No Variables Found", description = "No configuration keys match the selected category.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredSettings) { item ->
                                ConfigItemCard(item, onEdit = { editingItem = it }, onToggle = { key, newVal ->
                                    adminViewModel.remoteConfigRepository.updateSystemSetting(key, newVal)
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingItem != null) {
        var newVal by remember { mutableStateOf(editingItem!!.value) }
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Overwrite Variable", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(editingItem!!.key, style = MaterialTheme.typography.labelSmall, color = AdminDesign.Primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newVal,
                        onValueChange = { newVal = it },
                        label = { Text("New Value") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { adminViewModel.remoteConfigRepository.updateRemoteConfig(editingItem!!.key, newVal); editingItem = null },
                    shape = AdminDesign.ButtonShape
                ) {
                    Text("SAVE OVERWRITE")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun ConfigItemCard(item: SupabaseSystemSetting, onEdit: (SupabaseSystemSetting) -> Unit, onToggle: (String, String) -> Unit) {
    val isToggleable = item.value == "on" || item.value == "off" || item.value == "true" || item.value == "false"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.key.uppercase(), fontWeight = FontWeight.Black, fontSize = 12.sp, color = AdminDesign.Primary)
                Text(item.description, fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                if (!isToggleable) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(color = AdminDesign.Background, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = item.value, 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 14.sp, 
                            color = AdminDesign.OnSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            if (isToggleable) {
                val isOn = item.value == "on" || item.value == "true"
                Switch(
                    checked = isOn,
                    onCheckedChange = { checked ->
                        val newVal = if (item.value == "on" || item.value == "off") (if (checked) "on" else "off") else (if (checked) "true" else "false")
                        onToggle(item.key, newVal)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AdminDesign.Primary
                    )
                )
            } else {
                IconButton(onClick = { onEdit(item) }) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun AdminAnnouncementsScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    val announcements by adminViewModel.remoteConfigRepository.announcements.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    AdminFoundationScaffold(
        title = "System Bulletins",
        adminViewModel = adminViewModel,
        onBack = onBack,
        actions = {
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.PostAdd, contentDescription = "Add Bulletin", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        if (isSyncing && announcements.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (announcements.isEmpty()) {
            AdminEmptyState(
                title = "No Active Bulletins", 
                description = "Communication channels are clear. Create a bulletin to notify all users of updates or events."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(announcements) { ann ->
                    AnnouncementCard(ann, adminViewModel)
                }
            }
        }
    }

    if (showCreateDialog) {
        AnnouncementCreateDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, content ->
                adminViewModel.remoteConfigRepository.createAnnouncement(title, content, true)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun AnnouncementCard(ann: SupabaseAnnouncement, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = if (ann.isActive) AdminDesign.Primary.copy(alpha = 0.1f) else AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (ann.isActive) Icons.Default.Campaign else Icons.Default.SpeakerNotesOff,
                            contentDescription = null,
                            tint = if (ann.isActive) AdminDesign.Primary else AdminDesign.OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                Text(ann.title, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), color = AdminDesign.OnSurface)
                Switch(
                    checked = ann.isActive,
                    onCheckedChange = { adminViewModel.remoteConfigRepository.toggleAnnouncementStatus(ann.id) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AdminDesign.Primary
                    )
                )
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Text(ann.content, fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(ann.createdAt, fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                IconButton(onClick = { adminViewModel.remoteConfigRepository.deleteAnnouncement(ann.id) }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = AdminDesign.Error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AnnouncementCreateDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compose Global Bulletin", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                OutlinedTextField(
                    value = title, 
                    onValueChange = { title = it }, 
                    label = { Text("Bulletin Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.InputShape
                )
                OutlinedTextField(
                    value = content, 
                    onValueChange = { content = it }, 
                    label = { Text("Broadcast Message") }, 
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = AdminDesign.InputShape
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, content) },
                shape = AdminDesign.ButtonShape
            ) {
                Icon(Icons.Default.Send, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("DISPATCH BULLETIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("DISCARD") }
        }
    )
}

@Composable
fun AdminConfigHistoryScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    val auditLogs by adminViewModel.adminRepository.adminAuditLogs.collectAsStateWithLifecycle()
    val configLogs = auditLogs.filter { it.action.contains("CONFIG", ignoreCase = true) || it.target == "system_settings" }

    AdminFoundationScaffold(
        title = "Configuration Rollbacks",
        adminViewModel = adminViewModel,
        onBack = onBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            item {
                Text("VARIABLE HISTORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            }
            if (configLogs.isEmpty()) {
                item {
                    Text("No configuration changes recorded yet.", fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
                }
            }
            items(configLogs) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.History, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(entry.action, fontWeight = FontWeight.Black, fontSize = 12.sp, color = AdminDesign.Primary, modifier = Modifier.weight(1f))
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            Text(sdf.format(java.util.Date(entry.timestamp)), fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TARGET", fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(entry.target, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AdminDesign.OnSurface)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Author: ${entry.adminId}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                            TextButton(onClick = { /* Rollback */ }) {
                                Text("ROLLBACK", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.Warning)
                            }
                        }
                    }
                }
            }
        }
    }
}