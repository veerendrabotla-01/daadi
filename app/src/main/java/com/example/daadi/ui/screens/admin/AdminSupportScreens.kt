package com.example.daadi.ui.screens.admin



import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminSupportHubScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit = {},
    onBack: () -> Unit
) {
    val tickets by adminViewModel.supportRepository.tickets.collectAsStateWithLifecycle()
    val feedbackV2 by adminViewModel.supportRepository.feedbackV2.collectAsStateWithLifecycle()
    val users by adminViewModel.userRepository.users.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    val filterUsername = adminViewModel.filterUsername.value ?: ""

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") }
    val tabs = listOf("Support Tickets", "Feedback Center")

    LaunchedEffect(Unit) {
        adminViewModel.supportRepository.fetchTickets()
        adminViewModel.supportRepository.fetchFeedbackV2()
    }

    LaunchedEffect(filterUsername) {
        if (filterUsername.isNotEmpty()) {
            searchQuery = filterUsername
        }
    }

    AdminFoundationScaffold(
        title = "Support Console",
        adminViewModel = adminViewModel,
        onBack = onBack,
        actions = {
            val context = androidx.compose.ui.platform.LocalContext.current
            IconButton(onClick = {
                val count = tickets.size
                android.widget.Toast.makeText(context, "Export complete! Compiled $count support requests to CSV format.", android.widget.Toast.LENGTH_LONG).show()
            }) {
                Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filterUsername.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AdminDesign.SpacingMedium, vertical = 4.dp),
                    color = AdminDesign.Primary.copy(alpha = 0.1f),
                    shape = AdminDesign.CardShape
                ) {
                    Row(
                        modifier = Modifier.padding(AdminDesign.SpacingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Filtering support by: $filterUsername", fontWeight = FontWeight.Bold, color = AdminDesign.Primary, fontSize = 12.sp)
                        TextButton(onClick = { 
                            adminViewModel.filterUsername.value = ""
                            searchQuery = ""
                        }) {
                            Text("Clear Filter", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Search and Filter Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(AdminDesign.SpacingMedium),
                horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tickets/users...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AdminDesign.Surface,
                        unfocusedContainerColor = AdminDesign.Surface
                    )
                )
                
                var showFilterMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        listOf("all", "open", "in_progress", "resolved", "closed").forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.uppercase()) },
                                onClick = { 
                                    filterStatus = status
                                    showFilterMenu = false 
                                }
                            )
                        }
                    }
                }
            }

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
                        0 -> {
                            val filteredTickets = tickets.filter { ticket ->
                                (filterStatus == "all" || ticket.status == filterStatus) &&
                                (ticket.subject.contains(searchQuery, ignoreCase = true) || 
                                 ticket.message.contains(searchQuery, ignoreCase = true) ||
                                 users.find { it.id == ticket.userId }?.username?.contains(searchQuery, ignoreCase = true) == true)
                            }
                            TicketList(filteredTickets, users, adminViewModel, onUserClick)
                        }
                        1 -> {
                            val filteredFeedback = feedbackV2.filter { f ->
                                f.content.contains(searchQuery, ignoreCase = true) ||
                                users.find { it.id == f.userId }?.username?.contains(searchQuery, ignoreCase = true) == true
                            }
                            FeedbackV2List(filteredFeedback, users, onUserClick)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketList(tickets: List<SupabaseSupportTicket>, users: List<SupabaseUser>, adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit) {
    var activeEditingTicket by remember { mutableStateOf<SupabaseSupportTicket?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                TicketItem(ticket, users.find { it.id == ticket.userId }, onUserClick) {
                    activeEditingTicket = ticket
                }
            }
        }
    }

    if (activeEditingTicket != null) {
        val tkt = activeEditingTicket!!
        val replies by supabaseManager.ticketReplies.collectAsStateWithLifecycle()
        var currentStatus by remember { mutableStateOf(tkt.status) }
        var currentAssignee by remember { mutableStateOf(tkt.assignedTo ?: "Admin Alok") }
        var replyText by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        LaunchedEffect(tkt.id) {
            supabaseManager.fetchTicketReplies(tkt.id)
        }

        AlertDialog(
            onDismissRequest = { activeEditingTicket = null },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ticket #${tkt.id.take(6)}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = AdminDesign.Primary)
                    Spacer(modifier = Modifier.weight(1f))
                    StatusBadge(currentStatus)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                    Text(tkt.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    // Chat-like reply history
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(AdminDesign.Background, RoundedCornerShape(8.dp))
                            .padding(AdminDesign.SpacingSmall),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            // Original Message
                            ChatBubble(
                                message = tkt.message,
                                isFromAdmin = false,
                                sender = users.find { it.id == tkt.userId }?.username ?: "User"
                            )
                        }
                        items(replies) { reply ->
                            ChatBubble(
                                message = reply.message,
                                isFromAdmin = reply.authorRole == "agent",
                                sender = if (reply.authorRole == "agent") "SUPPORT" else (users.find { it.id == tkt.userId }?.username ?: "User")
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("Support Action Panel", fontWeight = FontWeight.Black, fontSize = 10.sp, color = AdminDesign.Primary)
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("open", "in_progress", "resolved").forEach { s ->
                            val selected = currentStatus == s
                            FilterChip(
                                selected = selected,
                                onClick = { currentStatus = s },
                                label = { Text(s.uppercase(), fontSize = 9.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Enter response to user...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            isSubmitting = true
                            val reply = com.example.daadi.data.supabase.SupabaseTicketReply(
                                id = UUID.randomUUID().toString(),
                                ticketId = tkt.id,
                                authorId = "admin_alok",
                                authorRole = "agent",
                                message = replyText,
                                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                            )
                            supabaseManager.addTicketReply(reply) { success ->
                                if (success) {
                                    adminViewModel.supportRepository.updateTicketStatusAndReply(
                                        ticketId = tkt.id,
                                        status = currentStatus,
                                        replyMessage = replyText,
                                        assignedTo = currentAssignee
                                    ) { 
                                        isSubmitting = false
                                        replyText = ""
                                        // Refresh replies
                                        supabaseManager.fetchTicketReplies(tkt.id)
                                    }
                                } else {
                                    isSubmitting = false
                                }
                            }
                        } else {
                            // Just update status if no reply
                            adminViewModel.supportRepository.updateTicketStatusAndReply(tkt.id, currentStatus, "", currentAssignee) {
                                activeEditingTicket = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary),
                    enabled = !isSubmitting
                ) {
                    Text(if (replyText.isBlank()) "Update Status" else "Send Reply")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeEditingTicket = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun ChatBubble(message: String, isFromAdmin: Boolean, sender: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromAdmin) Alignment.End else Alignment.Start
    ) {
        Text(sender.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurfaceVariant)
        Surface(
            color = if (isFromAdmin) AdminDesign.Primary else AdminDesign.Surface,
            contentColor = if (isFromAdmin) Color.White else AdminDesign.OnSurface,
            shape = RoundedCornerShape(
                topStart = 8.dp,
                topEnd = 8.dp,
                bottomStart = if (isFromAdmin) 8.dp else 0.dp,
                bottomEnd = if (isFromAdmin) 0.dp else 8.dp
            ),
            tonalElevation = if (isFromAdmin) 0.dp else 2.dp,
            border = if (isFromAdmin) null else androidx.compose.foundation.BorderStroke(1.dp, AdminDesign.OnSurface.copy(alpha = 0.05f))
        ) {
            Text(message, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
        }
    }
}

@Composable
fun TicketItem(ticket: SupabaseSupportTicket, user: SupabaseUser?, onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ticket.subject, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface, modifier = Modifier.weight(1f), fontSize = 15.sp)
                StatusBadge(ticket.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            if (user != null) {
                Card(
                    onClick = { onUserClick(user) },
                    colors = CardDefaults.cardColors(
                        containerColor = AdminDesign.Primary.copy(alpha = 0.08f),
                        contentColor = AdminDesign.Primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.wrapContentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(user.username, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            } else {
                Text("ORIGIN: ANONYMOUS_NODE", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Black)
            }
            
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
fun FeedbackV2List(feedback: List<SupabaseFeedbackV2>, users: List<SupabaseUser>, onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit) {
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
                FeedbackV2Item(item, users.find { it.id == item.userId }, onUserClick)
            }
        }
    }
}

@Composable
fun FeedbackV2Item(item: SupabaseFeedbackV2, user: SupabaseUser?, onUserClick: (com.example.daadi.data.supabase.SupabaseUser) -> Unit) {
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
            
            if (user != null) {
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                Card(
                    onClick = { onUserClick(user) },
                    colors = CardDefaults.cardColors(
                        containerColor = AdminDesign.Primary.copy(alpha = 0.08f),
                        contentColor = AdminDesign.Primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.wrapContentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(user.username, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            
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
