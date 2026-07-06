package com.example.daadi.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.example.daadi.ui.screens.admin.AdminNavigator
import com.example.daadi.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseAdminScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    AdminNavigator(
        adminViewModel = adminViewModel,
        onExitAdmin = onBack
    )
}
