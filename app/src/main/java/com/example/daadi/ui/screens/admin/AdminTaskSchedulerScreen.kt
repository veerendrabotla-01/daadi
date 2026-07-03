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
import com.example.daadi.data.supabase.SupabaseManager

data class ScheduledTask(
    val id: String,
    val name: String,
    val schedule: String, // e.g. "0 0 * * 0"
    val nextRun: String,
    val isEnabled: Boolean,
    val lastStatus: String
)

@Composable
fun AdminTaskSchedulerScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val tasks = remember {
        mutableStateListOf(
            ScheduledTask("T1", "Weekly Leaderboard Reset", "0 0 * * 0", "Next Sunday, 00:00 UTC", true, "Success"),
            ScheduledTask("T2", "Daily Daily-Reward Distribution", "0 0 * * *", "Tomorrow, 00:00 UTC", true, "Success"),
            ScheduledTask("T3", "Monthly Inactive User Pruning", "0 0 1 * *", "Aug 1st, 00:00 UTC", false, "Skipped"),
            ScheduledTask("T4", "Hourly DB Snapshot", "0 * * * *", "In 15 mins", true, "Failed")
        )
    }

    AdminFoundationScaffold(
        title = "Task Scheduler",
        supabaseManager = supabaseManager,
        onBack = onBack,
        actions = {
            IconButton(onClick = { /* Handle Add */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
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
                        val idx = tasks.indexOf(task)
                        if(idx != -1) {
                            tasks[idx] = task.copy(isEnabled = isEnabled)
                        }
                    },
                    onRunNow = { /* Run task */ }
                )
            }
        }
    }
}

@Composable
fun TaskSchedulerCard(task: ScheduledTask, onToggle: (Boolean) -> Unit, onRunNow: () -> Unit) {
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
