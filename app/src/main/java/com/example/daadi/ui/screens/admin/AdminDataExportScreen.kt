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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDataExportScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
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

    AdminFoundationScaffold(
        title = "Data Exports & Archives",
        adminViewModel = adminViewModel,
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
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
    }
}
