package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseSeasonPass

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminSeasonPassManager(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val seasonPasses by adminViewModel.liveOpsRepository.seasonPasses.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminViewModel.liveOpsRepository.fetchSeasonPasses()
    }

    AdminFoundationScaffold(
        title = "Season Pass Matrix",
        adminViewModel = adminViewModel,
        onBack = onBack,
        actions = {
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.AddBox, contentDescription = "Create Season", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        if (isSyncing && seasonPasses.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(3) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (seasonPasses.isEmpty()) {
            AdminEmptyState(
                title = "No Seasons Found", 
                description = "Progression tracks are empty. Initialize a new Season Pass to start player journey cycles.",
                actionButton = {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary),
                        shape = AdminDesign.ButtonShape
                    ) {
                        Text("LAUNCH BATTLE PASS")
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                item {
                    Text("SEASONAL PROGRESSION TRACKS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
                items(seasonPasses) { pass ->
                    SeasonPassCard(
                        pass = pass,
                        onToggleActive = { active ->
                            adminViewModel.liveOpsRepository.toggleSeasonPassActive(pass.id, active)
                        },
                        onDelete = {
                            adminViewModel.liveOpsRepository.deleteSeasonPass(pass.id)
                        }
                    )
                }
            }
        }

        if (showCreateDialog) {
            CreateSeasonPassDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { title ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    val start = sdf.format(Date())
                    val end = sdf.format(Date(System.currentTimeMillis() + 2592000000)) // 30 days
                    adminViewModel.liveOpsRepository.createSeasonPass(
                        title = title,
                        startTime = start,
                        endTime = end,
                        isActive = true
                    )
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun SeasonPassCard(
    pass: SupabaseSeasonPass,
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
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = AdminDesign.Primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(pass.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AdminDesign.OnSurface)
                    Text(
                        text = "WINDOW: ${pass.startTime?.take(10)} TO ${pass.endTime?.take(10)}", 
                        fontSize = 10.sp, 
                        color = AdminDesign.OnSurfaceVariant,
                        fontWeight = FontWeight.Black
                    )
                }
                
                Switch(
                    checked = pass.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AdminDesign.Primary
                    )
                )

                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete season pass", tint = AdminDesign.Error)
                }
            }
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                Button(
                    onClick = { /* View Tiers */ }, 
                    modifier = Modifier.weight(1f),
                    shape = AdminDesign.ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Background, contentColor = AdminDesign.Primary)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Text("TIER MATRIX", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { /* View Stats */ }, 
                    modifier = Modifier.weight(1f),
                    shape = AdminDesign.ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary)
                ) {
                    Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Text("ANALYTICS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CreateSeasonPassDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deploy Battle Pass Track", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Season Pass Title (e.g. Genesis Track)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.InputShape
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title) },
                shape = AdminDesign.ButtonShape
            ) {
                Text("DEPLOY EXPEDITION")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ABORT") }
        }
    )
}
