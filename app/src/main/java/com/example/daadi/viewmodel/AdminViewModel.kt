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
) : ViewModel()
