package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.daadi.data.supabase.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminTournamentScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val tournaments by supabaseManager.tournaments.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        supabaseManager.fetchTournaments()
    }

    AdminFoundationScaffold(
        title = "Tournaments",
        supabaseManager = supabaseManager,
        onBack = onBack,
        actions = {
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "Create Tournament", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        if (isSyncing && tournaments.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (tournaments.isEmpty()) {
            AdminEmptyState(
                title = "No Tournaments", 
                description = "Competitive play is currently quiet. Schedule a new tournament to drive high-stakes engagement."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item {
                    Text("ACTIVE & UPCOMING COMPETITIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
                items(tournaments) { tournament ->
                    TournamentItem(tournament)
                }
            }
        }

        if (showCreateDialog) {
            CreateTournamentDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { title, desc, fee, prize ->
                    supabaseManager.createTournament(title, desc, fee, prize)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun TournamentItem(tournament: SupabaseTournament) {
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
                    shape = RoundedCornerShape(8.dp),
                    color = AdminDesign.Primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(tournament.title, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface, fontSize = 15.sp)
                    Text(tournament.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = AdminDesign.Success, fontWeight = FontWeight.Black)
                }
                StatusBadge(tournament.status)
            }
            
            Text(
                text = tournament.description ?: "No description provided.", 
                fontSize = 12.sp, 
                color = AdminDesign.OnSurfaceVariant, 
                modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall), color = AdminDesign.OnSurface.copy(alpha = 0.05f))
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TournamentStat("ENTRY FEE", "${tournament.entryFee}", Icons.Default.Login)
                TournamentStat("PRIZE POOL", "${tournament.prizePoolCoins}", Icons.Default.MilitaryTech)
                TournamentStat("MAX CAPACITY", "${tournament.maxParticipants}", Icons.Default.Groups)
            }
        }
    }
}

@Composable
fun TournamentStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(10.dp), tint = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface)
    }
}

@Composable
fun CreateTournamentDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("100") }
    var prize by remember { mutableStateOf("1000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Orchestrate Tournament", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tournament Title") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Mission Statement / Rules") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                    OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Buy-in Fee") }, modifier = Modifier.weight(1f), shape = AdminDesign.InputShape)
                    OutlinedTextField(value = prize, onValueChange = { prize = it }, label = { Text("Grand Prize") }, modifier = Modifier.weight(1f), shape = AdminDesign.InputShape)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, desc, fee.toIntOrNull() ?: 0, prize.toIntOrNull() ?: 0) },
                shape = AdminDesign.ButtonShape
            ) {
                Text("DEPLOY COMPETITION")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ABORT") }
        }
    )
}
