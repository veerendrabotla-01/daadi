package com.example.daadi.ui.screens.admin



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

@Composable
fun AdminDeviceCenterScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit = {},
    onBack: () -> Unit
) {
    val deviceRecords by adminViewModel.analyticsRepository.deviceRecords.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    val users by adminViewModel.userRepository.users.collectAsStateWithLifecycle()
    val filterDeviceId = adminViewModel.filterDeviceId.value ?: ""

    val filteredRecords = remember(deviceRecords, filterDeviceId) {
        if (filterDeviceId.isNotEmpty()) {
            deviceRecords.filter { it.deviceId.equals(filterDeviceId, ignoreCase = true) }
        } else {
            deviceRecords
        }
    }

    AdminFoundationScaffold("Device Command", supabaseManager, onBack) { padding ->
        if (isSyncing && filteredRecords.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(6) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (filteredRecords.isEmpty()) {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (filterDeviceId.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(AdminDesign.SpacingMedium),
                        color = AdminDesign.Primary.copy(alpha = 0.1f),
                        shape = AdminDesign.CardShape
                    ) {
                        Row(
                            modifier = Modifier.padding(AdminDesign.SpacingMedium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("No match for Device ID: ${filterDeviceId.take(16)}...", fontWeight = FontWeight.Bold, color = AdminDesign.Primary, fontSize = 12.sp)
                            TextButton(onClick = { adminViewModel.filterDeviceId.value = "" }) {
                                Text("Clear Filter", fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
                AdminEmptyState(
                    title = "No Nodes Registered", 
                    description = "Zero device identifiers have been captured in the current security perimeter."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                if (filterDeviceId.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            color = AdminDesign.Primary.copy(alpha = 0.1f),
                            shape = AdminDesign.CardShape
                        ) {
                            Row(
                                modifier = Modifier.padding(AdminDesign.SpacingMedium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Filtering by Device ID: ${filterDeviceId.take(16)}...", fontWeight = FontWeight.Bold, color = AdminDesign.Primary, fontSize = 12.sp)
                                TextButton(onClick = { adminViewModel.filterDeviceId.value = "" }) {
                                    Text("Clear Filter", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                        MetricMiniCard("REGISTERED", deviceRecords.size.toString(), AdminDesign.Primary, Modifier.weight(1f))
                        MetricMiniCard("SUSPICIOUS", deviceRecords.count { it.isRooted || it.isEmulator }.toString(), AdminDesign.Error, Modifier.weight(1f))
                        MetricMiniCard("QUARANTINED", deviceRecords.count { it.isBlocked }.toString(), AdminDesign.OnSurfaceVariant, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                }

                item {
                    Text("HARDWARE IDENTIFIER MATRIX", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }

                items(filteredRecords) { record ->
                    DeviceRecordCard(
                        record = record,
                        users = users,
                        onUserClick = onUserClick,
                        onQuarantineToggle = { isQuarantined ->
                            adminViewModel.analyticsRepository.setDeviceQuarantine(record.deviceId, isQuarantined)
                        },
                        onTerminate = {
                            adminViewModel.analyticsRepository.terminateDeviceAccess(record.deviceId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceRecordCard(
    record: com.example.daadi.data.supabase.SupabaseDeviceRecord,
    users: List<com.example.daadi.data.supabase.SupabaseUser>,
    onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit,
    onQuarantineToggle: (Boolean) -> Unit,
    onTerminate: () -> Unit
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
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (record.isBlocked) AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f) else AdminDesign.Primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                record.isEmulator -> Icons.Default.Computer
                                else -> Icons.Default.Smartphone
                            },
                            contentDescription = null,
                            tint = if (record.isBlocked) AdminDesign.OnSurfaceVariant else AdminDesign.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.deviceId.take(16) + "...", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = AdminDesign.OnSurface)
                    Text(
                        text = "LAST_POLL: ${record.lastSeen}", 
                        fontSize = 10.sp, 
                        color = AdminDesign.OnSurfaceVariant,
                        fontWeight = FontWeight.Black
                    )
                }
                if (record.isBlocked) {
                    StatusBadge("QUARANTINED")
                }
            }
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                RiskBadge("ROOT_DETECTION", record.isRooted)
                RiskBadge("VPN_PROXY", record.isVpn)
                RiskBadge("EMU_HEURISTIC", record.isEmulator)
            }
            
            val associatedUsers = remember(users, record.deviceId) {
                users.filter { it.deviceId.equals(record.deviceId, ignoreCase = true) }
            }
            if (associatedUsers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                Text("ASSOCIATED PLAYERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(associatedUsers) { user ->
                        Card(
                            onClick = { onUserClick(user) },
                            colors = CardDefaults.cardColors(
                                containerColor = AdminDesign.Primary.copy(alpha = 0.08f),
                                contentColor = AdminDesign.Primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(user.username, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.End, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                if (record.isBlocked) {
                    TextButton(onClick = { 
                        onQuarantineToggle(false)
                        android.widget.Toast.makeText(context, "Quarantine lifted for device.", android.widget.Toast.LENGTH_SHORT).show()
                    }) { 
                        Text("RELEASE QUARANTINE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminDesign.Primary) 
                    }
                } else {
                    TextButton(onClick = { 
                        onQuarantineToggle(true)
                        android.widget.Toast.makeText(context, "Device placed under quarantine.", android.widget.Toast.LENGTH_SHORT).show()
                    }) { 
                        Text("QUARANTINE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminDesign.Warning) 
                    }
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Button(
                        onClick = { 
                            onTerminate()
                            android.widget.Toast.makeText(context, "Hardware access terminated.", android.widget.Toast.LENGTH_SHORT).show()
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Error),
                        shape = AdminDesign.ButtonShape
                    ) { 
                        Text("TERMINATE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RiskBadge(label: String, active: Boolean) {
    Surface(
        color = if (active) AdminDesign.Error.copy(alpha = 0.1f) else AdminDesign.Success.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label, 
            fontSize = 9.sp, 
            fontWeight = FontWeight.Black,
            color = if (active) AdminDesign.Error else AdminDesign.Success,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
