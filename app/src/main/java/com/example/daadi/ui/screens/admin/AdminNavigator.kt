package com.example.daadi.ui.screens.admin

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.daadi.data.supabase.SupabaseManager
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
                onNavigateToSystem = { adminNavController.navigate("feedback") },
                onNavigateToHealth = { adminNavController.navigate("health") },
                onNavigateToBulletins = { adminNavController.navigate("announcements") },
                onBack = onExitAdmin
            )
        }

        composable("user_list") {
            AdminUserListScreen(
                supabaseManager = supabaseManager,
                onUserClick = { user -> 
                    selectedUser = user
                    adminNavController.navigate("user_details")
                },
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("user_details") {
            selectedUser?.let { user ->
                AdminUserDetailsScreen(
                    user = user,
                    supabaseManager = supabaseManager,
                    onBack = { adminNavController.popBackStack() }
                )
            }
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
            AdminMatchArchiveScreen(
                supabaseManager = supabaseManager,
                onMatchClick = { match ->
                    selectedMatch = match
                    adminNavController.navigate("match_details")
                },
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("match_details") {
            selectedMatch?.let { match ->
                AdminMatchDetailScreen(
                    match = match,
                    onBack = { adminNavController.popBackStack() }
                )
            }
        }

        composable("config") {
            AdminSystemConfigScreen(
                supabaseManager = supabaseManager,
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("analytics") {
            AdminSystemScreens(
                supabaseManager = supabaseManager,
                type = "analytics",
                onBack = { adminNavController.popBackStack() }
            )
        }

        composable("feedback") {
            AdminSystemScreens(
                supabaseManager = supabaseManager,
                type = "feedback",
                onBack = { adminNavController.popBackStack() }
            )
        }
        
        composable("logs") {
            AdminSystemScreens(
                supabaseManager = supabaseManager,
                type = "logs",
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
            AdminSystemScreens(
                supabaseManager = supabaseManager,
                type = "health",
                onBack = { adminNavController.popBackStack() }
            )
        }
    }
}
