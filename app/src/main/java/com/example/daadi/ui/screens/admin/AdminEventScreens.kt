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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminEventScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val events by supabaseManager.gameEvents.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        supabaseManager.fetchGameEvents()
    }

    AdminFoundationScaffold(
        title = "Live Operations",
        supabaseManager = supabaseManager,
        onBack = onBack,
        actions = {
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.EventAvailable, contentDescription = "New Event", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        if (isSyncing && events.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (events.isEmpty()) {
            AdminEmptyState(
                title = "No Active Events", 
                description = "Game world is currently static. Launch a bonus event to increase player engagement."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item {
                    Text("BONUS & MULTIPLIER EVENTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
                items(events) { event ->
                    EventItem(event) { isOn -> supabaseManager.toggleGameEvent(event.id, isOn) }
                }
            }
        }

        if (showCreateDialog) {
            CreateEventDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { title, type, mult ->
                    supabaseManager.createGameEvent(
                        title, 
                        type, 
                        mult, 
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(java.util.Date()),
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(java.util.Date(System.currentTimeMillis() + 86400000 * 7)) // 1 week
                    )
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun CreateEventDialog(onDismiss: () -> Unit, onConfirm: (String, String, Double) -> Unit) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("double_xp") }
    var multiplier by remember { mutableStateOf("2.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Initiate Bonus Event", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Campaign Name") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Event Type ID") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
                OutlinedTextField(value = multiplier, onValueChange = { multiplier = it }, label = { Text("Benefit Multiplier") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, type, multiplier.toDoubleOrNull() ?: 1.0) },
                shape = AdminDesign.ButtonShape
            ) {
                Text("LAUNCH CAMPAIGN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("DISCARD") }
        }
    )
}

@Composable
fun EventItem(event: SupabaseGameEvent, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = when(event.type) {
                    "double_xp" -> AdminDesign.Primary.copy(alpha = 0.1f)
                    "coin_bonus" -> AdminDesign.Secondary.copy(alpha = 0.1f)
                    else -> AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when(event.type) {
                            "double_xp" -> Icons.Default.TrendingUp
                            "coin_bonus" -> Icons.Default.MonetizationOn
                            else -> Icons.Default.RocketLaunch
                        }, 
                        contentDescription = null, 
                        tint = when(event.type) {
                            "double_xp" -> AdminDesign.Primary
                            "coin_bonus" -> AdminDesign.Secondary
                            else -> AdminDesign.OnSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface, fontSize = 15.sp)
                Text("${event.multiplier}x PAYOUT MULTIPLIER", fontSize = 10.sp, color = AdminDesign.Primary, fontWeight = FontWeight.Black)
                Text("EXPIRY: ${event.endTime?.take(16) ?: "PERMANENT"}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
            }
            
            Switch(
                checked = event.isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AdminDesign.Primary
                )
            )
        }
    }
}
