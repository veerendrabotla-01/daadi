package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.*
import com.example.daadi.ui.screens.admin.AdminDesign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminTournamentScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    val tournaments by adminViewModel.tournamentRepository.tournaments.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var selectedTournament by remember { mutableStateOf<SupabaseTournament?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminViewModel.tournamentRepository.fetchTournaments()
    }

    if (selectedTournament == null) {
        AdminFoundationScaffold(
            title = "Tournament Center",
            adminViewModel = adminViewModel,
            onBack = onBack,
            actions = {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create", tint = AdminDesign.Primary)
                }
            }
        ) { padding ->
            if (isSyncing && tournaments.isEmpty()) {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                    items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                }
            } else if (tournaments.isEmpty()) {
                AdminEmptyState("No Tournaments", "Schedule a new competition to start.", actionButton = {
                    Button(onClick = { showCreateDialog = true }) { Text("Create Tournament") }
                })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    items(tournaments) { tournament ->
                        TournamentItem(
                            tournament = tournament,
                            onClick = { selectedTournament = tournament },
                            onDelete = { adminViewModel.tournamentRepository.deleteTournament(tournament.id) }
                        )
                    }
                }
            }

            if (showCreateDialog) {
                CreateTournamentDialog(
                    onDismiss = { showCreateDialog = false },
                    onConfirm = { title, desc, fee, prize ->
                        adminViewModel.tournamentRepository.createTournament(title, desc, fee, prize)
                        showCreateDialog = false
                    }
                )
            }
        }
    } else {
        TournamentDetailView(
            tournament = selectedTournament!!,
            adminViewModel = adminViewModel,
            onBack = { selectedTournament = null }
        )
    }
}

@Composable
fun TournamentDetailView(
    tournament: SupabaseTournament,
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Participants", "Brackets", "Announce")

    AdminFoundationScaffold(
        title = tournament.title,
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = AdminDesign.Primary) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontSize = 11.sp) })
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> TournamentOverviewTab(tournament, adminViewModel)
                    1 -> TournamentParticipantsTab(tournament, adminViewModel)
                    2 -> TournamentBracketsTab(tournament, adminViewModel)
                    3 -> TournamentAnnouncementsTab(tournament, adminViewModel)
                }
            }
        }
    }
}

@Composable
fun TournamentOverviewTab(tournament: SupabaseTournament, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium), verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    Text("CONTROL PANEL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { adminViewModel.tournamentRepository.updateTournamentStatus(tournament.id, "active") }, modifier = Modifier.weight(1f)) { Text("START") }
                        Button(onClick = { adminViewModel.tournamentRepository.updateTournamentStatus(tournament.id, "completed") }, modifier = Modifier.weight(1f)) { Text("END") }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    Text("DETAILS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(tournament.description ?: "No description", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Entry Fee: ${tournament.entryFee} coins", fontWeight = FontWeight.Bold)
                    Text("Prize Pool: ${tournament.prizePoolCoins} coins", fontWeight = FontWeight.Bold)
                    Text("Max Participants: ${tournament.maxParticipants}")
                }
            }
        }
    }
}

@Composable
fun TournamentParticipantsTab(tournament: SupabaseTournament, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    val participants by adminViewModel.tournamentRepository.participants.collectAsStateWithLifecycle()
    
    LaunchedEffect(tournament.id) {
        adminViewModel.tournamentRepository.network.fetchTournamentParticipants(tournament.id)
    }

    if (participants.isEmpty()) {
        AdminEmptyState("No Entrants", "Nobody has registered for this tournament yet.")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(AdminDesign.SpacingMedium), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(participants) { participant ->
                Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlayerIcon(participant.userId.take(8)) // Mock username if not joined
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("User ID: ${participant.userId}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Joined: ${participant.joinedAt}", fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { /* Disqualify logic */ }) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = AdminDesign.Error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentBracketsTab(tournament: SupabaseTournament, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    val brackets by adminViewModel.tournamentRepository.brackets.collectAsStateWithLifecycle()

    LaunchedEffect(tournament.id) {
        adminViewModel.tournamentRepository.network.fetchTournamentBrackets(tournament.id)
    }

    if (brackets.isEmpty()) {
        AdminEmptyState("Bracket Empty", "Brackets haven't been generated or synced for this tournament.")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(AdminDesign.SpacingMedium), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(brackets) { bracket ->
                Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ROUND ${bracket.round} - POS ${bracket.position}", fontWeight = FontWeight.Black, fontSize = 10.sp, color = AdminDesign.Primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(bracket.player1Id ?: "TBD", fontWeight = FontWeight.Bold)
                            Text("VS", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
                            Text(bracket.player2Id ?: "TBD", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentAnnouncementsTab(tournament: SupabaseTournament, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    val announcements by adminViewModel.tournamentRepository.announcements.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(tournament.id) {
        adminViewModel.tournamentRepository.network.fetchTournamentAnnouncements(tournament.id)
    }

    Column(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
        Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)) {
            Column(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NEW ANNOUNCEMENT", fontWeight = FontWeight.Black, fontSize = 10.sp)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(
                    onClick = {
                        val ann = com.example.daadi.data.supabase.SupabaseTournamentAnnouncement(
                            id = UUID.randomUUID().toString(),
                            tournamentId = tournament.id,
                            title = title,
                            message = message,
                            createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                        )
                        adminViewModel.tournamentRepository.network.postTournamentAnnouncement(ann) {
                            title = ""
                            message = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("BROADCAST")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("PREVIOUS ANNOUNCEMENTS", fontWeight = FontWeight.Black, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(announcements) { ann ->
                Card(colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(ann.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(ann.message, fontSize = 12.sp)
                        Text(ann.createdAt, fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentItem(tournament: SupabaseTournament, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AdminDesign.Primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tournament.title, fontWeight = FontWeight.Bold)
                Text(tournament.status.uppercase(), fontSize = 10.sp, color = AdminDesign.Success, fontWeight = FontWeight.Black)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = AdminDesign.Error.copy(alpha = 0.5f))
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
