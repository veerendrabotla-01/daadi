package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.daadi.data.supabase.SupabaseUser

@Composable
fun AdminNavigator(
    supabaseManager: SupabaseManager,
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
                supabaseManager = supabaseManager,
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
                onBack = onExitAdmin
            )
        }

        composable("user_list") {
            AdminUserManagementScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("safety") {
            AdminSafetyHubScreen(
                supabaseManager = supabaseManager,
                onUserClick = { user ->
                    selectedUser = user
                    adminNavController.navigate("user_details")
                },
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("match_archive") {
            AdminMatchManagementScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("config") {
            AdminSystemConfigScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("analytics") {
            AdminBIPlatformSuite(
                supabaseManager = supabaseManager,
                type = "analytics",
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("feedback") {
            AdminSupportHubScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }
        
        composable("logs") {
            AdminAuditTrailScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }
        
        // Announcements can be another screen or part of config
        composable("announcements") {
            AdminAnnouncementsScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("health") {
            AdminMonitoringScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("crashes") {
            AdminCrashCenterScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("devices") {
            AdminDeviceCenterScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("fraud") {
            AdminFraudDetectionScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("ai_assistant") {
            AdminAIAssistantScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("audit_logs") {
            AdminAuditTrailScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("permission_matrix") {
            AdminPermissionMatrixScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("sessions") {
            AdminSessionManagerScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("anti_cheat") {
            AdminAntiCheatDashboard(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("tournaments") {
            AdminTournamentScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("events") {
            AdminEventScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("bi_analytics") {
            AdminBIAnalyticsScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("bi_revenue") {
            AdminBIAnalyticsScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("bi_notifications") {
            AdminBIPlatformSuite(
                supabaseManager = supabaseManager,
                type = "notifications",
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("economy") {
            AdminEconomyCenter(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("store") {
            AdminStoreManagement(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("rewards") {
            AdminRewardEditor(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("liveops") {
            AdminLiveOpsCenter(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("season_pass") {
            AdminSeasonPassManager(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("cms") {
            AdminCMSCenter(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("exports") {
            AdminDataExportScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("approvals") {
            AdminWorkflowApprovalsScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("scheduler") {
            AdminTaskSchedulerScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("rollbacks") {
            AdminConfigHistoryScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }
    }
}
