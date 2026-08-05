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
    val appVersions by adminViewModel.remoteConfigRepository.appVersions.collectAsStateWithLifecycle()
    val maintenanceSchedules by adminViewModel.remoteConfigRepository.maintenanceSchedules.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf(0) }
    val subTabs = listOf("Variables", "Feature Flags", "Kill Switches", "App Versions", "Maintenance")

    var editingItem by remember { mutableStateOf<SupabaseSystemSetting?>(null) }
    var showAddVariableDialog by remember { mutableStateOf(false) }
    var showAddVersionDialog by remember { mutableStateOf(false) }
    var showAddMaintenanceDialog by remember { mutableStateOf(false) }

    val categories = listOf("SYSTEM", "MULTIPLIERS", "FEATURES", "ADS", "VERSION")
    var selectedCategory by remember { mutableStateOf("SYSTEM") }

    AdminFoundationScaffold(
        title = "Configuration Grid",
        adminViewModel = adminViewModel,
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                when (activeSubTab) {
                    0 -> showAddVariableDialog = true
                    1 -> showAddVersionDialog = true
                    2 -> showAddMaintenanceDialog = true
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add New", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = activeSubTab,
                containerColor = AdminDesign.Surface,
                contentColor = AdminDesign.Primary
            ) {
                subTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeSubTab == index,
                        onClick = { activeSubTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (activeSubTab == 0 || activeSubTab == 1 || activeSubTab == 2) {
                // VARIABLES
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
                        val filteredSettings = remember(settings, selectedCategory, activeSubTab) {
                            settings.filter { item ->
                                val expectedType = when (activeSubTab) {
                                    1 -> "feature_flag"
                                    2 -> "kill_switch"
                                    else -> "variable"
                                }
                                val actualType = item.type ?: "variable"
                                if (actualType != expectedType) return@filter false
                                
                                when (selectedCategory) {
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
                                    ConfigItemCard(
                                        item = item,
                                        onEdit = { editingItem = it },
                                        onToggle = { key, newVal ->
                                            adminViewModel.remoteConfigRepository.updateSystemSetting(key, newVal)
                                        },
                                        onDelete = { key ->
                                            adminViewModel.remoteConfigRepository.deleteSystemSetting(key)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (activeSubTab == 3) {
                // APP VERSIONS
                Box(modifier = Modifier.weight(1f)) {
                    if (appVersions.isEmpty()) {
                        AdminEmptyState(
                            title = "No Version Logs",
                            description = "No app version builds are registered. Click the '+' icon to register a build."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(appVersions) { ver ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = AdminDesign.CardShape,
                                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
                                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
                                ) {
                                    Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("v${ver.versionName}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = AdminDesign.Primary)
                                                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                                                Text("(Build: ${ver.versionCode})", fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
                                                if (ver.isMandatory) {
                                                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                                                    Badge(containerColor = AdminDesign.Error) { Text("MANDATORY", color = Color.White, fontSize = 8.sp) }
                                                }
                                            }
                                            Text("Min Supported: Build ${ver.minSupportedVersion} | Rollout: ${ver.stagedRolloutPercentage}%", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                                            if (!ver.releaseNotes.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Changelog: ${ver.releaseNotes}", fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
                                            }
                                            Text("Published At: ${ver.createdAt}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(onClick = { adminViewModel.remoteConfigRepository.deleteAppVersion(ver.versionCode) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete build", tint = AdminDesign.Error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // MAINTENANCE
                Box(modifier = Modifier.weight(1f)) {
                    if (maintenanceSchedules.isEmpty()) {
                        AdminEmptyState(
                            title = "No Scheduled Outages",
                            description = "Server cluster communication lines are clear. Scheduled maintenance slots will appear here."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(maintenanceSchedules) { schedule ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = AdminDesign.CardShape,
                                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
                                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
                                ) {
                                    Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(schedule.reason ?: "System Maintenance", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                                                Badge(containerColor = if (schedule.isActive) AdminDesign.Success else AdminDesign.OnSurfaceVariant) {
                                                    Text(if (schedule.isActive) "ACTIVE" else "DISABLED", color = Color.White, fontSize = 8.sp)
                                                }
                                            }
                                            Text("Start: ${schedule.startTime}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                                            Text("End: ${schedule.endTime}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                                            Text("Created: ${schedule.createdAt}", fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                                        }
                                        Row {
                                            Switch(
                                                checked = schedule.isActive,
                                                onCheckedChange = { adminViewModel.remoteConfigRepository.toggleMaintenanceSchedule(schedule.id) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AdminDesign.Primary)
                                            )
                                            Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                                            IconButton(onClick = { adminViewModel.remoteConfigRepository.deleteMaintenanceSchedule(schedule.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete maintenance schedule", tint = AdminDesign.Error)
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

    // DIALOGS
    if (editingItem != null) {
        var newVal by remember { mutableStateOf(editingItem!!.value) }
        val isSensitive = remember(editingItem!!.key) {
            val lower = editingItem!!.key.lowercase()
            lower.contains("key") || lower.contains("secret") || lower.contains("token") || 
            lower.contains("password") || lower.contains("credential") || lower.contains("auth") ||
            lower.contains("url")
        }
        var passwordVisible by remember { mutableStateOf(!isSensitive) }

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
                        shape = AdminDesign.InputShape,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            if (isSensitive) {
                                val image = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = "Toggle Visibility")
                                }
                            }
                        }
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

    if (showAddVariableDialog) {
        var key by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        val type = when (activeSubTab) {
            1 -> "feature_flag"
            2 -> "kill_switch"
            else -> "variable"
        }

        AlertDialog(
            onDismissRequest = { showAddVariableDialog = false },
            title = { Text("Create Config Variable", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Variable Key (e.g. max_gold_bonus)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Initial Value") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (key.isNotBlank()) {
                            adminViewModel.remoteConfigRepository.addSystemSetting(key.trim(), value.trim(), desc.trim(), type)
                            showAddVariableDialog = false
                        }
                    },
                    enabled = key.isNotBlank()
                ) {
                    Text("CREATE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVariableDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showAddVersionDialog) {
        var vCode by remember { mutableStateOf("") }
        var vName by remember { mutableStateOf("") }
        var isMandatory by remember { mutableStateOf(false) }
        var minSupported by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var stagedRollout by remember { mutableStateOf("100") }

        AlertDialog(
            onDismissRequest = { showAddVersionDialog = false },
            title = { Text("Register App Build Version", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vCode, onValueChange = { vCode = it }, label = { Text("Version Code (Int)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = vName, onValueChange = { vName = it }, label = { Text("Version Name (e.g. 1.2.0)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = minSupported, onValueChange = { minSupported = it }, label = { Text("Min Supported Build Code (Int)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Release Notes") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = stagedRollout, onValueChange = { stagedRollout = it }, label = { Text("Staged Rollout Percentage (0-100)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isMandatory, onCheckedChange = { isMandatory = it })
                        Text("Force Update Required (Mandatory)", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = vCode.toIntOrNull()
                        val min = minSupported.toIntOrNull() ?: 1
                        if (code != null && vName.isNotBlank()) {
                            adminViewModel.remoteConfigRepository.addAppVersion(code, vName, isMandatory, min, notes, stagedRollout.toIntOrNull() ?: 100)
                            showAddVersionDialog = false
                        }
                    },
                    enabled = vCode.toIntOrNull() != null && vName.isNotBlank()
                ) {
                    Text("REGISTER BUILD")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVersionDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showAddMaintenanceDialog) {
        var start by remember { mutableStateOf("") }
        var end by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddMaintenanceDialog = false },
            title = { Text("Schedule Server Outage", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start Time (YYYY-MM-DD HH:MM)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End Time (YYYY-MM-DD HH:MM)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for Outage") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (start.isNotBlank() && end.isNotBlank()) {
                            adminViewModel.remoteConfigRepository.addMaintenanceSchedule(start, end, reason)
                            showAddMaintenanceDialog = false
                        }
                    },
                    enabled = start.isNotBlank() && end.isNotBlank()
                ) {
                    Text("SCHEDULE OUTAGE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMaintenanceDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun ConfigItemCard(
    item: SupabaseSystemSetting, 
    onEdit: (SupabaseSystemSetting) -> Unit, 
    onToggle: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    val isToggleable = item.value == "on" || item.value == "off" || item.value == "true" || item.value == "false"
    val isSensitive = remember(item.key) {
        val lower = item.key.lowercase()
        lower.contains("key") || lower.contains("secret") || lower.contains("token") || 
        lower.contains("password") || lower.contains("credential") || lower.contains("auth") ||
        lower.contains("url")
    }
    var isRevealed by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = AdminDesign.Background, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = if (isSensitive && !isRevealed) "••••••••" else item.value, 
                                fontWeight = FontWeight.ExtraBold, 
                                fontSize = 14.sp, 
                                color = AdminDesign.OnSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (isSensitive) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { isRevealed = !isRevealed },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = AdminDesign.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete config", tint = AdminDesign.Error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Configuration", fontWeight = FontWeight.Bold) },
            text = { Text("Are you absolutely sure you want to delete the variable '${item.key}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(item.key); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Error)
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCEL") }
            }
        )
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
            onConfirm = { ann ->
                adminViewModel.remoteConfigRepository.createAnnouncementFull(ann)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(ann.title, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(containerColor = AdminDesign.Primary) { Text(ann.priority.uppercase(), fontSize = 8.sp, color = Color.White) }
                        Spacer(Modifier.width(4.dp))
                        if (!ann.isGlobal && ann.region != null) {
                            Badge(containerColor = AdminDesign.Secondary) { Text("REGION: ${ann.region.uppercase()}", fontSize = 8.sp, color = Color.White) }
                            Spacer(Modifier.width(4.dp))
                        }
                        if (ann.userSegment != null) {
                            Badge(containerColor = AdminDesign.Tertiary) { Text("SEGMENT: ${ann.userSegment.uppercase()}", fontSize = 8.sp, color = Color.White) }
                        }
                    }
                }
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
            if (ann.imageUrl != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Attached Image: ${ann.imageUrl}", fontSize = 10.sp, color = AdminDesign.Primary)
            }
            if (ann.deepLink != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Deep Link: ${ann.deepLink}", fontSize = 10.sp, color = AdminDesign.Secondary)
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Created: ${ann.createdAt}", fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                    if (ann.scheduledAt != null) {
                        Text("Scheduled: ${ann.scheduledAt}", fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    if (ann.expiryAt != null) {
                        Text("Expires: ${ann.expiryAt}", fontSize = 9.sp, color = AdminDesign.Error, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = { adminViewModel.remoteConfigRepository.deleteAnnouncement(ann.id) }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = AdminDesign.Error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AnnouncementCreateDialog(onDismiss: () -> Unit, onConfirm: (SupabaseAnnouncement) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("low") }
    var isGlobal by remember { mutableStateOf(true) }
    var region by remember { mutableStateOf("") }
    var userSegment by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var deepLink by remember { mutableStateOf("") }
    var scheduledAt by remember { mutableStateOf("") }
    var expiryAt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compose Global Bulletin", fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                item {
                    OutlinedTextField(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = { Text("Bulletin Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    OutlinedTextField(
                        value = content, 
                        onValueChange = { content = it }, 
                        label = { Text("Broadcast Message") }, 
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isGlobal, onCheckedChange = { isGlobal = it })
                        Text("Global Broadcast")
                    }
                }
                if (!isGlobal) {
                    item {
                        OutlinedTextField(
                            value = region, 
                            onValueChange = { region = it }, 
                            label = { Text("Region Code (e.g. NA, EU)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AdminDesign.InputShape
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = userSegment, 
                            onValueChange = { userSegment = it }, 
                            label = { Text("User Segment (e.g. VIP, NEW)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AdminDesign.InputShape
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = priority, 
                        onValueChange = { priority = it }, 
                        label = { Text("Priority (low, medium, high)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    OutlinedTextField(
                        value = imageUrl, 
                        onValueChange = { imageUrl = it }, 
                        label = { Text("Attached Image URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    OutlinedTextField(
                        value = deepLink, 
                        onValueChange = { deepLink = it }, 
                        label = { Text("Deep Link (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    OutlinedTextField(
                        value = scheduledAt, 
                        onValueChange = { scheduledAt = it }, 
                        label = { Text("Scheduled Time (YYYY-MM-DD HH:MM) (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    OutlinedTextField(
                        value = expiryAt, 
                        onValueChange = { expiryAt = it }, 
                        label = { Text("Expiry Time (YYYY-MM-DD HH:MM) (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ann = SupabaseAnnouncement(
                        id = 0,
                        title = title,
                        content = content,
                        isActive = true,
                        createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                        priority = priority,
                        isGlobal = isGlobal,
                        region = if (region.isNotBlank() && !isGlobal) region else null,
                        userSegment = if (userSegment.isNotBlank() && !isGlobal) userSegment else null,
                        imageUrl = if (imageUrl.isNotBlank()) imageUrl else null,
                        deepLink = if (deepLink.isNotBlank()) deepLink else null,
                        scheduledAt = if (scheduledAt.isNotBlank()) scheduledAt else null,
                        expiryAt = if (expiryAt.isNotBlank()) expiryAt else null
                    )
                    onConfirm(ann) 
                },
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
    val configLogs = auditLogs.filter { it.action.contains("CONFIG", ignoreCase = true) || it.target == "system_settings" || it.action.contains("SETTING") }
    var rollbackLogEntry by remember { mutableStateOf<AdminAuditLog?>(null) }

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
                            TextButton(onClick = { 
                                rollbackLogEntry = entry
                            }) {
                                Text("ROLLBACK", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.Warning)
                            }
                        }
                    }
                }
            }
        }
    }

    if (rollbackLogEntry != null) {
        val entry = rollbackLogEntry!!
        val parts = entry.target.split(" -> ")
        val key = parts.firstOrNull() ?: entry.target
        val suggestedVal = parts.getOrNull(1) ?: ""
        var revertVal by remember { mutableStateOf(suggestedVal) }
        val context = androidx.compose.ui.platform.LocalContext.current

        AlertDialog(
            onDismissRequest = { rollbackLogEntry = null },
            title = { Text("Revert Variable Setting", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Revert setting for config key:", fontSize = 12.sp)
                    Text(key, fontWeight = FontWeight.Bold, color = AdminDesign.Primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = revertVal,
                        onValueChange = { revertVal = it },
                        label = { Text("Value to Roll Back To") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.remoteConfigRepository.rollbackConfig(key, revertVal)
                        android.widget.Toast.makeText(context, "Rollback successful: $key is now '$revertVal'", android.widget.Toast.LENGTH_SHORT).show()
                        rollbackLogEntry = null
                    },
                    shape = AdminDesign.ButtonShape
                ) {
                    Text("CONFIRM ROLLBACK")
                }
            },
            dismissButton = {
                TextButton(onClick = { rollbackLogEntry = null }) { Text("CANCEL") }
            }
        )
    }
}