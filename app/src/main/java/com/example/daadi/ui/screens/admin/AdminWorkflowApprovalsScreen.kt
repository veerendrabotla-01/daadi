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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daadi.data.supabase.SupabaseApprovalRequest
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminWorkflowApprovalsScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    val requests by adminViewModel.adminRepository.approvalRequests.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        adminViewModel.adminRepository.fetchWorkflowApprovals()
    }
    
    val pendingRequests = requests.filter { it.status == "pending" }

    AdminFoundationScaffold(
        title = "Workflow Approvals",
        adminViewModel = adminViewModel,
        onBack = onBack
    ) { padding ->
        if (pendingRequests.isEmpty()) {
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
                    Text("PENDING REQUESTS (${pendingRequests.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                }
                items(pendingRequests) { req ->
                    ApprovalRequestCard(
                        request = req,
                        onApprove = { adminViewModel.adminRepository.approveRequest(req.id) },
                        onReject = { adminViewModel.adminRepository.rejectRequest(req.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ApprovalRequestCard(request: SupabaseApprovalRequest, onApprove: () -> Unit, onReject: () -> Unit) {
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
            HorizontalDivider(color = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f))
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
