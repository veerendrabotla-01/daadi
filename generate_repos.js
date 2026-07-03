const fs = require('fs');

const inputFile = 'app/src/main/java/com/example/daadi/data/supabase/SupabaseManager.kt';
const content = fs.readFileSync(inputFile, 'utf-8');

const classStart = content.indexOf('class SupabaseManager(private val context: Context) {');
const beforeClass = content.substring(0, classStart);
const classBody = content.substring(classStart + 'class SupabaseManager(private val context: Context) {'.length, content.lastIndexOf('}'));

const chunks = [];
let i = 0;
while (i < classBody.length) {
    if (/\s/.test(classBody[i])) { i++; continue; }
    let start = i;
    let depth = 0;
    let end = i;
    while (i < classBody.length) {
        if (classBody[i] === '{') depth++;
        if (classBody[i] === '}') {
            depth--;
            if (depth === 0) { i++; end = i; break; }
        }
        if (depth === 0 && classBody[i] === '\n') {
            let j = i + 1;
            while (j < classBody.length && /[ \t]/.test(classBody[j])) j++;
            if (j < classBody.length && classBody[j] === '\n') {
                i = j; end = i; break;
            }
            const nextWord = classBody.substring(j, j + 20);
            if (/^(private |val |var |fun |suspend |init )/.test(nextWord)) { end = i; break; }
        }
        i++;
    }
    if (i === classBody.length && depth === 0) end = i;
    let chunkText = classBody.substring(start, end).trim();
    if (chunkText) chunks.push(chunkText);
}

const repos = {
    AuthRepository: [], UserRepository: [], AdminRepository: [], AnalyticsRepository: [],
    RemoteGameRepository: [], EconomyRepository: [], LiveOpsRepository: [], SupportRepository: [],
    TournamentRepository: [], RemoteConfigRepository: []
};

const mappings = [
    { keywords: ['currentUser', 'userSession', 'errorMessage', 'isConfigured', 'refreshUserProfile', 'login', 'signUp', 'logout', 'deleteUser', 'signInWithOAuth', 'resetPassword', 'updateUserVerification', 'setShadowBan', 'updateInternalNotes', 'resetUsername', 'resetAvatar', 'forceLogout', 'processUserAndPromoteIfAdmin', 'hasRole', 'hasPermission', 'checkRolePermissions', 'loadInitialData'], repo: 'AuthRepository' },
    { keywords: ['adminInvitations', 'adminActivities', 'auditLogs', 'adminSessions', 'promoteUserToRole', 'demoteAdmin', 'terminateAdminSession', 'fetchAdminSessions', 'revokeAdminInvitation', 'inviteAdmin', 'updateAdminRole', 'saveAdminActivity', 'logAudit', 'fetchAdmins', 'fetchAdminActivities', 'fetchAdminInvitations'], repo: 'AdminRepository' },
    { keywords: ['users', 'fetchUsers', 'fetchLoginHistory'], repo: 'UserRepository' },
    { keywords: ['biMetrics', 'crashLogs', 'fraudAlerts', 'financeReports', 'queueMetrics', 'deviceRecords', 'healthMetrics', 'biDailyMetrics', 'biNotifications', 'biAppLogs', 'biHealthMetrics', 'logBIEvent', 'fetchBIMetrics', 'fetchCrashLogs', 'fetchFraudAlerts', 'fetchFinanceReports', 'fetchQueueMetrics', 'fetchDeviceRecords', 'fetchHealthMetrics', 'fetchBIDailyMetrics', 'fetchBINotifications', 'fetchBIAppLogs', 'fetchBIHealthMetrics', 'scheduleNotification', 'askAiAssistant', 'isSyncing', 'incrementAdImpressions'], repo: 'AnalyticsRepository' },
    { keywords: ['economyTransactions', 'fetchEconomyTransactions', 'adjustUserEconomy', 'adjustUserStats', 'storeItems', 'coupons', 'fetchStoreItems', 'fetchCoupons', 'dailyRewards', 'spinWheelRewards', 'fetchDailyRewards', 'fetchSpinWheelRewards'], repo: 'EconomyRepository' },
    { keywords: ['liveOpsEvents', 'seasonPasses', 'seasonPassTiers', 'gameEvents', 'fetchLiveOpsEvents', 'fetchSeasonPasses', 'fetchSeasonPassTiers', 'fetchGameEvents', 'createGameEvent', 'toggleGameEvent'], repo: 'LiveOpsRepository' },
    { keywords: ['tickets', 'feedbacks', 'fetchTickets', 'fetchFeedbackV2', 'submitFeedback'], repo: 'SupportRepository' },
    { keywords: ['tournaments', 'tournamentParticipants', 'fetchTournaments', 'createTournament', 'joinTournament'], repo: 'TournamentRepository' },
    { keywords: ['systemSettings', 'appVersions', 'maintenanceSchedules', 'cmsContent', 'adConfig', 'fetchSystemSettings', 'fetchAppVersions', 'fetchMaintenanceSchedules', 'fetchCMSContent', 'saveCMSContent', 'updateRemoteConfig'], repo: 'RemoteConfigRepository' },
    { keywords: ['matches', 'antiCheatLogs', 'fetchAntiCheatLogs', 'logAntiCheatViolation', 'updateMatchStatus', 'fetchMatchDetails', 'findWaitingMatch', 'hostWaitingMatch', 'joinWaitingMatch', 'updateMatchState', 'updateMatchWinner'], repo: 'RemoteGameRepository' }
];

let networkChunks = [];

for (let chunk of chunks) {
    if (chunk.startsWith('private val moshi') || chunk.startsWith('private val client') || chunk.includes('GeminiApiService') || chunk.includes('geminiService') || chunk.startsWith('private fun <T> fetchList') || chunk.startsWith('private val _') && chunk.includes('adapter') || chunk.includes('private val userListAdapter') || chunk.includes('adapter<List<') || chunk.startsWith('private val tag') || chunk.startsWith('private val scope') || chunk.startsWith('private val prefs') || chunk.startsWith('private var sessionToken') || chunk.startsWith('private val supabaseUrl') || chunk.startsWith('private val supabaseKey') || chunk.startsWith('private fun getHeaders')) {
        // Remove 'private ' so they can be accessed from repositories
        let pub = chunk.replace(/^private /, '');
        networkChunks.push(pub);
        continue;
    }

    let assigned = false;
    for (let map of mappings) {
        for (let kw of map.keywords) {
            if (new RegExp(`\\b${kw}\\b`).test(chunk)) {
                repos[map.repo].push(chunk);
                assigned = true;
                break;
            }
        }
        if (assigned) break;
    }
    if (!assigned) {
        let pub = chunk.replace(/^private /, '');
        networkChunks.push(pub);
    }
}

let networkFileContent = beforeClass + '\n' +
'class SupabaseNetworkClient(val context: Context) {\n' +
'    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)\n' +
    networkChunks.map(c => c.replace(/private val scope = .*/g, '')).join('\n\n') +
'\n}\n';

fs.writeFileSync('app/src/main/java/com/example/daadi/data/supabase/SupabaseNetworkClient.kt', networkFileContent);

let reposFileContent = 'package com.example.daadi.data.repository.supabase\n\n' +
'import com.example.daadi.data.supabase.*\n' +
'import kotlinx.coroutines.flow.*\n' +
'import kotlinx.coroutines.launch\n' +
'import okhttp3.*\n' +
'import okhttp3.MediaType.Companion.toMediaType\n' +
'import okhttp3.RequestBody.Companion.toRequestBody\n' +
'import java.io.IOException\n\n';

for (let repo in repos) {
    reposFileContent += `class ${repo}(val network: SupabaseNetworkClient) {\n`;
    let modifiedChunks = repos[repo].map(c => {
        let text = c;
        text = text.replace(/\bscope\.launch/g, 'network.scope.launch');
        text = text.replace(/\bclient\.newCall/g, 'network.client.newCall');
        text = text.replace(/\bmoshi\.adapter/g, 'network.moshi.adapter');
        text = text.replace(/\bgetHeaders\(\)/g, 'network.getHeaders()');
        text = text.replace(/\bsupabaseUrl\b/g, 'network.supabaseUrl');
        text = text.replace(/\bfetchList\(/g, 'network.fetchList(');
        text = text.replace(/\bgeminiService\b/g, 'network.geminiService');
        text = text.replace(/\bsessionToken\b/g, 'network.sessionToken');
        text = text.replace(/\btag\b/g, 'network.tag');
        text = text.replace(/\bprefs\b/g, 'network.prefs');
        text = text.replace(/\b([a-zA-Z0-9]+Adapter)\b/g, 'network.$1');
        return text;
    });
    reposFileContent += modifiedChunks.join('\n\n');
    reposFileContent += '\n}\n\n';
}

fs.mkdirSync('app/src/main/java/com/example/daadi/data/repository/supabase', { recursive: true });
fs.writeFileSync('app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt', reposFileContent);

console.log('Generated network client and repositories!');
