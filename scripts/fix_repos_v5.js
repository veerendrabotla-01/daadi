const fs = require('fs');
const path = 'app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt';
let content = fs.readFileSync(path, 'utf-8');

// 1. Fix common local variables and parameters that were incorrectly prefixed
const localVars = ['permission', 'violationType', 'severity', 'details', 'action', 'payload', 'id', 'authId', 'email', 'username', 'role', 'permissions', 'createdAt', 'it', 'body', 'request', 'response', 'json', 'userId', 'onResult', 'token', 'authMap', 'accessToken', 'finalUser', 'reqBody', 'update', 'matched', 'user', 'trimmedEmail', 'trimmedUsername', 'trimmedPass', 'signUpUrl', 'bodyMap', 'errorMsg', 'finalNewUser', 'dateStr', 'userObj', 'userAdapter', 'isVerified', 'enabled', 'notes', 'newName', 'amount', 'currency', 'type', 'source', 'reason', 'uBody', 'list', 'matchedUser', 'newUser', 'newUserJson', 'createReq', 'exists', 'u', 'reportPayload', 'reportJson', 'reportReqBody', 'reportRequest', 'reportId', 'reporterId', 'newReport', 'exists', 'isWin', 'isLoss'];

for (const v of localVars) {
    const regex = new RegExp(`(?<!\\.)\\bnetwork\\.${v}\\b`, 'g');
    content = content.replace(regex, v);
}

// 2. Fix specific unresolved references found in logs
content = content.replace(/network\.fetchUsers\(\)/g, 'network.fetchRemoteUsers()');
content = content.replace(/network\.not\s*\(/g, '!(');
content = content.replace(/network\.exists/g, 'exists');
content = content.replace(/network\.matchedUser/g, 'matchedUser');
content = content.replace(/network\.list/g, 'list');
content = content.replace(/network\.newUser/g, 'newUser');
content = content.replace(/network\.createReq/g, 'createReq');
content = content.replace(/network\.newUserJson/g, 'newUserJson');
content = content.replace(/network\.payload/g, 'payload');
content = content.replace(/network\.action/g, 'action');
content = content.replace(/network\.it/g, 'it');
content = content.replace(/network\.u/g, 'u');

// 3. Fix Flow collect issues
content = content.replace(/network\._profiles\.asStateFlow\(\)\.list/g, 'network._profiles.value');
content = content.replace(/network\._users\.asStateFlow\(\)\.any/g, 'network._users.value.any');

fs.writeFileSync(path, content);
console.log('Fixed SupabaseRepositories.kt v5');
