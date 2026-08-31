const fs = require('fs');
const path = 'app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt';
let content = fs.readFileSync(path, 'utf-8');

// 1. Re-add network. prefix to properties that need it
const networkProps = ['userAdapter', 'userListAdapter', 'matchListAdapter', 'announcementsAdapter', 'settingsAdapter', 'feedbackListAdapter', 'feedbackV2ListAdapter', 'ticketListAdapter', 'loginHistoryListAdapter', 'banListAdapter', 'reportListAdapter', 'invitationListAdapter', 'activityListAdapter', 'auditListAdapter', 'sessionListAdapter', 'economyTransactionListAdapter', 'storeItemListAdapter', 'couponListAdapter', 'dailyRewardListAdapter', 'spinWheelRewardListAdapter', 'hasPermission', 'logAdminAction'];

for (const p of networkProps) {
    const regex = new RegExp(`(?<!network\\.)\\b${p}\\b`, 'g');
    content = content.replace(regex, `network.${p}`);
}

// 2. Fix Double-prefixing if any
content = content.replace(/network\.network\./g, 'network.');

// 3. Fix specific cases
content = content.replace(/network\.hasPermission\s*\(/g, 'network.hasPermission(');
content = content.replace(/network\.logAdminAction\s*\(/g, 'network.logAdminAction(');

fs.writeFileSync(path, content);
console.log('Fixed SupabaseRepositories.kt v6');
