package com.example.daadi.ui.screens.admin



import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminFraudDetectionScreen(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val alerts by adminViewModel.analyticsRepository.fraudAlerts.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()

    AdminFoundationScaffold("Fraud Intelligence", supabaseManager, onBack) { padding ->
        if (isSyncing && alerts.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(6) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (alerts.isEmpty()) {
            AdminEmptyState(
                title = "No Fraud Detected", 
                description = "Algorithmic monitoring shows zero anomalous transaction or behavioral patterns."
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
            ) {
                item {
                    Text("BEHAVIORAL ANOMALY FEED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }

                items(alerts) { alert ->
                    FraudAlertCard(alert)
                }
            }
        }
    }
}

@Composable
fun FraudAlertCard(alert: com.example.daadi.data.supabase.SupabaseFraudAlert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (alert.confidence > 0.8) AdminDesign.Error.copy(alpha = 0.1f) else AdminDesign.Secondary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security, 
                            contentDescription = null, 
                            tint = if (alert.confidence > 0.8) AdminDesign.Error else AdminDesign.Secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(alert.type.replace("_", " ").uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = AdminDesign.OnSurface)
                    Text(
                        text = "UID_POINTER: ${alert.userId}", 
                        fontSize = 10.sp, 
                        color = AdminDesign.OnSurfaceVariant,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${(alert.confidence * 100).toInt()}%", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 18.sp, 
                        color = if (alert.confidence > 0.8) AdminDesign.Error else AdminDesign.Secondary
                    )
                    Text("PROBABILITY", fontSize = 8.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AdminDesign.Background,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Automated heuristic analysis detected ${alert.type.replace("_", " ")} patterns. Confidence level exceeds safety thresholds.", 
                    fontSize = 11.sp, 
                    color = AdminDesign.OnSurfaceVariant,
                    modifier = Modifier.padding(AdminDesign.SpacingSmall),
                    lineHeight = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { /* Dismiss */ }) { 
                    Text("DISMISS ALERT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurfaceVariant) 
                }
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                Button(
                    onClick = { /* Ban User */ }, 
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Error),
                    shape = AdminDesign.ButtonShape
                ) {
                    Icon(Icons.Default.GppBad, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Text("TERMINATE USER", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
