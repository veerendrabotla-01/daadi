package com.example.daadi.viewmodel

import androidx.lifecycle.ViewModel
import com.example.daadi.data.repository.supabase.*

class AdminViewModel(
    val authRepository: AuthRepository,
    val adminRepository: AdminRepository,
    val analyticsRepository: AnalyticsRepository,
    val remoteGameRepository: RemoteGameRepository,
    val economyRepository: EconomyRepository,
    val liveOpsRepository: LiveOpsRepository,
    val supportRepository: SupportRepository,
    val tournamentRepository: TournamentRepository,
    val remoteConfigRepository: RemoteConfigRepository,
    val userRepository: UserRepository
) : ViewModel() {
    // Shared filters for deep links / navigation across modules
    val filterUserId = androidx.compose.runtime.mutableStateOf<String?>(null)
    val filterUsername = androidx.compose.runtime.mutableStateOf<String?>(null)
    val filterDeviceId = androidx.compose.runtime.mutableStateOf<String?>(null)
    val filterMatchId = androidx.compose.runtime.mutableStateOf<String?>(null)

    fun clearAllFilters() {
        filterUserId.value = null
        filterUsername.value = null
        filterDeviceId.value = null
        filterMatchId.value = null
    }
}
