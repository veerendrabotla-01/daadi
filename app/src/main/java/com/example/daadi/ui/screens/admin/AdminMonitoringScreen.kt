package com.example.daadi.ui.screens.admin



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.CircleShape
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
fun AdminMonitoringScreen(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val healthMetrics by adminViewModel.analyticsRepository.biHealthMetrics.collectAsStateWithLifecycle()
    val queueMetrics by adminViewModel.analyticsRepository.queueMetrics.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()

    AdminFoundationScaffold("Infrastructure Grid", supabaseManager, onBack) { padding ->
        if (isSyncing && healthMetrics.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(10) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
            ) {
                item {
                    Text("REAL-TIME CLUSTER HEALTH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }

                items(healthMetrics) { metric ->
                    HealthStatusCard(metric)
                }

                item {
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    Text("ASYNC PIPELINE MONITOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }

                items(queueMetrics) { queue ->
                    QueueMetricCard(queue)
                }
            }
        }
    }
}

@Composable
fun HealthStatusCard(metric: com.example.daadi.data.supabase.SupabaseBIHealthMetric) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(10.dp),
                shape = CircleShape,
                color = if (metric.status == "healthy") AdminDesign.Success else AdminDesign.Error
            ) {}
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(metric.serviceName.uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AdminDesign.OnSurface)
                Text(
                    text = "LATENCY: ${metric.latencyMs ?: 0}ms » LOAD: ${metric.cpuUsage ?: 0.0}%", 
                    fontSize = 10.sp, 
                    color = AdminDesign.OnSurfaceVariant,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = metric.status.uppercase(), 
                fontWeight = FontWeight.Black, 
                fontSize = 12.sp, 
                color = if (metric.status == "healthy") AdminDesign.Success else AdminDesign.Error
            )
        }
    }
}

@Composable
fun QueueMetricCard(queue: com.example.daadi.data.supabase.SupabaseQueueMetric) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.size(16.dp), tint = AdminDesign.Primary)
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                Text(queue.queueName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AdminDesign.OnSurface)
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("PENDING_OPS", queue.size.toString(), AdminDesign.OnSurface)
                MetricItem("RETRY_VECTORS", queue.retryCount.toString(), AdminDesign.Secondary)
                MetricItem("FAIL_SINK (DLQ)", queue.deadLetterCount.toString(), AdminDesign.Error)
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Black)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
    }
}
