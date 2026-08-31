package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseLiveOpsEvent
import com.example.daadi.data.supabase.SupabaseAnnouncement

import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminLiveOpsCenter(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val events by adminViewModel.liveOpsRepository.liveOpsEvents.collectAsStateWithLifecycle()
    val announcements by adminViewModel.liveOpsRepository.announcements.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateAnnouncement by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminViewModel.liveOpsRepository.fetchLiveOpsEvents()
        adminViewModel.liveOpsRepository.fetchAnnouncements()
    }

    AdminFoundationScaffold(
        title = "LiveOps Command",
        adminViewModel = adminViewModel,
        onBack = onBack,
        actions = {
            if (selectedTab == 0) {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Schedule Event", tint = AdminDesign.Primary)
                }
            } else {
                IconButton(onClick = { showCreateAnnouncement = true }) {
                    Icon(Icons.Default.Campaign, contentDescription = "New Announcement", tint = AdminDesign.Primary)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AdminDesign.Primary,
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("EVENTS", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("ANNOUNCEMENTS", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isSyncing && events.isEmpty() && announcements.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                        items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                    }
                } else {
                    when (selectedTab) {
                        0 -> LiveOpsEventsList(events, showCreateDialog, { showCreateDialog = true }, adminViewModel)
                        1 -> GlobalAnnouncementsList(announcements, { adminViewModel.adminRepository.deleteAnnouncement(it) })
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateLiveOpsDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { title, desc, type, xp, coin ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    val start = sdf.format(Date())
                    val end = sdf.format(Date(System.currentTimeMillis() + 172800000)) // 48h from now
                    adminViewModel.liveOpsRepository.createLiveOpsEvent(
                        title = title,
                        description = desc,
                        type = type,
                        xpMultiplier = xp,
                        coinMultiplier = coin,
                        startTime = start,
                        endTime = end,
                        isActive = true
                    )
                    showCreateDialog = false
                }
            )
        }

        if (showCreateAnnouncement) {
            CreateAnnouncementDialog(
                onDismiss = { showCreateAnnouncement = false },
                onConfirm = { title, content, priority ->
                    adminViewModel.adminRepository.createAnnouncement(
                        SupabaseAnnouncement(
                            id = (1000..9999).random(),
                            title = title,
                            content = content,
                            priority = priority,
                            isActive = true,
                            createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                        )
                    )
                    showCreateAnnouncement = false
                }
            )
        }
    }
}

@Composable
fun LiveOpsEventsList(
    events: List<SupabaseLiveOpsEvent>,
    showCreateDialog: Boolean,
    onShowCreate: () -> Unit,
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel
) {
    if (events.isEmpty()) {
        AdminEmptyState(
            title = "Ops Silence", 
            description = "No LiveOps events are currently scheduled or active. Game logic is operating on baseline parameters.",
            actionButton = {
                Button(
                    onClick = onShowCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary),
                    shape = AdminDesign.ButtonShape
                ) {
                    Text("SCHEDULE NEW EVENT")
                }
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            item {
                LiveOpsStatusBanner(events)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            }
            item {
                Text("ACTIVE OPERATIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            }
            items(events) { event ->
                LiveOpsEventCard(
                    event = event,
                    onToggleActive = { active ->
                        adminViewModel.liveOpsRepository.toggleLiveOpsEventActive(event.id, active)
                    },
                    onDelete = {
                        adminViewModel.liveOpsRepository.deleteLiveOpsEvent(event.id)
                    }
                )
            }
        }
    }
}

@Composable
fun GlobalAnnouncementsList(
    announcements: List<com.example.daadi.data.supabase.SupabaseAnnouncement>,
    onDelete: (Int) -> Unit
) {
    if (announcements.isEmpty()) {
        AdminEmptyState(
            title = "No Announcements",
            description = "The global announcement board is empty. Send updates to all active players."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
        ) {
            items(announcements) { ann ->
                AdminCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, null, tint = AdminDesign.Primary)
                            Spacer(Modifier.width(12.dp))
                            Text(ann.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDelete(ann.id) }) {
                                Icon(Icons.Default.Delete, null, tint = AdminDesign.Error)
                            }
                        }
                        Text(ann.content, style = AdminDesign.BodyStyle)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Badge(containerColor = AdminDesign.Secondary) { Text(ann.priority.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                            Badge(containerColor = if (ann.isActive) Color(0xFF2E7D32) else AdminDesign.OnSurfaceVariant) { 
                                Text(if (ann.isActive) "ACTIVE" else "INACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold) 
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveOpsStatusBanner(events: List<SupabaseLiveOpsEvent>) {
    val activeCount = events.count { it.isActive }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Primary),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingLarge), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column {
                Text("GLOBAL LIVE OPERATIONS", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("$activeCount ACTIVE CLUSTERS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LiveOpsEventCard(
    event: SupabaseLiveOpsEvent,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(event.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AdminDesign.OnSurface)
                    Text("TYPE: ${event.type.uppercase()}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = AdminDesign.Secondary)
                }
                
                Switch(
                    checked = event.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AdminDesign.Primary
                    )
                )

                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete event", tint = AdminDesign.Error)
                }
            }
            Text(
                text = event.description ?: "No operation description provided.", 
                fontSize = 12.sp, 
                color = AdminDesign.OnSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall), color = AdminDesign.OnSurface.copy(alpha = 0.05f))
            
            Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingLarge)) {
                MultiplerIndicator("XP GAIN", event.xpMultiplier, AdminDesign.Primary)
                MultiplerIndicator("COIN DROP", event.coinMultiplier, AdminDesign.Secondary)
            }
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AdminDesign.Background,
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(modifier = Modifier.padding(AdminDesign.SpacingSmall), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Text(
                        text = "WINDOW: ${event.startTime.take(16)} » ${event.endTime.take(16)}", 
                        fontSize = 9.sp, 
                        color = AdminDesign.OnSurfaceVariant,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiplerIndicator(label: String, multiplier: Double, color: Color) {
    Column {
        Text(label, fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Black)
        Text("x${multiplier}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun CreateLiveOpsDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, Double, Double) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("xp_weekend") }
    var xpMultiplier by remember { mutableStateOf("2.0") }
    var coinMultiplier by remember { mutableStateOf("1.5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule LiveOps Cluster", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                AdminTextField(value = title, onValueChange = { title = it }, label = "Event Title")
                AdminTextField(value = desc, onValueChange = { desc = it }, label = "Campaign Description", minLines = 2)
                
                Text("EVENT TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("xp_weekend", "coin_rush", "seasonal").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.replace("_", " ").uppercase(), fontSize = 10.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    AdminTextField(modifier = Modifier.weight(1f), value = xpMultiplier, onValueChange = { xpMultiplier = it }, label = "XP Mult")
                    AdminTextField(modifier = Modifier.weight(1f), value = coinMultiplier, onValueChange = { coinMultiplier = it }, label = "Coin Mult")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(
                        title, 
                        desc, 
                        type, 
                        xpMultiplier.toDoubleOrNull() ?: 1.0, 
                        coinMultiplier.toDoubleOrNull() ?: 1.0
                    ) 
                },
                shape = AdminDesign.ButtonShape
            ) {
                Text("LAUNCH CLUSTER")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ABORT") }
        }
    )
}

@Composable
fun CreateAnnouncementDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("low") }

    AdminDialog(title = "New Global Announcement", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminTextField(value = title, onValueChange = { title = it }, label = "Title")
            AdminTextField(value = content, onValueChange = { content = it }, label = "Message Content", minLines = 3)
            
            Text("PRIORITY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("low", "medium", "high", "critical").forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.uppercase(), fontSize = 10.sp) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(title, content, priority) },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary)
                ) {
                    Text("Broadcast")
                }
            }
        }
    }
}
