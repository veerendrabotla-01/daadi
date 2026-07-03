package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


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
fun AdminBIAnalyticsScreen(supabaseManager: SupabaseManager, onBack: () -> Unit) {
    val biMetrics by supabaseManager.biMetrics.collectAsStateWithLifecycle()
    val financeReports by supabaseManager.financeReports.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()
    
    val latestMetrics = biMetrics.firstOrNull()
    val latestFinance = financeReports.firstOrNull()

    AdminFoundationScaffold("Market Intelligence", supabaseManager, onBack) { padding ->
        if (isSyncing && biMetrics.isEmpty()) {
            AdminLoadingScreen()
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
            ) {
                item {
                    Text("ACTIVE USER TRAJECTORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                        MetricMiniCard("DAU", latestMetrics?.dau?.toString() ?: "0", AdminDesign.Primary, Modifier.weight(1f))
                        MetricMiniCard("WAU", latestMetrics?.wau?.toString() ?: "0", AdminDesign.Secondary, Modifier.weight(1f))
                        MetricMiniCard("MAU", latestMetrics?.mau?.toString() ?: "0", AdminDesign.Tertiary, Modifier.weight(1f))
                    }
                }

                item {
                    Text("RETENTION COHORTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                        shape = AdminDesign.CardShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                    ) {
                        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                            RetentionRow("Day 1 Retention", (latestMetrics?.retentionD1 ?: 0.0))
                            HorizontalDivider(modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall), color = AdminDesign.OnSurface.copy(alpha = 0.05f))
                            RetentionRow("Day 7 Retention", (latestMetrics?.retentionD7 ?: 0.0))
                            HorizontalDivider(modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall), color = AdminDesign.OnSurface.copy(alpha = 0.05f))
                            RetentionRow("Day 30 Retention", (latestMetrics?.retentionD30 ?: 0.0))
                            HorizontalDivider(modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall), color = AdminDesign.OnSurface.copy(alpha = 0.05f))
                            RetentionRow("Churn Probability", (latestMetrics?.churnRate ?: 0.0), isNegative = true)
                        }
                    }
                }

                item {
                    Text("UNIT ECONOMICS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                        MetricMiniCard("ARPU", "$${String.format("%.2f", latestMetrics?.arpu ?: 0.0)}", AdminDesign.Primary, Modifier.weight(1f))
                        MetricMiniCard("ARPPU", "$${String.format("%.2f", latestMetrics?.arppu ?: 0.0)}", AdminDesign.Secondary, Modifier.weight(1f))
                    }
                }

                item {
                    latestFinance?.let { report ->
                        FinanceSnapshotCard(report)
                    }
                }

                item {
                    Text("GEOSPATIAL DISTRIBUTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    DistributionSection("Top Regions by Engagement", latestMetrics?.countryDistribution ?: emptyMap())
                }
            }
        }
    }
}

@Composable
fun MetricMiniCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Text(label, fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Black)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun RetentionRow(label: String, value: Double, isNegative: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface)
        Surface(
            color = (if (isNegative) AdminDesign.Error else AdminDesign.Success).copy(alpha = 0.1f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "${String.format("%.1f", value * 100)}%", 
                fontWeight = FontWeight.Black, 
                fontSize = 12.sp,
                color = if (isNegative) AdminDesign.Error else AdminDesign.Success,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun FinanceSnapshotCard(report: com.example.daadi.data.supabase.SupabaseFinanceReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Primary)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Text("FINANCIAL VECTOR", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text("Total Yield: $${String.format("%.2f", report.revenue)}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FinanceMetric("Ads Yield", report.ads)
                FinanceMetric("Store Yield", report.purchases)
                FinanceMetric("Reversals", report.refunds, isNegative = true)
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(AdminDesign.SpacingSmall), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Text("Forecasted trajectory: $${String.format("%.2f", report.forecastNextMonth)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FinanceMetric(label: String, value: Double, isNegative: Boolean = false) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("${if (isNegative) "-" else ""}$${String.format("%.2f", value)}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
fun DistributionSection(title: String, data: Map<String, Int>) {
    Card(
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AdminDesign.OnSurface)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            data.entries.sortedByDescending { it.value }.take(5).forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall)) {
                    Text(entry.key, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface)
                    Text(entry.value.toString(), fontWeight = FontWeight.Black, fontSize = 12.sp, color = AdminDesign.Primary)
                }
                if (data.entries.toList().indexOf(entry) < 4) {
                    HorizontalDivider(color = AdminDesign.OnSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}
