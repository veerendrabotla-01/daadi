const fs = require('fs');

const repos = {
    AuthRepository: ['currentUser', 'currentUserRoles', 'currentUserPermissions', 'errorMessage', 'isConfigured', 'refreshUserProfile', 'login', 'signUp', 'logout', 'deleteUser', 'signInWithOAuth', 'resetPassword', 'updateUserVerification', 'setShadowBan', 'updateInternalNotes', 'resetUsername', 'resetAvatar', 'forceLogout', 'processUserAndPromoteIfAdmin', 'hasRole', 'hasPermission', 'checkRolePermissions', 'loadInitialData'],
    AdminRepository: ['adminInvitations', 'adminActivities', 'auditLogs', 'adminSessions', 'promoteUserToRole', 'demoteAdmin', 'terminateAdminSession', 'fetchAdminSessions', 'revokeAdminInvitation', 'inviteAdmin', 'updateAdminRole', 'saveAdminActivity', 'logAudit', 'fetchAdmins', 'fetchAdminActivities', 'fetchAdminInvitations'],
    UserRepository: ['users', 'fetchUsers', 'fetchLoginHistory'],
    AnalyticsRepository: ['biMetrics', 'crashLogs', 'fraudAlerts', 'financeReports', 'queueMetrics', 'deviceRecords', 'healthMetrics', 'biDailyMetrics', 'biNotifications', 'biAppLogs', 'biHealthMetrics', 'logBIEvent', 'fetchBIMetrics', 'fetchCrashLogs', 'fetchFraudAlerts', 'fetchFinanceReports', 'fetchQueueMetrics', 'fetchDeviceRecords', 'fetchHealthMetrics', 'fetchBIDailyMetrics', 'fetchBINotifications', 'fetchBIAppLogs', 'fetchBIHealthMetrics', 'scheduleNotification', 'askAiAssistant', 'isSyncing', 'incrementAdImpressions'],
    EconomyRepository: ['economyTransactions', 'fetchEconomyTransactions', 'adjustUserEconomy', 'adjustUserStats', 'storeItems', 'coupons', 'fetchStoreItems', 'fetchCoupons', 'dailyRewards', 'spinWheelRewards', 'fetchDailyRewards', 'fetchSpinWheelRewards'],
    LiveOpsRepository: ['liveOpsEvents', 'seasonPasses', 'seasonPassTiers', 'gameEvents', 'fetchLiveOpsEvents', 'fetchSeasonPasses', 'fetchSeasonPassTiers', 'fetchGameEvents', 'createGameEvent', 'toggleGameEvent'],
    SupportRepository: ['tickets', 'feedbacks', 'fetchTickets', 'fetchFeedbackV2', 'submitFeedback'],
    TournamentRepository: ['tournaments', 'tournamentParticipants', 'fetchTournaments', 'createTournament', 'joinTournament'],
    RemoteConfigRepository: ['systemSettings', 'appVersions', 'maintenanceSchedules', 'cmsContent', 'adConfig', 'fetchSystemSettings', 'fetchAppVersions', 'fetchMaintenanceSchedules', 'fetchCMSContent', 'saveCMSContent', 'updateRemoteConfig'],
    RemoteGameRepository: ['matches', 'antiCheatLogs', 'fetchAntiCheatLogs', 'logAntiCheatViolation', 'updateMatchStatus', 'fetchMatchDetails', 'findWaitingMatch', 'hostWaitingMatch', 'joinWaitingMatch', 'updateMatchState', 'updateMatchWinner']
};

let content = 'package com.example.daadi.data.repository.supabase\n\n' +
'import com.example.daadi.data.supabase.SupabaseManager\n\n';

for (let repo in repos) {
    content += `class ${repo}(private val supabaseManager: SupabaseManager) {\n`;
    for (let method of repos[repo]) {
        // We don't know the exact signature. 
        // We can just rely on the fact that UI screens were refactored to `adminViewModel.authRepository.method(...)`.
        // Wait, if we use facades, we must define the properties/methods with exactly matching signatures.
        // Doing this for 200 methods is hard.
    }
    content += '}\n\n';
}

fs.writeFileSync('app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseFacades.kt', content);
console.log('Facades generated.');
