const fs = require('fs');
const path = 'app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt';
let content = fs.readFileSync(path, 'utf-8');

// 1. Fix parameter lists
content = content.replace(/(\(|\,)\s*network\.([a-zA-Z0-9_]+)\s*:/g, '$1 $2:');

// 2. Fix lambda parameters
content = content.replace(/\{\s*network\.([a-zA-Z0-9_]+)\s*->/g, '{ $1 ->');

// 3. Fix .network.method -> .method
content = content.replace(/\.network\.([a-zA-Z0-9_]+)\(/g, '.$1(');

// 4. Fix double network.network
content = content.replace(/network\.network/g, 'network');

// 5. Fix nested property access
content = content.replace(/([a-zA-Z0-9_]+)\.network\.([a-zA-Z0-9_]+)/g, '$1.$2');

// 6. Fix "network.prop =" -> "prop =" (in constructors and maps)
content = content.replace(/([\(,])\s*network\.([a-zA-Z0-9_]+)\s*=/g, '$1 $2 =');
content = content.replace(/\"network\.([a-zA-Z0-9_]+)\"\s*to/g, '"$1" to');

// 7. Fix common local variables that were incorrectly prefixed
const localVars = ['authId', 'id', 'email', 'username', 'role', 'permissions', 'createdAt', 'it', 'body', 'request', 'response', 'json', 'userId', 'onResult', 'token', 'authMap', 'accessToken', 'finalUser', 'reqBody', 'update', 'matched', 'user', 'trimmedEmail', 'trimmedUsername', 'trimmedPass', 'signUpUrl', 'bodyMap', 'errorMsg', 'finalNewUser', 'dateStr', 'userObj', 'userAdapter', 'isVerified', 'enabled', 'notes', 'newName', 'amount', 'currency', 'type', 'source', 'reason', 'uBody', 'list', 'matchedUser', 'newUser', 'newUserJson', 'createReq', 'exists', 'action', 'payload', 'u'];
for (const v of localVars) {
    const regex = new RegExp(`(?<!\\.)\\bnetwork\\.${v}\\b`, 'g');
    content = content.replace(regex, v);
}

// 8. Ensure core manager properties DO have the prefix
const managerProps = ['scope', 'moshi', 'client', 'tag', 'supabaseUrl', 'prefs', 'isConfigured', 'errorMessage', 'sessionToken', 'getHeaders', 'runOnMain', 'getFeedbackHeaders', 'fetchRemoteUsers', 'fetchRemoteMatches', 'fetchCurrentPermissions', 'saveSimulatorUsers', 'saveSimulatorMatches', 'updateRemoteUserRankStats', 'processUserAndPromoteIfAdmin', 'isSyncing', 'currentUser', 'passwordResetRequired', 'currentUserPermissions', 'currentUserRoles'];
const flows = ['_currentUser', '_currentUserRoles', '_currentUserPermissions', '_passwordResetRequired', '_cmsContent', '_announcements', '_systemSettings', '_adConfig', '_maintenanceMode', '_multiplayerEnabled', '_globalBroadcast', '_appVersions', '_maintenanceSchedules', '_bans', '_reports', '_auditLogs', '_feedback', '_feedbackV2', '_tickets', '_userLoginHistory', '_tournaments', '_gameEvents', '_roles', '_permissions', '_rolePermissions', '_antiCheatLogs', '_currentAdminRole', '_profiles', '_healthMetrics', '_biDailyMetrics', '_biNotifications', '_biAppLogs', '_biHealthMetrics', '_adminAuditLogs', '_adminInvitations', '_adminActivities', '_adminSessions', '_economyTransactions', '_storeItems', '_coupons', '_dailyRewards', '_spinWheelRewards', '_liveOpsEvents', '_seasonPasses'];
const adapters = ['userListAdapter', 'userAdapter', 'matchListAdapter', 'announcementListAdapter', 'settingListAdapter', 'feedbackListAdapter', 'feedbackV2ListAdapter', 'ticketListAdapter', 'loginHistoryListAdapter', 'tournamentListAdapter', 'eventListAdapter', 'roleListAdapter', 'permissionListAdapter', 'rolePermissionListAdapter', 'antiCheatListAdapter', 'biDailyMetricListAdapter', 'biNotificationListAdapter', 'biAppLogListAdapter', 'biHealthMetricListAdapter', 'adminAuditLogListAdapter', 'banListAdapter', 'reportListAdapter', 'auditLogListAdapter'];

const allToPrefix = [...managerProps, ...flows, ...adapters];
for (const p of allToPrefix) {
    const regex = new RegExp(`(?<!network\\.|val |var |fun |class |data class |\"|\\.|\\?|\\$)\\b${p}\\b`, 'g');
    content = content.replace(regex, 'network.' + p);
}

// 9. Final cleanup of strings
content = content.replace(/\"network\.([a-zA-Z0-9_]+)\"/g, '"$1"');

// 10. Switch to SupabaseManager
content = content.replace(/SupabaseNetworkClient/g, 'SupabaseManager');

// 11. Fix specific unresolved references
content = content.replace(/fetchUsers\(\)/g, 'network.fetchRemoteUsers()');

fs.writeFileSync(path, content);
console.log('Fixed SupabaseRepositories.kt v4');
