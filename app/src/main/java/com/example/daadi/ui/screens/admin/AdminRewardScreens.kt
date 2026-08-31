package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseDailyReward
import com.example.daadi.data.supabase.SupabaseSpinWheelReward
import com.example.daadi.data.supabase.SupabaseMission
import com.example.daadi.data.supabase.SupabaseReferralReward

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminRewardEditor(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val dailyRewards by adminViewModel.economyRepository.dailyRewards.collectAsStateWithLifecycle()
    val spinWheelRewards by adminViewModel.economyRepository.spinWheelRewards.collectAsStateWithLifecycle()
    val missions by adminViewModel.economyRepository.missions.collectAsStateWithLifecycle()
    val referralRewards by adminViewModel.economyRepository.referralRewards.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    var editingDailyReward by remember { mutableStateOf<SupabaseDailyReward?>(null) }
    var editingSpinReward by remember { mutableStateOf<SupabaseSpinWheelReward?>(null) }
    var showCreateMissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminViewModel.economyRepository.fetchDailyRewards()
        adminViewModel.economyRepository.fetchSpinWheelRewards()
        adminViewModel.economyRepository.fetchMissions()
        adminViewModel.economyRepository.fetchReferralRewards()
    }

    AdminFoundationScaffold("Reward Orchestrator", adminViewModel, onBack) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AdminDesign.Primary,
                divider = {},
                edgePadding = 16.dp
            ) {
                listOf("Daily", "Spin Wheel", "Missions", "Referrals").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index, 
                        onClick = { selectedTab = index }, 
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isSyncing && dailyRewards.isEmpty() && spinWheelRewards.isEmpty() && missions.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                        items(8) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                    }
                } else {
                    when (selectedTab) {
                        0 -> DailyRewardGrid(dailyRewards, onEdit = { reward -> editingDailyReward = reward })
                        1 -> SpinWheelRewardList(spinWheelRewards, onEdit = { reward -> editingSpinReward = reward })
                        2 -> MissionRewardList(
                            missions = missions, 
                            onDelete = { id -> adminViewModel.economyRepository.deleteMission(id) },
                            onCreate = { showCreateMissionDialog = true }
                        )
                        3 -> ReferralRewardConfig(
                            referralReward = referralRewards.firstOrNull(),
                            onSave = { ref, referred -> adminViewModel.economyRepository.saveReferralReward(ref, referred) }
                        )
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

        if (showCreateMissionDialog) {
            CreateMissionDialog(
                onDismiss = { showCreateMissionDialog = false },
                onConfirm = { title, desc, type, xp, coins, target ->
                    adminViewModel.economyRepository.createMission(title, desc, type, xp, coins, target)
                    showCreateMissionDialog = false
                }
            )
        }
    }
}

@Composable
fun MissionRewardList(
    missions: List<SupabaseMission>,
    onDelete: (String) -> Unit,
    onCreate: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Missions", style = AdminDesign.HeadingStyle)
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary),
                modifier = Modifier.testTag("admin_create_mission_button")
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("New Mission")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        if (missions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No missions configured", style = AdminDesign.BodyStyle)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(missions) { mission ->
                    AdminCard {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(mission.title, fontWeight = FontWeight.Bold, color = AdminDesign.Primary)
                                Text(mission.description ?: "", style = AdminDesign.BodyStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RewardTag("${mission.xpReward} XP", AdminDesign.Secondary)
                                    RewardTag("${mission.coinReward} Coins", Color(0xFFFFD700))
                                }
                            }
                            IconButton(onClick = { onDelete(mission.id) }) {
                                Icon(Icons.Default.Delete, null, tint = AdminDesign.Error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReferralRewardConfig(
    referralReward: SupabaseReferralReward?,
    onSave: (Int, Int) -> Unit
) {
    var refCoins by remember(referralReward) { mutableStateOf(referralReward?.referrerCoins?.toString() ?: "500") }
    var referredCoins by remember(referralReward) { mutableStateOf(referralReward?.referredCoins?.toString() ?: "200") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Referral Program Configuration", style = AdminDesign.HeadingStyle)
        Spacer(Modifier.height(24.dp))
        
        AdminCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AdminTextField(
                    value = refCoins,
                    onValueChange = { refCoins = it },
                    label = "Referrer Reward (Coins)"
                )
                
                AdminTextField(
                    value = referredCoins,
                    onValueChange = { referredCoins = it },
                    label = "Referred User Reward (Coins)"
                )
                
                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = { onSave(refCoins.toIntOrNull() ?: 0, referredCoins.toIntOrNull() ?: 0) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Configuration", modifier = Modifier.padding(8.dp))
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text(
            "When a user joins using a referral code, both the referrer and the new user will receive the coins specified above.",
            style = AdminDesign.BodyStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = AdminDesign.OnSurfaceVariant
        )
    }
}

@Composable
fun RewardTag(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = AdminDesign.BodyStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        )
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

                AdminTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Reward Amount",
                    modifier = Modifier.fillMaxWidth()
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

                AdminTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Reward Amount",
                    modifier = Modifier.fillMaxWidth()
                )

                AdminTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = "Probability Weight (Higher = More Common)",
                    modifier = Modifier.fillMaxWidth()
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

@Composable
fun CreateMissionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, Int, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("daily") }
    var xp by remember { mutableStateOf("100") }
    var coins by remember { mutableStateOf("50") }
    var target by remember { mutableStateOf("1") }

    AdminDialog(title = "New Mission", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminTextField(value = title, onValueChange = { title = it }, label = "Mission Title")
            AdminTextField(value = desc, onValueChange = { desc = it }, label = "Description")
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminTextField(Modifier.weight(1f), value = xp, onValueChange = { xp = it }, label = "XP Reward")
                AdminTextField(Modifier.weight(1f), value = coins, onValueChange = { coins = it }, label = "Coin Reward")
            }
            
            AdminTextField(value = target, onValueChange = { target = it }, label = "Target Value (e.g. 5 Games)")

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { 
                        onConfirm(title, desc, type, xp.toIntOrNull() ?: 0, coins.toIntOrNull() ?: 0, target.toIntOrNull() ?: 1) 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary)
                ) {
                    Text("Create")
                }
            }
        }
    }
}
