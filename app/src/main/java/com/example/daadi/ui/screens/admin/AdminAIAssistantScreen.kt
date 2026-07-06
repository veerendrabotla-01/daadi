package com.example.daadi.ui.screens.admin



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AdminAIAssistantScreen(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var response by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val suggestions = listOf(
        "Check overall system health & latency",
        "Audit active user registration & login trends",
        "Inspect anti-cheat & security logs",
        "Review fraud alerts & transaction compliance",
        "Analyze AdMob fill rate & ad telemetry",
        "Summarize live match & multiplayer stats"
    )

    AdminFoundationScaffold("Insight Engine", supabaseManager, onBack) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            Box(modifier = Modifier.weight(1f)) {
                if (response == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = AdminDesign.Primary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
                        Text("DAADI LOCAL INTEL ENGINE", fontWeight = FontWeight.Black, fontSize = 20.sp, color = AdminDesign.OnSurface)
                        Text(
                            text = "Instant, secure diagnostics from active data bounds.", 
                            fontSize = 14.sp, 
                            color = AdminDesign.OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingExtraLarge))
                        
                        Text("DIAGNOSTIC PROBES", fontWeight = FontWeight.Black, fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                        Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall), horizontalAlignment = Alignment.CenterHorizontally) {
                            suggestions.forEach { suggestion ->
                                SuggestionChip(suggestion) { query = suggestion }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = AdminDesign.CardShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
                        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
                    ) {
                        LazyColumn(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Analytics, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                                    Text("LOCAL DIAGNOSTIC ENGINE REPORT", fontWeight = FontWeight.Black, color = AdminDesign.Primary, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                                Text(
                                    text = response!!, 
                                    fontSize = 15.sp, 
                                    lineHeight = 24.sp, 
                                    color = AdminDesign.OnSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))

            if (response != null) {
                Button(
                    onClick = { response = null; query = "" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f), contentColor = AdminDesign.OnSurfaceVariant)
                ) {
                    Text("RESET TERMINAL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = AdminDesign.Surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Probe system metrics...") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        singleLine = true
                    )
                    
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp).size(24.dp), strokeWidth = 3.dp, color = AdminDesign.Primary)
                    } else {
                        IconButton(
                            onClick = {
                                if (query.isNotBlank()) {
                                    isSearching = true
                                    scope.launch {
                                        response = adminViewModel.analyticsRepository.askAiAssistant(query)
                                        isSearching = false
                                    }
                                }
                            },
                            modifier = Modifier.background(AdminDesign.Primary, CircleShape)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Ask", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = AdminDesign.Surface,
        border = BorderStroke(1.dp, AdminDesign.OnSurface.copy(alpha = 0.05f)),
        shadowElevation = 2.dp
    ) {
        Text(
            text = text, 
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold,
            color = AdminDesign.OnSurface
        )
    }
}
