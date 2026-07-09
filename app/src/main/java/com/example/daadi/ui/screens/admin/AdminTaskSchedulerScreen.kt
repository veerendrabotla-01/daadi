package com.example.daadi.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daadi.data.supabase.SupabaseScheduledTask

@Composable
fun AdminTaskSchedulerScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    val tasks by adminViewModel.adminRepository.scheduledTasks.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        adminViewModel.adminRepository.fetchScheduledTasks()
    }

    var showAddTaskDialog by remember { mutableStateOf(false) }

    AdminFoundationScaffold(
        title = "Task Scheduler",
        adminViewModel = adminViewModel,
        onBack = onBack,
        actions = {
            IconButton(onClick = { showAddTaskDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                item {
                    Text("CRON JOBS & AUTOMATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                }
                items(tasks) { task ->
                    TaskSchedulerCard(
                        task = task,
                        onToggle = { isEnabled ->
                            adminViewModel.adminRepository.toggleScheduledTask(task.id, isEnabled)
                        },
                        onRunNow = { 
                            adminViewModel.adminRepository.triggerScheduledTask(task.id)
                        }
                    )
                }
            }

            if (showAddTaskDialog) {
                var name by remember { mutableStateOf("") }
                var schedule by remember { mutableStateOf("*/5 * * * *") }
                
                AlertDialog(
                    onDismissRequest = { showAddTaskDialog = false },
                    title = { Text("Create Scheduled Task", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Task Name") },
                                placeholder = { Text("e.g. Purge Temporary Data") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = schedule,
                                onValueChange = { schedule = it },
                                label = { Text("Cron Schedule") },
                                placeholder = { Text("e.g. */10 * * * *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    adminViewModel.adminRepository.createScheduledTask(name, schedule)
                                    showAddTaskDialog = false
                                }
                            }
                        ) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
fun TaskSchedulerCard(task: SupabaseScheduledTask, onToggle: (Boolean) -> Unit, onRunNow: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(task.name, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = task.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = AdminDesign.Primary)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Schedule: ${task.schedule}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text("Next: ${task.nextRun}", fontSize = 11.sp, color = AdminDesign.Primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (task.lastStatus) {
                        "Success" -> Color(0xFF4CAF50)
                        "Failed" -> AdminDesign.Error
                        else -> AdminDesign.OnSurfaceVariant
                    }
                    Box(modifier = Modifier.size(8.dp).background(statusColor, androidx.compose.foundation.shape.CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Last run: ${task.lastStatus}", fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Bold)
                }
                
                TextButton(onClick = onRunNow, enabled = task.isEnabled) {
                    Text("RUN NOW", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
