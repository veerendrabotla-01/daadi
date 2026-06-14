package com.example.daadi.ui.screens.admin

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseManager
import com.example.daadi.data.supabase.SupabaseUser
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSafetyHubScreen(
    supabaseManager: SupabaseManager,
    onUserClick: (SupabaseUser) -> Unit,
    onBack: () -> Unit
) {
    val users by supabaseManager.users.collectAsStateWithLifecycle()
    val reportedUsers = users.filter { it.isReported }.sortedByDescending { it.reportsCount }
    val bannedUsers = users.filter { it.isBanned }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Reported Queue", "Banned List")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trust & Safety", fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFFFFFBF4), contentColor = Color(0xFF5C2D0A)) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTab == 0) {
                if (reportedUsers.isEmpty()) {
                    EmptyStateView("No pending reports. Great job!")
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(reportedUsers) { user ->
                            SafetyUserItem(user, showReports = true, onClick = { onUserClick(user) })
                        }
                    }
                }
            } else {
                if (bannedUsers.isEmpty()) {
                    EmptyStateView("No players currently banned.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(bannedUsers) { user ->
                            SafetyUserItem(user, showReports = false, onClick = { onUserClick(user) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyUserItem(user: SupabaseUser, showReports: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.Bold, color = Color(0xFF5C2D0A))
                Text(user.email, fontSize = 11.sp, color = Color.Gray)
            }
            if (showReports) {
                Text(
                    "${user.reportsCount} FLAGS", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White,
                    modifier = Modifier.background(Color(0xFFC62828), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32).copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = Color.Gray, fontSize = 14.sp)
        }
    }
}
