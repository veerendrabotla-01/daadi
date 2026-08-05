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
        "The User Directory is the heart of player management. Use the search bar to find users by username or email. Toggling 'Bots' allows you to filter out system-generated profiles. These bots are essential for consistent matchmaking and ensuring a competitive environment is always available.",
        listOf(
            "Account Status: Ban or Unban users instantly from the profile details.",
            "Verification: Manually verify trusted players to give them 'Verified' status.",
            "Bot Management: Monitor system profiles to ensure matchmaking health.",
            "Statistics: View total games, wins, and losses for every player.",
            "Compliance: Clear labeling of system vs. human accounts for store verification."
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
    ),
    HelpItem(
        "Approvals",
        "Manage administrative workflow requests and structural system changes.",
        Icons.Default.FactCheck,
        Color(0xFF00695C),
        "This screen centralizes all actions that require multi-stage verification or administrative sign-off.",
        listOf(
            "Role Requests: Review and approve requests for elevated user permissions.",
            "Economy Adjustments: Large currency injections require supervisor approval.",
            "Content Moderation: Review flagged content that was auto-held for manual review.",
            "Audit Sign-off: Weekly administrative reviews of critical system logs."
        ),
        "Establish clear 'Terms of Approval' to ensure consistency across the moderation team."
    ),
    HelpItem(
        "Data Exports",
        "Extract system data for external analysis, reporting, or archival purposes.",
        Icons.Default.FileDownload,
        Color(0xFF455A64),
        "Generate and download snapshots of the system database in multiple formats (CSV, JSON, Parquet).",
        listOf(
            "Bulk Export: Select specific modules (Users, Matches, Economy) to export.",
            "Audit Trail: Every data export is logged with the admin ID and timestamp.",
            "Format Selection: Choose the most appropriate format for your analysis tools.",
            "Scheduled Exports: Configure automated weekly data backups to cloud storage."
        ),
        "Use Parquet format for extremely large datasets to maintain high query performance in BI tools."
    ),
    HelpItem(
        "Config Rollbacks",
        "Revert Remote Config changes to previous stable versions in case of errors.",
        Icons.Default.History,
        Color(0xFF5D4037),
        "Safety net for system configuration. Every change to Remote Config creates a versioned snapshot.",
        listOf(
            "Version History: View a chronological list of all config changes.",
            "Diff View: Compare current settings with any previous version.",
            "Instant Revert: Restore a known-good configuration with a single click.",
            "Snapshot Tags: Tag stable versions (e.g., 'v1.2-Stable') for easy identification."
        ),
        "Always tag a stable configuration before starting major live operations or events."
    ),
    HelpItem(
        "Reward Editor",
        "Configure daily rewards, spin wheel odds, and achievement milestones.",
        Icons.Default.Redeem,
        Color(0xFFC62828),
        "Manage the incentive structures that drive player retention and engagement.",
        listOf(
            "Login Sequence: Define the exact rewards for each day of the 7-day streak.",
            "Probability Matrix: Adjust the weights for different items on the Spin Wheel.",
            "Milestone Rewards: Set prizes for reaching specific level or win counts.",
            "Seasonal Adjustments: Boost reward values during holidays or special events."
        ),
        "Check the economy balance weekly to ensure rewards are not causing inflation."
    ),
    HelpItem(
        "Season Pass",
        "Manage Battle Pass tiers, seasonal themes, and premium reward tracks.",
        Icons.Default.ConfirmationNumber,
        Color(0xFFE65100),
        "Drive long-term engagement by structuring rewards into progressive seasonal tiers.",
        listOf(
            "Tier Configuration: Set XP requirements and rewards for every pass level.",
            "Premium Track: Manage the exclusive items available to paying pass holders.",
            "Season Scheduling: Define start and end dates for seasonal content cycles.",
            "Level Skips: Configure the cost and availability of tier skips for players."
        ),
        "Include high-value exclusive cosmetics in early tiers to drive early season pass adoption."
    ),
    HelpItem(
        "CMS Console",
        "Update patch notes, manage FAQs, and dispatch system-wide announcements.",
        Icons.Default.Article,
        Color(0xFF455A64),
        "The primary communication tool for reaching your entire player base directly in the app.",
        listOf(
            "Patch Notes: Draft and publish detailed logs of game updates and fixes.",
            "In-Game Mail: Send targeted notifications to specific segments of users.",
            "Help Center: Maintain and update the player-facing FAQ and support guides.",
            "Urgent Bulletins: Display critical alerts (like maintenance) on the home screen."
        ),
        "Use rich formatting and clear headings in patch notes to improve readability for players."
    ),
    HelpItem(
        "AI Assistant",
        "Predictive analytics and automated insights powered by the Chanakya engine.",
        Icons.Default.AutoAwesome,
        Color(0xFF7B1FA2),
        "Leverage machine learning to identify trends, predict churn, and optimize game balance.",
        listOf(
            "Churn Prediction: Identify users likely to stop playing before they leave.",
            "Anomaly Detection: Auto-flag unusual spikes in revenue or match activity.",
            "Balance Suggestions: Get AI-driven recommendations for reward multipliers.",
            "Automated Insights: Daily summaries of key system performance indicators."
        ),
        "Act on churn predictions early by sending targeted retention rewards via the CMS mail."
    ),
    HelpItem(
        "System Health",
        "Monitor server latency, database performance, and API uptime in real-time.",
        Icons.Default.HealthAndSafety,
        AdminDesign.Secondary,
        "The dashboard for ensuring technical stability and high availability of the game services.",
        listOf(
            "Latency Tracking: Monitor real-time response times for critical API endpoints.",
            "Resource Usage: Track CPU, Memory, and Storage utilization for backend services.",
            "Error Rates: View the percentage of failed requests in the last hour.",
            "Service Status: Real-time health indicators for Supabase, OkHttp, and CDNs."
        ),
        "Set up external alerts to notify the dev team if the error rate exceeds 1%."
    ),
    HelpItem(
        "Crash Center",
        "Centralized repository for unhandled exceptions, ANRs, and client-side crashes.",
        Icons.Default.BugReport,
        AdminDesign.Error,
        "Technical diagnostic tool for identifying and resolving bugs affecting your users.",
        listOf(
            "Stack Traces: View detailed code-level logs for every reported application crash.",
            "Affected Users: See which specific devices and OS versions are experiencing issues.",
            "Grouping: Automatically cluster similar crashes into high-priority issues.",
            "Resolution Tracking: Mark bugs as 'In Progress', 'Fixed', or 'Ignored'."
        ),
        "Prioritize fixing crashes that affect more than 5% of your Daily Active Users."
    ),
    HelpItem(
        "Device Center",
        "Monitor registered hardware, handle device bans, and track multi-accounting.",
        Icons.Default.Smartphone,
        AdminDesign.Primary,
        "Security tool for managing the physical devices used to access the game.",
        listOf(
            "Hardware Fingerprinting: Identify unique device signatures to prevent fraud.",
            "Device Bans: Permanently block specific hardware from accessing the system.",
            "User-Device Mapping: See all accounts that have logged in from a single device.",
            "Emulator Detection: Monitor and restrict access from non-physical Android environments."
        ),
        "A single device with more than 5 accounts is a high-confidence signal for bot farming."
    ),
    HelpItem(
        "Fraud Radar",
        "Advanced bot detection and financial fraud monitoring using behavioral heuristics.",
        Icons.Default.Shield,
        AdminDesign.Warning,
        "Protect the game economy from malicious actors and automated exploit attempts.",
        listOf(
            "Pattern Analysis: Detect inhumanly fast move sequences in game sessions.",
            "Transaction Scrubbing: Identify suspicious payment patterns and chargeback risks.",
            "Heuristic Alerts: Automated flags for accounts showing known exploitative behaviors.",
            "Investigation Queue: Manual review interface for suspected fraudulent accounts."
        ),
        "Cross-reference Fraud Radar alerts with the Device Center to find coordinated bot networks."
    ),
    HelpItem(
        "Admin Sessions",
        "Monitor and manage active logins to the administrative console.",
        Icons.Default.DeviceUnknown,
        Color(0xFF7B1FA2),
        "Internal security tool to ensure that administrative access is tightly controlled.",
        listOf(
            "Live Session List: View all current active logins to the admin dashboard.",
            "IP Tracking: Monitor the geographic location of every administrative access.",
            "Session Termination: Instantly revoke access for any active admin session.",
            "Access Logs: Historical record of every login attempt to the console."
        ),
        "Implement IP-whitelisting for administrative accounts to add an extra layer of security."
    ),
    HelpItem(
        "Elo Rankings",
        "View competitive snapshots, adjust ranking weights, and reset seasons.",
        Icons.Default.TrendingUp,
        Color(0xFFFBC02D),
        "Manage the competitive integrity and player ranking system of DAADI.",
        listOf(
            "Global Standings: Real-time leaderboard showing the top-ranked players worldwide.",
            "Rank Distribution: View the percentage of players in every skill tier.",
            "Decay Settings: Configure how ranking points decrease over periods of inactivity.",
            "Season Reset: Perform bulk ranking resets and award seasonal prizes."
        ),
        "Use ranking decay to ensure that the top of the leaderboard remains active and competitive."
    ),
    HelpItem(
        "Global Events",
        "Design, schedule, and monitor seasonal and time-limited game events.",
        Icons.Default.Celebration,
        Color(0xFF8E24AA),
        "The command center for live game operations and temporary content injections.",
        listOf(
            "Event Creator: Design custom goal sets and reward tracks for new events.",
            "Live Monitoring: Track player participation and goal completion in real-time.",
            "Event Segments: Target specific events to certain user groups (e.g., Newbies).",
            "Asset Injection: Associate custom visual assets (banners, icons) with events."
        ),
        "Run 'Double XP' events on weekends to maximize concurrent player counts."
    ),
    HelpItem(
        "AI Strategy",
        "Fine-tune the behavior, difficulty, and tactical logic of the AI Engine.",
        Icons.Default.PrecisionManufacturing,
        Color(0xFF673AB7),
        "Control the 'brain' of Chanakya and other automated game participants.",
        listOf(
            "Difficulty Tiers: Adjust the look-ahead depth and error rate for AI levels.",
            "Behavior Profiles: Define different AI 'personalities' (Aggressive, Defensive).",
            "Win-Rate Balancing: Tweak AI logic to maintain a target player win percentage.",
            "Logic Updates: Push new tactical decision trees without a client update."
        ),
        "Aim for a 45-55% win rate for average players against 'Medium' AI to maintain engagement."
    ),
    HelpItem(
        "Player Support",
        "Manage user feedback, bug reports, and support tickets from players.",
        Icons.Default.SupportAgent,
        AdminDesign.Primary,
        "Centralized support hub for interacting with your player base. Review feedback and respond to critical issues.",
        listOf(
            "Feedback Queue: Review suggestions and complaints from players.",
            "Bug Tracking: Monitor and prioritize player-reported software issues.",
            "Response System: Draft and send responses directly to user mailboxes.",
            "Ticket Status: Track the lifecycle of player issues from 'Open' to 'Resolved'."
        ),
        "Promptly responding to feedback significantly improves player retention and community trust."
    ),
    HelpItem(
        "Announcements",
        "Create and manage system-wide news, bulletins, and event notifications.",
        Icons.Default.Campaign,
        AdminDesign.Secondary,
        "Primary broadcasting tool for reaching all players. Schedule news items and urgent system alerts.",
        listOf(
            "Bulletin Creator: Draft rich-text news items with custom banners.",
            "Urgent Alerts: Dispatch high-priority pop-ups for critical information.",
            "Scheduling: Set start and end times for news visibility.",
            "Targeting: Send specific announcements to localized player regions."
        ),
        "Keep bulletins concise and use high-impact visuals to ensure they are read by players."
    ),
    HelpItem(
        "Anti-Cheat",
        "Dedicated dashboard for monitoring and mitigating game exploitation.",
        Icons.Default.Shield,
        AdminDesign.Error,
        "Technical security layer focused on maintaining the competitive integrity of the game.",
        listOf(
            "Anomaly Detection: Automated flags for suspicious move-sequences.",
            "Signature Blocking: Mitigate known cheat engines and memory editors.",
            "Behavioral Analysis: identify 'humanly impossible' response times.",
            "Mass Actions: Bulk-ban identified cheating networks instantly."
        ),
        "Regularly update anti-cheat signatures to stay ahead of evolving exploitation methods."
    ),
    HelpItem(
        "Tournaments",
        "Structure, schedule, and manage competitive tournament events and prizes.",
        Icons.Default.EmojiEvents,
        Color(0xFFFFD700),
        "The competitive engine for DAADI. Design and run large-scale player brackets.",
        listOf(
            "Bracket Design: Create Single-Elimination, Double-Elimination, or Round-Robin structures.",
            "Prize Pools: Define coin, XP, and exclusive item rewards for winners.",
            "Matchmaking: Automated seeding based on player Elo rankings.",
            "Live Brackets: Monitor tournament progress in real-time as matches complete."
        ),
        "Running weekly tournaments with exclusive badges drives high peak concurrent user counts."
    )
)
