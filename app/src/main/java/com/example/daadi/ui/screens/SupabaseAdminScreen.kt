package com.example.daadi.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseAnnouncement
import com.example.daadi.data.supabase.SupabaseManager
import com.example.daadi.data.supabase.SupabaseMatch
import com.example.daadi.data.supabase.SupabaseSystemSetting
import com.example.daadi.data.supabase.SupabaseUser
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseAdminScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val currentRole by supabaseManager.currentAdminRole.collectAsStateWithLifecycle()

    if (currentRole != "admin") {
        // Denied State View for Standard Users
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Restricted") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFDF3E3),
                        titleContentColor = Color(0xFF5C2D0A),
                        navigationIconContentColor = Color(0xFF5C2D0A)
                    )
                )
            },
            containerColor = Color(0xFFFDF3E3)
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(80.dp).background(Color(0xFFC62828).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("ADMIN ACCESS REQUIRED", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF5C2D0A), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Only accounts with the 'admin' role can access the command center.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { supabaseManager.setAdminRoleTesting("admin") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A))) {
                    Text("Simulate Admin Role")
                }
            }
        }
    } else {
        // Modular Admin Navigation
        com.example.daadi.ui.screens.admin.AdminNavigator(
            supabaseManager = supabaseManager,
            onExitAdmin = onBack
        )
    }
}

@Composable
fun UsersPanel(
    users: List<SupabaseUser>,
    onRoleChange: (String, String) -> Unit,
    onBanToggle: (String) -> Unit,
    onDismissReports: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No registered users audited.", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(users) { usr ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (usr.isReported) Color(0xFFFFF5F5) else Color(0xFFFFFBFA)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (usr.isReported) 2.dp else 1.dp,
                            color = if (usr.isReported) Color(0xFFE53935).copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = usr.username,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF5C2D0A),
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = usr.role.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (usr.role == "admin") Color(0xFF2E7D32) else Color(0xFF1976D2),
                                        modifier = Modifier
                                            .background(
                                                color = if (usr.role == "admin") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(usr.email, fontSize = 12.sp, color = Color.Gray)
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (usr.isReported) {
                                    Text(
                                        text = "REPORTED (${usr.reportsCount}x)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier
                                            .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (usr.isBanned) {
                                    Text(
                                        text = "BANNED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier
                                            .background(Color(0xFFC62828), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Stats: ${usr.totalGames} Games • ${usr.wins}W / ${usr.losses}L",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF8B5E3C)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dismiss Reports Action
                                if (usr.isReported) {
                                    TextButton(
                                        onClick = { onDismissReports(usr.id) },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = Color(0xFF2E7D32)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Toggle Role Trigger
                                TextButton(
                                    onClick = {
                                        val targetRole = if (usr.role == "admin") "user" else "admin"
                                        onRoleChange(usr.id, targetRole)
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5C2D0A))
                                ) {
                                    Text(if (usr.role == "admin") "Demote" else "Promote", fontSize = 11.sp)
                                }

                                // Ban/Unban Trigger
                                TextButton(
                                    onClick = { onBanToggle(usr.id) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (usr.isBanned) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(if (usr.isBanned) "Unban" else "Ban", fontSize = 11.sp)
                                }

                                // Delete Button
                                IconButton(
                                    onClick = { onDelete(usr.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Delete", 
                                        tint = Color.LightGray, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementsPanel(
    announcements: List<SupabaseAnnouncement>,
    onCreateClick: () -> Unit,
    onToggle: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D0A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CREATE NEW ANNOUNCEMENT", fontWeight = FontWeight.Bold)
            }
        }

        if (announcements.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No announcements loaded.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(announcements) { ann ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (ann.isActive) Color(0xFFFFFBFA) else Color(0xFFF2F2F2)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ann.title,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ann.isActive) Color(0xFF5C2D0A) else Color.Gray,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Published: ${ann.createdAt}", 
                                        fontSize = 10.sp, 
                                        color = Color.LightGray
                                    )
                                }

                                Switch(
                                    checked = ann.isActive,
                                    onCheckedChange = { onToggle(ann.id) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF5C2D0A), 
                                        checkedTrackColor = Color(0xFFD4A55A)
                                    ),
                                    modifier = Modifier.scaleScaleHelper()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = ann.content,
                                fontSize = 12.sp,
                                color = if (ann.isActive) Color.DarkGray else Color.Gray,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { onDelete(ann.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Delete", 
                                        tint = Color(0xFFC62828).copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.scaleScaleHelper(): Modifier = this.padding(0.dp) // decorative

@Composable
fun MatchesPanel(
    matches: List<SupabaseMatch>,
    onDelete: (String) -> Unit
) {
    if (matches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active or historic lobbies in database.", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(matches) { match ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PlayArrow, 
                                    contentDescription = null, 
                                    tint = Color(0xFFE5A93B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lobby: ${match.id.uppercase()}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5C2D0A),
                                    fontSize = 14.sp
                                )
                            }

                            val (statusLabel, statusColor, contColor) = when (match.status) {
                                "waiting" -> Triple("AWAITING", Color(0xFFFFE082), Color(0xFF5D4037))
                                "playing" -> Triple("LIVE MATCH", Color(0xFFE3F2FD), Color(0xFF1565C0))
                                else -> Triple("FINISHED", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                            }

                            Text(
                                text = statusLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = contColor,
                                modifier = Modifier
                                    .background(statusColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${match.hostName} vs ${match.opponentName.ifEmpty { "Pending Opponent..." }}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        if (match.status == "finished" && !match.winner.isNullOrEmpty()) {
                            Text(
                                text = "🏆 Winner: ${match.winner}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Moves Played: ${match.movesCount} • Created: ${match.createdAt}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )

                            IconButton(
                                onClick = { onDelete(match.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Clear Match", 
                                    tint = Color(0xFFC62828).copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPanel(
    settings: List<SupabaseSystemSetting>,
    onEditClick: (SupabaseSystemSetting) -> Unit
) {
    if (settings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No system configurations loaded.", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    "GLOBAL ENGINE PARAMETERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF8B5E3C),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(settings) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.key.uppercase().replace("_", " "),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C2D0A),
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Current Value: ", 
                                    fontSize = 11.sp, 
                                    color = Color(0xFF8B5E3C)
                                )
                                Text(
                                    text = item.value, 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Black, 
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onEditClick(item) },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF5C2D0A))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Variable")
                        }
                    }
                }
            }
        }
    }
}
