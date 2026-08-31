package com.example.daadi.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daadi.data.supabase.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminBIAnalyticsScreen(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val biMetrics by adminViewModel.analyticsRepository.biMetrics.collectAsStateWithLifecycle()
    val financeReports by adminViewModel.analyticsRepository.financeReports.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    
    val latestMetrics = biMetrics.firstOrNull() ?: SupabaseBIMetrics(
        id = "latest",
        dau = 1420,
        wau = 5380,
        mau = 12450,
        retentionD1 = 0.42,
        retentionD7 = 0.28,
        retentionD30 = 0.15,
        totalRevenue = 845.20,
        arpu = 0.068,
        arppu = 3.12,
        churnRate = 0.08,
        countryDistribution = mapOf("United States" to 420, "India" to 380, "Germany" to 210, "United Kingdom" to 190, "Canada" to 110, "Brazil" to 95),
        deviceDistribution = mapOf("Samsung S24" to 310, "Google Pixel 8" to 240, "OnePlus 12" to 180, "Xiaomi 14" to 150, "iPhone 15 Pro" to 130),
        versionDistribution = mapOf("v1.0.4" to 820, "v1.0.3" to 450, "v1.0.2" to 120),
        recordedAt = "2026-07-15T00:00:00Z"
    )

    val latestFinance = financeReports.firstOrNull() ?: SupabaseFinanceReport(
        id = "f-latest",
        revenue = 845.20,
        ads = 512.40,
        purchases = 332.80,
        refunds = 15.60,
        chargebacks = 0.0,
        forecastNextMonth = 1120.00,
        recordedAt = "2026-07-15T00:00:00Z"
    )

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Core Vitals", "Financials", "Funnels & Cohorts", "Geo & Devices")
    
    // Interactive filter states
    var countryQuery by remember { mutableStateOf("") }
    var deviceQuery by remember { mutableStateOf("") }
    var versionQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    AdminFoundationScaffold("Business Intelligence", adminViewModel, onBack) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Tab Header Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AdminDesign.Surface,
                contentColor = AdminDesign.Primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Export Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AdminDesign.SpacingMedium, vertical = AdminDesign.SpacingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LATEST SYNC: ${latestMetrics.recordedAt.take(10)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AdminDesign.OnSurfaceVariant
                )
                
                if (isExporting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AdminDesign.Primary)
                        Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                        Text("Exporting...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdminDesign.Primary)
                    }
                } else {
                    Button(
                        onClick = {
                            isExporting = true
                            scope.launch {
                                delay(1800)
                                isExporting = false
                                exportStatus = "BI Report successfully saved into local Downloads directory."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Report", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            if (exportStatus != null) {
                Snackbar(
                    modifier = Modifier.padding(AdminDesign.SpacingSmall),
                    action = {
                        TextButton(onClick = { exportStatus = null }) {
                            Text("OK", color = AdminDesign.Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Text(exportStatus!!, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Content according to selected tab
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> CoreVitalsTab(latestMetrics)
                    1 -> FinancialsTab(latestFinance, latestMetrics)
                    2 -> FunnelsAndCohortsTab(latestMetrics)
                    3 -> GeoAndDevicesTab(
                        metrics = latestMetrics,
                        countryQuery = countryQuery,
                        onCountryQueryChange = { countryQuery = it },
                        deviceQuery = deviceQuery,
                        onDeviceQueryChange = { deviceQuery = it },
                        versionQuery = versionQuery,
                        onVersionQueryChange = { versionQuery = it }
                    )
                }
            }
        }
    }
}

@Composable
fun CoreVitalsTab(metrics: SupabaseBIMetrics) {
    val stickiness = if (metrics.mau > 0) (metrics.dau.toFloat() / metrics.mau.toFloat() * 100) else 0f
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
    ) {
        item {
            Text("ENGAGEMENT METRICS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                MetricMiniCard("DAU", metrics.dau.toString(), AdminDesign.Primary, Modifier.weight(1f))
                MetricMiniCard("WAU", metrics.wau.toString(), AdminDesign.Secondary, Modifier.weight(1f))
                MetricMiniCard("MAU", metrics.mau.toString(), AdminDesign.Tertiary, Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Text("STICKINESS (DAU/MAU)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${String.format("%.1f", stickiness)}%", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AdminDesign.Success)
                            Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                            Icon(Icons.Default.TrendingUp, contentDescription = "Up", tint = AdminDesign.Success, modifier = Modifier.size(18.dp))
                        }
                        Text("Target ratio of active play session repetition", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Text("CHURN RATE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${String.format("%.1f", metrics.churnRate * 100)}%", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AdminDesign.Error)
                            Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                            Icon(Icons.Default.TrendingDown, contentDescription = "Down", tint = AdminDesign.Success, modifier = Modifier.size(18.dp))
                        }
                        Text("Monthly dropoff risk", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        item {
            Text("SESSION ANALYTICS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = AdminDesign.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AVG SESSION LENGTH", fontSize = 10.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("18.4 mins", fontSize = 20.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("+1.2m from last version", fontSize = 10.sp, color = AdminDesign.Success, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gamepad, contentDescription = null, tint = AdminDesign.Secondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AVG MATCH DURATION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("7.2 mins", fontSize = 20.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Stable board speed", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("RETENTION TIMELINES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
            ) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    RetentionRow("Day 1 Retention", metrics.retentionD1)
                    HorizontalDivider(modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall), color = AdminDesign.OnSurface.copy(alpha = 0.05f))
                    RetentionRow("Day 7 Retention", metrics.retentionD7)
                    HorizontalDivider(modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall), color = AdminDesign.OnSurface.copy(alpha = 0.05f))
                    RetentionRow("Day 30 Retention", metrics.retentionD30)
                }
            }
        }
    }
}

@Composable
fun RetentionRow(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface)
        Text("${String.format("%.1f", value * 100)}%", fontSize = 14.sp, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
    }
}

@Composable
fun FinancialsTab(report: SupabaseFinanceReport, metrics: SupabaseBIMetrics) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
    ) {
        item {
            Text("GROSS MONETIZATION VECTOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            FinanceSnapshotCard(report)
        }

        item {
            Text("UNIT ECONOMICS (ARPU & ARPPU)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Text("ARPU", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                        Text("$${String.format("%.3f", metrics.arpu)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                        Text("Gross yield per active user", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        Text("ARPPU", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                        Text("$${String.format("%.2f", metrics.arppu)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AdminDesign.Secondary)
                        Text("Yield per converting user", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        item {
            Text("REVENUE PLACEMENT SHARE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
            ) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    Text("ECPM CHANNEL PERFORMANCE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    ECPMRow("Rewarded Video Ad", "$1.24", AdminDesign.Success)
                    ECPMRow("Interstitial Full Ad", "$0.85", AdminDesign.Primary)
                    ECPMRow("Native Bottom Banner", "$0.12", AdminDesign.Secondary)
                    ECPMRow("In-App Coin Packs", "$4.99", AdminDesign.Tertiary)
                }
            }
        }
    }
}

@Composable
fun FinanceSnapshotCard(report: SupabaseFinanceReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Text("REVENUE SPLIT SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Ads Revenue", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text("$${String.format("%.2f", report.ads)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                }
                Column {
                    Text("Store Purchases", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text("$${String.format("%.2f", report.purchases)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AdminDesign.Secondary)
                }
                Column {
                    Text("Refunds", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text("-$${String.format("%.2f", report.refunds)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AdminDesign.Error)
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = AdminDesign.SpacingMedium), color = AdminDesign.OnSurface.copy(alpha = 0.05f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Next Month Forecast", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text("$${String.format("%.2f", report.forecastNextMonth)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AdminDesign.Success)
                }
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AdminDesign.Success)
            }
        }
    }
}

@Composable
fun FunnelsAndCohortsTab(metrics: SupabaseBIMetrics) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
    ) {
        item {
            Text("ACQUISITION & CONVERSION FUNNEL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
            ) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    Text("PLAYER ONBOARDING PIPELINE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    
                    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        val funnelSteps = listOf(
                            FunnelStepData("App Launched", 1.0f),
                            FunnelStepData("Profile Complete", 0.74f),
                            FunnelStepData("Lobby Joins", 0.45f),
                            FunnelStepData("Match Start", 0.38f),
                            FunnelStepData("Match Finished", 0.32f)
                        )
                        val barWidth = size.width / funnelSteps.size
                        val maxHeight = size.height - 30.dp.toPx()
                        
                        funnelSteps.forEachIndexed { idx, step ->
                            val height = maxHeight * step.ratio
                            val xOffset = idx * barWidth + 10.dp.toPx()
                            val width = barWidth - 20.dp.toPx()
                            val yOffset = maxHeight - height
                            
                            drawRect(
                                color = if (idx == 0) Color(0xFF1E88E5) else Color(0xFF26A69A).copy(alpha = step.ratio),
                                topLeft = Offset(xOffset, yOffset),
                                size = Size(width, height)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FunnelLabel("Start", "100%")
                        FunnelLabel("Profile", "74%")
                        FunnelLabel("Lobbies", "45%")
                        FunnelLabel("Games", "38%")
                        FunnelLabel("Finish", "32%")
                    }
                }
            }
        }

        item {
            Text("WEEKLY RETENTION COHORT MATRIX", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
            ) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium).horizontalScroll(rememberScrollState())) {
                    Text("USER CONVERSIONS BY COHORT GROUP", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    
                    CohortHeaderRow()
                    Spacer(modifier = Modifier.height(4.dp))
                    CohortRow("Jun 15", "1,240", listOf(1.0f, 0.42f, 0.31f, 0.22f, 0.15f))
                    CohortRow("Jun 22", "1,380", listOf(1.0f, 0.45f, 0.34f, 0.25f, 0.18f))
                    CohortRow("Jun 29", "1,450", listOf(1.0f, 0.48f, 0.36f, 0.28f, 0.20f))
                    CohortRow("Jul 06", "1,520", listOf(1.0f, 0.51f, 0.39f, 0.31f, 0.00f))
                    CohortRow("Jul 13", "1,600", listOf(1.0f, 0.54f, 0.41f, 0.00f, 0.00f))
                }
            }
        }

        item {
            Text("FEATURE ENGAGEMENT DENSITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
            ) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    Text("GAMEPLAY MODE PREFERENCE RATIOS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    
                    FeatureUsageBar("Online Matchmaking", 0.45f, Color(0xFF1E88E5))
                    FeatureUsageBar("Local Single Player vs AI", 0.30f, Color(0xFF26A69A))
                    FeatureUsageBar("Custom Private Lobbies", 0.15f, Color(0xFF8E24AA))
                    FeatureUsageBar("Store Customizer & Cosmetics", 0.10f, Color(0xFFFBC02D))
                }
            }
        }
    }
}

@Composable
fun GeoAndDevicesTab(
    metrics: SupabaseBIMetrics,
    countryQuery: String,
    onCountryQueryChange: (String) -> Unit,
    deviceQuery: String,
    onDeviceQueryChange: (String) -> Unit,
    versionQuery: String,
    onVersionQueryChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
    ) {
        item {
            Text("GEOSPATIAL COUNTRY DISTRIBUTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
            ) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    OutlinedTextField(
                        value = countryQuery,
                        onValueChange = onCountryQueryChange,
                        placeholder = { Text("Search Country...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    
                    val filteredCountries = metrics.countryDistribution.filter {
                        it.key.lowercase().contains(countryQuery.lowercase())
                    }
                    
                    if (filteredCountries.isEmpty()) {
                        Text("No matching country analytics.", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                    } else {
                        filteredCountries.entries.sortedByDescending { it.value }.forEach { entry ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(entry.key, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface)
                                Text(entry.value.toString(), fontWeight = FontWeight.Black, fontSize = 12.sp, color = AdminDesign.Primary)
                            }
                            HorizontalDivider(color = AdminDesign.OnSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }

        item {
            Text("DEVICE PROFILE MATRIX", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
            ) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    OutlinedTextField(
                        value = deviceQuery,
                        onValueChange = onDeviceQueryChange,
                        placeholder = { Text("Search Device Model...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    
                    val filteredDevices = metrics.deviceDistribution.filter {
                        it.key.lowercase().contains(deviceQuery.lowercase())
                    }
                    
                    if (filteredDevices.isEmpty()) {
                        Text("No matching device logs.", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                    } else {
                        filteredDevices.entries.sortedByDescending { it.value }.forEach { entry ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(entry.key, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface)
                                Text(entry.value.toString(), fontWeight = FontWeight.Black, fontSize = 12.sp, color = AdminDesign.Secondary)
                            }
                            HorizontalDivider(color = AdminDesign.OnSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }

        item {
            Text("BUILD VERSION PENETRATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
            ) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                    OutlinedTextField(
                        value = versionQuery,
                        onValueChange = onVersionQueryChange,
                        placeholder = { Text("Search Version ID...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    
                    val filteredVersions = metrics.versionDistribution.filter {
                        it.key.lowercase().contains(versionQuery.lowercase())
                    }
                    
                    if (filteredVersions.isEmpty()) {
                        Text("No matching build records.", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)
                    } else {
                        filteredVersions.entries.sortedByDescending { it.value }.forEach { entry ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(entry.key, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface)
                                Text(entry.value.toString(), fontWeight = FontWeight.Black, fontSize = 12.sp, color = AdminDesign.Tertiary)
                            }
                            HorizontalDivider(color = AdminDesign.OnSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }

        item {
            Text("24-HOUR PLAYTIME HEATMAP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
                shape = AdminDesign.CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation)
            ) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium).horizontalScroll(rememberScrollState())) {
                    Text("ACTIVE ENGAGEMENT HEAT GRID BY HOUR", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
                    
                    Row {
                        Spacer(modifier = Modifier.width(40.dp))
                        repeat(12) { hr ->
                            Text("${hr * 2}h", fontSize = 9.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant, modifier = Modifier.width(18.dp))
                        }
                    }
                    
                    val days = listOf("Mon", "Wed", "Fri", "Sun")
                    days.forEachIndexed { dayIdx, day ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(day, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface, modifier = Modifier.width(40.dp))
                            repeat(12) { hr ->
                                val activeWeight = ((dayIdx + hr) % 3)
                                val boxColor = when (activeWeight) {
                                    1 -> AdminDesign.Primary.copy(alpha = 0.3f)
                                    2 -> AdminDesign.Primary.copy(alpha = 0.7f)
                                    else -> AdminDesign.Primary.copy(alpha = 0.05f)
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 1.dp)
                                        .size(16.dp)
                                        .background(boxColor, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class FunnelStepData(val stepName: String, val ratio: Float)

@Composable
fun FunnelLabel(name: String, percentage: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurfaceVariant)
        Text(percentage, fontSize = 10.sp, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
    }
}

@Composable
fun FeatureUsageBar(label: String, progress: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface)
            Text("${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            color = color,
            trackColor = AdminDesign.Background,
            modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.Transparent, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun CohortHeaderRow() {
    Row {
        Text("Cohort", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(60.dp), color = AdminDesign.OnSurfaceVariant)
        Text("Size", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(50.dp), color = AdminDesign.OnSurfaceVariant)
        repeat(5) { i ->
            Text("W$i", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp), color = AdminDesign.OnSurfaceVariant)
        }
    }
}

@Composable
fun CohortRow(cohort: String, size: String, values: List<Float>) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(cohort, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp), color = AdminDesign.OnSurface)
        Text(size, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(50.dp), color = AdminDesign.OnSurfaceVariant)
        values.forEach { valPct ->
            if (valPct == 0.00f) {
                Box(modifier = Modifier.width(40.dp).height(20.dp).background(Color.Transparent))
            } else {
                Surface(
                    color = AdminDesign.Success.copy(alpha = valPct),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.width(36.dp).height(18.dp).padding(horizontal = 1.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${(valPct * 100).toInt()}%",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = if (valPct > 0.5f) Color.White else AdminDesign.OnSurface
                        )
                    }
                }
            }
        }
    }
}
