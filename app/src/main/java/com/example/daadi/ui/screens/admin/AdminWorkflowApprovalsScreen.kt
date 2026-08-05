package com.example.daadi.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pending Queue", "Approval History")

    LaunchedEffect(Unit) {
        adminViewModel.adminRepository.fetchWorkflowApprovals()
    }
    
    val pendingRequests = requests.filter { it.status == "pending" }
    val historyRequests = requests.filter { it.status != "pending" }

    AdminFoundationScaffold(
        title = "Workflow Approvals",
        adminViewModel = adminViewModel,
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AdminDesign.Surface,
                contentColor = AdminDesign.Primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTab == 0) {
                if (pendingRequests.isEmpty()) {
                    AdminEmptyState(title = "No Pending Approvals", description = "All maker-checker workflows are cleared.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
                    ) {
                        item {
                            Text("PENDING QUEUE (${pendingRequests.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                        }
                        items(pendingRequests) { req ->
                            ApprovalRequestCard(
                                request = req,
                                adminViewModel = adminViewModel
                            )
                        }
                    }
                }
            } else {
                if (historyRequests.isEmpty()) {
                    AdminEmptyState(title = "History Empty", description = "Previous approval/rejection logs will appear here.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
                    ) {
                        items(historyRequests) { req ->
                            HistoryRequestCard(request = req)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApprovalRequestCard(request: SupabaseApprovalRequest, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    var showDetail by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        onClick = { showDetail = true }
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Badge(
                    containerColor = when(request.severity.lowercase()) {
                        "high" -> AdminDesign.Error
                        "medium" -> AdminDesign.Warning
                        else -> AdminDesign.Primary
                    }
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
            
            Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showRejectDialog = true },
                    shape = AdminDesign.ButtonShape,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminDesign.Error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REJECT")
                }
                Button(
                    onClick = { adminViewModel.adminRepository.approveRequest(request.id) },
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

    if (showDetail) {
        ApprovalDetailDialog(request = request, onDismiss = { showDetail = false })
    }

    if (showRejectDialog) {
        RejectionReasonDialog(
            onDismiss = { showRejectDialog = false },
            onConfirm = { reason ->
                adminViewModel.adminRepository.rejectRequest(request.id, reason)
                showRejectDialog = false
            }
        )
    }
}

@Composable
fun HistoryRequestCard(request: SupabaseApprovalRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(
                    containerColor = if (request.status == "approved") AdminDesign.Secondary else AdminDesign.Error
                ) {
                    Text(request.status.uppercase(), fontSize = 8.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(request.type, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(request.timestamp, fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(request.description, fontSize = 12.sp)
            if (request.status == "rejected" && !request.rejectionReason.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = AdminDesign.Error.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reason: ${request.rejectionReason}",
                        modifier = Modifier.padding(8.dp),
                        fontSize = 11.sp,
                        color = AdminDesign.Error,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun ApprovalDetailDialog(request: SupabaseApprovalRequest, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Details", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(request.description, style = MaterialTheme.typography.bodyMedium)
                
                if (request.oldValue != null || request.newValue != null) {
                    Text("DATA CHANGE DIFF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    
                    Surface(
                        color = AdminDesign.Background,
                        shape = AdminDesign.CardShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row {
                                Text("OLD:", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = AdminDesign.Error)
                                Text(request.oldValue ?: "null", fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Text("NEW:", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = AdminDesign.Secondary)
                                Text(request.newValue ?: "null", fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }
                }
                
                MetadataRow("Requester", request.requester)
                MetadataRow("Severity", request.severity)
                MetadataRow("System ID", request.id)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
fun RejectionReasonDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rejection Reason", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Provide a mandatory reason for declining this request. The requester will be notified.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    placeholder = { Text("Enter reason...") },
                    shape = AdminDesign.InputShape
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (reason.isNotBlank()) onConfirm(reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Error),
                shape = AdminDesign.ButtonShape
            ) {
                Text("REJECT REQUEST")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
