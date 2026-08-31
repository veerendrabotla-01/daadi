const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/data/supabase/SupabaseManager.kt';
let content = fs.readFileSync(path, 'utf8');

content = content.replace(
    /val userMetadata = authMap\?\.get\("user_metadata"\) as\? Map<\*, \*>/,
    'val userMetadata = (authMap?.get("user_metadata") as? Map<*, *>) ?: (authMap?.get("raw_user_meta_data") as? Map<*, *>)'
);

fs.writeFileSync(path, content, 'utf8');
console.log('Fixed user metadata key');
