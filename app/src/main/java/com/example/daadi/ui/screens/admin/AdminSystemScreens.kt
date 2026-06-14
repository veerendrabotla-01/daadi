package com.example.daadi.ui.screens.admin

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseAuditLog
import com.example.daadi.data.supabase.SupabaseFeedback
import com.example.daadi.data.supabase.SupabaseManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSystemScreens(
    supabaseManager: SupabaseManager,
    type: String, // "analytics", "feedback", "logs"
    onBack: () -> Unit
) {
    val title = when(type) {
        "analytics" -> "Growth Analytics"
        "feedback" -> "User Feedback"
        "health" -> "Application Health"
        else -> "Security Audit Logs"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
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
            when(type) {
                "analytics" -> AnalyticsPanel()
                "feedback" -> FeedbackPanel(supabaseManager)
                "logs" -> AuditLogsPanel(supabaseManager)
                "health" -> AppHealthPanel(supabaseManager)
            }
        }
    }
}

@Composable
fun AppHealthPanel(supabaseManager: SupabaseManager) {
    val settings by supabaseManager.systemSettings.collectAsStateWithLifecycle()
    val adsSetting = settings.find { it.key == "ads_launcher" }?.value ?: "off"
    val isAdsOn = adsSetting == "on"

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Ads Control
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF8BC34A).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF388E3C))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ad Visibility", fontWeight = FontWeight.Bold)
                }
                Text("Toggle ads and rewarded video placements across the app.", fontSize = 11.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status: ${if (isAdsOn) "ON" else "OFF"}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = if (isAdsOn) Color(0xFF2E7D32) else Color.Red)
                    Switch(
                        checked = isAdsOn,
                        onCheckedChange = { isOn -> supabaseManager.updateSystemSetting("ads_launcher", if (isOn) "on" else "off") }
                    )
                }
            }
        }

        HealthIndicator("Main Database (Supabase)", "OPERATIONAL", Color(0xFF2E7D32))
        HealthIndicator("Multiplayer Hub (Piesocket)", "DEGRADED (40ms)", Color(0xFFE5A93B))
        HealthIndicator("CDN (Image Assets)", "STABLE", Color(0xFF2E7D32))
        HealthIndicator("AI Engine (Local Worker)", "IDLE", Color(0xFF1976D2))
    }
}

@Composable
fun HealthIndicator(service: String, status: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color, CircleShape))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(service, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(status, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun FeedbackPanel(supabaseManager: SupabaseManager) {
    val feedbacks by supabaseManager.feedback.collectAsStateWithLifecycle()
    if (feedbacks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No feedback submitted yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(feedbacks) { fb ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(fb.username, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(
                                fb.category.uppercase(), 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Black,
                                color = if (fb.category == "bug") Color.Red else Color(0xFF1976D2),
                                modifier = Modifier.background(if (fb.category == "bug") Color(0xFFFFEBEE) else Color(0xFFE3F2FD), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(fb.content, fontSize = 12.sp, color = Color.DarkGray)
                        Text(fb.createdAt, fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogsPanel(supabaseManager: SupabaseManager) {
    val logs by supabaseManager.auditLogs.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(logs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF5C2D0A), CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("${log.adminName} performed ${log.action} on ${log.target}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(log.timestamp, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsPanel() {
    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(200.dp).border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFFE65100).copy(alpha = 0.2f))
                Text("RETENTION GROWTH CHART", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFFE65100))
                Text("(Live Data Visualization Placeholder)", fontSize = 10.sp, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsMiniCard("DAU", "42", Modifier.weight(1f))
            AnalyticsMiniCard("MAU", "156", Modifier.weight(1f))
        }
    }
}

@Composable
fun AnalyticsMiniCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF5C2D0A))
        }
    }
}
