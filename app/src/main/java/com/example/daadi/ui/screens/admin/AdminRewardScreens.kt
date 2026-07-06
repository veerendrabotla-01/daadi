package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseDailyReward
import com.example.daadi.data.supabase.SupabaseSpinWheelReward

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun AdminRewardEditor(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val dailyRewards by adminViewModel.economyRepository.dailyRewards.collectAsStateWithLifecycle()
    val spinWheelRewards by adminViewModel.economyRepository.spinWheelRewards.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    var editingDailyReward by remember { mutableStateOf<SupabaseDailyReward?>(null) }
    var editingSpinReward by remember { mutableStateOf<SupabaseSpinWheelReward?>(null) }

    LaunchedEffect(Unit) {
        adminViewModel.economyRepository.fetchDailyRewards()
        adminViewModel.economyRepository.fetchSpinWheelRewards()
    }

    AdminFoundationScaffold("Reward Orchestrator", supabaseManager, onBack) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AdminDesign.Primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0, 
                    onClick = { selectedTab = 0 }, 
                    text = { Text("Daily (30-Day Cycle)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1, 
                    onClick = { selectedTab = 1 }, 
                    text = { Text("Spin Wheel Matrix", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isSyncing && dailyRewards.isEmpty() && spinWheelRewards.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                        items(8) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                    }
                } else {
                    when (selectedTab) {
                        0 -> DailyRewardGrid(dailyRewards, onEdit = { reward -> editingDailyReward = reward })
                        1 -> SpinWheelRewardList(spinWheelRewards, onEdit = { reward -> editingSpinReward = reward })
                    }
                }
            }
        }

        if (editingDailyReward != null) {
            EditDailyRewardDialog(
                reward = editingDailyReward!!,
                onDismiss = { editingDailyReward = null },
                onConfirm = { type, amount ->
                    adminViewModel.economyRepository.saveDailyReward(editingDailyReward!!.day, type, amount)
                    editingDailyReward = null
                }
            )
        }

        if (editingSpinReward != null) {
            EditSpinWheelRewardDialog(
                reward = editingSpinReward!!,
                onDismiss = { editingSpinReward = null },
                onConfirm = { type, amount, weight ->
                    adminViewModel.economyRepository.saveSpinWheelReward(editingSpinReward!!.id, type, amount, weight)
                    editingSpinReward = null
                }
            )
        }
    }
}

@Composable
fun DailyRewardGrid(
    rewards: List<SupabaseDailyReward>,
    onEdit: (SupabaseDailyReward) -> Unit
) {
    val displayRewards = remember(rewards) {
        (1..30).map { day -> 
            rewards.find { it.day == day } ?: SupabaseDailyReward(day, "coins", 100 * day, null, null) 
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(80.dp),
        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
        modifier = Modifier.fillMaxSize()
    ) {
        items(displayRewards) { reward ->
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onEdit(reward) }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingSmall),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("DAY ${reward.day}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = when (reward.type) {
                            "coins" -> AdminDesign.Secondary.copy(alpha = 0.1f)
                            "xp" -> AdminDesign.Primary.copy(alpha = 0.1f)
                            else -> AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (reward.type) {
                                    "coins" -> Icons.Default.MonetizationOn
                                    "xp" -> Icons.Default.TrendingUp
                                    else -> Icons.Default.Redeem
                                },
                                contentDescription = null,
                                tint = when (reward.type) {
                                    "coins" -> AdminDesign.Secondary
                                    "xp" -> AdminDesign.Primary
                                    else -> AdminDesign.OnSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reward.amount.toString(), 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        color = AdminDesign.OnSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SpinWheelRewardList(
    rewards: List<SupabaseSpinWheelReward>,
    onEdit: (SupabaseSpinWheelReward) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
    ) {
        item {
            Text("PROBABILITY DISTRIBUTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
        }
        items(rewards) { reward ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
            ) {
                Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color(android.graphics.Color.parseColor(reward.color ?: "#CCCCCC")),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reward.weight.toString(), 
                            color = Color.White, 
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${reward.amount} ${reward.type.uppercase()}", fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface)
                        Text("Selection Weight: ${reward.weight}", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { onEdit(reward) },
                        modifier = Modifier
                            .background(AdminDesign.Background, CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Config", tint = AdminDesign.Primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EditDailyRewardDialog(
    reward: SupabaseDailyReward,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var type by remember { mutableStateOf(reward.type) }
    var amount by remember { mutableStateOf(reward.amount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Day ${reward.day} Reward", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                Text("REWARD TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("coins", "xp", "item").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.uppercase(), fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Reward Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.InputShape
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(type, amount.toIntOrNull() ?: 0) },
                shape = AdminDesign.ButtonShape
            ) {
                Text("UPDATE DAY ${reward.day}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ABORT") }
        }
    )
}

@Composable
fun EditSpinWheelRewardDialog(
    reward: SupabaseSpinWheelReward,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int) -> Unit
) {
    var type by remember { mutableStateOf(reward.type) }
    var amount by remember { mutableStateOf(reward.amount.toString()) }
    var weight by remember { mutableStateOf(reward.weight.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Wheel Sector", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                Text("REWARD TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("coins", "xp", "gems").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.uppercase(), fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Reward Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.InputShape
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Probability Weight (Higher = More Common)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.InputShape
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(type, amount.toIntOrNull() ?: 0, weight.toIntOrNull() ?: 1) },
                shape = AdminDesign.ButtonShape
            ) {
                Text("SAVE SECTOR CONFIG")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ABORT") }
        }
    )
}
