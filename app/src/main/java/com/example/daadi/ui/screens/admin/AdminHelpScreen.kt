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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminHelpScreen(onBack: () -> Unit) {
    var selectedItem by remember { mutableStateOf<HelpItem?>(null) }
    val currentItem = selectedItem

    AdminFoundationScaffold(
        title = if (currentItem == null) "Help & Documentation" else currentItem.title,
        onBack = { if (selectedItem == null) onBack() else selectedItem = null }
    ) { padding ->
        if (currentItem == null) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
            ) {
                item {
                    FoundersCard()
                }

                item {
                    Text("ADMIN CONSOLE GUIDE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
                }

                items(adminHelpItems) { item ->
                    HelpSectionCard(item, onClick = { selectedItem = item })
                }
                
                item {
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
                    Text(
                        "© 2026 DAADI Enterprise. All rights reserved.\nDesigned for high-scale gaming operations.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdminDesign.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            HelpDetailView(currentItem, padding)
        }
    }
}

@Composable
fun HelpDetailView(item: HelpItem, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.padding(padding).fillMaxSize(),
        contentPadding = PaddingValues(AdminDesign.SpacingLarge),
        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
    ) {
        item {
            Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Text(item.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(item.description, style = MaterialTheme.typography.bodyLarge, color = AdminDesign.OnSurfaceVariant)
            Divider(modifier = Modifier.padding(vertical = AdminDesign.SpacingLarge))
        }

        item {
            Text("USAGE GUIDE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Text(item.usageGuide, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
        }

        item {
            Text("MANAGEMENT & CONTROLS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.Primary)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item.controls.forEach { control ->
                    Row {
                        Text("• ", fontWeight = FontWeight.Bold, color = AdminDesign.Primary)
                        Text(control, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = item.color.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = item.color)
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                    Text("Pro Tip: ${item.proTip}", style = MaterialTheme.typography.bodySmall, color = item.color, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FoundersCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingLarge), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Stars, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Text("DAADI FOUNDERS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Text(
                "This application is founded and developed by a dedicated team committed to providing the best traditional gaming experience.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FounderItem("Botla Veerendra", "veerendrabotla@gmail.com")
                FounderItem("Macha Praveen", "praveenmacha777@gmail.com")
            }
        }
    }
}

@Composable
fun FounderItem(name: String, email: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(name.take(1), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
        Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        Text(email, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun HelpSectionCard(item: HelpItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = AdminDesign.CardShape,
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = item.color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AdminDesign.OnSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = AdminDesign.OnSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdminDesign.OnSurfaceVariant)
        }
    }
}

data class HelpItem(
    val title: String, 
    val description: String, 
    val icon: ImageVector, 
    val color: Color,
    val usageGuide: String,
    val controls: List<String>,
    val proTip: String
)

val adminHelpItems = listOf(
    HelpItem(
        "User Directory",
        "Manage all registered players and roles. You can search for users, view their stats, and manage their account status (Ban/Unban).",
        Icons.Default.People,
        AdminDesign.Primary,
        "The User Directory is the heart of player management. Use the search bar to find users by username or email. Toggling 'Bots' allows you to filter out system-generated test profiles.",
        listOf(
            "Account Status: Ban or Unban users instantly from the profile details.",
            "Verification: Manually verify trusted players to give them 'Verified' status.",
            "Statistics: View total games, wins, and losses for every player.",
            "Shadow Banning: Discreetly restrict problematic users without notifying them."
        ),
        "Use the 'Export' feature in the dashboard to get a full list of user IDs for external analysis."
    ),
    HelpItem(
        "Admin Matrix",
        "Configure Role-Based Access Control (RBAC). Define which roles have access to specific system permissions.",
        Icons.Default.Security,
        Color(0xFFC2185B),
        "This screen manages the security hierarchy of DAADI. It uses a grid system where you can toggle specific permissions for different system roles (Admin, Player, PublicUser).",
        listOf(
            "Role Creation: Add new administrative levels as your team grows.",
            "Permission Seeding: Use the 'Seed Data' button if your database is fresh.",
            "Real-time Updates: Changes to permissions take effect on the next user login.",
            "Granular Control: Toggle access for everything from 'Match Control' to 'BI Analytics'."
        ),
        "Keep the 'PublicUser' role strictly limited to casual game play to ensure system security."
    ),
    HelpItem(
        "Match Control",
        "Monitor live game lobbies. View active matches, moves count, and terminate problematic sessions.",
        Icons.Default.PlayArrow,
        AdminDesign.Secondary,
        "Real-time monitoring of all active gaming sessions. This screen provides visibility into game health and player interactions.",
        listOf(
            "Live Feed: View a list of all matches currently in 'playing' or 'waiting' status.",
            "Session Termination: Forcefully end matches that are stuck or involve cheating.",
            "Turn Tracking: See how many moves have been made in a session.",
            "Player Attribution: Click a match to see which users are involved."
        ),
        "Frequent 'Waiting' matches might indicate a need to adjust matchmaking parameters in Remote Config."
    ),
    HelpItem(
        "Remote Config",
        "Adjust game engine variables in real-time. Manage system-wide toggles like maintenance mode.",
        Icons.Default.Settings,
        Color(0xFF5C2D0A),
        "Control the behavior of the application without releasing a new version. These variables are fetched by all clients on startup.",
        listOf(
            "Maintenance Mode: Toggle a system-wide block for scheduled updates.",
            "Registration Status: Enable or disable new user sign-ups.",
            "Game Logic: Tweak variables like move timers and reward multipliers.",
            "Content Toggles: Enable or disable specific features (like Spin Wheel) on the fly."
        ),
        "Always test Remote Config changes in a staging environment before pushing to global users."
    ),
    HelpItem(
        "Audit Trail",
        "The security log of the system. Tracks all administrative actions, showing who changed what, when, and why.",
        Icons.Default.History,
        Color(0xFF455A64),
        "Transparency and accountability for all admin actions. Every change in the admin console is recorded here for security review.",
        listOf(
            "Action History: A chronological list of all administrative modifications.",
            "Actor Identification: See which admin user performed each specific action.",
            "Target Tracking: Identifies which user or system entity was modified.",
            "Security Filtering: Filter logs to find suspicious administrative activity."
        ),
        "Review Audit Logs weekly to ensure that administrative permissions are being used appropriately."
    ),
    HelpItem(
        "Task Scheduler",
        "Manage automated background tasks and cron jobs for system maintenance.",
        Icons.Default.Schedule,
        Color(0xFFE65100),
        "Automate repetitive system tasks. This screen allows you to monitor and trigger background processes.",
        listOf(
            "Cron Jobs: Configure recurring tasks like 'Daily Reward Reset'.",
            "Manual Override: Trigger a scheduled task immediately for testing.",
            "Success Metrics: View the last run time and success status of every task.",
            "Task Logs: Inspect detailed execution logs for failed background jobs."
        ),
        "Scheduled tasks are critical for economy stability; ensure 'Wallet Sync' is always running."
    ),
    HelpItem(
        "Economy & Rewards",
        "Control the game's economy. Manage transactions, daily rewards, and spin wheel probabilities.",
        Icons.Default.AccountBalanceWallet,
        AdminDesign.Secondary,
        "The financial heart of the game. Manage how currency flows in and out of the ecosystem.",
        listOf(
            "Transaction Ledger: A full history of all coin and XP movements.",
            "Daily Rewards: Configure the reward sequence for the 7-day login streak.",
            "Spin Wheel Logic: Adjust the weights and probabilities for the lucky wheel.",
            "Currency Adjustments: Manually add or remove coins for specific users (Support Tool)."
        ),
        "A high XP multiplier in LiveOps can significantly increase Daily Active Users (DAU)."
    ),
    HelpItem(
        "Store Management",
        "Configure In-App Purchases (IAP), bundles, and discount coupons for the game store.",
        Icons.Default.Storefront,
        AdminDesign.Primary,
        "Manage the commercial offerings of the game. Update products and marketing campaigns.",
        listOf(
            "IAP Configuration: Link store items to actual App Store/Play Store IDs.",
            "Bundles & Sales: Create discounted coin packs for limited-time offers.",
            "Coupon System: Generate and manage promotional codes for marketing.",
            "Inventory Control: Toggle visibility of items in the game store."
        ),
        "Use 'Limited Time' tags on store items to drive higher conversion rates."
    ),
    HelpItem(
        "Safety & Trust",
        "Review user reports and fraud alerts. Use bot detection metrics to maintain fairness.",
        Icons.Default.GppGood,
        AdminDesign.Error,
        "Maintain a healthy community. This screen consolidates all signals of bad behavior.",
        listOf(
            "Report Queue: Review and resolve user-submitted complaints about other players.",
            "Fraud Detection: View alerts for suspicious financial transactions.",
            "Bot Detection: Analyze player behavior patterns to flag automated accounts.",
            "Appeal Management: Review and process ban appeal requests from players."
        ),
        "Cross-reference Fraud Alerts with Audit Logs to identify coordinated malicious activity."
    ),
    HelpItem(
        "BI Analytics",
        "Enterprise-grade business intelligence. Track Daily Active Users (DAU) and revenue metrics.",
        Icons.Default.Analytics,
        AdminDesign.Primary,
        "Data-driven decision making. Monitor the health and growth of the DAADI ecosystem.",
        listOf(
            "Revenue Dashboard: Real-time tracking of total system income.",
            "Retention Charts: Visualize how many users return after 1, 7, and 30 days.",
            "Active Users: Monitor concurrent and daily player counts.",
            "Geographic Data: See where your players are located globally."
        ),
        "Spikes in DAU without a corresponding revenue increase may indicate a need for new monetization strategies."
    )
)
