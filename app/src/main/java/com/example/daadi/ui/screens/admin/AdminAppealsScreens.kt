package com.example.daadi.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.daadi.data.supabase.SupabaseAppeal
import com.example.daadi.data.supabase.SupabaseUser
import com.example.daadi.viewmodel.AdminViewModel

@Composable
fun AdminAppealsScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val appeals by supabaseManager.appeals.collectAsStateWithLifecycle()
    val users by adminViewModel.userRepository.users.collectAsStateWithLifecycle()
    val reports by supabaseManager.reports.collectAsStateWithLifecycle()
    
    var selectedAppeal by remember { mutableStateOf<SupabaseAppeal?>(null) }
    var showDecisionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        supabaseManager.fetchAppeals()
        supabaseManager.fetchReports()
    }

    AdminFoundationScaffold(
        title = "Reports & Appeals",
        adminViewModel = adminViewModel,
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (appeals.isEmpty()) {
                AdminEmptyState(
                    title = "No Pending Appeals",
                    description = "All ban appeals have been processed. The community queue is clear."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
                ) {
                    items(appeals, key = { it.id }) { appeal ->
                        AppealItem(
                            appeal = appeal,
                            user = users.find { it.id == appeal.userId },
                            onClick = {
                                selectedAppeal = appeal
                                showDecisionDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDecisionDialog && selectedAppeal != null) {
        AppealDecisionDialog(
            appeal = selectedAppeal!!,
            user = users.find { it.id == selectedAppeal!!.userId },
            onDismiss = { showDecisionDialog = false },
            onDecision = { status, notes ->
                supabaseManager.submitAppealDecision(selectedAppeal!!.id, status, notes) { success ->
                    if (success) showDecisionDialog = false
                }
            }
        )
    }
}

@Composable
fun AppealItem(appeal: SupabaseAppeal, user: SupabaseUser?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = when(appeal.status) {
                    "approved" -> AdminDesign.Secondary.copy(alpha = 0.1f)
                    "rejected" -> AdminDesign.Error.copy(alpha = 0.1f)
                    else -> AdminDesign.Primary.copy(alpha = 0.1f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when(appeal.status) {
                            "approved" -> Icons.Default.CheckCircle
                            "rejected" -> Icons.Default.Cancel
                            else -> Icons.Default.Gavel
                        },
                        contentDescription = null,
                        tint = when(appeal.status) {
                            "approved" -> AdminDesign.Secondary
                            "rejected" -> AdminDesign.Error
                            else -> AdminDesign.Primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(user?.username ?: "User: ${appeal.userId.take(8)}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AdminDesign.OnSurface)
                Text(appeal.reason, fontSize = 11.sp, color = AdminDesign.OnSurface, maxLines = 1)
                Text(
                    text = "STATUS: ${appeal.status.uppercase()}", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Black,
                    color = when(appeal.status) {
                        "approved" -> AdminDesign.Secondary
                        "rejected" -> AdminDesign.Error
                        else -> AdminDesign.Primary
                    }
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdminDesign.OnSurfaceVariant)
        }
    }
}

@Composable
fun AppealDecisionDialog(
    appeal: SupabaseAppeal,
    user: SupabaseUser?,
    onDismiss: () -> Unit,
    onDecision: (String, String) -> Unit
) {
    var notes by remember { mutableStateOf(appeal.moderatorNotes ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appeal Review: ${user?.username ?: "User"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text("User Reason:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(appeal.reason, fontSize = 14.sp)
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Moderator Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onDecision("rejected", notes) },
                    colors = ButtonDefaults.textButtonColors(contentColor = AdminDesign.Error)
                ) {
                    Text("REJECT")
                }
                Button(
                    onClick = { onDecision("approved", notes) },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Secondary)
                ) {
                    Text("APPROVE")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
