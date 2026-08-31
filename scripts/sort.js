const fs = require('fs');
const chunks = JSON.parse(fs.readFileSync('parsed_chunks.json', 'utf-8'));

const repos = {
    AuthRepository: [],
    UserRepository: [],
    AdminRepository: [],
    AnalyticsRepository: [],
    GameRepository: [],
    EconomyRepository: [],
    LiveOpsRepository: [],
    SupportRepository: [],
    TournamentRepository: [],
    RemoteConfigRepository: []
};

// Define keyword matching
const mappings = [
    // Auth
    { keywords: ['currentUser', 'userSession', 'errorMessage', 'isConfigured', 'refreshUserProfile', 'login', 'signUp', 'logout', 'deleteUser', 'signInWithOAuth', 'resetPassword', 'updateUserVerification', 'setShadowBan', 'updateInternalNotes', 'resetUsername', 'resetAvatar', 'forceLogout', 'processUserAndPromoteIfAdmin', 'hasRole', 'hasPermission', 'checkRolePermissions', 'loadInitialData'], repo: 'AuthRepository' },
    // Admin
    { keywords: ['adminInvitations', 'adminActivities', 'auditLogs', 'adminSessions', 'promoteUserToRole', 'demoteAdmin', 'terminateAdminSession', 'fetchAdminSessions', 'revokeAdminInvitation', 'inviteAdmin', 'updateAdminRole', 'saveAdminActivity', 'logAudit', 'fetchAdmins', 'fetchAdminActivities', 'fetchAdminInvitations'], repo: 'AdminRepository' },
    // User
    { keywords: ['users', 'fetchUsers', 'fetchLoginHistory'], repo: 'UserRepository' },
    // Analytics
    { keywords: ['biMetrics', 'crashLogs', 'fraudAlerts', 'financeReports', 'queueMetrics', 'deviceRecords', 'healthMetrics', 'biDailyMetrics', 'biNotifications', 'biAppLogs', 'biHealthMetrics', 'logBIEvent', 'fetchBIMetrics', 'fetchCrashLogs', 'fetchFraudAlerts', 'fetchFinanceReports', 'fetchQueueMetrics', 'fetchDeviceRecords', 'fetchHealthMetrics', 'fetchBIDailyMetrics', 'fetchBINotifications', 'fetchBIAppLogs', 'fetchBIHealthMetrics', 'scheduleNotification', 'askAiAssistant', 'isSyncing', 'incrementAdImpressions'], repo: 'AnalyticsRepository' },
    // Economy
    { keywords: ['economyTransactions', 'fetchEconomyTransactions', 'adjustUserEconomy', 'adjustUserStats', 'storeItems', 'coupons', 'fetchStoreItems', 'fetchCoupons', 'dailyRewards', 'spinWheelRewards', 'fetchDailyRewards', 'fetchSpinWheelRewards'], repo: 'EconomyRepository' },
    // LiveOps
    { keywords: ['liveOpsEvents', 'seasonPasses', 'seasonPassTiers', 'gameEvents', 'fetchLiveOpsEvents', 'fetchSeasonPasses', 'fetchSeasonPassTiers', 'fetchGameEvents', 'createGameEvent', 'toggleGameEvent'], repo: 'LiveOpsRepository' },
    // Support
    { keywords: ['tickets', 'feedbacks', 'fetchTickets', 'fetchFeedbackV2', 'submitFeedback'], repo: 'SupportRepository' },
    // Tournament
    { keywords: ['tournaments', 'tournamentParticipants', 'fetchTournaments', 'createTournament', 'joinTournament'], repo: 'TournamentRepository' },
    // RemoteConfig
    { keywords: ['systemSettings', 'appVersions', 'maintenanceSchedules', 'cmsContent', 'adConfig', 'fetchSystemSettings', 'fetchAppVersions', 'fetchMaintenanceSchedules', 'fetchCMSContent', 'saveCMSContent', 'updateRemoteConfig'], repo: 'RemoteConfigRepository' },
    // Game
    { keywords: ['matches', 'antiCheatLogs', 'fetchAntiCheatLogs', 'logAntiCheatViolation', 'updateMatchStatus', 'fetchMatchDetails', 'findWaitingMatch', 'hostWaitingMatch', 'joinWaitingMatch', 'updateMatchState', 'updateMatchWinner'], repo: 'GameRepository' }
];

let unmatched = [];
let networkChunks = [];

for (let chunk of chunks) {
    // Check if network chunk
    if (chunk.includes('private val moshi') || chunk.includes('private val client') || chunk.includes('GeminiApiService') || chunk.includes('geminiService') || chunk.includes('getHeaders') || chunk.includes('fetchList')) {
        networkChunks.push(chunk);
        continue;
    }
    if (chunk.includes('private val tag') || chunk.includes('private val scope') || chunk.includes('private val prefs') || chunk.includes('private var sessionToken')) {
        networkChunks.push(chunk);
        continue;
    }
    
    // Check adapter declarations
    if (chunk.includes('val userListAdapter =') || chunk.includes('adapter<List<')) {
        networkChunks.push(chunk); // we will keep adapters in network for now
        continue;
    }

    let assigned = false;
    for (let map of mappings) {
        // match word exactly or roughly
        for (let kw of map.keywords) {
            // Look for `val kw` or `fun kw` or `_kw`
            if (new RegExp(`\\b${kw}\\b`).test(chunk)) {
                repos[map.repo].push(chunk);
                assigned = true;
                break;
            }
        }
        if (assigned) break;
    }
    if (!assigned) {
        unmatched.push(chunk);
    }
}

console.log('Unmatched chunks:', unmatched.length);
console.log('Network chunks:', networkChunks.length);
for (let repo in repos) {
    console.log(repo, repos[repo].length);
}
