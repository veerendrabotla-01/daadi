package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


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
fun AdminDeviceCenterScreen(supabaseManager: SupabaseManager, onBack: () -> Unit) {
    val deviceRecords by supabaseManager.deviceRecords.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()

    AdminFoundationScaffold("Device Command", supabaseManager, onBack) { padding ->
        if (isSyncing && deviceRecords.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(6) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (deviceRecords.isEmpty()) {
            AdminEmptyState(
                title = "No Nodes Registered", 
                description = "Zero device identifiers have been captured in the current security perimeter."
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
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

                items(deviceRecords) { record ->
                    DeviceRecordCard(record)
                }
            }
        }
    }
}

@Composable
fun DeviceRecordCard(record: com.example.daadi.data.supabase.SupabaseDeviceRecord) {
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
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (record.isBlocked) {
                    TextButton(onClick = { /* Unblock */ }) { 
                        Text("RELEASE QUARANTINE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminDesign.Primary) 
                    }
                } else {
                    Button(
                        onClick = { /* Block */ }, 
                        colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.OnSurfaceVariant),
                        shape = AdminDesign.ButtonShape
                    ) { 
                        Text("TERMINATE ACCESS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
