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

    fun executeQuery(prompt: String) {
        if (prompt.isNotBlank()) {
            isSearching = true
            query = prompt
            scope.launch {
                response = adminViewModel.analyticsRepository.askAiAssistant(prompt)
                isSearching = false
            }
        }
    }

    AdminFoundationScaffold("Insight Engine", adminViewModel, onBack) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            Box(modifier = Modifier.weight(1f)) {
                if (response == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = AdminDesign.Primary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                        Text("DAADI LOCAL INTEL ENGINE", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AdminDesign.OnSurface)
                        Text(
                            text = "Instant, secure diagnostic scanning and LiveOps recommendations.", 
                            fontSize = 12.sp, 
                            color = AdminDesign.OnSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // EXECUTIVE QUICK COMMAND CARDS
                        Text("CORE TELEMETRY TRIGGERS", fontWeight = FontWeight.Black, fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                modifier = Modifier.weight(1f).height(80.dp),
                                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                                shape = AdminDesign.CardShape,
                                onClick = { executeQuery("generate report summary") }
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Assignment, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Summary Report", fontSize = 10.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurface)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f).height(80.dp),
                                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                                shape = AdminDesign.CardShape,
                                onClick = { executeQuery("scan anomalies") }
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = AdminDesign.Secondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Anomaly Scan", fontSize = 10.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurface)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f).height(80.dp),
                                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                                shape = AdminDesign.CardShape,
                                onClick = { executeQuery("recommend liveops") }
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AdminDesign.Tertiary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("LiveOps Ideas", fontSize = 10.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurface)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("DIAGNOSTIC PROBES", fontWeight = FontWeight.Black, fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            items(suggestions) { suggestion ->
                                SuggestionChip(suggestion) { executeQuery(suggestion) }
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
                                    fontSize = 14.sp, 
                                    lineHeight = 22.sp, 
                                    color = AdminDesign.OnSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))

            if (response != null) {
                Button(
                    onClick = { response = null; query = "" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f), contentColor = AdminDesign.OnSurfaceVariant)
                ) {
                    Text("RESET TERMINAL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = AdminDesign.Surface,
                shadowElevation = 4.dp
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
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = AdminDesign.OnSurface,
                            unfocusedTextColor = AdminDesign.OnSurface
                        ),
                        textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface),
                        singleLine = true
                    )
                    
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp).size(24.dp), strokeWidth = 3.dp, color = AdminDesign.Primary)
                    } else {
                        IconButton(
                            onClick = { executeQuery(query) },
                            modifier = Modifier.background(AdminDesign.Primary, CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Ask", tint = Color.White)
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
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = AdminDesign.Primary.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold,
                color = AdminDesign.OnSurface
            )
        }
    }
}
