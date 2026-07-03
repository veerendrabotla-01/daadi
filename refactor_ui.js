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
    'currentUser': 'auth', 'currentUserRoles': 'auth', 'currentUserPermissions': 'auth', 'errorMessage': 'auth', 'isConfigured': 'auth', 'refreshUserProfile': 'auth', 'login': 'auth', 'signUp': 'auth', 'logout': 'auth', 'deleteUser': 'auth', 'signInWithOAuth': 'auth', 'resetPassword': 'auth', 'updateUserVerification': 'auth', 'setShadowBan': 'auth', 'updateInternalNotes': 'auth', 'resetUsername': 'auth', 'resetAvatar': 'auth', 'forceLogout': 'auth', 'processUserAndPromoteIfAdmin': 'auth', 'hasRole': 'auth', 'hasPermission': 'auth', 'checkRolePermissions': 'auth', 'loadInitialData': 'auth',
    // Admin
    'adminInvitations': 'admin', 'adminActivities': 'admin', 'auditLogs': 'admin', 'adminSessions': 'admin', 'promoteUserToRole': 'admin', 'demoteAdmin': 'admin', 'terminateAdminSession': 'admin', 'fetchAdminSessions': 'admin', 'revokeAdminInvitation': 'admin', 'inviteAdmin': 'admin', 'updateAdminRole': 'admin', 'saveAdminActivity': 'admin', 'logAudit': 'admin', 'fetchAdmins': 'admin', 'fetchAdminActivities': 'admin', 'fetchAdminInvitations': 'admin',
    // User
    'users': 'user', 'fetchUsers': 'user', 'fetchLoginHistory': 'user',
    // Analytics
    'biMetrics': 'analytics', 'crashLogs': 'analytics', 'fraudAlerts': 'analytics', 'financeReports': 'analytics', 'queueMetrics': 'analytics', 'deviceRecords': 'analytics', 'healthMetrics': 'analytics', 'biDailyMetrics': 'analytics', 'biNotifications': 'analytics', 'biAppLogs': 'analytics', 'biHealthMetrics': 'analytics', 'logBIEvent': 'analytics', 'fetchBIMetrics': 'analytics', 'fetchCrashLogs': 'analytics', 'fetchFraudAlerts': 'analytics', 'fetchFinanceReports': 'analytics', 'fetchQueueMetrics': 'analytics', 'fetchDeviceRecords': 'analytics', 'fetchHealthMetrics': 'analytics', 'fetchBIDailyMetrics': 'analytics', 'fetchBINotifications': 'analytics', 'fetchBIAppLogs': 'analytics', 'fetchBIHealthMetrics': 'analytics', 'scheduleNotification': 'analytics', 'askAiAssistant': 'analytics', 'isSyncing': 'analytics', 'incrementAdImpressions': 'analytics',
    // Economy
    'economyTransactions': 'economy', 'fetchEconomyTransactions': 'economy', 'adjustUserEconomy': 'economy', 'adjustUserStats': 'economy', 'storeItems': 'economy', 'coupons': 'economy', 'fetchStoreItems': 'economy', 'fetchCoupons': 'economy', 'dailyRewards': 'economy', 'spinWheelRewards': 'economy', 'fetchDailyRewards': 'economy', 'fetchSpinWheelRewards': 'economy',
    // LiveOps
    'liveOpsEvents': 'liveOps', 'seasonPasses': 'liveOps', 'seasonPassTiers': 'liveOps', 'gameEvents': 'liveOps', 'fetchLiveOpsEvents': 'liveOps', 'fetchSeasonPasses': 'liveOps', 'fetchSeasonPassTiers': 'liveOps', 'fetchGameEvents': 'liveOps', 'createGameEvent': 'liveOps', 'toggleGameEvent': 'liveOps',
    // Support
    'tickets': 'support', 'feedbacks': 'support', 'fetchTickets': 'support', 'fetchFeedbackV2': 'support', 'submitFeedback': 'support',
    // Tournament
    'tournaments': 'tournament', 'tournamentParticipants': 'tournament', 'fetchTournaments': 'tournament', 'createTournament': 'tournament', 'joinTournament': 'tournament',
    // RemoteConfig
    'systemSettings': 'remoteConfig', 'appVersions': 'remoteConfig', 'maintenanceSchedules': 'remoteConfig', 'cmsContent': 'remoteConfig', 'adConfig': 'remoteConfig', 'fetchSystemSettings': 'remoteConfig', 'fetchAppVersions': 'remoteConfig', 'fetchMaintenanceSchedules': 'remoteConfig', 'fetchCMSContent': 'remoteConfig', 'saveCMSContent': 'remoteConfig', 'updateRemoteConfig': 'remoteConfig',
    // Game
    'matches': 'remoteGame', 'antiCheatLogs': 'remoteGame', 'fetchAntiCheatLogs': 'remoteGame', 'logAntiCheatViolation': 'remoteGame', 'updateMatchStatus': 'remoteGame', 'fetchMatchDetails': 'remoteGame', 'findWaitingMatch': 'remoteGame', 'hostWaitingMatch': 'remoteGame', 'joinWaitingMatch': 'remoteGame', 'updateMatchState': 'remoteGame', 'updateMatchWinner': 'remoteGame'
};

walkDir(uiDir, function(filePath) {
    if (!filePath.endsWith('.kt')) return;
    let content = fs.readFileSync(filePath, 'utf-8');
    let isChanged = false;
    let isAdminScreen = filePath.includes('/admin/');
    let viewModelVar = isAdminScreen ? 'adminViewModel' : 'sharedGameViewModel';

    if (content.includes('supabaseManager: SupabaseManager')) {
        content = content.replace(/supabaseManager:\s*(?:com\.example\.daadi\.data\.supabase\.)?SupabaseManager/g, `${viewModelVar}: ${isAdminScreen ? 'com.example.daadi.viewmodel.AdminViewModel' : 'com.example.daadi.viewmodel.GameViewModel'}`);
        isChanged = true;
    }

    for (const [method, repo] of Object.entries(mapMethodToRepo)) {
        let regex = new RegExp(`supabaseManager\\.${method}`, 'g');
        if (regex.test(content)) {
            content = content.replace(regex, `${viewModelVar}.${repo}Repository.${method}`);
            isChanged = true;
        }
        
        let regexApp = new RegExp(`app\\.supabaseManager\\.${method}`, 'g');
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

    if (isChanged) {
        fs.writeFileSync(filePath, content);
        console.log('Updated ' + filePath);
    }
});

let adManagerPath = 'app/src/main/java/com/example/daadi/data/ads/AdManager.kt';
let adContent = fs.readFileSync(adManagerPath, 'utf-8');
adContent = adContent.replace(/val supabaseManager:\s*SupabaseManager/, 'val remoteConfigRepository: com.example.daadi.data.repository.supabase.RemoteConfigRepository, val analyticsRepository: com.example.daadi.data.repository.supabase.AnalyticsRepository');
adContent = adContent.replace(/supabaseManager\.adConfig/g, 'remoteConfigRepository.adConfig');
adContent = adContent.replace(/supabaseManager\.systemSettings/g, 'remoteConfigRepository.systemSettings');
adContent = adContent.replace(/supabaseManager\.logBIEvent/g, 'analyticsRepository.logBIEvent');
fs.writeFileSync(adManagerPath, adContent);

let multiManagerPath = 'app/src/main/java/com/example/daadi/data/multiplayer/MultiplayerManager.kt';
let multiContent = fs.readFileSync(multiManagerPath, 'utf-8');
// if there is no supabaseManager, do not replace
if(multiContent.includes('supabaseManager')) {
    multiContent = multiContent.replace(/val supabaseManager:\s*SupabaseManager/, 'val remoteGameRepository: com.example.daadi.data.repository.supabase.RemoteGameRepository');
    multiContent = multiContent.replace(/supabaseManager\.logAntiCheatViolation/g, 'remoteGameRepository.logAntiCheatViolation');
    fs.writeFileSync(multiManagerPath, multiContent);
}

// Fix MainActivity
let mainPath = 'app/src/main/java/com/example/MainActivity.kt';
let mainContent = fs.readFileSync(mainPath, 'utf-8');
mainContent = mainContent.replace(/val supabaseManager = application\.supabaseManager/g, 'val authRepository = application.authRepository');
mainContent = mainContent.replace(/supabaseManager\.currentUser/g, 'authRepository.currentUser');
mainContent = mainContent.replace(/supabaseManager\.hasPermission/g, 'authRepository.hasPermission');
fs.writeFileSync(mainPath, mainContent);

console.log('UI refactored.');
