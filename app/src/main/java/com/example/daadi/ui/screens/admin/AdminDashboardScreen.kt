package com.example.daadi.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    supabaseManager: SupabaseManager,
    onNavigateToUsers: () -> Unit,
    onNavigateToSafety: () -> Unit,
    onNavigateToMatches: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSystem: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToBulletins: () -> Unit,
    onBack: () -> Unit
) {
    val users by supabaseManager.users.collectAsStateWithLifecycle()
    val matches by supabaseManager.matches.collectAsStateWithLifecycle()
    val settings by supabaseManager.systemSettings.collectAsStateWithLifecycle()
    
    val menuItems = listOf(
        AdminMenuItem("User Management", "Audit & edit players", Icons.Default.Person, onNavigateToUsers, Color(0xFF1976D2)),
        AdminMenuItem("Trust & Safety", "Bans & reports queue", Icons.Default.Shield, onNavigateToSafety, Color(0xFFC62828)),
        AdminMenuItem("Match Control", "Live lobbies & archive", Icons.Default.PlayArrow, onNavigateToMatches, Color(0xFF2E7D32)),
        AdminMenuItem("Remote Config", "Engine & UI variables", Icons.Default.Settings, onNavigateToConfig, Color(0xFF5C2D0A)),
        AdminMenuItem("App Bulletins", "Broadcast & rules", Icons.Default.Campaign, onNavigateToBulletins, Color(0xFFC75D27)),
        AdminMenuItem("Performance", "Usage & growth data", Icons.Default.Timeline, onNavigateToAnalytics, Color(0xFFE65100)),
        AdminMenuItem("System Audit", "Logs & user feedback", Icons.Default.List, onNavigateToSystem, Color(0xFF455A64)),
        AdminMenuItem("App Health", "Service & infra health", Icons.Default.HealthAndSafety, onNavigateToHealth, Color(0xFF2E7D32))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Admin Command Center", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Total registered database entities: ${users.size + matches.size}", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_dash_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit Admin")
                    }
                },
                actions = {
                    IconButton(onClick = { supabaseManager.loadInitialData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A),
                    navigationIconContentColor = Color(0xFF5C2D0A),
                    actionIconContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Summary Stats Strip
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard("Active Users", "${users.size}", Modifier.weight(1f))
                QuickStatCard("Live Matches", "${matches.count { it.status == "playing" }}", Modifier.weight(1f))
                QuickStatCard("Reports", "${users.count { it.isReported }}", Modifier.weight(1f), isAlert = true)
            }

            // QUICK ACTIONS PANEL
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "CRITICAL OPERATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Red.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF5C2D0A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Maintenance Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            val isMaintenanceMode = settings.find { it.key == "maintenance_mode" }?.value == "on"
                            Switch(
                                checked = isMaintenanceMode,
                                onCheckedChange = { supabaseManager.updateSystemSetting("maintenance_mode", if (it) "on" else "off") },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFC75D27), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("In-App Ads Enabled", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            val isAdsOn = settings.find { it.key == "ads_launcher" }?.value == "on"
                            Switch(
                                checked = isAdsOn,
                                onCheckedChange = { supabaseManager.updateSystemSetting("ads_launcher", if (it) "on" else "off") },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }
            }

            Text(
                "MANAGEMENT MODULES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF8B5E3C),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuItems) { item ->
                    AdminMenuCard(item)
                }
            }
        }
    }
}

data class AdminMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val accentColor: Color
)

@Composable
fun AdminMenuCard(item: AdminMenuItem) {
    Card(
        onClick = item.onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(140.dp).border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(item.accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = item.accentColor, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF5C2D0A))
                Text(item.subtitle, fontSize = 10.sp, color = Color.Gray, lineHeight = 12.sp)
            }
        }
    }
}

@Composable
fun QuickStatCard(label: String, value: String, modifier: Modifier = Modifier, isAlert: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isAlert && value != "0") Color(0xFFFFEBEE) else Color(0xFFFFF7EA)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, if (isAlert && value != "0") Color.Red.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = if (isAlert && value != "0") Color.Red else Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isAlert && value != "0") Color.Red else Color(0xFF5C2D0A))
        }
    }
}
