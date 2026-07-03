package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminSupportHubScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val tickets by supabaseManager.tickets.collectAsStateWithLifecycle()
    val feedbackV2 by supabaseManager.feedbackV2.collectAsStateWithLifecycle()
    val users by supabaseManager.users.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Support Tickets", "Feedback Center")

    LaunchedEffect(Unit) {
        supabaseManager.fetchTickets()
        supabaseManager.fetchFeedbackV2()
    }

    AdminFoundationScaffold(
        title = "Support Console",
        supabaseManager = supabaseManager,
        onBack = onBack,
        actions = {
            IconButton(onClick = { /* Export CSV */ }) {
                Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AdminDesign.Primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isSyncing && tickets.isEmpty() && feedbackV2.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                        items(8) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                    }
                } else {
                    when (selectedTab) {
                        0 -> TicketList(tickets, users)
                        1 -> FeedbackV2List(feedbackV2, users)
                    }
                }
            }
        }
    }
}

@Composable
fun TicketList(tickets: List<SupabaseSupportTicket>, users: List<SupabaseUser>) {
    if (tickets.isEmpty()) {
        AdminEmptyState(
            title = "Queue Clear", 
            description = "No active support tickets pending intervention. All users are operating within normal parameters."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(AdminDesign.SpacingMedium), 
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            items(tickets) { ticket ->
                TicketItem(ticket, users.find { it.id == ticket.userId })
            }
        }
    }
}

@Composable
fun TicketItem(ticket: SupabaseSupportTicket, user: SupabaseUser?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ticket.subject, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface, modifier = Modifier.weight(1f), fontSize = 15.sp)
                StatusBadge(ticket.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("ORIGIN: ${user?.username ?: "ANONYMOUS_NODE"}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Black)
            
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = AdminDesign.SpacingSmall),
                color = AdminDesign.Background,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = ticket.message, 
                    fontSize = 12.sp, 
                    color = AdminDesign.OnSurface, 
                    modifier = Modifier.padding(AdminDesign.SpacingSmall),
                    lineHeight = 18.sp
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when(ticket.priority) {
                        "high", "urgent" -> AdminDesign.Error.copy(alpha = 0.1f)
                        else -> AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PriorityHigh, contentDescription = null, modifier = Modifier.size(10.dp), 
                            tint = if (ticket.priority == "high") AdminDesign.Error else AdminDesign.OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(ticket.priority.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, 
                            color = if (ticket.priority == "high") AdminDesign.Error else AdminDesign.OnSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("LOGGED: ${ticket.createdAt.take(10)}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun FeedbackV2List(feedback: List<SupabaseFeedbackV2>, users: List<SupabaseUser>) {
    if (feedback.isEmpty()) {
        AdminEmptyState(
            title = "No Feedback", 
            description = "Community sentiment data is currently unavailable. No feedback submissions captured in this window."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(AdminDesign.SpacingMedium), 
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
        ) {
            items(feedback) { item ->
                FeedbackV2Item(item, users.find { it.id == item.userId })
            }
        }
    }
}

@Composable
fun FeedbackV2Item(item: SupabaseFeedbackV2, user: SupabaseUser?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = AdminDesign.Secondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.category.uppercase(), 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.Black, 
                        color = AdminDesign.Secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                RatingStars(item.rating ?: 0)
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Text(item.content, fontSize = 13.sp, color = AdminDesign.OnSurface, lineHeight = 18.sp)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = AdminDesign.SpacingSmall), color = AdminDesign.OnSurface.copy(alpha = 0.05f))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SENTIMENT: ${item.sentiment?.uppercase() ?: "NEUTRAL"}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.weight(1f))
                if (item.status == "fixed" || item.status == "resolved") {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AdminDesign.Success, modifier = Modifier.size(16.dp))
                } else {
                    StatusBadge(item.status ?: "pending")
                }
            }
        }
    }
}

@Composable
fun RatingStars(rating: Int) {
    Row {
        repeat(5) { index ->
            Icon(
                imageVector = Icons.Default.Star, 
                contentDescription = null, 
                modifier = Modifier.size(14.dp), 
                tint = if (index < rating) AdminDesign.Secondary else AdminDesign.OnSurfaceVariant.copy(alpha = 0.2f)
            )
        }
    }
}
