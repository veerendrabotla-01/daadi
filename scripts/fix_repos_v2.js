const fs = require('fs');
const path = 'app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt';
let content = fs.readFileSync(path, 'utf-8');

console.log('Original content length:', content.length);

// 1. Fix parameter lists: network.param: Type -> param: Type
content = content.replace(/(\(|\,)\s*network\.([a-zA-Z0-9_]+)\s*:/g, '$1 $2:');

// 2. Fix lambda parameters: { network.it -> -> { it ->
content = content.replace(/\{\s*network\.([a-zA-Z0-9_]+)\s*->/g, '{ $1 ->');

// 3. Fix method calls on network that were incorrectly prefixed: .network.url -> .url
// Adding more methods that are commonly used in chains
const methods = ['url', 'headers', 'post', 'patch', 'put', 'delete', 'get', 'build', 'newCall', 'enqueue', 'execute', 'use', 'string', 'close', 'edit', 'putString', 'apply', 'launch', 'withContext', 'toJson', 'fromJson', 'toRequestBody', 'toMediaType', 'format', 'getAndSet', 'setTagForUnderAgeOfConsent', 'find', 'maxOfOrNull', 'asStateFlow', 'update', 'contains', 'lowercase', 'trim', 'enqueue', 'onFailure', 'onResponse', 'onResult', 'runOnMain'];
const methodRegex = new RegExp(`\\.network\\.(${methods.join('|')})\\(`, 'g');
content = content.replace(methodRegex, '.$1(');

// 4. Fix properties on local variables that were incorrectly prefixed
// e.g. network.response.network.body -> response.body
// This is more general: identifier.network.identifier -> identifier.identifier
content = content.replace(/([a-zA-Z0-9_]+)\.network\.([a-zA-Z0-9_]+)/g, '$1.$2');

// 5. Fix standalone network. prefixes on things that are likely local or parameters
const localVars = ['body', 'request', 'response', 'json', 'it', 'userId', 'email', 'username', 'role', 'permissions', 'onResult', 'token', 'authMap', 'accessToken', 'finalUser', 'reqBody', 'update', 'matched', 'user', 'trimmedEmail', 'trimmedUsername', 'trimmedPass', 'signUpUrl', 'bodyMap', 'errorMsg', 'finalNewUser', 'dateStr', 'userObj', 'userAdapter', 'isVerified', 'enabled', 'notes', 'newName', 'amount', 'currency', 'type', 'source', 'reason'];
// Note: removed 'isConfigured', 'errorMessage', 'scope', 'moshi', 'client', 'tag', 'supabaseUrl', 'prefs' from localVars to keep network. prefix

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

// 8. Fix some specific broken patterns found in previous views
content = content.replace(/network\.currentUser/g, 'network.currentUser'); // This might be redundant but safe
content = content.replace(/network\.network\._/g, 'network._');

fs.writeFileSync(path, content);
console.log('Fixed SupabaseRepositories.kt');
