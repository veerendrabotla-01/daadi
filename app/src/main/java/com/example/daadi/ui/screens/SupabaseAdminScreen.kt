package com.example.daadi.ui.screens

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.example.daadi.ui.screens.admin.AdminNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseAdminScreen(
    viewModel: com.example.daadi.viewmodel.GameViewModel,
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    AdminNavigator(
        supabaseManager = supabaseManager,
        onExitAdmin = onBack
    )
}
