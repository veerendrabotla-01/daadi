package com.example.daadi.ui.screens.admin



import com.example.daadi.data.supabase.SupabaseAuditLog
import com.example.daadi.data.supabase.SupabaseEconomyTransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun AdminEconomyCenter(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit = {},
    onBack: () -> Unit
) {
    val transactions by adminViewModel.economyRepository.economyTransactions.collectAsStateWithLifecycle()
    val auditLogs by adminViewModel.adminRepository.auditLogs.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    val users by adminViewModel.userRepository.users.collectAsStateWithLifecycle()
    val filterUserId = adminViewModel.filterUserId.value ?: ""

    var showAdjustDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val filteredTransactions = remember(transactions, filterUserId) {
        if (filterUserId.isNotEmpty()) {
            transactions.filter { it.userId.equals(filterUserId, ignoreCase = true) }
        } else {
            transactions
        }
    }

    LaunchedEffect(Unit) {
        adminViewModel.economyRepository.fetchEconomyTransactions()
        adminViewModel.adminRepository.fetchAuditLogs()
    }

    AdminFoundationScaffold(
        title = "Economy Hub",
        adminViewModel = adminViewModel,
        onBack = onBack,
        actions = {
            IconButton(onClick = { showAdjustDialog = true }) {
                Icon(Icons.Default.AddChart, contentDescription = "Manual Adjustment", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Stats Header
            EconomyStatsHeader(transactions)

            if (filterUserId.isNotEmpty()) {
                val matchedUser = remember(users, filterUserId) {
                    users.find { it.id.equals(filterUserId, ignoreCase = true) }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AdminDesign.SpacingMedium, vertical = 4.dp),
                    color = AdminDesign.Primary.copy(alpha = 0.1f),
                    shape = AdminDesign.CardShape
                ) {
                    Row(
                        modifier = Modifier.padding(AdminDesign.SpacingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Filtering transactions for: ${matchedUser?.username ?: filterUserId.take(16)}", 
                            fontWeight = FontWeight.Bold, 
                            color = AdminDesign.Primary, 
                            fontSize = 12.sp
                        )
                        TextButton(onClick = { adminViewModel.filterUserId.value = "" }) {
                            Text("Clear Filter", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AdminDesign.Primary,
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("LEDGER", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("AUDIT LOGS", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isSyncing && transactions.isEmpty() && auditLogs.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                        items(10) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                    }
                } else {
                    when (selectedTab) {
                        0 -> TransactionList(filteredTransactions, users, onUserClick)
                        1 -> EconomyAuditList(auditLogs.filter { it.actionType.contains("ECONOMY", true) || it.targetTable?.contains("ECONOMY", true) == true })
                    }
                }
            }
        }

        if (showAdjustDialog) {
            EconomyAdjustmentDialog(
                initialUserId = filterUserId,
                onDismiss = { showAdjustDialog = false },
                onConfirm = { userId, amount, currency, reason ->
                    adminViewModel.economyRepository.adjustUserEconomy(userId, amount, currency, reason)
                    showAdjustDialog = false
                }
            )
        }
    }
}

@Composable
fun TransactionList(
    transactions: List<com.example.daadi.data.supabase.SupabaseEconomyTransaction>,
    users: List<com.example.daadi.data.supabase.SupabaseUser>,
    onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit
) {
    if (transactions.isEmpty()) {
        AdminEmptyState(
            title = "No Transactions",
            description = "Economic activity is silent. Financial logs will appear here as users earn or spend currency."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            item {
                Text("TRANSACTION LEDGER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            }
            items(transactions) { tx ->
                EconomyTransactionCard(tx, users, onUserClick)
            }
        }
    }
}

@Composable
fun EconomyAuditList(logs: List<com.example.daadi.data.supabase.SupabaseAuditLog>) {
    if (logs.isEmpty()) {
        AdminEmptyState(
            title = "No Audit Logs",
            description = "No administrative economy actions have been recorded yet."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            item {
                Text("ADMINISTRATIVE AUDIT TRAIL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            }
            items(logs) { log ->
                AdminCard {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(32.dp), shape = CircleShape, color = AdminDesign.Primary.copy(alpha = 0.1f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.HistoryEdu, null, tint = AdminDesign.Primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(log.actionType, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(log.reason ?: "Target: ${log.targetTable} (${log.targetId})", style = AdminDesign.BodyStyle, fontSize = 12.sp)
                            Text(log.createdAt.take(16), fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EconomyStatsHeader(transactions: List<com.example.daadi.data.supabase.SupabaseEconomyTransaction>) {
    val totalCoins = transactions.filter { it.currency == "coins" }.sumOf { it.amount }
    val totalXp = transactions.filter { it.currency == "xp" }.sumOf { it.amount }

    Surface(
        color = AdminDesign.Primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AdminDesign.SpacingLarge),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CIRCULATING COINS", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(totalCoins.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.2f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CUMULATIVE XP", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(totalXp.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun EconomyTransactionCard(
    tx: com.example.daadi.data.supabase.SupabaseEconomyTransaction,
    users: List<com.example.daadi.data.supabase.SupabaseUser>,
    onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit
) {
    val matchedUser = remember(users, tx.userId) {
        users.find { it.id.equals(tx.userId, ignoreCase = true) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (tx.amount >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (tx.amount >= 0) Icons.Default.AddCircle else Icons.Default.RemoveCircle,
                        contentDescription = null,
                        tint = if (tx.amount >= 0) Color(0xFF2E7D32) else AdminDesign.Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                if (matchedUser != null) {
                    Card(
                        onClick = { onUserClick(matchedUser) },
                        colors = CardDefaults.cardColors(
                            containerColor = AdminDesign.Primary.copy(alpha = 0.08f),
                            contentColor = AdminDesign.Primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.wrapContentSize().padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(matchedUser.username, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    Text("User: ${tx.userId.take(12)}...", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AdminDesign.OnSurface)
                }
                
                Text("${tx.type.uppercase()} • ${tx.source.uppercase()}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                if (!tx.reason.isNullOrBlank()) {
                    Text(tx.reason, fontSize = 11.sp, color = AdminDesign.OnSurface, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (tx.amount >= 0) "+" else ""}${tx.amount} ${tx.currency.uppercase()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (tx.amount >= 0) Color(0xFF2E7D32) else AdminDesign.Error
                )
                Text(tx.createdAt.take(10), fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant)
            }
        }
    }
}

@Composable
fun EconomyAdjustmentDialog(
    initialUserId: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, String) -> Unit
) {
    var userId by remember { mutableStateOf(initialUserId) }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("coins") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Economy Overwrite", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                AdminTextField(
                    value = userId, 
                    onValueChange = { userId = it }, 
                    label = "Target User ID", 
                    modifier = Modifier.fillMaxWidth()
                )
                AdminTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = "Amount (+/-)", 
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    FilterChip(
                        selected = currency == "coins",
                        onClick = { currency = "coins" },
                        label = { Text("Coins") },
                        leadingIcon = { if (currency == "coins") Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = currency == "xp",
                        onClick = { currency = "xp" },
                        label = { Text("XP") },
                        leadingIcon = { if (currency == "xp") Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                    )
                }
                
                AdminTextField(
                    value = reason, 
                    onValueChange = { reason = it }, 
                    label = "Reason for Adjustment", 
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(userId, amount.toIntOrNull() ?: 0, currency, reason) },
                shape = AdminDesign.ButtonShape
            ) {
                Text("EXECUTE ADJUSTMENT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
