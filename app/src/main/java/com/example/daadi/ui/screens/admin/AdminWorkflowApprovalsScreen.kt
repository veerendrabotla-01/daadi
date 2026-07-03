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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseManager
import java.text.SimpleDateFormat
import java.util.*

data class ApprovalRequest(
    val id: String,
    val requester: String,
    val type: String,
    val description: String,
    val timestamp: String,
    val severity: String // "High", "Medium", "Low"
)

@Composable
fun AdminWorkflowApprovalsScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val mockRequests = remember {
        mutableStateListOf(
            ApprovalRequest("REQ-001", "admin_jane", "Remote Config", "Enable Global 2x XP Multiplier", sdf.format(Date(System.currentTimeMillis() - 3600000)), "High"),
            ApprovalRequest("REQ-002", "mod_bob", "Mass Ban", "Ban 150 identified bot accounts", sdf.format(Date(System.currentTimeMillis() - 7200000)), "High"),
            ApprovalRequest("REQ-003", "admin_jane", "Economy", "Gift 1000 Coins to top 10 players", sdf.format(Date()), "Medium")
        )
    }
    
    AdminFoundationScaffold(
        title = "Workflow Approvals",
        supabaseManager = supabaseManager,
        onBack = onBack
    ) { padding ->
        if (mockRequests.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                AdminEmptyState(title = "No Pending Approvals", description = "All maker-checker workflows are cleared.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                item {
                    Text("PENDING REQUESTS (${mockRequests.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                }
                items(mockRequests) { req ->
                    ApprovalRequestCard(
                        request = req,
                        onApprove = { mockRequests.remove(req) },
                        onReject = { mockRequests.remove(req) }
                    )
                }
            }
        }
    }
}

@Composable
fun ApprovalRequestCard(request: ApprovalRequest, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Badge(
                    containerColor = if (request.severity == "High") AdminDesign.Error else AdminDesign.Warning
                ) {
                    Text(request.severity.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(request.type, fontWeight = FontWeight.Black, fontSize = 12.sp, color = AdminDesign.Primary)
                Spacer(modifier = Modifier.weight(1f))
                Text(request.timestamp, fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(request.description, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AdminDesign.OnSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Requested by: ${request.requester}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Divider(color = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onReject,
                    shape = AdminDesign.ButtonShape,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminDesign.Error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REJECT")
                }
                Button(
                    onClick = onApprove,
                    shape = AdminDesign.ButtonShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("APPROVE")
                }
            }
        }
    }
}
