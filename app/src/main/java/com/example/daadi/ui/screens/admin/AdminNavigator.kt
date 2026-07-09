package com.example.daadi.ui.screens.admin



import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.daadi.data.supabase.SupabaseUser

@Composable
fun AdminNavigator(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onExitAdmin: () -> Unit
) {
    val adminNavController = rememberNavController()
    
    // Track selected objects for detail screens
    var selectedUser by remember { mutableStateOf<SupabaseUser?>(null) }
    var selectedMatch by remember { mutableStateOf<com.example.daadi.data.supabase.SupabaseMatch?>(null) }

    NavHost(
        navController = adminNavController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            AdminDashboardScreen(
                adminViewModel = adminViewModel,
                onNavigateToUsers = { adminNavController.navigate("user_list") },
                onNavigateToSafety = { adminNavController.navigate("safety") },
                onNavigateToMatches = { adminNavController.navigate("match_archive") },
                onNavigateToConfig = { adminNavController.navigate("config") },
                onNavigateToAnalytics = { adminNavController.navigate("analytics") },
                onNavigateToSystem = { adminNavController.navigate("logs") },
                onNavigateToBulletins = { adminNavController.navigate("announcements") },
                onNavigateToAntiCheat = { adminNavController.navigate("anti_cheat") },
                onNavigateToTournaments = { adminNavController.navigate("tournaments") },
                onNavigateToEvents = { adminNavController.navigate("events") },
                onNavigateToBIAnalytics = { adminNavController.navigate("bi_analytics") },
                onNavigateToBIRevenue = { adminNavController.navigate("bi_revenue") },
                onNavigateToBINotifications = { adminNavController.navigate("bi_notifications") },
                onNavigateToAuditTrail = { adminNavController.navigate("audit_logs") },
                onNavigateToPermissionMatrix = { adminNavController.navigate("permission_matrix") },
                onNavigateToSessions = { adminNavController.navigate("sessions") },
                onNavigateToEconomy = { adminNavController.navigate("economy") },
                onNavigateToStore = { adminNavController.navigate("store") },
                onNavigateToRewards = { adminNavController.navigate("rewards") },
                onNavigateToLiveOps = { adminNavController.navigate("liveops") },
                onNavigateToSeasonPass = { adminNavController.navigate("season_pass") },
                onNavigateToCMS = { adminNavController.navigate("cms") },
                onNavigateToCrashes = { adminNavController.navigate("crashes") },
                onNavigateToDevices = { adminNavController.navigate("devices") },
                onNavigateToFraud = { adminNavController.navigate("fraud") },
                onNavigateToAIAssistant = { adminNavController.navigate("ai_assistant") },
                onNavigateToMonitoring = { adminNavController.navigate("health") },
                onNavigateToExports = { adminNavController.navigate("exports") },
                onNavigateToApprovals = { adminNavController.navigate("approvals") },
                onNavigateToScheduler = { adminNavController.navigate("scheduler") },
                onNavigateToRollbacks = { adminNavController.navigate("rollbacks") },
                onNavigateToAIEngine = { adminNavController.navigate("ai_engine") },
                onNavigateToLeaderboards = { adminNavController.navigate("leaderboards") },
                onBack = onExitAdmin
            )
        }

        composable("user_list") {
            AdminUserManagementScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("safety") {
            AdminSafetyHubScreen(
                adminViewModel = adminViewModel,
                onUserClick = { user ->
                    selectedUser = user
                    adminNavController.navigate("user_details")
                },
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("match_archive") {
            AdminMatchManagementScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("config") {
            AdminSystemConfigScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("analytics") {
            AdminBIPlatformSuite(
                adminViewModel = adminViewModel,
                type = "analytics",
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("feedback") {
            AdminSupportHubScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }
        
        composable("logs") {
            AdminAuditTrailScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }
        
        // Announcements can be another screen or part of config
        composable("announcements") {
            AdminAnnouncementsScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("health") {
            AdminMonitoringScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("crashes") {
            AdminCrashCenterScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("devices") {
            AdminDeviceCenterScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("fraud") {
            AdminFraudDetectionScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("ai_assistant") {
            AdminAIAssistantScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("audit_logs") {
            AdminAuditTrailScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("permission_matrix") {
            AdminPermissionMatrixScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("sessions") {
            AdminSessionManagerScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("anti_cheat") {
            AdminAntiCheatDashboard(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("tournaments") {
            AdminTournamentScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("events") {
            AdminEventScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("bi_analytics") {
            AdminBIAnalyticsScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("bi_revenue") {
            AdminBIAnalyticsScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("bi_notifications") {
            AdminBIPlatformSuite(
                adminViewModel = adminViewModel,
                type = "notifications",
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("economy") {
            AdminEconomyCenter(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("store") {
            AdminStoreManagement(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("rewards") {
            AdminRewardEditor(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("liveops") {
            AdminLiveOpsCenter(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("season_pass") {
            AdminSeasonPassManager(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("cms") {
            AdminCMSCenter(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("exports") {
            AdminDataExportScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("approvals") {
            AdminWorkflowApprovalsScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("scheduler") {
            AdminTaskSchedulerScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("rollbacks") {
            AdminConfigHistoryScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("ai_engine") {
            AdminAIEngineScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("leaderboards") {
            AdminLeaderboardManagerScreen(
                adminViewModel = adminViewModel,
                onBack = { adminNavController.popBackStack() }
            )
        }
    }
}
