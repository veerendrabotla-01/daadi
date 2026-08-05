package com.example.daadi.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daadi.data.supabase.SupabaseDatabaseBackup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDataExportScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Bulk Data Exports", "Database Backups")

    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf<String?>(null) }
    
    val modules = listOf(
        "Users & Profiles" to "Includes all PII, balances, and stats.",
        "Transactions & Economy" to "IAP history and currency flow.",
        "Match History" to "Game logs, moves, and outcomes.",
        "Audit Logs" to "Admin actions and security events.",
        "LiveOps Events" to "Event configurations and participant data."
    )
    
    var selectedModules by remember { mutableStateOf(setOf<String>()) }
    var exportFormat by remember { mutableStateOf("CSV") }

    // Backup state
    val backups by adminViewModel.analyticsRepository.databaseBackups.collectAsStateWithLifecycle()
    var isBackingUp by remember { mutableStateOf(false) }
    var backupSuccess by remember { mutableStateOf<String?>(null) }
    var restoreTargetId by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    AdminFoundationScaffold(
        title = "Exports & Database Backups",
        adminViewModel = adminViewModel,
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = AdminDesign.Surface,
                contentColor = AdminDesign.Primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (activeTab == 0) {
                // BULK DATA EXPORTS
                Column(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                    Text("BULK DATA EXPORT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    Text("Select modules to export. All exports are logged in the audit trail.", fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
                    
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                        shape = AdminDesign.CardShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                    ) {
                        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            modules.forEach { (module, desc) ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Checkbox(
                                        checked = selectedModules.contains(module),
                                        onCheckedChange = { checked ->
                                            selectedModules = if (checked) selectedModules + module else selectedModules - module
                                        }
                                    )
                                    Column {
                                        Text(module, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(desc, fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
                    
                    Text("EXPORT SETTINGS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)) {
                        listOf("CSV", "JSON", "Parquet").forEach { format ->
                            FilterChip(
                                selected = exportFormat == format,
                                onClick = { exportFormat = format },
                                label = { Text(format) },
                                leadingIcon = if (exportFormat == format) { { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) } } else null
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    AnimatedVisibility(visible = exportSuccess != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Primary.copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth().padding(bottom = AdminDesign.SpacingMedium)) {
                            Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AdminDesign.Primary)
                                Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                                Text(exportSuccess ?: "", color = AdminDesign.Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (selectedModules.isNotEmpty()) {
                                isExporting = true
                                exportSuccess = null
                                adminViewModel.adminRepository.requestDataExport(selectedModules.toList(), exportFormat) { success, msg ->
                                    isExporting = false
                                    exportSuccess = msg
                                }
                            }
                        },
                        enabled = selectedModules.isNotEmpty() && !isExporting,
                        shape = AdminDesign.ButtonShape,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GENERATE EXPORT", fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else {
                // DATABASE BACKUPS & SANITY CHECKS
                Column(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DATABASE DISASTER RECOVERY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Trigger snapshots and revert table cluster indices.", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                isBackingUp = true
                                backupSuccess = null
                                adminViewModel.analyticsRepository.triggerDatabaseBackup { success, msg ->
                                    isBackingUp = false
                                    backupSuccess = msg
                                }
                            },
                            enabled = !isBackingUp,
                            shape = AdminDesign.ButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary)
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SNAPSHOT", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))

                    AnimatedVisibility(visible = backupSuccess != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Success.copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth().padding(bottom = AdminDesign.SpacingMedium)) {
                            Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AdminDesign.Success)
                                Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                                Text(backupSuccess ?: "", color = AdminDesign.Success, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(backups.reversed()) { backup ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                                shape = AdminDesign.CardShape,
                                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                            ) {
                                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Storage, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = backup.filename, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 12.sp, 
                                            modifier = Modifier.weight(1f),
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Badge(containerColor = if (backup.status == "completed") AdminDesign.Success else AdminDesign.Warning) {
                                            Text(backup.status.uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("Size: ${formatBytes(backup.sizeBytes)}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                                            Text("Type: ${backup.type.uppercase()} | Saved: ${backup.createdAt}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                                        }
                                        TextButton(onClick = { restoreTargetId = backup.id }) {
                                            Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(14.dp), tint = AdminDesign.Warning)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("RESTORE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.Warning)
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

    if (restoreTargetId != null) {
        val targetId = restoreTargetId!!
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { if (!isRestoring) restoreTargetId = null },
            title = { Text("Restore System State", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("Are you absolutely sure you want to restore the live database to backup '$targetId'?")
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    Text("This will rollback all user profiles, matches, transactions, and settings to the snapshot state. Connection sessions will temporarily disconnect.", color = AdminDesign.Error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (isRestoring) {
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Rolling back database cluster nodes...", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRestoring = true
                        adminViewModel.analyticsRepository.restoreDatabaseBackup(targetId) { success, msg ->
                            isRestoring = false
                            restoreTargetId = null
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Error),
                    enabled = !isRestoring
                ) {
                    Text("RESTORE BACKUP")
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTargetId = null }, enabled = !isRestoring) { Text("CANCEL") }
            }
        )
    }
}

fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.1f KB", kb)
    }
}
