const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/data/supabase/SupabaseManager.kt';
let content = fs.readFileSync(path, 'utf8');

content = content.replace(
    /val bodyMap = mapOf\(\s*"email" to trimmedEmail,\s*"password" to trimmedPass,\s*"data" to mapOf\("username" to trimmedUsername\)\s*\)/,
    'val bodyMap = mapOf(\n                    "email" to trimmedEmail,\n                    "password" to trimmedPass,\n                    "options" to mapOf("data" to mapOf("username" to trimmedUsername)),\n                    "data" to mapOf("username" to trimmedUsername)\n                )'
);

fs.writeFileSync(path, content, 'utf8');
console.log('Fixed signup payload');
