const fs = require('fs');

let path = 'app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt';
let content = fs.readFileSync(path, 'utf-8');

// The properties in SupabaseNetworkClient.kt that were just `val _xxx`
// Let's just find all `_` variables and replace them with `network._`
content = content.replace(/\b_([a-zA-Z0-9]+)\b/g, 'network._$1');

// There are other variables like 'sessionToken', 'tag', 'prefs' which might need `network.` 
// I already replaced some in `generate_repos.js` but let's do a wider replace
// We need to replace all properties that are in NetworkClient.
let networkClient = fs.readFileSync('app/src/main/java/com/example/daadi/data/supabase/SupabaseNetworkClient.kt', 'utf-8');
let props = [];
let regex = /val ([a-zA-Z0-9_]+)\s*(:|=)/g;
let match;
while(match = regex.exec(networkClient)) {
    props.push(match[1]);
}
let varRegex = /var ([a-zA-Z0-9_]+)\s*(:|=)/g;
while(match = varRegex.exec(networkClient)) {
    props.push(match[1]);
}

for (let prop of props) {
    if (prop === 'network' || prop === 'context' || prop === 'scope') continue;
    let r = new RegExp(`\\b${prop}\\b(?!\s*:)`, 'g');
    // But we should only replace if it's not already preceded by `network.`
    content = content.replace(r, (m, offset, str) => {
        if (offset > 8 && str.substring(offset - 8, offset) === 'network.') return m;
        // Don't replace if it's a parameter (e.g. `val prop`)
        if (str.substring(offset - 4, offset) === 'val ') return m;
        return 'network.' + prop;
    });
}

// We also need to fix `fetchRemoteUsers()` etc which were inside SupabaseNetworkClient.kt but I might have moved them to repos!
// If `fetchRemoteUsers` is in AuthRepository, we should NOT call `network.fetchRemoteUsers()`. We should call `fetchRemoteUsers()`.
content = content.replace(/network\.fetchRemoteUsers/g, 'fetchRemoteUsers');
content = content.replace(/network\.runOnMain/g, 'runOnMain');
content = content.replace(/network\.Log/g, 'Log');
content = content.replace(/network\.SimpleDateFormat/g, 'SimpleDateFormat');
content = content.replace(/network\.Locale/g, 'Locale');
content = content.replace(/network\.Date/g, 'Date');
content = content.replace(/network\.UUID/g, 'UUID');

fs.writeFileSync(path, content);
console.log('Fixed properties');
