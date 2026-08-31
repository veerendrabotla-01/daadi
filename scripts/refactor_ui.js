const fs = require('fs');
const path = require('path');

const uiDir = 'app/src/main/java/com/example/daadi/ui/screens/';

function walkDir(dir, callback) {
    fs.readdirSync(dir).forEach(f => {
        let dirPath = path.join(dir, f);
        let isDirectory = fs.statSync(dirPath).isDirectory();
        isDirectory ? walkDir(dirPath, callback) : callback(dirPath);
    });
}

const mapMethodToRepo = {
    // Auth
    'currentUser': 'auth',
    'userSession': 'auth',
    'errorMessage': 'auth',
    'isConfigured': 'auth',
    'passwordResetRequired': 'auth',
    'clearPasswordResetRequired': 'auth',
    'updatePassword': 'auth',
    'refreshUserProfile': 'auth',
    'login': 'auth',
    'signUp': 'auth',
    'logout': 'auth',
    'deleteUser': 'auth',
    'signInWithOAuth': 'auth',
    'resetPassword': 'auth',
    'updateUserVerification': 'auth',
    'setShadowBan': 'auth',
    'updateInternalNotes': 'auth',
    'resetUsername': 'auth',
    'resetAvatar': 'auth',
    'forceLogout': 'auth',
    'processUserAndPromoteIfAdmin': 'auth',
    'hasRole': 'auth',
    'hasPermission': 'auth',
    'checkRolePermissions': 'auth',
    'loadInitialData': 'auth',
    'logAdminAction': 'auth',

    // Admin
    'adminInvitations': 'admin',
    'adminActivities': 'admin',
    'auditLogs': 'admin',
    'adminSessions': 'admin',
    'promoteUserToRole': 'admin',
    'demoteAdmin': 'admin',
    'terminateAdminSession': 'admin',
    'fetchAdminSessions': 'admin',
    'revokeAdminInvitation': 'admin',
    'inviteAdmin': 'admin',
    'updateAdminRole': 'admin',
    'saveAdminActivity': 'admin',
    'logAudit': 'admin',
    'fetchAdmins': 'admin',
    'fetchAdminActivities': 'admin',
    'fetchAdminInvitations': 'admin',

    // User
    'users': 'user',
    'fetchUsers': 'user',
    'fetchLoginHistory': 'user',
    'userLoginHistory': 'user',
    'toggleUserBan': 'user',

    // Analytics
    'biMetrics': 'analytics',
    'crashLogs': 'analytics',
    'fraudAlerts': 'analytics',
    'financeReports': 'analytics',
    'queueMetrics': 'analytics',
    'deviceRecords': 'analytics',
    'healthMetrics': 'analytics',
    'biDailyMetrics': 'analytics',
    'biNotifications': 'analytics',
    'biAppLogs': 'analytics',
    'biHealthMetrics': 'analytics',
    'logBIEvent': 'analytics',
    'fetchBIMetrics': 'analytics',
    'fetchCrashLogs': 'analytics',
    'fetchFraudAlerts': 'analytics',
    'fetchFinanceReports': 'analytics',
    'fetchQueueMetrics': 'analytics',
    'fetchDeviceRecords': 'analytics',
    'fetchHealthMetrics': 'analytics',
    'fetchBIDailyMetrics': 'analytics',
    'fetchBINotifications': 'analytics',
    'fetchBIAppLogs': 'analytics',
    'fetchBIHealthMetrics': 'analytics',
    'scheduleNotification': 'analytics',
    'askAiAssistant': 'analytics',
    'isSyncing': 'analytics',
    'adTelemetry': 'analytics',
    'resetAdTelemetryRemote': 'analytics',
    'incrementAdRequests': 'analytics',
    'incrementAdImpressions': 'analytics',

    // Economy
    'economyTransactions': 'economy',
    'fetchEconomyTransactions': 'economy',
    'adjustUserEconomy': 'economy',
    'adjustUserStats': 'economy',
    'storeItems': 'economy',
    'coupons': 'economy',
    'fetchStoreItems': 'economy',
    'fetchCoupons': 'economy',
    'dailyRewards': 'economy',
    'spinWheelRewards': 'economy',
    'fetchDailyRewards': 'economy',
    'fetchSpinWheelRewards': 'economy',
    'createStoreItem': 'economy',
    'deleteStoreItem': 'economy',
    'createCoupon': 'economy',
    'deleteCoupon': 'economy',
    'saveDailyReward': 'economy',
    'saveSpinWheelReward': 'economy',

    // LiveOps
    'liveOpsEvents': 'liveOps',
    'seasonPasses': 'liveOps',
    'seasonPassTiers': 'liveOps',
    'gameEvents': 'liveOps',
    'fetchLiveOpsEvents': 'liveOps',
    'fetchSeasonPasses': 'liveOps',
    'fetchSeasonPassTiers': 'liveOps',
    'fetchGameEvents': 'liveOps',
    'createGameEvent': 'liveOps',
    'toggleGameEvent': 'liveOps',
    'deleteGameEvent': 'liveOps',
    'createLiveOpsEvent': 'liveOps',
    'deleteLiveOpsEvent': 'liveOps',
    'toggleLiveOpsEventActive': 'liveOps',
    'createSeasonPass': 'liveOps',
    'deleteSeasonPass': 'liveOps',
    'toggleSeasonPassActive': 'liveOps',

    // Support
    'tickets': 'support',
    'feedbacks': 'support',
    'fetchTickets': 'support',
    'fetchFeedbackV2': 'support',
    'submitFeedback': 'support',
    'feedbackV2': 'support',
    'reportUserByName': 'support',

    // Tournament
    'tournaments': 'tournament',
    'tournamentParticipants': 'tournament',
    'fetchTournaments': 'tournament',
    'createTournament': 'tournament',
    'joinTournament': 'tournament',
    'deleteTournament': 'tournament',
    'updateTournamentStatus': 'tournament',

    // RemoteConfig
    'systemSettings': 'remoteConfig',
    'appVersions': 'remoteConfig',
    'maintenanceSchedules': 'remoteConfig',
    'cmsContent': 'remoteConfig',
    'adConfig': 'remoteConfig',
    'fetchSystemSettings': 'remoteConfig',
    'fetchAppVersions': 'remoteConfig',
    'fetchMaintenanceSchedules': 'remoteConfig',
    'fetchCMSContent': 'remoteConfig',
    'saveCMSContent': 'remoteConfig',
    'updateRemoteConfig': 'remoteConfig',
    'maintenanceMode': 'remoteConfig',
    'multiplayerEnabled': 'remoteConfig',
    'globalBroadcast': 'remoteConfig',
    'dispatchBroadcast': 'remoteConfig',
    'clearBroadcast': 'remoteConfig',
    'updateSystemSetting': 'remoteConfig',
    'updateAdConfigurationRemote': 'remoteConfig',
    'announcements': 'remoteConfig',
    'createAnnouncement': 'remoteConfig',
    'toggleAnnouncementStatus': 'remoteConfig',
    'deleteAnnouncement': 'remoteConfig',

    // RemoteGame
    'matches': 'remoteGame',
    'antiCheatLogs': 'remoteGame',
    'fetchAntiCheatLogs': 'remoteGame',
    'logAntiCheatViolation': 'remoteGame',
    'updateMatchStatus': 'remoteGame',
    'fetchMatchDetails': 'remoteGame',
    'findWaitingMatch': 'remoteGame',
    'hostWaitingMatch': 'remoteGame',
    'joinWaitingMatch': 'remoteGame',
    'updateMatchState': 'remoteGame',
    'updateMatchWinner': 'remoteGame',
    'deleteMatch': 'remoteGame',
    'updateMatchMoves': 'remoteGame',
    'registerMatchResult': 'remoteGame'
};

walkDir(uiDir, function(filePath) {
    if (!filePath.endsWith('.kt')) return;
    let content = fs.readFileSync(filePath, 'utf-8');
    let isChanged = false;
    let isAdminScreen = filePath.includes('/admin/');
    let viewModelVar = isAdminScreen ? 'adminViewModel' : 'sharedGameViewModel';
    let viewModelType = isAdminScreen ? 'com.example.daadi.viewmodel.AdminViewModel' : 'com.example.daadi.viewmodel.GameViewModel';

    // Looser pattern matching for parameter injection
    let paramRegex = /supabaseManager:\s*(?:com\.example\.daadi\.data\.supabase\.)?SupabaseManager/g;
    if (paramRegex.test(content)) {
        content = content.replace(paramRegex, `${viewModelVar}: ${viewModelType}`);
        isChanged = true;
    }

    for (const [method, repo] of Object.entries(mapMethodToRepo)) {
        let regex = new RegExp(`\\bsupabaseManager\\.${method}\\b`, 'g');
        if (regex.test(content)) {
            content = content.replace(regex, `${viewModelVar}.${repo}Repository.${method}`);
            isChanged = true;
        }
        
        let regexApp = new RegExp(`\\bapp\\.supabaseManager\\.${method}\\b`, 'g');
        if (regexApp.test(content)) {
            content = content.replace(regexApp, `app.${repo}Repository.${method}`);
            isChanged = true;
        }
    }
    
    if (content.includes('supabaseManager = supabaseManager')) {
        content = content.replace(/supabaseManager\s*=\s*supabaseManager/g, `${viewModelVar} = ${viewModelVar}`);
        isChanged = true;
    }
    if (content.includes(', supabaseManager)')) {
        content = content.replace(/,\s*supabaseManager\)/g, `, ${viewModelVar})`);
        isChanged = true;
    }

    // Clean up unused SupabaseManager imports
    if (content.includes('import com.example.daadi.data.supabase.SupabaseManager')) {
        content = content.replace(/import com.example.daadi.data.supabase.SupabaseManager\r?\n/g, '');
        isChanged = true;
    }

    if (isChanged) {
        fs.writeFileSync(filePath, content);
        console.log('Updated ' + filePath);
    }
});

let adManagerPath = 'app/src/main/java/com/example/daadi/data/ads/AdManager.kt';
let adContent = fs.readFileSync(adManagerPath, 'utf-8');
if (adContent.includes('supabaseManager: SupabaseManager')) {
    adContent = adContent.replace(/val supabaseManager:\s*SupabaseManager/, 'val remoteConfigRepository: com.example.daadi.data.repository.supabase.RemoteConfigRepository, val analyticsRepository: com.example.daadi.data.repository.supabase.AnalyticsRepository');
    adContent = adContent.replace(/supabaseManager\.adConfig/g, 'remoteConfigRepository.adConfig');
    adContent = adContent.replace(/supabaseManager\.systemSettings/g, 'remoteConfigRepository.systemSettings');
    adContent = adContent.replace(/supabaseManager\.logBIEvent/g, 'analyticsRepository.logBIEvent');
    fs.writeFileSync(adManagerPath, adContent);
    console.log('Updated AdManager.kt');
}

let multiManagerPath = 'app/src/main/java/com/example/daadi/data/multiplayer/MultiplayerManager.kt';
let multiContent = fs.readFileSync(multiManagerPath, 'utf-8');
if (multiContent.includes('supabaseManager: SupabaseManager')) {
    multiContent = multiContent.replace(/val supabaseManager:\s*SupabaseManager/, 'val remoteGameRepository: com.example.daadi.data.repository.supabase.RemoteGameRepository');
    multiContent = multiContent.replace(/supabaseManager\.logAntiCheatViolation/g, 'remoteGameRepository.logAntiCheatViolation');
    fs.writeFileSync(multiManagerPath, multiContent);
    console.log('Updated MultiplayerManager.kt');
}

console.log('UI refactored.');
