const fs = require('fs');

const inputFile = 'app/src/main/java/com/example/daadi/data/supabase/SupabaseManager.kt';
const content = fs.readFileSync(inputFile, 'utf-8');

const classStart = content.indexOf('class SupabaseManager(private val context: Context) {');
if (classStart === -1) {
    console.error("Could not find class SupabaseManager definition in the file!");
    process.exit(1);
}
const beforeClass = content.substring(0, classStart);
const classBody = content.substring(classStart + 'class SupabaseManager(private val context: Context) {'.length, content.lastIndexOf('}'));

const chunks = [];
let i = 0;
while (i < classBody.length) {
    if (/\s/.test(classBody[i])) { i++; continue; }
    
    let start = i;
    let depth = 0;
    let pDepth = 0;
    let end = i;

    let inString = false;
    let inTripleQuote = false;
    let inLineComment = false;
    let inBlockComment = false;

    while (i < classBody.length) {
        let char = classBody[i];
        let nextChar = classBody[i + 1] || '';
        let prevChar = classBody[i - 1] || '';

        // Handle comments and strings
        if (inLineComment) {
            if (char === '\n') inLineComment = false;
        } else if (inBlockComment) {
            if (char === '*' && nextChar === '/') {
                inBlockComment = false;
                i++; // skip '/'
            }
        } else if (inTripleQuote) {
            if (char === '"' && nextChar === '"' && classBody[i + 2] === '"') {
                inTripleQuote = false;
                i += 2; // skip other quotes
            }
        } else if (inString) {
            if (char === '\\') {
                i++; // skip escaped char
            } else if (char === '"') {
                inString = false;
            }
        } else {
            // Not in any comment or string
            if (char === '/' && nextChar === '/') {
                inLineComment = true;
                i++;
            } else if (char === '/' && nextChar === '*') {
                inBlockComment = true;
                i++;
            } else if (char === '"' && nextChar === '"' && classBody[i + 2] === '"') {
                inTripleQuote = true;
                i += 2;
            } else if (char === '"') {
                inString = true;
            } else if (char === '{') {
                depth++;
            } else if (char === '}') {
                depth--;
                if (depth === 0 && pDepth === 0) {
                    i++;
                    end = i;
                    break;
                }
            } else if (char === '(') {
                pDepth++;
            } else if (char === ')') {
                pDepth--;
            } else if (depth === 0 && pDepth === 0 && char === '\n') {
                // Empty line or keyword boundary
                let j = i + 1;
                while (j < classBody.length && /[ \t]/.test(classBody[j])) j++;
                if (j < classBody.length && classBody[j] === '\n') {
                    i = j;
                    end = i;
                    break;
                }
                const nextWord = classBody.substring(j, j + 20);
                if (/^(private |val |var |fun |suspend |init |data |class |enum )/.test(nextWord)) {
                    end = i;
                    break;
                }
            }
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
    {
        repo: 'AuthRepository',
        keywords: [
            'currentUser', 'userSession', 'errorMessage', 'isConfigured', 'passwordResetRequired',
            'clearPasswordResetRequired', 'updatePassword', 'refreshUserProfile', 'login', 'signUp',
            'logout', 'deleteUser', 'signInWithOAuth', 'resetPassword', 'updateUserVerification',
            'setShadowBan', 'updateInternalNotes', 'resetUsername', 'resetAvatar', 'forceLogout',
            'processUserAndPromoteIfAdmin', 'hasRole', 'hasPermission', 'checkRolePermissions'
        ]
    },
    {
        repo: 'AdminRepository',
        keywords: [
            'adminInvitations', 'adminActivities', 'auditLogs', 'adminSessions', 'promoteUserToRole',
            'demoteAdmin', 'terminateAdminSession', 'fetchAdminSessions', 'revokeAdminInvitation',
            'inviteAdmin', 'updateAdminRole', 'saveAdminActivity', 'logAudit', 'fetchAdmins',
            'fetchAdminActivities', 'fetchAdminInvitations'
        ]
    },
    {
        repo: 'UserRepository',
        keywords: [
            'users', 'fetchUsers', 'fetchLoginHistory', 'userLoginHistory', 'toggleUserBan'
        ]
    },
    {
        repo: 'AnalyticsRepository',
        keywords: [
            'biMetrics', 'crashLogs', 'fraudAlerts', 'financeReports', 'queueMetrics', 'deviceRecords',
            'healthMetrics', 'biDailyMetrics', 'biNotifications', 'biAppLogs', 'biHealthMetrics',
            'logBIEvent', 'fetchBIMetrics', 'fetchCrashLogs', 'fetchFraudAlerts', 'fetchFinanceReports',
            'fetchQueueMetrics', 'fetchDeviceRecords', 'fetchHealthMetrics', 'fetchBIDailyMetrics',
            'fetchBINotifications', 'fetchBIAppLogs', 'fetchBIHealthMetrics', 'scheduleNotification',
            'askAiAssistant', 'isSyncing', 'adTelemetry', 'resetAdTelemetryRemote', 'incrementAdRequests',
            'incrementAdImpressions'
        ]
    },
    {
        repo: 'EconomyRepository',
        keywords: [
            'economyTransactions', 'adjustUserEconomy', 'adjustUserStats',
            'storeItems', 'coupons', 'fetchStoreItems', 'fetchCoupons', 'dailyRewards', 'spinWheelRewards',
            'fetchDailyRewards', 'fetchSpinWheelRewards', 'createStoreItem', 'deleteStoreItem',
            'createCoupon', 'deleteCoupon', 'saveDailyReward', 'saveSpinWheelReward'
        ]
    },
    {
        repo: 'LiveOpsRepository',
        keywords: [
            'liveOpsEvents', 'seasonPasses', 'seasonPassTiers', 'gameEvents', 'fetchLiveOpsEvents',
            'fetchSeasonPasses', 'fetchSeasonPassTiers', 'fetchGameEvents', 'createGameEvent',
            'toggleGameEvent', 'deleteGameEvent', 'createLiveOpsEvent', 'deleteLiveOpsEvent',
            'toggleLiveOpsEventActive', 'createSeasonPass', 'deleteSeasonPass', 'toggleSeasonPassActive'
        ]
    },
    {
        repo: 'SupportRepository',
        keywords: [
            'tickets', 'feedbacks', 'fetchTickets', 'fetchFeedbackV2', 'submitFeedback', 'feedbackV2',
            'reportUserByName'
        ]
    },
    {
        repo: 'TournamentRepository',
        keywords: [
            'tournaments', 'tournamentParticipants', 'fetchTournaments', 'createTournament',
            'joinTournament', 'deleteTournament', 'updateTournamentStatus'
        ]
    },
    {
        repo: 'RemoteConfigRepository',
        keywords: [
            'systemSettings', 'appVersions', 'maintenanceSchedules', 'cmsContent', 'adConfig',
            'fetchSystemSettings', 'fetchAppVersions', 'fetchCMSContent',
            'saveCMSContent', 'updateRemoteConfig', 'maintenanceMode', 'multiplayerEnabled',
            'globalBroadcast', 'dispatchBroadcast', 'clearBroadcast', 'updateSystemSetting',
            'updateAdConfigurationRemote', 'announcements', 'createAnnouncement', 'toggleAnnouncementStatus',
            'deleteAnnouncement'
        ]
    },
    {
        repo: 'RemoteGameRepository',
        keywords: [
            'matches', 'antiCheatLogs', 'fetchAntiCheatLogs', 'logAntiCheatViolation', 'updateMatchStatus',
            'fetchMatchDetails', 'findWaitingMatch', 'hostWaitingMatch', 'joinWaitingMatch', 'updateMatchState',
            'updateMatchWinner', 'deleteMatch', 'updateMatchMoves', 'registerMatchResult'
        ]
    }
];

function getChunkName(chunk) {
    // Strip leading comments and whitespace
    let clean = chunk.replace(/^\s*(?:\/\/.*|\/\*[\s\S]*?\*\/)\s*/, '');
    let match = clean.match(/^(?:private\s+|internal\s+)?(?:suspend\s+)?(?:fun|val|var|data\s+class|enum\s+class|class|interface)\s+([a-zA-Z0-9_]+(?:\.[a-zA-Z0-9_]+)?)/);
    if (match) {
        let name = match[1];
        if (name.includes('.')) {
            name = name.split('.').pop();
        }
        return name;
    }
    return null;
}

let networkChunks = [];
let unmatched = [];

const networkKeywords = [
    'moshi', 'client', 'tag', 'scope', 'prefs', 'sessionToken', '_sessionToken',
    'supabaseUrl', 'supabaseKey', 'getHeaders', 'fetchList', 'userListAdapter',
    'geminiService', 'isConfigured', '_isConfigured', 'errorMessage', '_errorMessage',
    'loadInitialData', 'fetchEconomyTransactions', 'fetchStoreItems', 'fetchCoupons', 
    'fetchDailyRewards', 'fetchSpinWheelRewards', 'fetchLiveOpsEvents', 'fetchSeasonPasses', 
    'fetchCMSContent', 'fetchBIMetrics', 'fetchCrashLogs', 'fetchFraudAlerts', 
    'fetchFinanceReports', 'fetchQueueMetrics', 'fetchDeviceRecords', 'fetchHealthMetrics',
    'fetchAdConfiguration', 'fetchMaintenanceSchedules', 'fetchAppVersions', 'fetchSystemSettings'
];

for (let chunk of chunks) {
    let name = getChunkName(chunk);
    
    // Explicitly check for Network client dependencies first
    if (name && networkKeywords.includes(name)) {
        let pub = chunk.replace(/^private /, '');
        networkChunks.push(pub);
        continue;
    }

    // Force all backing fields starting with '_' to go to the network client
    if (name && name.startsWith('_')) {
        let pub = chunk.replace(/^private /, '');
        networkChunks.push(pub);
        continue;
    }

    if (chunk.startsWith('private val moshi') || chunk.startsWith('private val client') || 
        chunk.includes('GeminiApiService') || chunk.includes('geminiService') || 
        chunk.startsWith('private fun <T> fetchList') || (chunk.startsWith('private val _') && chunk.includes('adapter')) || 
        chunk.includes('private val userListAdapter') || chunk.includes('adapter<List<')) {
        let pub = chunk.replace(/^private /, '');
        networkChunks.push(pub);
        continue;
    }

    // Is it a data class, enum class, interface or a top-level model helper?
    if (chunk.startsWith('data class') || chunk.startsWith('enum class') || chunk.startsWith('interface') || chunk.startsWith('class')) {
        let pub = chunk.replace(/^private /, '');
        networkChunks.push(pub);
        continue;
    }

    let assigned = false;
    if (name) {
        let cleanName = name.startsWith('_') ? name.substring(1) : name;
        for (let map of mappings) {
            if (map.keywords.includes(cleanName)) {
                repos[map.repo].push(chunk);
                assigned = true;
                break;
            }
        }
    }

    if (!assigned) {
        if (name) {
            unmatched.push(name);
        }
        let pub = chunk.replace(/^private /, '');
        networkChunks.push(pub);
    }
}

if (unmatched.length > 0) {
    console.log("Unmatched chunks assigned to SupabaseNetworkClient:", unmatched);
}

// Separate helper/data/enum/interface classes so they are TOP-LEVEL elements inside SupabaseNetworkClient.kt
let topLevelChunks = [];
let networkClassChunks = [];

for (let chunk of networkChunks) {
    if (/^(data\s+class|enum\s+class|class|interface)\s+([a-zA-Z0-9_]+)/.test(chunk)) {
        topLevelChunks.push(chunk);
    } else {
        networkClassChunks.push(chunk);
    }
}

// Generate SupabaseNetworkClient.kt
let networkFileContent = beforeClass + '\n' +
'class SupabaseNetworkClient(val context: Context) {\n' +
networkClassChunks.map(c => {
    return '    ' + c.replace(/\n/g, '\n    ');
}).join('\n\n') +
'\n}\n\n' +
topLevelChunks.join('\n\n') + '\n';

fs.writeFileSync('app/src/main/java/com/example/daadi/data/supabase/SupabaseNetworkClient.kt', networkFileContent);

// Build the dynamic networkRefs list of things actually declared in SupabaseNetworkClient
const dynamicNetworkRefs = new Set();
for (let ref of networkKeywords) {
    dynamicNetworkRefs.add(ref);
}
for (let ref of unmatched) {
    dynamicNetworkRefs.add(ref);
}
for (let chunk of networkClassChunks) {
    let name = getChunkName(chunk);
    if (name) {
        dynamicNetworkRefs.add(name);
    }
}

const networkRefs = Array.from(dynamicNetworkRefs);

// Generate SupabaseRepositories.kt
let reposFileContent = 'package com.example.daadi.data.repository.supabase\n\n' +
'import com.example.daadi.data.supabase.*\n' +
'import kotlinx.coroutines.flow.*\n' +
'import kotlinx.coroutines.launch\n' +
'import okhttp3.*\n' +
'import okhttp3.MediaType.Companion.toMediaType\n' +
'import okhttp3.RequestBody.Companion.toRequestBody\n' +
'import java.io.IOException\n' +
'import java.text.SimpleDateFormat\n' +
'import java.util.Locale\n' +
'import java.util.Date\n' +
'import java.util.UUID\n' +
'import kotlinx.coroutines.delay\n' +
'import android.util.Log\n\n';

for (let repo in repos) {
    reposFileContent += `class ${repo}(val network: SupabaseNetworkClient) {\n`;
    
    // Add delegate fields for AuthRepository so they are cleanly exposed
    if (repo === 'AuthRepository') {
        reposFileContent += `    val isConfigured: Boolean get() = network.isConfigured\n`;
        reposFileContent += `    val errorMessage: StateFlow<String?> get() = network.errorMessage\n\n`;
    }

    let modifiedChunks = repos[repo].map(c => {
        let text = c;
        
        // Use negative lookbehind so we do NOT replace keywords when they are being declared with fun, val, var, class, or in variable parameters/declarations, or when accessed via standard dot syntax (e.g. user.permissions)
        for (let ref of networkRefs) {
            let regex = new RegExp(`(?<!\\b(?:fun|val|var|class)\\s+)(?<!\\.\\s*|::\\s*)\\b${ref}\\b`, 'g');
            text = text.replace(regex, `network.${ref}`);
        }

        // Correct Kotlin string interpolation for properties
        text = text.replace(/\$network\.(_?[a-zA-Z0-9_]+)/g, '${network.$1}');
        
        return '    ' + text.replace(/\n/g, '\n    ');
    });
    reposFileContent += modifiedChunks.join('\n\n');
    reposFileContent += '\n}\n\n';
}

fs.mkdirSync('app/src/main/java/com/example/daadi/data/repository/supabase', { recursive: true });
fs.writeFileSync('app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt', reposFileContent);

console.log('Generated network client and repositories dynamically and cleanly!');
