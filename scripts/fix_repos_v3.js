const fs = require('fs');
const path = 'app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt';
let content = fs.readFileSync(path, 'utf-8');

console.log('Original content length:', content.length);

// 1. Fix parameter lists: network.param: Type -> param: Type
content = content.replace(/(\(|\,)\s*network\.([a-zA-Z0-9_]+)\s*:/g, '$1 $2:');

// 2. Fix lambda parameters: { network.it -> -> { it ->
content = content.replace(/\{\s*network\.([a-zA-Z0-9_]+)\s*->/g, '{ $1 ->');

// 3. Fix method calls on network that were incorrectly prefixed: .network.url -> .url
const methods = ['url', 'headers', 'post', 'patch', 'put', 'delete', 'get', 'build', 'newCall', 'enqueue', 'execute', 'use', 'string', 'close', 'edit', 'putString', 'apply', 'launch', 'withContext', 'toJson', 'fromJson', 'toRequestBody', 'toMediaType', 'format', 'getAndSet', 'setTagForUnderAgeOfConsent', 'find', 'maxOfOrNull', 'asStateFlow', 'update', 'contains', 'lowercase', 'trim', 'enqueue', 'onFailure', 'onResponse', 'onResult', 'runOnMain'];
const methodRegex = new RegExp(`\\.network\\.(${methods.join('|')})\\(`, 'g');
content = content.replace(methodRegex, '.$1(');

// 4. Fix properties on local variables that were incorrectly prefixed
content = content.replace(/([a-zA-Z0-9_]+)\.network\.([a-zA-Z0-9_]+)/g, '$1.$2');

// 5. Fix standalone network. prefixes on things that are likely local or parameters
const localVars = ['body', 'request', 'response', 'json', 'it', 'userId', 'email', 'username', 'role', 'permissions', 'onResult', 'token', 'authMap', 'accessToken', 'finalUser', 'reqBody', 'update', 'matched', 'user', 'trimmedEmail', 'trimmedUsername', 'trimmedPass', 'signUpUrl', 'bodyMap', 'errorMsg', 'finalNewUser', 'dateStr', 'userObj', 'userAdapter', 'isVerified', 'enabled', 'notes', 'newName', 'amount', 'currency', 'type', 'source', 'reason'];
for (const v of localVars) {
    const regex = new RegExp(`\\bnetwork\\.${v}\\b`, 'g');
    content = content.replace(regex, v);
}

// 6. Fix nested network.network
content = content.replace(/network\.network/g, 'network');

// 7. Fix strings
content = content.replace(/network\.id=eq\./g, 'id=eq.');
content = content.replace(/network\.username=eq\./g, 'username=eq.');
content = content.replace(/network\.email=eq\./g, 'email=eq.');

// 8. Restore prefixes for core properties of SupabaseManager
const coreProps = ['scope', 'moshi', 'client', 'tag', 'supabaseUrl', 'prefs', 'isConfigured', 'errorMessage', 'sessionToken', 'getHeaders', 'runOnMain', '_currentUser', '_currentUserRoles', '_currentUserPermissions', '_passwordResetRequired', '_cmsContent', '_announcements', '_systemSettings', '_adConfig', '_maintenanceMode', '_multiplayerEnabled', '_globalBroadcast', '_appVersions', '_maintenanceSchedules', '_bans', '_reports', '_auditLogsV2', '_feedback', '_feedbackV2', '_tickets', '_userLoginHistory', '_tournaments', '_gameEvents', '_roles', '_permissions', '_rolePermissions', '_antiCheatLogs', '_currentAdminRole', '_profiles', '_healthMetrics', '_biDailyMetrics', '_biNotifications', '_biAppLogs', '_biHealthMetrics', '_adminAuditLogs', '_adminInvitations', '_adminActivities', '_adminSessions', '_economyTransactions', '_storeItems', '_coupons', '_dailyRewards', '_spinWheelRewards', '_liveOpsEvents', '_seasonPasses'];

for (const p of coreProps) {
    const regex = new RegExp(`(?<!network\\.|val |var )\\b${p}\\b`, 'g');
    content = content.replace(regex, 'network.' + p);
}

// 9. Switch to SupabaseManager
content = content.replace(/SupabaseNetworkClient/g, 'SupabaseManager');

fs.writeFileSync(path, content);
console.log('Fixed SupabaseRepositories.kt and switched to SupabaseManager');
