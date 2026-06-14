package com.example.daadi.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseManager
import com.example.daadi.data.supabase.SupabaseUser
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserListScreen(
    supabaseManager: SupabaseManager,
    onUserClick: (SupabaseUser) -> Unit,
    onBack: () -> Unit
) {
    val users by supabaseManager.users.collectAsStateWithLifecycle()
    var searchQueries by remember { mutableStateOf("") }
    
    val filteredUsers = users.filter { 
        it.username.contains(searchQueries, ignoreCase = true) || 
        it.email.contains(searchQueries, ignoreCase = true) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Directory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A),
                    navigationIconContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQueries,
                onValueChange = { searchQueries = it },
                placeholder = { Text("Search by username or email...") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF5C2D0A),
                    unfocusedTextColor = Color(0xFF5C2D0A),
                    focusedBorderColor = Color(0xFF5C2D0A),
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    focusedLabelColor = Color(0xFF5C2D0A),
                    unfocusedLabelColor = Color.Gray
                )
            )

            if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No players found matching your search.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserListItem(user, onClick = { onUserClick(user) })
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(user: SupabaseUser, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFF5C2D0A).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(user.username.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF5C2D0A))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(user.email, fontSize = 11.sp, color = Color.Gray)
            }
            if (user.role == "admin") {
                Text(
                    "ADMIN", 
                    fontSize = 9.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White,
                    modifier = Modifier.background(Color(0xFF2E7D32), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailsScreen(
    user: SupabaseUser,
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Profile Audit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(Color(0xFF5C2D0A).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(user.username.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF5C2D0A))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(user.username, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF5C2D0A))
            Text(user.email, color = Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailStatBox("GAMES", "${user.totalGames}", Modifier.weight(1f))
                DetailStatBox("WINS", "${user.wins}", Modifier.weight(1f))
                DetailStatBox("LOSSES", "${user.losses}", Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("Actions", "Match History", "Login Logs")
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF5C2D0A),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF5C2D0A)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(targetState = selectedTab, label = "tab_content") { tabIndex ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (tabIndex) {
                        0 -> {
                            Text("ACCOUNT MANAGEMENT", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5E3C))
                            Spacer(modifier = Modifier.height(8.dp))
                            ActionTile(
                                title = if (user.isBanned) "Revoke Ban" else "Ban Account",
                                subtitle = if (user.isBanned) "Allow player to access game again" else "Restrict all app functionality",
                                icon = Icons.Default.Block,
                                color = if (user.isBanned) Color(0xFF2E7D32) else Color(0xFFC62828),
                                onClick = { supabaseManager.toggleUserBan(user.id) }
                            )
                            ActionTile(
                                title = if (user.role == "admin") "Demote to User" else "Promote to Admin",
                                subtitle = "Modify system-wide permissions",
                                icon = Icons.Default.Star,
                                color = Color(0xFF1976D2),
                                onClick = { 
                                    val next = if (user.role == "admin") "user" else "admin"
                                    supabaseManager.changeUserRole(user.id, next)
                                }
                            )
                            ActionTile(
                                title = "Clear reports (${user.reportsCount})",
                                subtitle = "Dismiss all community flags",
                                icon = Icons.Default.Check,
                                color = Color(0xFF8B5E3C),
                                onClick = { supabaseManager.dismissUserReports(user.id) }
                            )
                        }
                        1 -> {
                            Text("PAST 5 MATCHES", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5E3C))
                            Spacer(modifier = Modifier.height(8.dp))
                            // Simulated match history
                            listOf("Victory vs Bot (Medium)", "Defeat vs Player_88", "Victory vs Bot (Hard)", "Defeat vs Player_21", "Victory vs Bot (Easy)").forEach { match ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (match.startsWith("Victory")) Icons.Default.EmojiEvents else Icons.Default.Close, contentDescription = null, tint = if (match.startsWith("Victory")) Color(0xFF2E7D32) else Color(0xFFC62828), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(match, fontSize = 13.sp, color = Color(0xFF5C2D0A))
                                    }
                                }
                            }
                        }
                        2 -> {
                            Text("RECENT ACTIVITY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5E3C))
                            Spacer(modifier = Modifier.height(8.dp))
                            // Simulated login logs
                            listOf("Login from Android 14 (Jaipur, IN)", "Login from Android 13 (Delhi, IN)", "Password Change (Security)", "Email Verified (System)").forEach { log ->
                                Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF5C2D0A), CircleShape))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(log, fontSize = 12.sp, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Medium)
                                        Text("Today at 10:4${log.length} AM", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { supabaseManager.deleteUser(user.id); onBack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828).copy(alpha = 0.1f), contentColor = Color(0xFFC62828)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PERMANENTLY DELETE ACCOUNT")
            }
        }
    }
}

@Composable
fun DetailStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF5C2D0A))
        }
    }
}

@Composable
fun ActionTile(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

