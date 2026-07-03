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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminSystemConfigScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val settings by supabaseManager.systemSettings.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()
    var editingItem by remember { mutableStateOf<SupabaseSystemSetting?>(null) }
    
    val categories = listOf("SYSTEM", "MULTIPLIERS", "FEATURES", "ADS", "VERSION")
    var selectedCategory by remember { mutableStateOf("SYSTEM") }

    AdminFoundationScaffold(
        title = "Remote Variables",
        supabaseManager = supabaseManager,
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
                                    supabaseManager.updateSystemSetting(key, newVal)
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
                    onClick = { supabaseManager.updateRemoteConfig(editingItem!!.key, newVal); editingItem = null },
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
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val announcements by supabaseManager.announcements.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    AdminFoundationScaffold(
        title = "System Bulletins",
        supabaseManager = supabaseManager,
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
                    AnnouncementCard(ann, supabaseManager)
                }
            }
        }
    }

    if (showCreateDialog) {
        AnnouncementCreateDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, content ->
                supabaseManager.createAnnouncement(title, content, true)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun AnnouncementCard(ann: SupabaseAnnouncement, supabaseManager: SupabaseManager) {
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
                    onCheckedChange = { supabaseManager.toggleAnnouncementStatus(ann.id) },
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
                IconButton(onClick = { supabaseManager.deleteAnnouncement(ann.id) }) {
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

data class ConfigHistoryEntry(
    val id: String,
    val key: String,
    val oldValue: String,
    val newValue: String,
    val author: String,
    val timestamp: String
)

@Composable
fun AdminConfigHistoryScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val mockHistory = remember {
        listOf(
            ConfigHistoryEntry("H1", "maintenance_mode", "off", "on", "admin_jane", sdf.format(Date(System.currentTimeMillis() - 3600000))),
            ConfigHistoryEntry("H2", "ads_launcher", "off", "on", "admin_bob", sdf.format(Date(System.currentTimeMillis() - 7200000))),
            ConfigHistoryEntry("H3", "global_multiplier", "1.0", "2.0", "admin_jane", sdf.format(Date(System.currentTimeMillis() - 86400000))),
            ConfigHistoryEntry("H4", "daily_reward_coins", "50", "100", "mod_sam", sdf.format(Date(System.currentTimeMillis() - 172800000)))
        )
    }

    AdminFoundationScaffold(
        title = "Configuration Rollbacks",
        supabaseManager = supabaseManager,
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
            items(mockHistory) { entry ->
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
                            Text(entry.key, fontWeight = FontWeight.Black, fontSize = 12.sp, color = AdminDesign.Primary, modifier = Modifier.weight(1f))
                            Text(entry.timestamp, fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PREVIOUS", fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(entry.oldValue, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdminDesign.Error)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = AdminDesign.OnSurfaceVariant, modifier = Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("UPDATED", fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(entry.newValue, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Author: ${entry.author}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                            TextButton(onClick = { supabaseManager.updateSystemSetting(entry.key, entry.oldValue) }) {
                                Text("ROLLBACK", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.Warning)
                            }
                        }
                    }
                }
            }
        }
    }
}